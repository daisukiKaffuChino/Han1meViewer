package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Native endpoint for WebView fetch/XHR/form requests whose body is not
 * available through shouldInterceptRequest.
 */
class EchWebBridge(
    private val webView: WebView,
    private val onFormLoginSuccess: ((String) -> Unit)? = null,
) {

    @JavascriptInterface
    fun send(
        id: String,
        method: String,
        url: String,
        headersJson: String,
        bodyBase64: String,
        pageUrl: String,
    ) {
        if (!isProtectedUrl(url)) {
            resolve(id, errorPayload("Bridge rejected a non-protected URL"))
            return
        }

        Thread {
            val payload = runCatching {
                client.newCall(
                    buildRequest(method, url, headersJson, bodyBase64, pageUrl),
                ).execute().use { response ->
                    val body = response.body.bytes()
                    JSONObject()
                        .put("ok", true)
                        .put("status", response.code)
                        .put("statusText", response.message.ifBlank { "OK" })
                        .put("url", response.request.url.toString())
                        .put("headers", JSONObject(headerMap(response)))
                        .put("bodyB64", Base64.encodeToString(body, Base64.NO_WRAP))
                }
            }.getOrElse { throwable ->
                EchLog.w(TAG, "Bridge request failed: ${throwable.message}")
                errorPayload(throwable.message ?: throwable.javaClass.simpleName)
            }
            resolve(id, payload)
        }.start()
    }

    @JavascriptInterface
    fun postForm(url: String, bodyBase64: String, pageUrl: String) {
        if (!isProtectedUrl(url)) return

        Thread {
            val result = runCatching {
                formClient.newCall(
                    buildRequest("POST", url, "{}", bodyBase64, pageUrl),
                ).execute().use { response ->
                    val location = response.header("Location")
                    val target = location?.let { absolutize(it, url) } ?: url
                    val successful = !location.isNullOrBlank() &&
                            !location.lowercase().contains("login")
                    target to successful
                }
            }.getOrNull()

            val target = result?.first ?: pageUrl
            val successful = result?.second == true
            webView.post {
                if (successful && onFormLoginSuccess != null) {
                    val cookie = runCatching {
                        CookieManager.getInstance().getCookie(target)
                    }.getOrNull().orEmpty()
                    onFormLoginSuccess.invoke(cookie)
                } else {
                    runCatching { webView.loadUrl(target) }
                }
            }
        }.start()
    }

    @JavascriptInterface
    fun log(message: String) {
        EchLog.i(TAG, "[js] ${message.take(300)}")
    }

    private fun buildRequest(
        method: String,
        url: String,
        headersJson: String,
        bodyBase64: String,
        pageUrl: String,
    ): Request {
        if (!ConscryptEch.ready && !ConscryptEch.install()) {
            throw IOException("ECH transport is not ready")
        }

        val bytes = if (bodyBase64.isEmpty()) {
            ByteArray(0)
        } else {
            Base64.decode(bodyBase64, Base64.DEFAULT)
        }
        val body = if (method in setOf("POST", "PUT", "PATCH", "DELETE")) {
            bytes.toRequestBody(null)
        } else {
            null
        }
        val builder = Request.Builder().url(url)
        builder.method(method, body)

        var hasContentType = false
        runCatching {
            val headers = JSONObject(headersJson)
            headers.keys().forEach { key ->
                val value = headers.optString(key)
                if (key.equals("Host", true) ||
                    key.equals("Content-Length", true) ||
                    key.equals("Cookie", true)
                ) {
                    return@forEach
                }
                if (key.equals("Content-Type", true)) hasContentType = true
                runCatching { builder.header(key, value) }
            }
        }
        if (body != null && !hasContentType) {
            builder.header("Content-Type", FORM_TYPE)
        }
        if (pageUrl.startsWith("http")) {
            builder.header("Referer", pageUrl)
            originOf(pageUrl)?.let { builder.header("Origin", it) }
        }
        return builder.build()
    }

    private fun resolve(id: String, payload: JSONObject) {
        val script = "window.__echBridgeResolve(${JSONObject.quote(id)}, " +
                "${JSONObject.quote(payload.toString())});"
        webView.post { runCatching { webView.evaluateJavascript(script, null) } }
    }

    private fun errorPayload(message: String): JSONObject = JSONObject()
        .put("ok", false)
        .put("status", 502)
        .put("error", message)
        .put(
            "bodyB64",
            Base64.encodeToString(
                "ECH bridge refused to send this request in plaintext: $message"
                    .toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP,
            ),
        )

    private fun headerMap(response: okhttp3.Response): Map<String, String> =
        response.headers.names().associateWith { response.headers[it] ?: "" }

    companion object {
        const val NAME = "EchBridge"
        private const val TAG = "HY-ECH-BRIDGE"
        private const val FORM_TYPE = "application/x-www-form-urlencoded"

        fun isProtectedUrl(url: String): Boolean = runCatching {
            val host = URI(url).host?.lowercase() ?: return false
            HANIME_HOSTNAME.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        }.getOrDefault(false)

        private fun originOf(url: String): String? = runCatching {
            val uri = URI(url)
            if (uri.scheme == null || uri.host == null) {
                null
            } else {
                "${uri.scheme}://${uri.host}" + if (uri.port > 0) ":${uri.port}" else ""
            }
        }.getOrNull()

        private fun absolutize(location: String, base: String): String =
            if (location.startsWith("http")) {
                location
            } else {
                runCatching { URI(base).resolve(location).toString() }.getOrDefault(base)
            }

        private fun cookieSync(client: OkHttpClient.Builder): OkHttpClient.Builder =
            client.addNetworkInterceptor { chain ->
                val request = chain.request()
                val webViewCookie = runCatching {
                    CookieManager.getInstance().getCookie(request.url.toString())
                }.getOrNull()
                val requestWithCookie = if (webViewCookie.isNullOrBlank()) {
                    request
                } else {
                    request.newBuilder().header("Cookie", webViewCookie).build()
                }

                val response = chain.proceed(requestWithCookie)
                val cookieManager = CookieManager.getInstance()
                response.headers.values("Set-Cookie").forEach { raw ->
                    var cookie = raw.replace(
                        Regex(""";\s*Domain=[^;]+""", RegexOption.IGNORE_CASE),
                        "",
                    )
                    cookie = cookie.replace(
                        Regex(""";\s*Secure""", RegexOption.IGNORE_CASE),
                        "",
                    )
                    cookie = cookie.replace(
                        Regex(""";\s*SameSite=[^;]+""", RegexOption.IGNORE_CASE),
                        "; SameSite=Lax",
                    )
                    runCatching { cookieManager.setCookie(request.url.toString(), cookie) }
                }
                runCatching { cookieManager.flush() }
                response
            }

        private val client: OkHttpClient by lazy {
            ConscryptEch.install()
            cookieSync(
                OkHttpClient.Builder()
                    .sslSocketFactory(ConscryptEch.socketFactory, ConscryptEch.trustManager)
                    .dns(EchDns())
                    .cookieJar(okhttp3.CookieJar.NO_COOKIES)
                    .followRedirects(true)
                    .addInterceptor(EchRetryInterceptor())
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS),
            ).build()
        }

        private val formClient: OkHttpClient by lazy {
            ConscryptEch.install()
            cookieSync(
                OkHttpClient.Builder()
                    .sslSocketFactory(ConscryptEch.socketFactory, ConscryptEch.trustManager)
                    .dns(EchDns())
                    .cookieJar(okhttp3.CookieJar.NO_COOKIES)
                    .followRedirects(false)
                    .addInterceptor(EchRetryInterceptor())
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS),
            ).build()
        }
    }
}
