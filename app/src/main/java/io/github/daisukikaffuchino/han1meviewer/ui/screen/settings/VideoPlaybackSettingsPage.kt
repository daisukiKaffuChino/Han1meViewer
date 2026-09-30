package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSliderItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSwitchItem
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

@Composable
internal fun VideoPlaybackSettingsPage(
    state: HomeSettingsUiState,
    onOpenVideoLanguage: () -> Unit,
    onOpenVideoQuality: () -> Unit,
    onAllowPipModeChange: (Boolean) -> Unit,
    onAllowResumePlaybackChange: (Boolean) -> Unit,
    onAutoPlayChange: (Boolean) -> Unit,
    onShowPlayedIndicatorChange: (Boolean) -> Unit,
    onWatchedProgressThresholdChange: (Int) -> Unit,
    hKeyframeSettingsContent: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.small),
    ) {
        SettingsSection(stringResource(R.string.video)) {
        SettingNavigationItem(
            title = stringResource(R.string.video_language),
            valueText = state.videoLanguageLabel,
            iconRes = R.drawable.ic_simp_to_trad,
            onClick = onOpenVideoLanguage,
        )
        SettingNavigationItem(
            title = stringResource(R.string.default_video_quilty),
            valueText = state.defaultVideoQuality,
            iconRes = R.drawable.ic_video_quilty,
            onClick = onOpenVideoQuality,
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
            iconRes = R.drawable.ic_skip,
            onCheckedChange = onAllowResumePlaybackChange,
        )
        SettingSwitchItem(
            title = stringResource(R.string.auto_play_title),
            summary = stringResource(R.string.auto_play_summary),
            checked = state.autoPlay,
            iconRes = R.drawable.ic_autoplay,
            onCheckedChange = onAutoPlayChange,
        )
        SettingSwitchItem(
            title = stringResource(R.string.show_played_indicator),
            summary = stringResource(R.string.show_played_indicator_summary),
            checked = state.showPlayedIndicator,
            iconRes = R.drawable.ic_history,
            onCheckedChange = onShowPlayedIndicatorChange,
        )
        SettingSliderItem(
            title = stringResource(R.string.watched_progress_threshold),
            summary = stringResource(
                R.string.watched_progress_threshold_summary,
                state.watchedProgressThreshold,
            ),
            value = state.watchedProgressThreshold,
            valueRange = 0..100,
            step = 10,
            iconRes = R.drawable.ic_flag,
            onValueChange = onWatchedProgressThresholdChange,
        )
        }
        hKeyframeSettingsContent()
    }
}
