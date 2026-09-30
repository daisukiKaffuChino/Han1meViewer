package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.HorizontalCardCountConfig
import io.github.daisukikaffuchino.han1meviewer.SearchGridColumnsConfig
import io.github.daisukikaffuchino.han1meviewer.logic.model.AppLanguage
import io.github.daisukikaffuchino.han1meviewer.logic.model.AppSettings
import io.github.daisukikaffuchino.han1meviewer.logic.model.DisplayDensity
import io.github.daisukikaffuchino.han1meviewer.logic.model.PaletteStyle
import io.github.daisukikaffuchino.han1meviewer.logic.model.PlayerKernel
import io.github.daisukikaffuchino.han1meviewer.logic.model.SettingsStore
import io.github.daisukikaffuchino.han1meviewer.logic.model.ThemeAccent
import io.github.daisukikaffuchino.han1meviewer.logic.model.ThemeMode
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoLandscapeLayoutStyle
import io.github.daisukikaffuchino.han1meviewer.logic.model.DOWNLOAD_SPEED_BYTES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.net.URI

object SettingsRepository : SettingsStore {
    private lateinit var store: SettingsStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun install(store: SettingsStore) {
        check(!::store.isInitialized) { "SettingsRepository is already installed" }
        this.store = store
    }

    override val settings: StateFlow<AppSettings> get() = store.settings
    override suspend fun update(transform: (AppSettings) -> AppSettings) = store.update(transform)
    val current: AppSettings get() = settings.value

    val loginStateFlow by lazy { settings.map { it.isAlreadyLogin }.stateIn(scope, SharingStarted.Eagerly, current.isAlreadyLogin) }
    val checkInEnabledFlow by lazy { settings.map { it.checkInEnabled }.stateIn(scope, SharingStarted.Eagerly, current.checkInEnabled) }

    val isAlreadyLogin get() = current.isAlreadyLogin
    val localListNoticeDismissed get() = current.localListNoticeDismissed
    val usageNoticeAccepted get() = current.usageNoticeAccepted
    val usageSourceVerified get() = current.usageSourceVerified
    val usageSourcePending get() = current.usageSourcePending
    val savedUserId get() = current.savedUserId
    val cloudFlareCookieHost get() = current.cloudFlareCookieHost.lowercase()
    val switchPlayerKernel get() = current.playerKernel.value
    val enableGoogleCast get() = current.enableGoogleCast
    val showBottomProgress get() = current.showBottomProgress
    val playerSpeed get() = current.playerSpeed
    val slideSensitivity get() = current.slideSensitivity
    val longPressSpeedTime get() = current.longPressSpeedTime
    val videoLanguage get() = current.videoLanguage
    val videoQuality get() = current.videoQuality
    val showPlayedIndicator get() = current.showPlayedIndicator
    val watchedProgressThreshold get() = current.watchedProgressThreshold
    val isCheckInEnabled get() = current.checkInEnabled
    val fakeLauncherIcon get() = current.fakeLauncherIcon
    val baseUrl: String get() {
        if (current.useCustomMirrorSite && current.customMirrorSite.isNotBlank()) {
            val value = if (current.appendCustomMirrorPath) current.customMirrorSite else rootUrl(current.customMirrorSite)
            return value.withTrailingSlash()
        }
        return current.domainName
    }
    val homeUrl get() = if (current.useCustomMirrorSite && current.customMirrorSite.isNotBlank()) current.customMirrorSite else baseUrl
    val useCustomMirrorSite get() = current.useCustomMirrorSite
    val customMirrorSite get() = current.customMirrorSite
    val appendCustomMirrorPath get() = current.appendCustomMirrorPath
    val selectedBaseUrl get() = current.selectedBaseUrl
    val useBuiltInHosts get() = current.useBuiltInHosts
    val customHostsData get() = current.customHostsData
    val useDoH get() = current.useDoH
    val dohPreset get() = current.dohPreset
    val dohCustomUrl get() = current.dohCustomUrl
    val dohBootstrapIps get() = current.dohBootstrapIps
    val dohTimeoutSeconds get() = current.dohTimeoutSeconds
    val whenCountdownRemind get() = current.whenCountdownRemindSeconds * 1_000
    val showCommentWhenCountdown get() = current.showCommentWhenCountdown
    val hKeyframesEnable get() = current.hKeyframesEnable
    val sharedHKeyframesEnable get() = current.sharedHKeyframesEnable
    val sharedHKeyframesUseFirst get() = current.sharedHKeyframesUseFirst
    val proxyType get() = current.proxyType.id
    val proxyIp get() = current.proxyIp
    val proxyPort get() = current.proxyPort
    val downloadCountLimit get() = current.downloadCountLimit
    val collapseDownloadedGroup get() = current.collapseDownloadedGroup
    val isUsePrivateStorage get() = current.usePrivateStorage
    val safDownloadPath get() = current.safDownloadPath
    val useDarkMode get() = current.themeMode.value
    val useDynamicColor get() = current.useDynamicColor
    val allowResumePlayback get() = current.allowResumePlayback
    val autoPlay get() = current.autoPlay
    val autoPlayNext get() = current.autoPlayNext
    val searchPagination get() = current.searchPagination
    val searchArtistIgnoreVideoType get() = current.searchArtistIgnoreVideoType
    val disableMobileDataWarning get() = current.disableMobileDataWarning
    val disablePredictiveBack get() = current.disablePredictiveBack
    val tabletMode get() = current.tabletMode
    val videoLandscapeLayoutStyle get() = current.videoLandscapeLayoutStyle
    val hapticFeedbackEnabled get() = current.hapticFeedbackEnabled
    val funLoadingHints get() = current.funLoadingHints
    val secureMode get() = current.secureMode
    val mpvProfile get() = current.mpvProfile
    val enableGPUNextRenderer get() = current.enableGpuNextRenderer
    val mpvInterpolation get() = current.mpvInterpolation
    val mpvDeband get() = current.mpvDeband
    val mpvFramedrop get() = current.mpvFramedrop
    val mpvHwdec get() = current.mpvHwdec
    val mpvCacheSecs get() = current.mpvCacheSecs
    val mpvTlsVerify get() = current.mpvTlsVerify
    val mpvNetworkTimeout get() = current.mpvNetworkTimeout
    val customMpvParams get() = current.customMpvParams
    val downloadSpeedLimit get() = DOWNLOAD_SPEED_BYTES[current.downloadSpeedLimitIndex]
    val searchGridColumnsConfig get() = SearchGridColumnsConfig(current.searchGridColumnsCompact, current.searchGridColumnsMedium, current.searchGridColumnsExpanded, current.searchGridColumnsLarge)
    val horizontalCardCountConfig get() = HorizontalCardCountConfig(current.horizontalCardCountNarrow, current.horizontalCardCountCompact, current.horizontalCardCountMedium, current.horizontalCardCountExpanded)
    val subscriptionArtistRows get() = current.subscriptionArtistRows
    val alwaysShowUpdateCard get() = current.alwaysShowUpdateCard
    val displayDensity get() = current.displayDensity

