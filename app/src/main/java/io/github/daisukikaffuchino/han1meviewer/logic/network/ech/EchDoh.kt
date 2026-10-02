package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import android.util.Base64
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.network.DohConfig
import io.github.daisukikaffuchino.utils.LogUtil
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resolves protected hosts through authoritative DNS-over-HTTPS and obtains a
 * live ECHConfigList for Cloudflare-backed hosts.
 */
object EchDoh {

    private const val TAG = "HY-ECH-DOH"
    private const val LIVE_SOURCE_HOST = "cloudflare-ech.com"
    private const val CLOUDFLARE_ASN = 13335
    private const val FAIL_COOLDOWN_MS = 30_000L
    private const val MIN_TTL_MS = 60_000L
    private const val MAX_TTL_MS = 60 * 60 * 1000L
    private const val ECH_CACHE_MIN_MS = 60 * 60 * 1000L
    private const val ECH_CACHE_MAX_MS = 5 * 60 * 60 * 1000L
    private const val DNS_CACHE_TTL_MS = 5 * 60 * 1000L
    private const val SINGLE_QUERY_TIMEOUT_MS = 2_500L

    private val pureDohIps = listOf(
        "223.5.5.5",
        "223.6.6.6",
        "1.12.12.12",
        "120.53.53.53",
        "101.198.193.29",
        "101.198.192.33",
    )

    private val resolverCache = ConcurrentHashMap<String, Dns>()
    private val dnsCache = ConcurrentHashMap<String, DnsEntry>()
    private val asnCache = ConcurrentHashMap<String, Boolean>()
    private val cfHostCache = ConcurrentHashMap<String, Boolean>()
    private val echCache = ConcurrentHashMap<String, EchEntry>()
    private val echFailed = ConcurrentHashMap<String, Long>()
    private val ownFirst = ConcurrentHashMap.newKeySet<String>()

    private data class DohEndpoint(val url: String, val pins: List<InetAddress>)
    private data class DnsEntry(val addresses: List<InetAddress>, val expireAt: Long)
    private data class EchEntry(val wire: ByteArray, val expireAt: Long)

    private fun configuredEndpoint(): DohEndpoint? {
        if (!SettingsRepository.useDoH) return null
        val preset = DohConfig.selectedPreset()
        val url = if (SettingsRepository.dohPreset == "custom") {
            DohConfig.customUrl().takeIf { it.isNotBlank() }
        } else {
            preset.url
        } ?: return null
        val pins = if (SettingsRepository.dohPreset == "custom") {
            DohConfig.bootstrapIps()
        } else {
            DohConfig.bootstrapIps().ifEmpty { preset.bootstrapIps }
        }.mapNotNull { runCatching { InetAddress.getByName(it) }.getOrNull() }
        return DohEndpoint(url, pins)
    }

    private fun endpoints(): List<DohEndpoint> {
        val configured = configuredEndpoint()
        val pure = pureDohIps.map { DohEndpoint("https://$it/dns-query", emptyList()) }
        return (listOfNotNull(configured) + pure).distinctBy { it.url }
    }

    private fun resolver(endpoint: DohEndpoint): Dns = resolverCache.getOrPut(endpoint.url) {
        val timeout = DohConfig.timeoutSeconds().toLong()
        val client = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .build()
        DnsOverHttps.Builder()
            .client(client)
            .url(endpoint.url.toHttpUrl())
            .includeIPv6(true)
            .post(false)
            .resolvePrivateAddresses(true)
            .resolvePublicAddresses(true)
            .apply {
                if (endpoint.pins.isNotEmpty()) {
                    bootstrapDnsHosts(*endpoint.pins.toTypedArray())
                }
            }
            .build()
    }

    fun resolve(host: String): List<InetAddress> {
        val now = System.currentTimeMillis()
        dnsCache[host]?.let { if (it.expireAt > now) return it.addresses }

        endpoints().forEach { endpoint ->
            val addresses = runCatching { resolver(endpoint).lookup(host) }
                .getOrElse {
                    LogUtil.w(
                        TAG,
                        "DoH resolution failed for $host via ${endpoint.url}: ${it.message}"
                    )
                    null
                }
            if (!addresses.isNullOrEmpty()) {
                dnsCache[host] = DnsEntry(addresses, now + DNS_CACHE_TTL_MS)
                LogUtil.d(
                    TAG,
                    "Resolved $host via ${endpoint.url}: ${addresses.joinToString { it.hostAddress ?: "?" }}"
                )
                return addresses
            }
        }
        return emptyList()
    }

