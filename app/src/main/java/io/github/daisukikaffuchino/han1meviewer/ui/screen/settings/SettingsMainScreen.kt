@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

@Composable
fun SettingsMainScreen(
    onOpenVideoPlayback: () -> Unit,
    onOpenNetworkDownload: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        enableItemAnimation = false,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SettingNavigationItem(
                title = stringResource(R.string.settings_video_playback),
                summary = stringResource(R.string.settings_video_playback_summary),
                iconRes = R.drawable.ic_baseline_play_circle_outline_24,
                shapes = HanimeDefaults.largerShapes(),
                onClick = onOpenVideoPlayback,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.settings_network_download),
                summary = stringResource(R.string.settings_network_download_summary),
                iconRes = R.drawable.baseline_data_usage_24,
                shapes = HanimeDefaults.largerShapes(),
                onClick = onOpenNetworkDownload,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.settings_appearance),
                summary = stringResource(R.string.settings_appearance_summary),
                iconRes = R.drawable.ic_baseline_theme_24,
                shapes = HanimeDefaults.largerShapes(),
                onClick = onOpenAppearance,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.privacy),
                summary = stringResource(R.string.settings_privacy_summary),
                iconRes = R.drawable.ic_setting_applock,
                shapes = HanimeDefaults.largerShapes(),
                onClick = onOpenPrivacy,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.settings_data),
                summary = stringResource(R.string.settings_data_summary),
                iconRes = R.drawable.baseline_backup_24,
                shapes = HanimeDefaults.largerShapes(),
                onClick = onOpenData,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.about),
                summary = stringResource(R.string.settings_about_summary),
                iconRes = R.drawable.ic_baseline_info_24,
                shapes = HanimeDefaults.largerShapes(),
                onClick = onOpenAbout,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsMainScreenPreview() {
    ComponentPreview {
        SettingsMainScreen({}, {}, {}, {}, {}, {})
    }
}
