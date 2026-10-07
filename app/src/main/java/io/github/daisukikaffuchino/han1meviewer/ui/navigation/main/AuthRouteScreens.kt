package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import io.github.daisukikaffuchino.han1meviewer.HANIME_LOGIN_URL
import io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_URL
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.USER_AGENT
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo
import io.github.daisukikaffuchino.han1meviewer.logic.network.CloudflareVerificationCoordinator
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.HyWebViewHelper
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.login
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.screen.login.LoginDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.login.LoginScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.login.ManualInputCookiesScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.web.CloudflareScreen
import io.github.daisukikaffuchino.han1meviewer.util.CookieString
import io.github.daisukikaffuchino.utils.LogUtil
import io.github.daisukikaffuchino.utils.SonnerToast
import kotlinx.coroutines.launch

@Composable
fun LoginRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    onOpenManualCookies: () -> Unit,
    onLoginSucceeded: () -> Unit,
) {
    var isRefreshing by remember { mutableStateOf(true) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var canWebViewGoBack by remember { mutableStateOf(false) }
    val webViewState = remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()

    fun finishLogin(cookies: String) {
        scope.launch {
            login(cookies)
            onLoginSucceeded()
        }
    }

    fun navigateBack() {
        val webView = webViewState.value
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else {
            onBack()
        }
    }

    BackHandler(enabled = canWebViewGoBack, onBack = ::navigateBack)

    if (showLoginDialog) {
        LoginDialog(
            isLoggingIn = isLoggingIn,
            onDismiss = { showLoginDialog = false },
            onLogin = { username, password ->
                isLoggingIn = true
                scope.launch {
                    NetworkRepo.login(username, password).collect { state ->
                        when (state) {
                            WebsiteState.Loading -> Unit
                            is WebsiteState.Error -> {
                                isLoggingIn = false
                                LogUtil.e("Login", "登录失败", state.throwable)
                                if (state.throwable is IllegalStateException) {
                                    SonnerToast.error(R.string.account_or_password_wrong)
                                } else {
                                    SonnerToast.error(R.string.login_failed)
                                }
                            }
                            is WebsiteState.Success -> {
                                login(state.info)
                                isLoggingIn = false
                                showLoginDialog = false
                                SonnerToast.success(R.string.login_success)
                                onLoginSucceeded()
                            }
                        }
                    }
                }
            },
        )
    }

    LoginScreen(
        isRefreshing = isRefreshing,
        onBack = ::navigateBack,
        onRefresh = { webViewState.value?.loadUrl(HANIME_LOGIN_URL) },
        onOpenQrScanner = onOpenManualCookies,
        webViewFactory = {
            createLoginWebView(
                context = activity,
                onCreated = { webViewState.value = it },
                onPageStateChanged = { webView ->
                    isRefreshing = false
                    canWebViewGoBack = webView.canGoBack()
                },
                onLoginSucceeded = ::finishLogin,
                onLoadFailed = {
                    isRefreshing = false
                    showLoginDialog = true
                },
            )
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            webViewState.value?.run {
                removeAllViews()
                destroy()
            }
            webViewState.value = null
        }
    }
}

@Composable
fun ManualCookiesRouteScreen(
    onBack: () -> Unit,
    onLoginSucceeded: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    ManualInputCookiesScreen(
        onBack = onBack,
        onCookieScanned = { cookie ->
            scope.launch {
                login(cookie)
                onLoginSucceeded()
            }
        },
    )
}

@Composable
fun CloudflareRouteScreen(
    activity: MainActivity,
    route: CloudflareRoute,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var progress by remember(route.host) { mutableIntStateOf(0) }
    var tipText by remember(route.host) {
        mutableStateOf(activity.getString(R.string.complete_cloudflare_verification_with_warning))
    }
    val webViewState = remember(route.host) { mutableStateOf<WebView?>(null) }
    val finalizedState = remember(route.host) { mutableStateOf(false) }

    fun finishVerification(succeeded: Boolean) {
        if (finalizedState.value) return
        finalizedState.value = true
        CloudflareVerificationCoordinator.complete(route.host, succeeded)
        onBack()
    }

    CloudflareScreen(
        progress = progress,
        tipText = tipText,
        onClose = { finishVerification(false) },
        webViewFactory = {
            createCloudflareWebView(
                context = activity,
                url = route.url,
                onCreated = { webViewState.value = it },
                onProgressChanged = { progress = it },
                onUserAgent = { tipText = buildWebViewVersionTip(activity, it) },
                onVerificationReady = { completedUrl, cookieManager ->
                    scope.launch {
                        if (persistCloudflareCookies(completedUrl, route.host, cookieManager)) {
                            finishVerification(true)
                        }
                    }
                },
            )
        },
    )

    DisposableEffect(route.host) {
        onDispose {
            webViewState.value?.run {
                removeAllViews()
                destroy()
            }
            webViewState.value = null
            if (!finalizedState.value) {
                CloudflareVerificationCoordinator.complete(route.host, succeeded = false)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createLoginWebView(
    context: Context,
    onCreated: (WebView) -> Unit,
    onPageStateChanged: (WebView) -> Unit,
    onLoginSucceeded: (String) -> Unit,
    onLoadFailed: () -> Unit,
): WebView = WebView(context).apply {
    onCreated(this)
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.userAgentString = USER_AGENT
    HyWebViewHelper.installWebView(this, onLoginSucceeded)
    webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? =
            HyWebViewHelper.intercept(request)
                ?: super.shouldInterceptRequest(view, request)

        override fun onPageCommitVisible(view: WebView, url: String) {
            HyWebViewHelper.injectBridge(view, url)
            super.onPageCommitVisible(view, url)
        }

        override fun onPageFinished(view: WebView, url: String) {
            HyWebViewHelper.injectBridge(view, url)
            onPageStateChanged(view)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            if (request.isRedirect && HANIME_URL.contains(request.url.toString())) {
                val cookies = CookieManager.getInstance().getCookie(request.url.host).orEmpty()
                LogUtil.d("login_cookie", "Captured login cookies: ${cookies.isNotBlank()}")
                onLoginSucceeded(cookies)
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            if (request?.isForMainFrame == true) onLoadFailed()
        }
    }
    loadUrl(HANIME_LOGIN_URL)
}

@SuppressLint("SetJavaScriptEnabled")
private fun createCloudflareWebView(
    context: Context,
    url: String,
    onCreated: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onUserAgent: (String) -> Unit,
    onVerificationReady: (String, CookieManager) -> Unit,
): WebView = WebView(context).apply {
    onCreated(this)
    val cloudflareWebView = this
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        javaScriptCanOpenWindowsAutomatically = true
        userAgentString = USER_AGENT
    }
    val cookieManager = CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(cloudflareWebView, true)
    }
    HyWebViewHelper.installWebView(this)
    webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? =
            HyWebViewHelper.intercept(request)
                ?: super.shouldInterceptRequest(view, request)

        override fun onPageCommitVisible(view: WebView, url: String) {
            HyWebViewHelper.injectBridge(view, url)
            super.onPageCommitVisible(view, url)
        }

        override fun onPageFinished(view: WebView, url: String) {
            HyWebViewHelper.injectBridge(view, url)
            super.onPageFinished(view, url)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean = false
    }
    evaluateJavascript("navigator.userAgent", onUserAgent)
    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            onProgressChanged(newProgress)
            if (newProgress >= 90) {
                view?.postDelayed({
                    view.evaluateJavascript("document.head.innerHTML") { html ->
                        if (!html.contains("#challenge-form") &&
                            !html.contains("#challenge-success-text") &&
                            !html.contains("#challenge-error-text")
                        ) {
                            onVerificationReady(view.url ?: url, cookieManager)
                        }
                    }
                }, 1000)
            }
        }
    }
    loadUrl(url)
}

private suspend fun persistCloudflareCookies(
    completedUrl: String,
    fallbackHost: String,
    cookieManager: CookieManager,
): Boolean {
    val cookies = cookieManager.getCookie(completedUrl).orEmpty()
    if (!cookies.containsCookie("cf_clearance")) return false
    val cookieHost = completedUrl.toUri().host?.lowercase() ?: fallbackHost
    SettingsRepository.setCloudFlareCookie(cookies, cookieHost)
    cookieManager.flush()
    return true
}

private fun buildWebViewVersionTip(context: Context, output: String): String {
    val userAgent = output
        .removeSurrounding("\"")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
    val versionCode = "Chrome/(\\d+\\.\\d+\\.\\d+\\.\\d+)".toRegex()
        .find(userAgent)
        ?.groupValues
        ?.getOrNull(1)
        ?: userAgent
    var text = context.getString(R.string.complete_cloudflare_verification_with_warning)
    text += context.getString(R.string.current_webview_version, versionCode)
    text += try {
        val parts = versionCode.split(".").map { it.toIntOrNull() ?: 0 }
        when {
            parts.size < 4 -> context.getString(R.string.webview_version_unknown)
            parts[0] < 120 -> context.getString(R.string.webview_version_too_low)
            else -> ""
        }
    } catch (_: Exception) {
        context.getString(R.string.version_check_failed)
    }
    return text
}

private fun String.containsCookie(name: String): Boolean =
    split(';').any { it.trim().substringBefore('=') == name }
