package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSwitchItem
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.model.HomeSettingsUiState

@Composable
internal fun DeveloperOptionsSettingsPage(
    state: HomeSettingsUiState,
    onAlwaysShowUpdateCardChange: (Boolean) -> Unit,
    onOpenDisplayDensity: () -> Unit,
    onTriggerCrash: () -> Unit,
) {
    SettingsSection(stringResource(R.string.developer_options)) {
        SettingSwitchItem(
            title = stringResource(R.string.always_show_update_card),
            summary = stringResource(R.string.simulated_update_data),
            checked = state.alwaysShowUpdateCard,
            iconRes = R.drawable.ic_security_update,
            onCheckedChange = onAlwaysShowUpdateCardChange,
        )
        SettingNavigationItem(
            title = stringResource(R.string.application_dpi),
            summary = stringResource(R.string.display_density),
            valueText = "${state.displayDensityPercent}%",
            iconRes = R.drawable.ic_fullscreen,
            onClick = onOpenDisplayDensity,
        )
        SettingNavigationItem(
            title = stringResource(R.string.trigger_crash),
            summary = stringResource(R.string.trigger_crash_summary),
            iconRes = R.drawable.ic_bug_report,
            onClick = onTriggerCrash,
        )
    }
}
