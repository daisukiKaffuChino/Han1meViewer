package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.HorizontalCardCountConfig
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.SearchGridColumnsConfig
import io.github.daisukikaffuchino.han1meviewer.ui.component.ChoiceDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.dialog.HomeCategoryLayoutDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.dialog.HorizontalCardCountDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.dialog.SearchGridColumnsDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

enum class HomeSettingsPage {
    VideoPlayback,
    NetworkDownload,
    Appearance,
    InterfaceInteraction,
    DataPrivacy,
    DeveloperOptions,
    About,
}

private enum class HomeSettingsChoiceDialog {
    VideoLanguage,
    VideoQuality,
    AppLanguage,
    DisplayDensity,
}

/** Renders one settings category while keeping the existing preference callbacks intact. */
@Composable
fun HomeSettingsScreen(
    page: HomeSettingsPage,
    state: HomeSettingsUiState,
    isLoggedIn: Boolean,
    onVideoLanguageChange: (String) -> Unit,
    onVideoQualityChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onUseDynamicColorChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onFunLoadingHintsChange: (Boolean) -> Unit,
    onThemeAccentColorChange: (Int) -> Unit,
    onAppPaletteStyleChange: (Int) -> Unit,
    onAllowPipModeChange: (Boolean) -> Unit,
    onAllowResumePlaybackChange: (Boolean) -> Unit,
    onAutoPlayChange: (Boolean) -> Unit,
    onShowPlayedIndicatorChange: (Boolean) -> Unit,
    onWatchedProgressThresholdChange: (Int) -> Unit,
    onSearchPaginationChange: (Boolean) -> Unit,
    onSearchArtistIgnoreVideoTypeChange: (Boolean) -> Unit,
    onDisablePredictiveBackChange: (Boolean) -> Unit,
    onTabletModeChange: (Boolean) -> Unit,
    onVideoLandscapeLayoutStyleChange: (String) -> Unit,
    onCheckInEnabledChange: (Boolean) -> Unit,
    onDisableCommentsChange: (Boolean) -> Unit,
    onSearchGridColumnsConfigChange: (SearchGridColumnsConfig) -> Unit,
    onHorizontalCardCountConfigChange: (HorizontalCardCountConfig) -> Unit,
    onUseLockScreenChange: (Boolean) -> Unit,
    onSecureModeChange: (Boolean) -> Unit,
    onAlwaysShowUpdateCardChange: (Boolean) -> Unit,
    onDisplayDensityChange: (Int) -> Unit,
    onTriggerCrash: () -> Unit,
    onHomeCategoryPreferencesChange: (List<String>, Set<String>) -> Unit,
    hKeyframeSettingsContent: @Composable () -> Unit,
    networkSettingsContent: @Composable () -> Unit,
    downloadSettingsContent: @Composable () -> Unit,
    onOpenAppLanguageSettings: (String) -> Unit,
    onOpenFakeLauncherIcon: () -> Unit,
    onOpenOpenSourceLicense: () -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportLocalLists: () -> Unit,
    onImportLocalLists: () -> Unit,
    onExportOnlineLists: () -> Unit,
    onImportOnlineLists: () -> Unit,
    onSubmitBug: () -> Unit,
    onOpenForum: () -> Unit,
) {
    var activeDialog by rememberSaveable { mutableStateOf<HomeSettingsChoiceDialog?>(null) }
    var showSearchGridColumnsDialog by rememberSaveable { mutableStateOf(false) }
    var showHorizontalCardCountDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showUsageTerms by rememberSaveable { mutableStateOf(false) }
    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.VideoLanguage,
        title = stringResource(R.string.video_language),
        options = listOf(
            stringResource(R.string.traditional_chinese) to "zht",
            stringResource(R.string.simplified_chinese) to "zhs",
        ),
        selectedValue = state.videoLanguage,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onVideoLanguageChange(it)
        },
    )
    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.VideoQuality,
        title = stringResource(R.string.default_video_quilty),
        options = listOf("480P" to "480P", "720P" to "720P", "1080P" to "1080P"),
        selectedValue = state.defaultVideoQuality,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onVideoQualityChange(it)
        },
    )
    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.AppLanguage,
        title = stringResource(R.string.app_lang),
        options = listOf(
            stringResource(R.string.follow_system) to "system",
            "English" to "en",
            stringResource(R.string.simplified_chinese) to "zh-CN",
            stringResource(R.string.traditional_chinese) to "zh-TW",
        ),
        selectedValue = state.appLanguage,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onOpenAppLanguageSettings(it)
        },
    )
    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.DisplayDensity,
        title = stringResource(R.string.application_dpi),
        options = listOf("75%" to "75", "100%" to "100", "125%" to "125"),
        selectedValue = state.displayDensityPercent.toString(),
        onDismiss = { activeDialog = null },
        onSelect = { value ->
            activeDialog = null
            onDisplayDensityChange(value.toInt())
        },
    )

    if (showSearchGridColumnsDialog) {
        SearchGridColumnsDialog(
            initialConfig = state.searchGridColumnsConfig,
            onDismiss = { showSearchGridColumnsDialog = false },
            onConfirm = {
                showSearchGridColumnsDialog = false
                onSearchGridColumnsConfigChange(it)
            },
        )
    }
    if (showHorizontalCardCountDialog) {
        HorizontalCardCountDialog(
            initialConfig = state.horizontalCardCountConfig,
            onDismiss = { showHorizontalCardCountDialog = false },
            onConfirm = {
                showHorizontalCardCountDialog = false
                onHorizontalCardCountConfigChange(it)
            },
        )
    }
    if (showHomeCategoryDialog) {
        HomeCategoryLayoutDialog(
            state = state,
            onDismiss = { showHomeCategoryDialog = false },
            onConfirm = { order, hiddenKeys ->
                showHomeCategoryDialog = false
                onHomeCategoryPreferencesChange(order, hiddenKeys)
            },
        )
    }
    UsageTermsDialog(
        visible = showUsageTerms,
        onDismiss = { showUsageTerms = false },
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(),
        enableItemAnimation = false,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.small),
    ) {
        when (page) {
            HomeSettingsPage.VideoPlayback -> {
                item {
                    VideoPlaybackSettingsPage(
                        state = state,
                        onOpenVideoLanguage = {
                            activeDialog = HomeSettingsChoiceDialog.VideoLanguage
                        },
                        onOpenVideoQuality = {
                            activeDialog = HomeSettingsChoiceDialog.VideoQuality
                        },
                        onAllowPipModeChange = onAllowPipModeChange,
                        onAllowResumePlaybackChange = onAllowResumePlaybackChange,
                        onAutoPlayChange = onAutoPlayChange,
                        onShowPlayedIndicatorChange = onShowPlayedIndicatorChange,
                        onWatchedProgressThresholdChange = onWatchedProgressThresholdChange,
                        hKeyframeSettingsContent = hKeyframeSettingsContent,
                    )
                }
            }

            HomeSettingsPage.NetworkDownload -> {
                item {
                    NetworkDownloadSettingsPage(
                        networkSettingsContent = networkSettingsContent,
                        downloadSettingsContent = downloadSettingsContent,
                    )
                }
            }

            HomeSettingsPage.Appearance -> {
                item {
                    AppearanceSettingsPage(
                        state = state,
                        onUseDynamicColorChange = onUseDynamicColorChange,
                        onThemeAccentColorChange = onThemeAccentColorChange,
                        onDarkModeChange = onDarkModeChange,
                        onAppPaletteStyleChange = onAppPaletteStyleChange,
                        onOpenAppLanguage = {
                            activeDialog = HomeSettingsChoiceDialog.AppLanguage
                        },
                    )
                }
            }

            HomeSettingsPage.InterfaceInteraction -> {
                item {
                    InterfaceInteractionSettingsPage(
                        state = state,
                        onHapticFeedbackChange = onHapticFeedbackChange,
                        onSearchPaginationChange = onSearchPaginationChange,
                        onSearchArtistIgnoreVideoTypeChange = onSearchArtistIgnoreVideoTypeChange,
                        onDisablePredictiveBackChange = onDisablePredictiveBackChange,
                        onTabletModeChange = onTabletModeChange,
                        onVideoLandscapeLayoutStyleChange = onVideoLandscapeLayoutStyleChange,
                        onCheckInEnabledChange = onCheckInEnabledChange,
                        onFunLoadingHintsChange = onFunLoadingHintsChange,
                        onOpenHorizontalCardCount = {
                            showHorizontalCardCountDialog = true
                        },
                        onOpenSearchGridColumns = {
                            showSearchGridColumnsDialog = true
                        },
                        onOpenHomeCategoryLayout = {
                            showHomeCategoryDialog = true
                        },
                    )
                }
            }

            HomeSettingsPage.DataPrivacy -> {
                item {
                    DataPrivacySettingsPage(
                        state = state,
                        isLoggedIn = isLoggedIn,
                        onUseLockScreenChange = onUseLockScreenChange,
                        onSecureModeChange = onSecureModeChange,
                        onOpenFakeLauncherIcon = onOpenFakeLauncherIcon,
                        onDisableCommentsChange = onDisableCommentsChange,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup,
                        onClearCache = onClearCache,
                        onExportLocalLists = onExportLocalLists,
                        onImportLocalLists = onImportLocalLists,
                        onExportOnlineLists = onExportOnlineLists,
                        onImportOnlineLists = onImportOnlineLists,
                    )
                }
            }

            HomeSettingsPage.DeveloperOptions -> {
                item {
                    DeveloperOptionsSettingsPage(
                        state = state,
                        onAlwaysShowUpdateCardChange = onAlwaysShowUpdateCardChange,
                        onOpenDisplayDensity = {
                            activeDialog = HomeSettingsChoiceDialog.DisplayDensity
                        },
                        onTriggerCrash = onTriggerCrash,
                    )
                }
            }

            HomeSettingsPage.About -> {
                item {
                    AboutSettingsPage(
                        state = state,
                        onSubmitBug = onSubmitBug,
                        onOpenForum = onOpenForum,
                        onOpenOpenSourceLicense = onOpenOpenSourceLicense,
                        onOpenUsageTerms = { showUsageTerms = true },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 1000)
@Composable
private fun HomeSettingsScreenPreview() {
    ComponentPreview {
        HomeSettingsScreen(
            page = HomeSettingsPage.Appearance,
            state = previewHomeSettingsState(),
            isLoggedIn = true,
            onVideoLanguageChange = {},
            onVideoQualityChange = {},
            onDarkModeChange = {},
            onUseDynamicColorChange = {},
            onHapticFeedbackChange = {},
            onFunLoadingHintsChange = {},
            onThemeAccentColorChange = {},
            onAppPaletteStyleChange = {},
            onAllowPipModeChange = {},
            onAllowResumePlaybackChange = {},
            onAutoPlayChange = {},
            onShowPlayedIndicatorChange = {},
            onWatchedProgressThresholdChange = {},
            onSearchPaginationChange = {},
            onSearchArtistIgnoreVideoTypeChange = {},
            onDisablePredictiveBackChange = {},
            onTabletModeChange = {},
            onVideoLandscapeLayoutStyleChange = {},
            onCheckInEnabledChange = {},
            onDisableCommentsChange = {},
            onSearchGridColumnsConfigChange = {},
            onHorizontalCardCountConfigChange = {},
            onUseLockScreenChange = {},
            onSecureModeChange = {},
            onAlwaysShowUpdateCardChange = {},
            onDisplayDensityChange = {},
            onTriggerCrash = {},
            onHomeCategoryPreferencesChange = { _, _ -> },
            hKeyframeSettingsContent = {},
            networkSettingsContent = {},
            downloadSettingsContent = {},
            onOpenAppLanguageSettings = {},
            onOpenFakeLauncherIcon = {},
            onOpenOpenSourceLicense = {},
            onClearCache = {},
            onExportBackup = {},
            onImportBackup = {},
            onExportLocalLists = {},
            onImportLocalLists = {},
            onExportOnlineLists = {},
            onImportOnlineLists = {},
            onSubmitBug = {},
            onOpenForum = {},
        )
    }
}

private fun previewHomeSettingsState() = HomeSettingsUiState(
    videoLanguage = "zhs",
    videoLanguageLabel = "Simplified Chinese",
    defaultVideoQuality = "1080P",
    darkMode = "follow_system",
    appLanguage = "system",
    appLanguageLabel = "Follow system",
    allowPipMode = true,
    allowResumePlayback = true,
    autoPlay = true,
    showPlayedIndicator = true,
    watchedProgressThreshold = 50,
    searchPagination = false,
    searchArtistIgnoreVideoType = false,
    disablePredictiveBack = false,
    tabletMode = false,
    videoLandscapeLayoutStyle = "classic",
    disableComments = false,
    useDynamicColor = false,
    hapticFeedbackEnabled = false,
    funLoadingHints = true,
    useLockScreen = false,
    secureMode = false,
    fakeLauncherIconName = "Han1meViewer",
    cacheSummary = "12 MB",
    versionSummary = "v26.1.0",
    dynamicColorEnabled = true,
    themeAccentColorId = 0,
    appPaletteStyleId = 1,
    searchGridColumnsSummary = "2 / 3 / 4 / 5",
    searchGridColumnsConfig = SearchGridColumnsConfig(),
    horizontalCardCountSummary = "1.5 / 2.1 / 4.1 / 5.1",
    horizontalCardCountConfig = HorizontalCardCountConfig(),
    checkInEnabled = true,
    homeCategoryItems = emptyList(),
    homeCategoryOrder = emptyList(),
    hiddenHomeCategoryKeys = emptySet(),
    useAvHomeCategoryTitles = false,
    alwaysShowUpdateCard = false,
    displayDensityPercent = 100,
)
