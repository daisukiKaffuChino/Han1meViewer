package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.github.daisukikaffuchino.utils.LogUtil
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.Parser
import io.github.daisukikaffuchino.han1meviewer.logic.network.DohConfig
import io.github.daisukikaffuchino.han1meviewer.logic.network.HDns
import io.github.daisukikaffuchino.han1meviewer.logic.network.HProxySelector
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeNetwork
import io.github.daisukikaffuchino.han1meviewer.logic.network.ServiceCreator
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.EchHttp
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.logout
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.DelayResultUi
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.DohTestResultUi
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.NetworkSettingsScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.NetworkSettingsUiState
import io.github.daisukikaffuchino.utils.ActivityManager
import io.github.daisukikaffuchino.utils.applicationContext
import io.github.daisukikaffuchino.utils.SonnerToast
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

private enum class DohConflictTarget {
    EnableDoH,
    EnableBuiltInHosts,
}

@Composable
fun NetworkSettingsRouteScreen(
    embedded: Boolean = false,
    onOpenApplyDeepLinks: () -> Unit = {},
    onOpenEchTest: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    var currentHost by remember { mutableStateOf(SettingsRepository.baseUrl) }
    var isDelayTesting by remember { mutableStateOf(false) }
    var isDohTesting by remember { mutableStateOf(false) }
    var isCustomMirrorTesting by remember { mutableStateOf(false) }
    var customMirrorTestResult by remember { mutableStateOf<String?>(null) }
    var showDomainRestartConfirm by remember { mutableStateOf(false) }
    var showHostsRestartConfirm by remember { mutableStateOf(false) }
    var showCustomHostsValidationError by remember { mutableStateOf<List<String>?>(null) }
    var showCustomMirrorValidationError by remember { mutableStateOf(false) }
    var showCustomMirrorWarningConfirm by remember { mutableStateOf(false) }
    var showDohConflictConfirm by remember { mutableStateOf(false) }
    var showSocksWarning by remember { mutableStateOf(false) }
    var pendingDomainValue by remember { mutableStateOf("") }
    var pendingUseCustomMirrorSite by remember { mutableStateOf(SettingsRepository.useCustomMirrorSite) }
    var pendingCustomMirrorSite by remember { mutableStateOf(SettingsRepository.customMirrorSite) }
    var pendingAppendCustomMirrorPath by remember { mutableStateOf(SettingsRepository.appendCustomMirrorPath) }
    var pendingDohConflictTarget by remember { mutableStateOf(DohConflictTarget.EnableDoH) }
    var pendingDohEnabled by remember { mutableStateOf(SettingsRepository.useDoH) }
    var pendingDohPreset by remember { mutableStateOf(SettingsRepository.dohPreset) }
    var pendingDohCustomUrl by remember { mutableStateOf(SettingsRepository.dohCustomUrl) }
    var pendingDohBootstrapIps by remember { mutableStateOf(SettingsRepository.dohBootstrapIps) }
    var pendingDohTimeoutSeconds by remember { mutableIntStateOf(SettingsRepository.dohTimeoutSeconds) }
    val delayResults = remember { mutableStateListOf<DelayResultUi>() }
    val dohTestResults = remember { mutableStateListOf<DohTestResultUi>() }
    val delayHandler = remember { Handler(Looper.getMainLooper()) }
    val dohHandler = remember { Handler(Looper.getMainLooper()) }
    val executor = remember { Executors.newCachedThreadPool() }
    val uiState = remember(settings, context) { buildNetworkSettingsUiState(context) }
    val networkTimeoutText = stringResource(R.string.network_timeout_text)
    val customMirrorInvalidText = stringResource(R.string.custom_mirror_site_invalid)
    val customMirrorTestingText = stringResource(R.string.custom_mirror_site_testing)
    fun stopDelayTest() {
        isDelayTesting = false
        delayHandler.removeCallbacksAndMessages(null)
    }

    fun stopDohTest() {
        isDohTesting = false
        dohHandler.removeCallbacksAndMessages(null)
    }

    fun measureDelay(ip: String): Int {
        return try {
            val start = System.currentTimeMillis()
            val address = InetAddress.getByName(ip)
            val reachable = address.isReachable(2000)
            if (reachable) (System.currentTimeMillis() - start).toInt() else -1
        } catch (_: Exception) {
            -1
        }
    }

    fun testIp(ip: String) {
        if (!isDelayTesting) return
        executor.execute {
            val delay = measureDelay(ip)
            delayHandler.post {
                val index = delayResults.indexOfFirst { it.ip == ip }
                if (index >= 0) {
                    delayResults[index] = DelayResultUi(ip, delay)
                }
            }
        }
    }

    fun scheduleNextTest(ipList: List<String>) {
        if (!isDelayTesting) return
        ipList.forEach(::testIp)
        delayHandler.postDelayed({ scheduleNextTest(ipList) }, 2000)
    }

    fun runDohTest() {
        if (isDohTesting) return
        val host = SettingsRepository.baseUrl.toUri().host ?: applicationContext.getString(R.string.unknow)
        currentHost = SettingsRepository.baseUrl
        dohTestResults.clear()
        isDohTesting = true
        executor.execute {
            val start = System.currentTimeMillis()
            val result = runCatching { HDns().lookupByDoHOnly(host) }
            val delay = (System.currentTimeMillis() - start).toInt()
            dohHandler.post {
                dohTestResults.clear()
                result.onSuccess { list ->
                    dohTestResults.add(
                        DohTestResultUi(
                            host = host,
                            ips = list.mapNotNull { it.hostAddress }.distinct(),
                            delay = delay,
                            message = "",
                        )
                    )
                }.onFailure { throwable ->
                    LogUtil.w("DOH_TEST", "lookup failed for $host: ${throwable.message}")
                    dohTestResults.add(
                        DohTestResultUi(
                            host = host,
                            ips = emptyList(),
                            delay = -1,
                            message = throwable.message?.ifBlank { networkTimeoutText }
                                ?: networkTimeoutText,
                        )
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopDelayTest()
            stopDohTest()
            executor.shutdownNow()
        }
    }

    NetworkSettingsScreen(
        state = uiState,
        domainOptions = buildDomainOptions(context),
        currentHost = currentHost,
        delayResults = delayResults,
        dohTestResults = dohTestResults,
        isDelayTesting = isDelayTesting,
        isDohTesting = isDohTesting,
        echEnabled = SettingsRepository.useEch,
        proxyType = SettingsRepository.proxyType,
        proxyIp = SettingsRepository.proxyIp,
        proxyPort = SettingsRepository.proxyPort,
        dohEnabled = SettingsRepository.useDoH,
        dohPreset = SettingsRepository.dohPreset,
        dohCustomUrl = SettingsRepository.dohCustomUrl,
        dohBootstrapIps = SettingsRepository.dohBootstrapIps,
        dohTimeoutSeconds = SettingsRepository.dohTimeoutSeconds,
        useCustomMirrorSite = SettingsRepository.useCustomMirrorSite,
        customMirrorSite = SettingsRepository.customMirrorSite,
        appendCustomMirrorPath = SettingsRepository.appendCustomMirrorPath,
        customMirrorTestResult = customMirrorTestResult,
        isCustomMirrorTesting = isCustomMirrorTesting,
        onDomainChange = { newValue ->
            val origin = SettingsRepository.baseUrl
            if (newValue != origin) {
                pendingDomainValue = newValue
                pendingUseCustomMirrorSite = false
                pendingCustomMirrorSite = SettingsRepository.customMirrorSite
                pendingAppendCustomMirrorPath = SettingsRepository.appendCustomMirrorPath
                showDomainRestartConfirm = true
            }
        },
        onSaveCustomMirrorSite = { enabled, url, appendPath ->
            val normalizedUrl = normalizeCustomMirrorSite(url)
            if (enabled && normalizedUrl == null) {
                showCustomMirrorValidationError = true
                return@NetworkSettingsScreen
            }
            val customMirrorSite = normalizedUrl.orEmpty()
            if (enabled != SettingsRepository.useCustomMirrorSite ||
                customMirrorSite != SettingsRepository.customMirrorSite ||
                appendPath != SettingsRepository.appendCustomMirrorPath
            ) {
                pendingUseCustomMirrorSite = enabled
                pendingCustomMirrorSite = customMirrorSite
                pendingAppendCustomMirrorPath = appendPath
                if (enabled) {
                    showCustomMirrorWarningConfirm = true
                } else {
                    showDomainRestartConfirm = true
                }
            }
        },
        onTestCustomMirrorSite = { url, appendPath ->
            val normalizedUrl = normalizeCustomMirrorSite(url)
            if (normalizedUrl == null) {
                customMirrorTestResult = customMirrorInvalidText
                return@NetworkSettingsScreen
            }
            if (isCustomMirrorTesting) return@NetworkSettingsScreen
            isCustomMirrorTesting = true
            customMirrorTestResult = customMirrorTestingText
            executor.execute {
                val result = testCustomMirrorSite(context, normalizedUrl, appendPath)
                Handler(Looper.getMainLooper()).post {
                    customMirrorTestResult = result
                    isCustomMirrorTesting = false
                }
            }
        },
        onUseBuiltInHostsChange = { value ->
            if (value && SettingsRepository.useDoH) {
                showDohConflictConfirm = true
                pendingDohConflictTarget = DohConflictTarget.EnableBuiltInHosts
                return@NetworkSettingsScreen
            }
            coroutineScope.launch {
                SettingsRepository.update { it.copy(useBuiltInHosts = value) }
                showHostsRestartConfirm = true
            }
        },
        onUseBackupMediaCdnChange = { value ->
            coroutineScope.launch {
                SettingsRepository.update { it.copy(useBackupMediaCdn = value) }
                showHostsRestartConfirm = true
            }
        },
        onDisableMobileDataWarningChange = { value ->
            coroutineScope.launch {
                SettingsRepository.update { it.copy(disableMobileDataWarning = value) }
            }
        },
        onOpenApplyDeepLinks = onOpenApplyDeepLinks,
        onSaveCustomHosts = { data ->
            val errors = HDns.validateCustomHosts(data)
            if (errors.isNotEmpty()) {
                showCustomHostsValidationError = errors
                return@NetworkSettingsScreen
            }
            coroutineScope.launch {
                SettingsRepository.update { it.copy(customHostsData = data) }
                if (SettingsRepository.useBuiltInHosts) HanimeNetwork.rebuildNetwork()
            }
        },
        customHostsData = SettingsRepository.customHostsData,
        onSaveDohSettings = { enabled, preset, url, bootstrapIps, timeoutSeconds ->
            pendingDohEnabled = enabled
            pendingDohPreset = preset
            pendingDohCustomUrl = url
            pendingDohBootstrapIps = bootstrapIps
            pendingDohTimeoutSeconds = timeoutSeconds
            if (enabled && SettingsRepository.useBuiltInHosts) {
                showDohConflictConfirm = true
                pendingDohConflictTarget = DohConflictTarget.EnableDoH
                return@NetworkSettingsScreen
            }
            coroutineScope.launch {
                SettingsRepository.update { it.copy(useDoH = enabled, dohPreset = preset, dohCustomUrl = url, dohBootstrapIps = bootstrapIps, dohTimeoutSeconds = timeoutSeconds.coerceIn(1, 60)) }
                EchHttp.onDohSettingsChanged()
                currentHost = SettingsRepository.baseUrl
                HanimeNetwork.rebuildNetwork()
                SingletonImageLoader.reset()
            }
        },
        onEchEnabledChange = { enabled ->
            coroutineScope.launch {
                SettingsRepository.update { it.copy(useEch = enabled) }
                EchHttp.onDohSettingsChanged()
                HanimeNetwork.rebuildNetwork()
                SingletonImageLoader.reset()
            }
        },
        onOpenDelayTest = {
            val host =
                SettingsRepository.baseUrl.toUri().host ?: applicationContext.getString(R.string.unknow)
            currentHost = SettingsRepository.baseUrl
            delayResults.clear()
            isDelayTesting = true
            executor.execute {
                val ipList = HDns().getCDNList(host)
                Handler(Looper.getMainLooper()).post {
                    LogUtil.i("delayTest", ipList.toString())
                    delayResults.clear()
                    delayResults.addAll(ipList.map { DelayResultUi(it, -1) })
                    scheduleNextTest(ipList)
                }
            }
        },
        onOpenEchTest = onOpenEchTest,
        onOpenDohTest = { runDohTest() },
        onDismissDelayTest = { stopDelayTest() },
        onDismissDohTest = { stopDohTest() },
        onApplyProxy = { type, ip, port ->
            val valid = when (type) {
                HProxySelector.TYPE_DIRECT, HProxySelector.TYPE_SYSTEM -> true
                HProxySelector.TYPE_HTTP, HProxySelector.TYPE_SOCKS -> HProxySelector.validateIp(ip) && HProxySelector.validatePort(
                    port
                )

                else -> false
            }
            if (!valid) {
                SonnerToast.warning(R.string.invalid_ip_or_port)
                return@NetworkSettingsScreen
            }
            if (type == HProxySelector.TYPE_SOCKS) {
                showSocksWarning = true
            }
            coroutineScope.launch {
                SettingsRepository.update { it.copy(proxyType = io.github.daisukikaffuchino.han1meviewer.logic.model.ProxyType.fromId(type), proxyIp = ip, proxyPort = port) }
                HProxySelector.rebuildNetwork()
                HanimeNetwork.rebuildNetwork()
                SingletonImageLoader.reset()
            }
        },
        embedded = embedded,
    )

    ConfirmDialog(
        visible = showDomainRestartConfirm,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.domain_change_tips).trimIndent(),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = {
            coroutineScope.launch {
                SettingsRepository.update {
                    it.copy(
                        domainName = pendingDomainValue.ifEmpty { it.domainName },
                        selectedBaseUrl = pendingDomainValue.ifEmpty { it.selectedBaseUrl },
                        useCustomMirrorSite = pendingUseCustomMirrorSite,
                        customMirrorSite = pendingCustomMirrorSite,
                        appendCustomMirrorPath = pendingAppendCustomMirrorPath,
                    )
                }
                logout()
                ActivityManager.restart(killProcess = true)
            }
        },
        onDismiss = {
            pendingDomainValue = ""
            pendingUseCustomMirrorSite = SettingsRepository.useCustomMirrorSite
            pendingCustomMirrorSite = SettingsRepository.customMirrorSite
            pendingAppendCustomMirrorPath = SettingsRepository.appendCustomMirrorPath
            showDomainRestartConfirm = false
        },
    )

    if (showCustomMirrorValidationError) {
        AlertDialog(
            onDismissRequest = { showCustomMirrorValidationError = false },
            title = { Text(stringResource(R.string.attention)) },
            text = { Text(stringResource(R.string.custom_mirror_site_invalid)) },
            confirmButton = {
                TextButton(onClick = { showCustomMirrorValidationError = false }) {
                    Text(stringResource(R.string.confirm))
                }
            },
        )
    }

    ConfirmDialog(
        visible = showHostsRestartConfirm,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.restart_or_not_working, EMPTY_STRING),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = { ActivityManager.restart(killProcess = true) },
        onDismiss = { showHostsRestartConfirm = false },
    )

    val validationErrors = showCustomHostsValidationError
    if (validationErrors != null) {
        AlertDialog(
            onDismissRequest = { showCustomHostsValidationError = null },
            title = { Text(stringResource(R.string.attention)) },
            text = { Text(validationErrors.joinToString("\n")) },
            confirmButton = {
                TextButton(onClick = { showCustomHostsValidationError = null }) {
                    Text(stringResource(R.string.confirm))
                }
            },
        )
    }

    ConfirmDialog(
        visible = showCustomMirrorWarningConfirm,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.custom_mirror_site_warning),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = {
            showCustomMirrorWarningConfirm = false
            showDomainRestartConfirm = true
        },
        onDismiss = {
            pendingUseCustomMirrorSite = SettingsRepository.useCustomMirrorSite
            pendingCustomMirrorSite = SettingsRepository.customMirrorSite
            pendingAppendCustomMirrorPath = SettingsRepository.appendCustomMirrorPath
            showCustomMirrorWarningConfirm = false
        },
    )

    ConfirmDialog(
        visible = showDohConflictConfirm,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.doh_conflict_message),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = {
            coroutineScope.launch {
                SettingsRepository.update {
                    when (pendingDohConflictTarget) {
                        DohConflictTarget.EnableDoH -> it.copy(useBuiltInHosts = false, useDoH = pendingDohEnabled, dohPreset = pendingDohPreset, dohCustomUrl = pendingDohCustomUrl, dohBootstrapIps = pendingDohBootstrapIps, dohTimeoutSeconds = pendingDohTimeoutSeconds.coerceIn(1, 60))
                        DohConflictTarget.EnableBuiltInHosts -> it.copy(useDoH = false, useBuiltInHosts = true)
                    }
                }
                EchHttp.onDohSettingsChanged()
                showDohConflictConfirm = false
                HanimeNetwork.rebuildNetwork()
                SingletonImageLoader.reset()
            }
        },
        onDismiss = { showDohConflictConfirm = false },
    )

    ConfirmDialog(
        visible = showSocksWarning,
        title = stringResource(R.string.warning),
        message = stringResource(R.string.mpv_socks5_warning),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = { showSocksWarning = false },
        onDismiss = { showSocksWarning = false },
    )
}

