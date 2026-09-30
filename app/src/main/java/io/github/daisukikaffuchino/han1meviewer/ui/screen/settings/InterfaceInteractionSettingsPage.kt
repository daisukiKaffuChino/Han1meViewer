package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSwitchItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsAnimatedVisibility
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

@Composable
internal fun InterfaceInteractionSettingsPage(
    state: HomeSettingsUiState,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onSearchPaginationChange: (Boolean) -> Unit,
    onSearchArtistIgnoreVideoTypeChange: (Boolean) -> Unit,
    onDisablePredictiveBackChange: (Boolean) -> Unit,
    onTabletModeChange: (Boolean) -> Unit,
    onVideoLandscapeLayoutStyleChange: (String) -> Unit,
    onCheckInEnabledChange: (Boolean) -> Unit,
    onFunLoadingHintsChange: (Boolean) -> Unit,
    onOpenHorizontalCardCount: () -> Unit,
    onOpenSearchGridColumns: () -> Unit,
    onOpenHomeCategoryLayout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.small),
    ) {
        SettingsSection(stringResource(R.string.perception)) {
        SettingSwitchItem(
            title = stringResource(R.string.haptic_feedback),
            summary = stringResource(R.string.haptic_feedback_summary),
            checked = state.hapticFeedbackEnabled,
            iconRes = R.drawable.ic_mobile_vibrate,
            onCheckedChange = onHapticFeedbackChange,
        )
    }
        SettingsSection(stringResource(R.string.settings_layout_content)) {
        SettingNavigationItem(
            title = stringResource(R.string.horizontal_card_count_title),
            summary = stringResource(R.string.horizontal_card_count_summary),
            valueText = state.horizontalCardCountSummary,
            iconRes = R.drawable.ic_row,
            onClick = onOpenHorizontalCardCount,
        )
        SettingSwitchItem(
            title = stringResource(R.string.search_pagination),
            summary = stringResource(R.string.search_pagination_summary),
            checked = state.searchPagination,
            iconRes = R.drawable.ic_paging,
            onCheckedChange = onSearchPaginationChange,
        )
        SettingSwitchItem(
            title = stringResource(R.string.search_artist_ignore_video_type),
            summary = stringResource(R.string.search_artist_ignore_video_type_summary),
            checked = state.searchArtistIgnoreVideoType,
            iconRes = R.drawable.ic_prohibit,
            onCheckedChange = onSearchArtistIgnoreVideoTypeChange,
        )
        SettingSwitchItem(
            title = stringResource(R.string.disable_predictive_back_title),
            summary = stringResource(R.string.temporarily_unavailable),
            checked = state.disablePredictiveBack,
            iconRes = R.drawable.ic_swipe_right,
            onCheckedChange = onDisablePredictiveBackChange,
            enabled = false,
        )
        SettingSwitchItem(
            title = stringResource(R.string.tablet_mode),
            summary = stringResource(R.string.tablet_mode_summary),
            checked = state.tabletMode,
            iconRes = R.drawable.ic_tablet,
            onCheckedChange = onTabletModeChange,
        )
        SettingsAnimatedVisibility(visible = state.tabletMode) {
            VideoLandscapeLayoutStylePicker(
                selectedValue = state.videoLandscapeLayoutStyle,
                onSelect = onVideoLandscapeLayoutStyleChange,
            )
        }
        SettingSwitchItem(
            title = stringResource(R.string.enable_check_in_feature),
            summary = stringResource(R.string.enable_check_in_feature_summary),
            checked = state.checkInEnabled,
            iconRes = R.drawable.ic_thumb_up_off_alt,
            onCheckedChange = onCheckInEnabledChange,
        )
        SettingSwitchItem(
            title = stringResource(R.string.fun_loading_hints),
            summary = stringResource(R.string.fun_loading_hints_summary),
            checked = state.funLoadingHints,
            iconRes = R.drawable.ic_pet_supplies,
            onCheckedChange = onFunLoadingHintsChange,
        )
        SettingsAnimatedVisibility(visible = state.tabletMode) {
            SettingNavigationItem(
                title = stringResource(R.string.search_grid_columns_title),
                summary = stringResource(R.string.search_grid_columns_summary),
                valueText = state.searchGridColumnsSummary,
                iconRes = R.drawable.ic_grid,
                onClick = onOpenSearchGridColumns,
            )
        }
        SettingNavigationItem(
            title = stringResource(R.string.home_category_layout),
            summary = stringResource(
                R.string.home_category_layout_summary,
                state.homeCategoryItems.size - state.hiddenHomeCategoryKeys.size,
                state.homeCategoryItems.size,
            ),
            iconRes = R.drawable.ic_sort,
            onClick = onOpenHomeCategoryLayout,
        )
        }
    }
}
