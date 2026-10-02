package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Routes protected WebView traffic through Conscrypt ECH. GET responses can be
 * fully intercepted; non-GET traffic is handled by [EchWebBridge].
 */
object HyWebViewHelper {

    private const val TAG = "HY-ECH-WEBVIEW"

    fun installWebView(
        webView: WebView,
        onFormLoginSuccess: ((String) -> Unit)? = null,
    ): EchWebBridge {
        val bridge = EchWebBridge(webView, onFormLoginSuccess)
        webView.addJavascriptInterface(bridge, EchWebBridge.NAME)
        return bridge
    }

    fun injectBridge(webView: WebView, url: String?) {
        val host = url?.let { runCatching { URI(it).host }.getOrNull() } ?: return
        if (!EchHosts.isCoreDomain(host)) return
        runCatching {
            webView.evaluateJavascript(EchWebBridgeJs.script(HANIME_HOSTNAME), null)
        }.onFailure {
            EchLog.w(TAG, "Unable to inject ECH bridge: ${it.message}")
        }
    }

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url ?: return null
        val url = uri.toString()
        val host = uri.host ?: return null
        val method = (request.method ?: "GET").uppercase()

        // Do not change WebView behavior for unrelated sites.
        if (!EchHosts.isCoreDomain(host)) return null
        // POST bodies are unavailable here; the JavaScript bridge handles those.
        if (method != "GET") return null

        if (HyEchH3.isStaticAsset(url.toHttpUrl())) {
            val cacheFile = runCatching { HyEchH3.fetchResourceToFile(url) }.getOrNull()
            if (cacheFile != null && cacheFile.exists() && cacheFile.length() > 0L) {
                return WebResourceResponse(
                    HyEchH3.mimeFor(url.toHttpUrl()),
                    null,
                    200,
                    "OK",
                    emptyMap(),
                    DeletingFileInputStream(cacheFile),
                )
            }
        }

        if (!ConscryptEch.ready && !ConscryptEch.install()) {
            return failClosed(host, "ECH transport is not ready")
        }

        var lastError = "unknown error"
        repeat(2) { attempt ->
            try {
                val builder = Request.Builder().url(url).get()
                request.requestHeaders.forEach { (key, value) ->
                    if (key.equals("Host", true) ||
                        key.equals("Content-Length", true) ||
                        key.equals("Cookie", true)
                    ) {
                        return@forEach
                    }
                    runCatching { builder.header(key, value) }
                }

                val cookie = runCatching {
                    CookieManager.getInstance().getCookie(url)
                }.getOrNull()
                if (!cookie.isNullOrBlank()) builder.header("Cookie", cookie)

                client.newCall(builder.build()).execute().use { response ->
                    val body = response.body.bytes()
                    val contentType = response.header("Content-Type") ?: "text/html"
                    val (mime, charset) = parseContentType(contentType)
                    syncCookies(url, response)

                    val headers = linkedMapOf<String, String>()
                    response.headers.names().forEach { name ->
                        headers[name] = response.headers[name] ?: ""
                    }
                    return WebResourceResponse(
                        mime,
                        charset,
                        response.code,
                        response.message.ifBlank { "OK" },
                        headers,
                        ByteArrayInputStream(body),
                    )
                }
            } catch (throwable: Throwable) {
                lastError = throwable.message ?: throwable.javaClass.simpleName
                EchLog.w(TAG, "WebView ECH request failed for $host: $lastError")
                if (attempt == 0) {
                    EchDoh.invalidateEch(host)
                    runCatching { Thread.sleep(300) }
                }
            }
        }

        return failClosed(host, lastError)
    }

    private fun syncCookies(url: String, response: okhttp3.Response) {
        val manager = CookieManager.getInstance()
        response.headers.values("Set-Cookie").forEach { raw ->
            var cookie = raw.replace(
                Regex(""";\s*Domain=[^;]+""", RegexOption.IGNORE_CASE),
                "",
            )
            cookie = cookie.replace(
                Regex(""";\s*Secure""", RegexOption.IGNORE_CASE),
                "",
            )
            runCatching { manager.setCookie(url, cookie) }
        }
        runCatching { manager.flush() }
    }

    private fun parseContentType(value: String): Pair<String, String?> {
        var mime = "text/html"
        var charset: String? = "utf-8"
        value.split(';').forEachIndexed { index, part ->
            val trimmed = part.trim()
            if (index == 0) {
                mime = trimmed.ifBlank { "text/html" }
            } else if (trimmed.startsWith("charset=", ignoreCase = true)) {
                charset = trimmed.substringAfter('=').trim()
            }
        }
        return mime to charset
    }

    private fun failClosed(host: String, reason: String): WebResourceResponse {
        EchLog.e(TAG, "fail-closed host=$host reason=$reason")
        val safeReason = reason.replace("<", "&lt;")
        val page = """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="font-family:sans-serif;padding:24px;line-height:1.6">
            <h3>连接失败</h3>
            <p>无法安全地连接到 $host，已阻止本次访问。</p>
            <p style="color:#888;font-size:13px">原因：$safeReason</p>
            <p style="color:#888;font-size:13px">可到「设置 - 网络」检查 DoH 配置后重试。</p>
            </body></html>
        """.trimIndent()
        return WebResourceResponse(
            "text/html",
            "utf-8",
            502,
            "Bad Gateway",
            mapOf("Cache-Control" to "no-store"),
            ByteArrayInputStream(page.toByteArray(Charsets.UTF_8)),
        )
    }

    private val client: OkHttpClient by lazy {
        ConscryptEch.install()
        OkHttpClient.Builder()
            .sslSocketFactory(ConscryptEch.socketFactory, ConscryptEch.trustManager)
            .dns(EchDns())
            .cookieJar(okhttp3.CookieJar.NO_COOKIES)
            .addInterceptor(EchRetryInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private class DeletingFileInputStream(private val file: File) : FileInputStream(file) {
        override fun close() {
            try {
                super.close()
            } finally {
                file.delete()
            }
        }
    }
}