private fun buildNetworkSettingsUiState(context: Context): NetworkSettingsUiState {
    return NetworkSettingsUiState(
        domainName = SettingsRepository.baseUrl,
        domainDisplay = buildDomainOptions(context).firstOrNull { it.second == SettingsRepository.baseUrl }?.first
            ?: SettingsRepository.baseUrl,
        proxySummary = when (SettingsRepository.proxyType) {
            HProxySelector.TYPE_DIRECT -> context.getString(R.string.direct)
            HProxySelector.TYPE_SYSTEM -> context.getString(R.string.system_proxy)
            HProxySelector.TYPE_HTTP -> context.getString(
                R.string.http_proxy,
                SettingsRepository.proxyIp,
                SettingsRepository.proxyPort
            )

            HProxySelector.TYPE_SOCKS -> context.getString(
                R.string.socks_proxy,
                SettingsRepository.proxyIp,
                SettingsRepository.proxyPort
            )

            else -> context.getString(R.string.direct)
        },
        useBuiltInHosts = SettingsRepository.useBuiltInHosts,
        useBackupMediaCdn = SettingsRepository.useBackupMediaCdn,
        disableMobileDataWarning = SettingsRepository.disableMobileDataWarning,
        useCustomMirrorSite = SettingsRepository.useCustomMirrorSite,
        customMirrorSite = SettingsRepository.customMirrorSite,
        appendCustomMirrorPath = SettingsRepository.appendCustomMirrorPath,
        useDoH = SettingsRepository.useDoH,
        dohSummary = buildDohSummary(context),
        delaySummary = context.getString(R.string.node_latency_sum),
    )
}

