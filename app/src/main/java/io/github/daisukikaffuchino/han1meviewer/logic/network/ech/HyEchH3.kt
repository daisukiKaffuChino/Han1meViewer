package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import android.content.Context
import android.util.Base64
import io.github.daisukikaffuchino.han1meviewer.USER_AGENT
import okhttp3.HttpUrl
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * JNI entry point for the Rust quiche transport embedded inside libchino.so.
 */
object HyEchH3 {

    private const val TAG = "HY-ECH-H3"
    private const val LIB_NAME = "chino"
    private const val PREFS_NAME = "ech_h3_state"
    private const val FAIL_TTL_MS = 24 * 60 * 60 * 1000L

    private val staticExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "avif", "bmp", "ico", "svg",
        "css", "js", "mjs", "woff", "woff2", "ttf",
    )

    @Volatile
    private var loaded = false

    @Volatile
    private var appContext: Context? = null

    @Volatile
    var lastJson: String = ""
        private set

    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    fun isNativeLoaded(): Boolean = ensureLoaded()

    private fun ensureLoaded(): Boolean {
        if (loaded) return true
        return runCatching {
            System.loadLibrary(LIB_NAME)
            loaded = true
            true
        }.getOrElse {
            EchLog.w(TAG, "H3 native library is unavailable: ${it.message}")
            false
        }
    }

    fun isStaticAsset(url: HttpUrl): Boolean {
        val extension = url.pathSegments.lastOrNull()
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?: return false
        return extension in staticExtensions
    }

    fun mimeFor(url: HttpUrl): String = when (
        url.pathSegments.lastOrNull()?.substringAfterLast('.', "")?.lowercase()
    ) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "avif" -> "image/avif"
        "bmp" -> "image/bmp"
        "ico" -> "image/x-icon"
        "svg" -> "image/svg+xml"
        "css" -> "text/css"
        "js", "mjs" -> "application/javascript"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        else -> "application/octet-stream"
    }

    fun shouldTryH3(host: String): Boolean {
        val context = appContext ?: return false
        val retryAt = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong("bad:$host", 0L)
        return System.currentTimeMillis() >= retryAt
    }

    fun fetchResourceToFile(url: String, rememberResult: Boolean = true): File? {
        val context = appContext ?: return null
        if (!shouldTryH3(java.net.URI(url).host ?: return null)) return null

        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val ip = runCatching { EchDoh.resolve(host).firstOrNull()?.hostAddress }.getOrNull()
        if (ip.isNullOrBlank()) {
            if (rememberResult) rememberH3(context, host, success = false)
            return null
        }

        val ech = runCatching { EchDoh.echConfigList(host) }.getOrNull()
        if (EchHosts.isCoreDomain(host) && (ech == null || ech.isEmpty())) {
            EchLog.w(TAG, "Refusing plaintext QUIC for protected host=$host")
            return null
        }

        val path = buildString {
            append(uri.rawPath.ifBlank { "/" })
            uri.rawQuery?.let { append('?').append(it) }
        }
        val extension = uri.rawPath.substringAfterLast('.', "")
            .take(5)
            .filter(Char::isLetterOrDigit)
            .ifEmpty { "bin" }
        val output = File(context.cacheDir, "h3-${System.nanoTime()}.$extension")
        val saved = fetchToFile(context, host, ip, ech, path, output)
        if (saved == null) {
            if (rememberResult) rememberH3(context, host, success = false)
            output.delete()
            return null
        }

        if (rememberResult) rememberH3(context, host, success = true)
        return saved
    }

    private fun fetchToFile(
        context: Context,
        host: String,
        ip: String,
        ech: ByteArray?,
        path: String,
        output: File,
    ): File? {
        if (!ensureLoaded()) return null
        val echBase64 = ech?.takeIf { it.isNotEmpty() }
            ?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
            .orEmpty()
        val json = runCatching {
            h3Fetch(
                host,
                ip,
                echBase64,
                path,
                "",
                caBundlePath(context),
                output.absolutePath,
                USER_AGENT,
            )
        }.onFailure {
            EchLog.w(TAG, "H3 JNI call failed: ${it.message}")
        }.getOrNull() ?: return null

        lastJson = json
        val saved = runCatching { JSONObject(json).optString("saved_to", "") }.getOrDefault("")
        return output.takeIf {
            saved == it.absolutePath && it.exists() && it.length() > 0L
        }
    }

    private fun rememberH3(context: Context, host: String, success: Boolean) {
        val retryAt = if (success) 0L else System.currentTimeMillis() + FAIL_TTL_MS
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong("bad:$host", retryAt)
            .apply()
        EchLog.i(
            TAG,
            if (success) "H3 available for $host" else "H3 failed for $host; caching fallback"
        )
    }

    private fun caBundlePath(context: Context): String {
        val output = File(context.cacheDir, "han1me-system-ca.pem")
        if (output.exists() && output.length() > 1024L) return output.absolutePath
        return runCatching {
            val keyStore = KeyStore.getInstance("AndroidCAStore").apply { load(null, null) }
            val pem = StringBuilder()
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val certificate = keyStore.getCertificate(aliases.nextElement()) as? X509Certificate
                    ?: continue
                pem.append("-----BEGIN CERTIFICATE-----\n")
                pem.append(Base64.encodeToString(certificate.encoded, Base64.NO_WRAP))
                pem.append("\n-----END CERTIFICATE-----\n")
            }
            output.writeText(pem.toString())
            output.absolutePath
        }.getOrElse {
            EchLog.w(TAG, "Unable to export Android CA bundle: ${it.message}")
            ""
        }
    }

    external fun h3Fetch(
        host: String,
        peerIp: String,
        echBase64: String,
        path: String,
        referer: String,
        caPath: String,
        outputFile: String,
        userAgent: String,
    ): String
}
