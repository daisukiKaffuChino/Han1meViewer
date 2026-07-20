package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.model

import io.github.daisukikaffuchino.han1meviewer.HorizontalCardCountConfig
import io.github.daisukikaffuchino.han1meviewer.SearchGridColumnsConfig
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.HomeCategoryPreferenceItem

data class HomeSettingsUiState(
    val videoLanguage: String,
    val videoLanguageLabel: String,
    val defaultVideoQuality: String,
    val darkMode: String,
    val appLanguage: String,
    val appLanguageLabel: String,
    val allowPipMode: Boolean,
    val allowResumePlayback: Boolean,
    val showPlayedIndicator: Boolean,
    val searchArtistIgnoreVideoType: Boolean,
    val disableMobileDataWarning: Boolean,
    val disablePredictiveBack: Boolean,
    val tabletMode: Boolean,
    val disableComments: Boolean,
    val collapseDownloadedGroup: Boolean,
    val useDynamicColor: Boolean,
    val useLockScreen: Boolean,
    val fakeLauncherIconName: String,
    val cacheSummary: String,
    val versionSummary: String,
    val dynamicColorEnabled: Boolean,
    val themeAccentColorId: Int,
    val appPaletteStyleId: Int,
    val searchGridColumnsSummary: String,
    val searchGridColumnsConfig: SearchGridColumnsConfig,
    val horizontalCardCountSummary: String,
    val horizontalCardCountConfig: HorizontalCardCountConfig,
    val homeCategoryItems: List<HomeCategoryPreferenceItem>,
    val homeCategoryOrder: List<String>,
    val hiddenHomeCategoryKeys: Set<String>,
    val useAvHomeCategoryTitles: Boolean,
)