private fun normalizeCustomMirrorSite(url: String): String? {
    val trimmed = url.trim().trimEnd('/')
    val uri = runCatching { trimmed.toUri() }.getOrNull() ?: return null
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) return null
    if (!uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()) return null
    return url.trim()
}

private fun testCustomMirrorSite(context: Context, homeUrl: String, appendPath: Boolean): String {
    return runCatching {
        val request = Request.Builder().url(homeUrl).get().build()
        ServiceCreator.hClient.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val body = response.body.string()
            if (!response.isSuccessful) {
                return context.getString(
                    R.string.custom_mirror_site_test_failed_http,
                    response.code,
                    finalUrl,
                )
            }

            val apiBaseUrl = buildCustomMirrorApiBaseUrl(homeUrl, appendPath)
            val watchTestResult = testCustomMirrorWatchUrl(context, apiBaseUrl)
            when (val parseResult = Parser.homePageVer2(body)) {
                is WebsiteState.Success -> if (watchTestResult == null) {
                    context.getString(
                        R.string.custom_mirror_site_test_success,
                        finalUrl,
                        apiBaseUrl,
                    )
                } else {
                    context.getString(
                        R.string.custom_mirror_site_test_partial_success,
                        finalUrl,
                        apiBaseUrl,
                        watchTestResult,
                    )
                }

                is WebsiteState.Error -> context.getString(
                    R.string.custom_mirror_site_test_parse_failed,
                    finalUrl,
                    parseResult.throwable.message ?: parseResult.throwable::class.java.simpleName,
                )

                WebsiteState.Loading -> context.getString(
                    R.string.custom_mirror_site_test_parse_failed,
                    finalUrl,
                    context.getString(R.string.loading),
                )
            }
        }
    }.getOrElse { throwable ->
        context.getString(
            R.string.custom_mirror_site_test_failed,
            throwable.message ?: throwable::class.java.simpleName,
        )
    }
}

