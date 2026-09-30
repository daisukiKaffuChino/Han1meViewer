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
internal fun AppearanceSettingsPage(
    state: HomeSettingsUiState,
    onUseDynamicColorChange: (Boolean) -> Unit,
    onThemeAccentColorChange: (Int) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onAppPaletteStyleChange: (Int) -> Unit,
    onOpenAppLanguage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.small),
    ) {
        SettingsSection(stringResource(R.string.accent_color)) {
        SettingSwitchItem(
            title = stringResource(R.string.dynamic_color_title),
            summary = stringResource(R.string.dynamic_color_summary),
            checked = state.useDynamicColor,
            enabled = state.dynamicColorEnabled,
            iconRes = R.drawable.ic_palette,
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
        SettingsSection(stringResource(R.string.app_lang)) {
        SettingNavigationItem(
            title = stringResource(R.string.app_lang),
            summary = stringResource(R.string.app_lang_sum),
            valueText = state.appLanguageLabel,
            iconRes = R.drawable.ic_setting_lang,
            onClick = onOpenAppLanguage,
        )
        }
    }
}