    fun echConfigList(host: String): ByteArray? {
        if (!isCloudflareHost(host)) return null

        val now = System.currentTimeMillis()
        echCache[host]?.let { if (it.expireAt > now) return it.wire }
        val failedAt = echFailed[host]
        if (failedAt != null && now - failedAt < FAIL_COOLDOWN_MS) return null

        val first = if (host != LIVE_SOURCE_HOST && !ownFirst.contains(host)) {
            LIVE_SOURCE_HOST
        } else {
            host
        }
        val second = if (first == host) LIVE_SOURCE_HOST else host

        val hit = if (first == LIVE_SOURCE_HOST) {
            fetchLiveEch() ?: fetchEchJson(host)
        } else {
            fetchEchJson(first) ?: fetchLiveEch() ?: fetchEchJson(second)
        }

        if (hit == null) {
            LogUtil.w(TAG, "No ECHConfigList available for $host")
            echFailed[host] = now
            return null
        }

        echCache[host] = EchEntry(hit.first, now + hit.second)
        echFailed.remove(host)
        return hit.first
    }

    private fun fetchLiveEch(): Pair<ByteArray, Long>? {
        for (ip in pureDohIps.shuffled()) {
            val hit = runCatching { queryWire(ip, LIVE_SOURCE_HOST, TYPE_HTTPS) }.getOrNull()
            if (hit != null) {
                val parsed = parseSvcbEch(hit) ?: continue
                val ttl = parsed.second.coerceIn(ECH_CACHE_MIN_MS, ECH_CACHE_MAX_MS - 1) + 1
                LogUtil.i(TAG, "Fetched live ECHConfigList via $ip (${parsed.first.size} bytes)")
                return parsed.first to ttl
            }
        }
        return null
    }

    private fun fetchEchJson(host: String): Pair<ByteArray, Long>? {
        val endpoint = configuredEndpoint() ?: return null
        val body = runCatching { queryJson(endpoint, host, "HTTPS") }.getOrNull() ?: return null
        val encoded = Regex("""ech=([A-Za-z0-9+/=_-]+)""").find(body)?.groupValues?.get(1)
            ?: return null
        val wire = runCatching {
            Base64.decode(encoded, Base64.DEFAULT or Base64.URL_SAFE)
        }.getOrNull() ?: return null
        if (wire.isEmpty()) return null
        val ttlSeconds = Regex(""""TTL"\s*:\s*(\d+)""").findAll(body)
            .mapNotNull { it.groupValues[1].toLongOrNull() }
            .minOrNull() ?: 300L
        val ttl = (ttlSeconds * 1000).coerceIn(MIN_TTL_MS, MAX_TTL_MS)
        return wire to ttl
    }

    private fun isCloudflareHost(host: String): Boolean {
        cfHostCache[host]?.let { return it }
        val addresses = runCatching { resolve(host) }.getOrNull().orEmpty()
        if (addresses.isEmpty()) return true

        val ipv4 = addresses.filter { it.address.size == 4 }
        val cloudflare = if (ipv4.isEmpty()) true else ipv4.any(::isCloudflareIp)
        cfHostCache[host] = cloudflare
        return cloudflare
    }