private fun testCustomMirrorWatchUrl(context: Context, apiBaseUrl: String): String? {
    return runCatching {
        val url = apiBaseUrl + "search"
        val request = Request.Builder().url(url).get().build()
        ServiceCreator.hClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                null
            } else {
                context.getString(
                    R.string.custom_mirror_site_watch_test_failed_http,
                    response.code,
                    response.request.url.toString(),
                )
            }
        }
    }.getOrElse { throwable ->
        context.getString(
            R.string.custom_mirror_site_watch_test_failed,
            throwable.message ?: throwable::class.java.simpleName,
        )
    }
}

private fun buildCustomMirrorApiBaseUrl(homeUrl: String, appendPath: Boolean): String {
    val url = if (appendPath) homeUrl else {
        val uri = homeUrl.toUri()
        "${uri.scheme}://${uri.encodedAuthority}"
    }
    return if (url.endsWith('/')) url else "$url/"
}

private fun buildDohSummary(context: Context): String {
    if (!SettingsRepository.useDoH) return context.getString(R.string.doh_disabled_summary)
    if (SettingsRepository.useBuiltInHosts) return context.getString(R.string.doh_conflict_message)
    val core = if (SettingsRepository.dohPreset == "custom") {
        SettingsRepository.dohCustomUrl.ifBlank { context.getString(R.string.custom) }
    } else {
        DohConfig.selectedPreset().title
    }
    val bootstrap = DohConfig.bootstrapIps().takeIf { it.isNotEmpty() }?.joinToString()
    return if (bootstrap != null) "$core\nBootstrap: $bootstrap" else core
}
