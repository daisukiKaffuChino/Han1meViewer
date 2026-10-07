package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import io.github.daisukikaffuchino.han1meviewer.USER_AGENT
import io.github.daisukikaffuchino.han1meviewer.logic.network.ServiceCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class EchDiagnosticReport(
    val host: String,
    val resolvedAddresses: List<String>,
    val echConfigBytes: Int,
    val httpStatus: Int,
    val protocol: String,
    val elapsedMillis: Long,
    val h3Result: String,
    val error: String? = null,
) {
    val successful: Boolean
        get() = error == null && resolvedAddresses.isNotEmpty() && echConfigBytes > 0 && httpStatus > 0
}

object EchDiagnostics {

    private const val TAG = "HY-ECH-TEST"

    suspend fun run(rawHost: String): EchDiagnosticReport = withContext(Dispatchers.IO) {
        val host = rawHost.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .lowercase()
        if (host.isBlank()) {
            EchLog.e(TAG, "Host is empty")
            return@withContext EchDiagnosticReport(
                host = "",
                resolvedAddresses = emptyList(),
                echConfigBytes = 0,
                httpStatus = 0,
                protocol = "",
                elapsedMillis = 0,
                h3Result = "Skipped",
                error = "Host is empty",
            )
        }

        EchLog.i(TAG, "Starting ECH diagnostics for $host")
        val addresses = runCatching { EchDoh.resolve(host) }
            .onFailure { EchLog.e(TAG, "DoH resolution failed: ${it.message}") }
            .getOrDefault(emptyList())
        val addressStrings = addresses.mapNotNull { it.hostAddress }.distinct()
        if (addressStrings.isEmpty()) {
            EchLog.e(TAG, "No authoritative addresses were resolved")
        } else {
            EchLog.i(TAG, "Resolved $host -> ${addressStrings.joinToString()}")
        }

        val echConfig = runCatching { EchDoh.echConfigList(host) }
            .onFailure { EchLog.e(TAG, "ECHConfigList query failed: ${it.message}") }
            .getOrNull()
        if (echConfig == null) {
            EchLog.w(TAG, "No ECHConfigList was returned")
        } else {
            EchLog.i(TAG, "Obtained ECHConfigList (${echConfig.size} bytes)")
        }

        var httpStatus = 0
        var protocol = ""
        var elapsed = 0L
        var error: String? = null
        try {
            val request = Request.Builder()
                .url("https://$host/")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            val startedAt = System.currentTimeMillis()
            ServiceCreator.hClient.newCall(request).execute().use { response ->
                elapsed = System.currentTimeMillis() - startedAt
                httpStatus = response.code
                protocol = response.protocol.toString()
                EchLog.i(
                    TAG,
                    "ECH transport response: HTTP $httpStatus, protocol=$protocol, ${elapsed}ms",
                )
            }
        } catch (throwable: Throwable) {
            error = throwable.message ?: throwable.javaClass.simpleName
            EchLog.e(TAG, "ECH transport request failed: $error")
        }

        val h3Result = if (!HyEchH3.isNativeLoaded()) {
            EchLog.w(TAG, "H3 native transport is unavailable")
            "Unavailable"
        } else if (echConfig == null) {
            EchLog.w(TAG, "H3 test skipped because ECHConfigList is unavailable")
            "Skipped: no ECH config"
        } else {
            runCatching {
                val startedAt = System.currentTimeMillis()
                val file = HyEchH3.fetchResourceToFile(
                    url = "https://$host/favicon.ico",
                    rememberResult = false,
                    respectTransportPolicy = false,
                )
                val elapsedMillis = System.currentTimeMillis() - startedAt
                if (file == null) {
                    EchLog.w(TAG, "H3 request failed; TCP+ECH fallback is available")
                    "Failed / fallback"
                } else {
                    val size = file.length()
                    file.delete()
                    EchLog.i(TAG, "H3 request succeeded ($size bytes, ${elapsedMillis}ms)")
                    "OK: $size bytes, ${elapsedMillis}ms"
                }
            }.getOrElse { throwable ->
                EchLog.w(TAG, "H3 test failed: ${throwable.message}")
                "Failed: ${throwable.message ?: throwable.javaClass.simpleName}"
            }
        }

        EchDiagnosticReport(
            host = host,
            resolvedAddresses = addressStrings,
            echConfigBytes = echConfig?.size ?: 0,
            httpStatus = httpStatus,
            protocol = protocol,
            elapsedMillis = elapsed,
            h3Result = h3Result,
            error = error,
        ).also { report ->
            EchLog.i(TAG, if (report.successful) "ECH diagnostics passed" else "ECH diagnostics incomplete")
        }
    }
}
