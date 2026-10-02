package io.github.daisukikaffuchino.han1meviewer.logic.network

import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import java.security.SecureRandom

data class DohPreset(
    val key: String,
    val title: String,
    val url: String,
    val bootstrapIps: List<String>,
)

object DohConfig {
    private val echGatewayBootstrapIps: List<String> by lazy {
        val random = SecureRandom()
        (4..250).shuffled(random).take(4).map { "172.64.229.$it" }
    }

    val presets = listOf(
        DohPreset(
            key = "alidns",
            title = "AliDNS",
            url = "https://dns.alidns.com/dns-query",
            bootstrapIps = listOf("223.5.5.5", "223.6.6.6"),
        ),
        DohPreset(
            key = "dnspod",
            title = "DNSPod",
            url = "https://doh.pub/dns-query",
            bootstrapIps = listOf("1.12.12.12", "120.53.53.53"),
        ),
        DohPreset(
            key = "cloudflare",
            title = "Cloudflare",
            url = "https://cloudflare-dns.com/dns-query",
            bootstrapIps = listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"),
        ),
        DohPreset(
            key = "ech_gateway",
            title = "小雅DoH (ECH/H3)",
            url = "https://tgxjjdszvu.cloudflare-gateway.com/dns-query",
            bootstrapIps = echGatewayBootstrapIps,
        ),
    )

    fun selectedPreset(): DohPreset = presets.firstOrNull { it.key == SettingsRepository.dohPreset } ?: presets.first()

    fun customUrl(): String = SettingsRepository.dohCustomUrl.trim()

    fun bootstrapIps(): List<String> {
        val customBootstrapIps = SettingsRepository.dohBootstrapIps
            .split(',', '\n', ';', ' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (customBootstrapIps.isNotEmpty()) return customBootstrapIps
        if (SettingsRepository.dohPreset == "custom") return emptyList()
        return selectedPreset().bootstrapIps
    }

    fun timeoutSeconds(): Int = SettingsRepository.dohTimeoutSeconds.coerceIn(1, 60)

    fun resolveUrl(): String? {
        if (!SettingsRepository.useDoH) return null
        return when (SettingsRepository.dohPreset) {
            "custom" -> customUrl().takeIf { it.isNotBlank() }
            else -> selectedPreset().url
        }
    }
}