    suspend fun setLoginState(value: Boolean) = update { it.copy(isAlreadyLogin = value) }
    suspend fun dismissLocalListNotice() = update { it.copy(localListNoticeDismissed = true) }
    suspend fun setCloudFlareCookie(value: String, host: String = current.cloudFlareCookieHost) = update { it.copy(cloudFlareCookie = value, cloudFlareCookieHost = host.lowercase()) }
    suspend fun setSavedUserId(value: String) = update { it.copy(savedUserId = value) }
    suspend fun setUsageNoticeAccepted(value: Boolean) = update { it.copy(usageNoticeAccepted = value) }
    suspend fun setUsageSourcePending(value: Boolean) = update { it.copy(usageSourcePending = value) }
    suspend fun setLanguage(value: AppLanguage) = update { it.copy(appLanguage = value) }
    suspend fun setThemeMode(value: ThemeMode) = update { it.copy(themeMode = value) }
    suspend fun setDynamicColor(value: Boolean) = update { it.copy(useDynamicColor = value) }
    suspend fun setThemeAccent(value: ThemeAccent) = update { it.copy(themeAccent = value) }
    suspend fun setPaletteStyle(value: PaletteStyle) = update { it.copy(paletteStyle = value) }
    suspend fun setLauncherIcon(value: String) = update { it.copy(fakeLauncherIcon = value) }
    suspend fun setHapticFeedback(value: Boolean) = update { it.copy(hapticFeedbackEnabled = value) }
    suspend fun setCheckInEnabled(value: Boolean) = update { it.copy(checkInEnabled = value) }
    suspend fun setUsePrivateStorage(value: Boolean) = update { it.copy(usePrivateStorage = value) }
    suspend fun setDownloadStorage(usePrivate: Boolean, path: String?) = update { it.copy(usePrivateStorage = usePrivate, safDownloadPath = path) }
    suspend fun setDownloadCountLimit(value: Int) = update { it.copy(downloadCountLimit = value) }
    suspend fun setDownloadSpeedLimitIndex(value: Int) = update { it.copy(downloadSpeedLimitIndex = value.coerceIn(DOWNLOAD_SPEED_BYTES.indices)) }
    suspend fun setSlideSensitivity(value: Int) = update { it.copy(slideSensitivity = value.coerceIn(1, 7)) }
    suspend fun setSubscriptionArtistRows(value: Int) = update { it.copy(subscriptionArtistRows = value.coerceIn(1, 3)) }
    suspend fun setHomeCategories(order: List<String>, hidden: Set<String>) = update { it.copy(homeCategoryOrder = order, hiddenHomeCategoryKeys = hidden) }
    suspend fun setCachedUpdateJson(value: String?) = update { it.copy(cachedUpdateJson = value) }
    suspend fun setIgnoredVersionCode(value: Int) = update { it.copy(ignoredVersionCode = value) }
    suspend fun setAlwaysShowUpdateCard(value: Boolean) = update { it.copy(alwaysShowUpdateCard = value) }
    suspend fun setDisplayDensity(value: DisplayDensity) = update { it.copy(displayDensity = value) }
    suspend fun setVideoLandscapeLayoutStyle(value: VideoLandscapeLayoutStyle) =
        update { it.copy(videoLandscapeLayoutStyle = value) }

    private fun String.withTrailingSlash() = if (endsWith('/')) this else "$this/"
    private fun rootUrl(value: String) = runCatching { URI(value).let { "${it.scheme}://${it.rawAuthority}" } }.getOrDefault(value)
}
