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
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

@Composable
internal fun DataPrivacySettingsPage(
    state: HomeSettingsUiState,
    isLoggedIn: Boolean,
    onUseLockScreenChange: (Boolean) -> Unit,
    onSecureModeChange: (Boolean) -> Unit,
    onOpenFakeLauncherIcon: () -> Unit,
    onDisableCommentsChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onClearCache: () -> Unit,
    onExportLocalLists: () -> Unit,
    onImportLocalLists: () -> Unit,
    onExportOnlineLists: () -> Unit,
    onImportOnlineLists: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.small),
    ) {
        SettingsSection(stringResource(R.string.privacy)) {
        SettingSwitchItem(
            title = stringResource(R.string.use_lock_screen),
            summary = stringResource(R.string.use_lock_screen_sum),
            checked = state.useLockScreen,
            iconRes = R.drawable.ic_setting_applock,
            onCheckedChange = onUseLockScreenChange,
        )
        SettingSwitchItem(
            title = stringResource(R.string.secure_mode),
            summary = stringResource(R.string.secure_mode_summary),
            checked = state.secureMode,
            iconRes = R.drawable.ic_admin_panel_settings,
            onCheckedChange = onSecureModeChange,
        )
        SettingNavigationItem(
            title = stringResource(R.string.fake_app_icon),
            summary = stringResource(R.string.select_fake_icon),
            valueText = state.fakeLauncherIconName,
            iconRes = R.drawable.ic_mask,
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
        SettingsSection(stringResource(R.string.settings_data)) {
        SettingNavigationItem(
            title = stringResource(R.string.backup_export_title),
            summary = stringResource(R.string.backup_export_summary),
            iconRes = R.drawable.ic_export,
            onClick = onExportBackup,
        )
        SettingNavigationItem(
            title = stringResource(R.string.backup_import_title),
            summary = stringResource(R.string.backup_import_summary),
            iconRes = R.drawable.ic_download,
            onClick = onImportBackup,
        )
    }
        SettingsSection(stringResource(R.string.cache_section)) {
        SettingNavigationItem(
            title = stringResource(R.string.clear_cache),
            summary = state.cacheSummary,
            iconRes = R.drawable.ic_clear_all,
            onClick = onClearCache,
        )
    }
        SettingsSection(stringResource(R.string.local_data_section)) {
        SettingNavigationItem(
            title = stringResource(R.string.local_data_export_title),
            summary = stringResource(R.string.local_data_export_summary),
            iconRes = R.drawable.ic_export,
            onClick = onExportLocalLists,
        )
        SettingNavigationItem(
            title = stringResource(R.string.local_data_import_title),
            summary = stringResource(R.string.local_data_import_summary),
            iconRes = R.drawable.ic_download,
            onClick = onImportLocalLists,
        )
    }
        if (isLoggedIn) {
            SettingsSection(stringResource(R.string.online_data_section)) {
            SettingNavigationItem(
                title = stringResource(R.string.online_data_export_title),
                summary = stringResource(R.string.online_data_export_summary),
                iconRes = R.drawable.ic_export,
                onClick = onExportOnlineLists,
            )
            SettingNavigationItem(
                title = stringResource(R.string.online_data_import_title),
                summary = stringResource(R.string.online_data_import_summary),
                iconRes = R.drawable.ic_download,
                onClick = onImportOnlineLists,
            )
            }
        }
    }
}
