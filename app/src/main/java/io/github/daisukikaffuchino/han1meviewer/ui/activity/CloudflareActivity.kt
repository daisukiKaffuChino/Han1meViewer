package io.github.daisukikaffuchino.han1meviewer.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import io.github.daisukikaffuchino.han1meviewer.Preferences.cloudFlareCookie
import io.github.daisukikaffuchino.han1meviewer.Preferences.cloudFlareCookieHost
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.USER_AGENT
import io.github.daisukikaffuchino.han1meviewer.logic.network.CloudflareVerificationCoordinator
import io.github.daisukikaffuchino.han1meviewer.ui.screen.web.CloudflareScreen
import io.github.daisukikaffuchino.han1meviewer.util.CookieString

class CloudflareActivity : BaseActivity() {

    companion object {
        const val EXTRA_URL = "request_url"
        const val EXTRA_VERIFICATION_HOST = "verification_host"
    }

    private val progressState = mutableIntStateOf(0)
    private val tipTextState = mutableStateOf("")
    private var verificationHost: String? = null
    private var verificationCompleted = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }
        verificationHost = intent.getStringExtra(EXTRA_VERIFICATION_HOST)
            ?: url.toUri().host?.lowercase()

        tipTextState.value = getString(R.string.complete_cloudflare_verification_with_warning)

        setHanimeContent {
            CloudflareScreen(
                progress = progressState.intValue,
                tipText = tipTextState.value,
                onClose = { finish() },
                webViewFactory = { createWebView(url) },
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(url: String): WebView {
        return WebView(this).apply {
            val wv = this
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                javaScriptCanOpenWindowsAutomatically = true
                userAgentString = USER_AGENT
            }

            val cookieMgr = CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(wv, true)
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean = false
            }

            evaluateJavascript("navigator.userAgent") { output ->
                val userAgent = output
                    .removeSurrounding("\"")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                val chromePattern = "Chrome/(\\d+\\.\\d+\\.\\d+\\.\\d+)".toRegex()
                val versionCode =
                    chromePattern.find(userAgent)?.groupValues?.getOrNull(1) ?: userAgent
                runOnUiThread {
                    var t = getString(R.string.complete_cloudflare_verification_with_warning)
                    t += getString(R.string.current_webview_version, versionCode)
                    try {
                        val parts = versionCode.split(".").map { it.toIntOrNull() ?: 0 }
                        if (parts.size >= 4) {
                            if (parts[0] < 120) {
                                t += getString(R.string.webview_version_too_low)
                            }
                        } else {
                            t += getString(R.string.webview_version_unknown)
                        }
                    } catch (_: Exception) {
                        t += getString(R.string.version_check_failed)
                    }
                    tipTextState.value = t
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressState.intValue = newProgress
                    if (newProgress >= 90) {
                        view?.postDelayed({
                            view.evaluateJavascript("document.head.innerHTML") { html ->
                                if (!html.contains("#challenge-form") &&
                                    !html.contains("#challenge-success-text") &&
                                    !html.contains("#challenge-error-text")
                                ) {
                                    val completedUrl = view?.url ?: url
                                    val cookies = cookieMgr.getCookie(completedUrl) ?: ""
                                    if (cookies.containsCookie("cf_clearance")) {
                                        val cookieHost = completedUrl.toUri().host?.lowercase()
                                            ?: verificationHost
                                            ?: return@evaluateJavascript
                                        cloudFlareCookie = CookieString(cookies)
                                        cloudFlareCookieHost = cookieHost
                                        cookieMgr.flush()
                                        verificationCompleted = true
                                        CloudflareVerificationCoordinator.complete(
                                            verificationHost ?: cookieHost,
                                            succeeded = true,
                                        )
                                        finish()
                                    }
                                }
                            }
                        }, 1000)
                    }
                }
            }
            loadUrl(url)
        }
    }

    override fun onDestroy() {
        if (!verificationCompleted && !isChangingConfigurations) {
            verificationHost?.let {
                CloudflareVerificationCoordinator.complete(it, succeeded = false)
            }
        }
        super.onDestroy()
    }

    private fun String.containsCookie(name: String): Boolean {
        return split(';').any { it.trim().substringBefore('=') == name }
    }

}