    private fun isCloudflareIp(address: InetAddress): Boolean {
        if (address.address.size != 4) return false
        val ip = address.hostAddress ?: return true
        asnCache[ip]?.let { return it }

        val queryName = ip.split('.').reversed().joinToString(".") + ".origin.asn.cymru.com"
        val txt = runCatching { queryPureWire(queryName, TYPE_TXT) }.getOrNull()
        val asn =
            txt?.let { Regex(""""?(\d{2,6})\s*\|""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
        // Unknown ASN is treated as Cloudflare rather than risking a protected host.
        val cloudflare = asn == null || asn == CLOUDFLARE_ASN
        if (asn != null) asnCache[ip] = cloudflare
        return cloudflare
    }

    fun invalidateEch(host: String) {
        echCache.remove(host)
        echFailed.remove(host)
        echCache.remove(LIVE_SOURCE_HOST)
        if (host != LIVE_SOURCE_HOST) ownFirst.add(host)
    }

    fun invalidateAll() {
        resolverCache.clear()
        dnsCache.clear()
        asnCache.clear()
        cfHostCache.clear()
        echCache.clear()
        echFailed.clear()
        ownFirst.clear()
    }

    private fun queryPureWire(name: String, type: Int): String? {
        for (ip in pureDohIps.shuffled()) {
            val msg = runCatching { queryWire(ip, name, type) }.getOrNull() ?: continue
            if (type == TYPE_TXT) {
                parseFirstTxt(msg)?.let { return it }
            }
        }
        return null
    }

    private fun queryWire(ip: String, name: String, type: Int): ByteArray? {
        val encoded = Base64.encodeToString(
            buildQuery(name, type),
            Base64.NO_WRAP or Base64.URL_SAFE,
        ).trimEnd('=')
        val request = Request.Builder()
            .url("https://$ip/dns-query?dns=$encoded")
            .header("Accept", "application/dns-message")
            .build()
        return shortClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body.bytes()
        }
    }

    private fun queryJson(endpoint: DohEndpoint, name: String, type: String): String? {
        val base = endpoint.url.toHttpUrl()
        val url = base.newBuilder()
            .addQueryParameter("name", name)
            .addQueryParameter("type", type)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/dns-json")
            .build()
        return shortClient(endpoint).newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body.string()
        }
    }

    private fun shortClient(endpoint: DohEndpoint? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(SINGLE_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(SINGLE_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(SINGLE_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (endpoint != null && endpoint.pins.isNotEmpty()) {
            builder.dns { endpoint.pins }
        }
        return builder.build()
    }

    private fun buildQuery(name: String, type: Int): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(0x12, 0x34, 0x01, 0x00, 0, 1, 0, 0, 0, 0, 0, 0))
        name.split('.').forEach { label ->
            output.write(label.length)
            output.write(label.toByteArray(Charsets.US_ASCII))
        }
        output.write(0)
        output.write((type ushr 8) and 0xFF)
        output.write(type and 0xFF)
        output.write(0)
        output.write(1)
        return output.toByteArray()
    }

    private fun skipName(message: ByteArray, start: Int): Int {
        var index = start
        while (index < message.size) {
            val length = message[index].toInt() and 0xFF
            if (length == 0) return index + 1
            if ((length and 0xC0) == 0xC0) return (index + 2).coerceAtMost(message.size)
            index += length + 1
        }
        return message.size
    }

    internal fun parseSvcbEch(message: ByteArray): Pair<ByteArray, Long>? {
        if (message.size < 12) return null
        val answerCount = ((message[6].toInt() and 0xFF) shl 8) or (message[7].toInt() and 0xFF)
        var index = skipName(message, 12) + 4
        repeat(answerCount) {
            index = skipName(message, index)
            if (index + 10 > message.size) return null
            val type =
                ((message[index].toInt() and 0xFF) shl 8) or (message[index + 1].toInt() and 0xFF)
            val ttl = (((message[index + 4].toInt() and 0xFF).toLong() shl 24) or
                    ((message[index + 5].toInt() and 0xFF).toLong() shl 16) or
                    ((message[index + 6].toInt() and 0xFF).toLong() shl 8) or
                    (message[index + 7].toInt() and 0xFF).toLong())
            val dataLength = ((message[index + 8].toInt() and 0xFF) shl 8) or
                    (message[index + 9].toInt() and 0xFF)
            val dataStart = index + 10
            val dataEnd = dataStart + dataLength
            if (dataEnd > message.size) return null

            if (type == TYPE_HTTPS && dataLength > 4) {
                var cursor = skipName(message, dataStart + 2)
                while (cursor + 4 <= dataEnd) {
                    val key = ((message[cursor].toInt() and 0xFF) shl 8) or
                            (message[cursor + 1].toInt() and 0xFF)
                    val length = ((message[cursor + 2].toInt() and 0xFF) shl 8) or
                            (message[cursor + 3].toInt() and 0xFF)
                    val valueStart = cursor + 4
                    val valueEnd = valueStart + length
                    if (valueEnd > dataEnd) return null
                    if (key == SVCB_KEY_ECH && length > 0) {
                        return message.copyOfRange(valueStart, valueEnd) to (ttl * 1000)
                    }
                    cursor = valueEnd
                }
            }
            index = dataEnd
        }
        return null
    }

    private fun parseFirstTxt(message: ByteArray): String? {
        if (message.size < 12) return null
        val answerCount = ((message[6].toInt() and 0xFF) shl 8) or (message[7].toInt() and 0xFF)
        var index = skipName(message, 12) + 4
        repeat(answerCount) {
            index = skipName(message, index)
            if (index + 10 > message.size) return null
            val type = ((message[index].toInt() and 0xFF) shl 8) or
                    (message[index + 1].toInt() and 0xFF)
            val dataLength = ((message[index + 8].toInt() and 0xFF) shl 8) or
                    (message[index + 9].toInt() and 0xFF)
            val dataStart = index + 10
            val dataEnd = dataStart + dataLength
            if (dataEnd > message.size) return null
            if (type == TYPE_TXT) {
                val text = StringBuilder()
                var cursor = dataStart
                while (cursor < dataEnd) {
                    val length = message[cursor].toInt() and 0xFF
                    cursor++
                    if (cursor + length > dataEnd) return null
                    text.append(String(message, cursor, length, Charsets.UTF_8))
                    cursor += length
                }
                return text.toString()
            }
            index = dataEnd
        }
        return null
    }

    private const val TYPE_TXT = 16
    private const val TYPE_HTTPS = 65
    private const val SVCB_KEY_ECH = 5
}

/**
 * All hosts use the ECH resolver first. Non-core hosts may fall back to their
 * previous resolver; core hosts never do.
 */
class EchDns(private val fallback: Dns = Dns.SYSTEM) : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = runCatching { EchDoh.resolve(hostname) }.getOrNull()
        if (!addresses.isNullOrEmpty()) return addresses

        if (EchHosts.isCoreDomain(hostname)) {
            throw UnknownHostException("DoH resolution failed for protected host $hostname")
        }
        return fallback.lookup(hostname)
    }
}
