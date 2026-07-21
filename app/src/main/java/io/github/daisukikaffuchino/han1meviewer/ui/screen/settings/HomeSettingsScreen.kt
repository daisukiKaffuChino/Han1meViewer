package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.HorizontalCardCountConfig
import io.github.daisukikaffuchino.han1meviewer.HA1_GITHUB_URL
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.SearchGridColumnsConfig
import io.github.daisukikaffuchino.han1meviewer.ui.component.ChoiceDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingInfoItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSwitchItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsAnimatedVisibility
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsSegmentedGroup
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
    Privacy,
    Data,
    About,
}

private enum class HomeSettingsChoiceDialog {
    VideoLanguage,
    VideoQuality,
    AppLanguage,
}

/** Renders one settings category while keeping the existing preference callbacks intact. */
@Composable
fun HomeSettingsScreen(
    page: HomeSettingsPage,
    state: HomeSettingsUiState,
    onVideoLanguageChange: (String) -> Unit,
    onVideoQualityChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onUseDynamicColorChange: (Boolean) -> Unit,
    onThemeAccentColorChange: (Int) -> Unit,
    onAppPaletteStyleChange: (Int) -> Unit,
    onAllowPipModeChange: (Boolean) -> Unit,
    onAllowResumePlaybackChange: (Boolean) -> Unit,
    onShowPlayedIndicatorChange: (Boolean) -> Unit,
    onSearchArtistIgnoreVideoTypeChange: (Boolean) -> Unit,
    onDisableMobileDataWarningChange: (Boolean) -> Unit,
    onDisablePredictiveBackChange: (Boolean) -> Unit,
    onTabletModeChange: (Boolean) -> Unit,
    onDisableCommentsChange: (Boolean) -> Unit,
    onCollapseDownloadedGroupChange: (Boolean) -> Unit,
    onSearchGridColumnsConfigChange: (SearchGridColumnsConfig) -> Unit,
    onHorizontalCardCountConfigChange: (HorizontalCardCountConfig) -> Unit,
    onUseLockScreenChange: (Boolean) -> Unit,
    onHomeCategoryPreferencesChange: (List<String>, Set<String>) -> Unit,
    hKeyframeSettingsContent: @Composable () -> Unit,
    networkSettingsContent: @Composable () -> Unit,
    downloadSettingsContent: @Composable () -> Unit,
    onOpenAppLanguageSettings: (String) -> Unit,
    onOpenApplyDeepLinks: () -> Unit,
    onOpenFakeLauncherIcon: () -> Unit,
    onOpenOpenSourceLicense: () -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onSubmitBug: () -> Unit,
    onOpenForum: () -> Unit,
) {
    var activeDialog by rememberSaveable { mutableStateOf<HomeSettingsChoiceDialog?>(null) }
    var showSearchGridColumnsDialog by rememberSaveable { mutableStateOf(false) }
    var showHorizontalCardCountDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showUsageTerms by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

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
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.settingsItemPadding),
    ) {
        when (page) {
            HomeSettingsPage.VideoPlayback -> {
                item {
                    SettingsSection(stringResource(R.string.video)) {
                        SettingNavigationItem(
                            title = stringResource(R.string.video_language),
                            valueText = state.videoLanguageLabel,
                            iconRes = R.drawable.baseline_simp_to_trad_24,
                            onClick = { activeDialog = HomeSettingsChoiceDialog.VideoLanguage },
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.default_video_quilty),
                            valueText = state.defaultVideoQuality,
                            iconRes = R.drawable.ic_video_quilty,
                            onClick = { activeDialog = HomeSettingsChoiceDialog.VideoQuality },
                        )
                        SettingSwitchItem(
                            title = stringResource(R.string.allow_pip_title),
                            summary = stringResource(R.string.allow_pip_disc),
                            checked = state.allowPipMode,
                            iconRes = R.drawable.ic_pip_mode,
                            onCheckedChange = onAllowPipModeChange,
                        )
                        SettingSwitchItem(
                            title = stringResource(R.string.resume_playback_title),
                            summary = stringResource(R.string.resume_playback_summary),
                            checked = state.allowResumePlayback,
                            iconRes = R.drawable.ic_baseline_skip_24,
                            onCheckedChange = onAllowResumePlaybackChange,
                        )
                        SettingSwitchItem(
                            title = stringResource(R.string.show_played_indicator),
                            summary = stringResource(R.string.show_played_indicator_summary),
                            checked = state.showPlayedIndicator,
                            iconRes = R.drawable.ic_baseline_history_24,
                            onCheckedChange = onShowPlayedIndicatorChange,
                        )
                    }
                }
                item {
                    hKeyframeSettingsContent()
                }
            }

            HomeSettingsPage.NetworkDownload -> {
                item {
                    networkSettingsContent()
                }
                item {
                    SettingsSegmentedGroup {
                        SettingSwitchItem(
                            title = stringResource(R.string.disable_mobile_data_warning),
                            summary = stringResource(R.string.disable_mobile_data_warning_summary),
                            checked = state.disableMobileDataWarning,
                            iconRes = R.drawable.baseline_mobile_data_24,
                            onCheckedChange = onDisableMobileDataWarningChange,
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.apply_deep_links),
                            summary = stringResource(R.string.apply_deep_links_summary),
                            iconRes = R.drawable.baseline_add_link_24,
                            onClick = onOpenApplyDeepLinks,
                        )
                    }
                }
                item {
                    downloadSettingsContent()
                }
                item {
                    SettingsSegmentedGroup {
                        SettingSwitchItem(
                            title = stringResource(R.string.collapse_downloaded_groups),
                            summary = stringResource(R.string.collapse_downloaded_groups_summary),
                            checked = state.collapseDownloadedGroup,
                            iconRes = R.drawable.ic_baseline_fold_24,
                            onCheckedChange = onCollapseDownloadedGroupChange,
                        )
                    }
                }
            }

            HomeSettingsPage.Appearance -> {
                item {
                    SettingsSection(stringResource(R.string.accent_color)) {
                        SettingSwitchItem(
                            title = stringResource(R.string.dynamic_color_title),
                            summary = stringResource(R.string.dynamic_color_summary),
                            checked = state.useDynamicColor,
                            enabled = state.dynamicColorEnabled,
                            iconRes = R.drawable.ic_baseline_theme_24,
                            onCheckedChange = onUseDynamicColorChange,
                        )
                        SettingsAnimatedVisibility(
                            visible = !state.useDynamicColor || !state.dynamicColorEnabled,
                        ) {
                            ThemeAccentColorPicker(
                                selectedId = state.themeAccentColorId,
                                onSelect = onThemeAccentColorChange,
                            )
                        }
                    }
                }
                item {
                    SettingsSection(stringResource(R.string.display)) {
                        DarkModePicker(
                            selectedValue = state.darkMode,
                            onSelect = onDarkModeChange,
                        )
                        AppPalettePicker(
                            selectedId = state.appPaletteStyleId,
                            accentColorId = state.themeAccentColorId,
                            dynamicColor = state.useDynamicColor,
                            darkMode = state.darkMode,
                            onSelect = onAppPaletteStyleChange,
                        )
                    }
                }
                item {
                    SettingsSection(stringResource(R.string.settings_layout_content)) {
                        SettingNavigationItem(
                            title = stringResource(R.string.horizontal_card_count_title),
                            summary = stringResource(R.string.horizontal_card_count_summary),
                            valueText = state.horizontalCardCountSummary,
                            iconRes = R.drawable.baseline_row_24,
                            onClick = { showHorizontalCardCountDialog = true },
                        )
                        SettingSwitchItem(
                            title = stringResource(R.string.search_artist_ignore_video_type),
                            summary = stringResource(R.string.search_artist_ignore_video_type_summary),
                            checked = state.searchArtistIgnoreVideoType,
                            iconRes = R.drawable.baseline_prohibit_24,
                            onCheckedChange = onSearchArtistIgnoreVideoTypeChange,
                        )
                        SettingSwitchItem(
                            title = stringResource(R.string.disable_predictive_back_title),
                            summary = stringResource(R.string.temporarily_unavailable),
                            checked = state.disablePredictiveBack,
                            iconRes = R.drawable.ic_baseline_arrow_back_24,
                            onCheckedChange = onDisablePredictiveBackChange,
                            enabled = false,
                        )
                        SettingSwitchItem(
                            title = stringResource(R.string.tablet_mode),
                            summary = stringResource(R.string.tablet_mode_summary),
                            checked = state.tabletMode,
                            iconRes = R.drawable.ic_baseline_tablet_24,
                            onCheckedChange = onTabletModeChange,
                        )
                        SettingsAnimatedVisibility(visible = state.tabletMode) {
                            SettingNavigationItem(
                                title = stringResource(R.string.search_grid_columns_title),
                                summary = stringResource(R.string.search_grid_columns_summary),
                                valueText = state.searchGridColumnsSummary,
                                iconRes = R.drawable.baseline_grid_24,
                                onClick = { showSearchGridColumnsDialog = true },
                            )
                        }
                        SettingNavigationItem(
                            title = stringResource(R.string.home_category_layout),
                            summary = stringResource(
                                R.string.home_category_layout_summary,
                                state.homeCategoryItems.size - state.hiddenHomeCategoryKeys.size,
                                state.homeCategoryItems.size,
                            ),
                            iconRes = R.drawable.baseline_sort_24,
                            onClick = { showHomeCategoryDialog = true },
                        )
                    }
                }
                item {
                    SettingsSection(stringResource(R.string.app_lang)) {
                        SettingNavigationItem(
                            title = stringResource(R.string.app_lang),
                            summary = stringResource(R.string.app_lang_sum),
                            valueText = state.appLanguageLabel,
                            iconRes = R.drawable.ic_setting_lang,
                            onClick = { activeDialog = HomeSettingsChoiceDialog.AppLanguage },
                        )
                    }
                }
            }

            HomeSettingsPage.Privacy -> {
                item {
                    SettingsSection(stringResource(R.string.privacy)) {
                        SettingSwitchItem(
                            title = stringResource(R.string.use_lock_screen),
                            summary = stringResource(R.string.use_lock_screen_sum),
                            checked = state.useLockScreen,
                            iconRes = R.drawable.ic_setting_applock,
                            onCheckedChange = onUseLockScreenChange,
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.fake_app_icon),
                            summary = stringResource(R.string.select_fake_icon),
                            valueText = state.fakeLauncherIconName,
                            iconRes = R.drawable.ic_baseline_mask,
                            onClick = onOpenFakeLauncherIcon,
                        )
                        SettingSwitchItem(
                            title = stringResource(R.string.disable_comments_title),
                            summary = stringResource(R.string.disable_comments_sum),
                            checked = state.disableComments,
                            iconRes = R.drawable.ic_comments,
                            onCheckedChange = onDisableCommentsChange,
                        )
                    }
                }
            }

            HomeSettingsPage.Data -> {
                item {
                    SettingsSection(stringResource(R.string.settings_data)) {
                        SettingNavigationItem(
                            title = stringResource(R.string.backup_export_title),
                            summary = stringResource(R.string.backup_export_summary),
                            iconRes = R.drawable.baseline_backup_24,
                            onClick = onExportBackup,
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.backup_import_title),
                            summary = stringResource(R.string.backup_import_summary),
                            iconRes = R.drawable.baseline_restore_24,
                            onClick = onImportBackup,
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.clear_cache),
                            summary = state.cacheSummary,
                            iconRes = R.drawable.ic_baseline_clear_all_24,
                            onClick = onClearCache,
                        )
                    }
                }
            }

            HomeSettingsPage.About -> {
                item {
                    SettingsSection(stringResource(R.string.information)) {
                        SettingInfoItem(
                            title = stringResource(R.string.version),
                            summary = state.versionSummary,
                            iconRes = R.drawable.ic_baseline_info_24,
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.developer),
                            summary = "@daisukiKaffuChino",
                            iconRes = R.drawable.person_24px,
                            onClick = { uriHandler.openUri("https://github.com/daisukiKaffuChino") },
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.user_terms),
                            summary = stringResource(R.string.user_terms_summary),
                            iconRes = R.drawable.inbox_text_24px,
                            onClick = { showUsageTerms = true },
                        )
                    }
                }
                item {
                    SettingsSection("GitHub") {
                        SettingNavigationItem(
                            title = stringResource(R.string.project_repository),
                            summary = "daisukiKaffuChino/Han1meViewer",
                            iconRes = R.drawable.ic_ext_link,
                            onClick = { uriHandler.openUri(HA1_GITHUB_URL) },
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.submit_bug),
                            summary = stringResource(R.string.submit_bug_summary),
                            iconRes = R.drawable.baseline_bug_report_24,
                            onClick = onSubmitBug,
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.forum),
                            summary = stringResource(R.string.forum_summary),
                            iconRes = R.drawable.baseline_forum_24,
                            onClick = onOpenForum,
                        )
                        SettingNavigationItem(
                            title = stringResource(R.string.open_source_license),
                            summary = stringResource(R.string.open_source_license_summary),
                            iconRes = R.drawable.ic_oss,
                            onClick = onOpenOpenSourceLicense,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
        SettingsSegmentedGroup(content = content)
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 1000)
@Composable
private fun HomeSettingsScreenPreview() {
    ComponentPreview {
        HomeSettingsScreen(
            page = HomeSettingsPage.Appearance,
            state = previewHomeSettingsState(),
            onVideoLanguageChange = {},
            onVideoQualityChange = {},
            onDarkModeChange = {},
            onUseDynamicColorChange = {},
            onThemeAccentColorChange = {},
            onAppPaletteStyleChange = {},
            onAllowPipModeChange = {},
            onAllowResumePlaybackChange = {},
            onShowPlayedIndicatorChange = {},
            onSearchArtistIgnoreVideoTypeChange = {},
            onDisableMobileDataWarningChange = {},
            onDisablePredictiveBackChange = {},
            onTabletModeChange = {},
            onDisableCommentsChange = {},
            onCollapseDownloadedGroupChange = {},
            onSearchGridColumnsConfigChange = {},
            onHorizontalCardCountConfigChange = {},
            onUseLockScreenChange = {},
            onHomeCategoryPreferencesChange = { _, _ -> },
            hKeyframeSettingsContent = {},
            networkSettingsContent = {},
            downloadSettingsContent = {},
            onOpenAppLanguageSettings = {},
            onOpenApplyDeepLinks = {},
            onOpenFakeLauncherIcon = {},
            onOpenOpenSourceLicense = {},
            onClearCache = {},
            onExportBackup = {},
            onImportBackup = {},
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
    showPlayedIndicator = true,
    searchArtistIgnoreVideoType = false,
    disableMobileDataWarning = false,
    disablePredictiveBack = false,
    tabletMode = false,
    disableComments = false,
    collapseDownloadedGroup = false,
    useDynamicColor = false,
    useLockScreen = false,
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
    homeCategoryItems = emptyList(),
    homeCategoryOrder = emptyList(),
    hiddenHomeCategoryKeys = emptySet(),
    useAvHomeCategoryTitles = false,
)
