package io.github.daisukikaffuchino.han1meviewer.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.kyant.m3color.dynamiccolor.ColorSpec
import com.kyant.m3color.dynamiccolor.DynamicScheme
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeContent
import com.kyant.m3color.scheme.SchemeExpressive
import com.kyant.m3color.scheme.SchemeFidelity
import com.kyant.m3color.scheme.SchemeFruitSalad
import com.kyant.m3color.scheme.SchemeNeutral
import com.kyant.m3color.scheme.SchemeRainbow
import com.kyant.m3color.scheme.SchemeTonalSpot
import com.kyant.m3color.scheme.SchemeVibrant
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HanimeTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val themePreferences = rememberThemePreferences()
    val resolvedDarkTheme = darkTheme ?: when (themePreferences.darkMode) {
        "always_on" -> true
        "always_off" -> false
        else -> systemDarkTheme
    }
    val accentColor = ThemeAccentColor.fromId(themePreferences.accentColorId)
    val paletteStyle = AppPaletteStyle.fromId(themePreferences.paletteStyleId)
    val keyColor = if (
        themePreferences.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        colorResource(android.R.color.system_accent1_500)
    } else {
        accentColor.colors.first()
    }
    val colorScheme = expressiveColorScheme(
        keyColor = keyColor,
        isDark = resolvedDarkTheme,
        style = paletteStyle,
        contrastLevel = 0.0,
    )
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !resolvedDarkTheme
                    isAppearanceLightNavigationBars = !resolvedDarkTheme
                }
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}

@Composable
private fun rememberThemePreferences(): ThemePreferences {
    val preferences = Preferences.preferenceSp
    var snapshot by remember(preferences) { mutableStateOf(preferences.readThemePreferences()) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key != null && key in THEME_PREFERENCE_KEYS) {
                snapshot = sharedPreferences.readThemePreferences()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return snapshot
}

private fun SharedPreferences.readThemePreferences() = ThemePreferences(
    darkMode = getString(SettingsPreferenceKeys.USE_DARK_MODE, "always_off") ?: "always_off",
    useDynamicColor = getBoolean(SettingsPreferenceKeys.USE_DYNAMIC_COLOR, false),
    accentColorId = getInt(SettingsPreferenceKeys.THEME_ACCENT_COLOR, 0),
    paletteStyleId = getInt(SettingsPreferenceKeys.APP_PALETTE_STYLE, 1),
)

private data class ThemePreferences(
    val darkMode: String,
    val useDynamicColor: Boolean,
    val accentColorId: Int,
    val paletteStyleId: Int,
)

private val THEME_PREFERENCE_KEYS = setOf(
    SettingsPreferenceKeys.USE_DARK_MODE,
    SettingsPreferenceKeys.USE_DYNAMIC_COLOR,
    SettingsPreferenceKeys.THEME_ACCENT_COLOR,
    SettingsPreferenceKeys.APP_PALETTE_STYLE,
)

@Composable
@Stable
internal fun expressiveColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: AppPaletteStyle = AppPaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    animationSpec: AnimationSpec<Color> = spring(),
): ColorScheme {
    val scheme = remember(keyColor, isDark, style, contrastLevel) {
        val hct = Hct.fromInt(keyColor.toArgb())
        val specVersion = ColorSpec.SpecVersion.SPEC_2021
        val platform = DynamicScheme.Platform.PHONE
        when (style) {
            AppPaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, contrastLevel, specVersion, platform)
            AppPaletteStyle.Neutral -> SchemeNeutral(hct, isDark, contrastLevel, specVersion, platform)
            AppPaletteStyle.Vibrant -> SchemeVibrant(hct, isDark, contrastLevel, specVersion, platform)
            AppPaletteStyle.Expressive -> SchemeExpressive(hct, isDark, contrastLevel, specVersion, platform)
            AppPaletteStyle.Rainbow -> SchemeRainbow(hct, isDark, contrastLevel, specVersion, platform)
            AppPaletteStyle.FruitSalad -> SchemeFruitSalad(hct, isDark, contrastLevel, specVersion, platform)
            AppPaletteStyle.Fidelity -> SchemeFidelity(hct, isDark, contrastLevel, specVersion, platform)
            AppPaletteStyle.Content -> SchemeContent(hct, isDark, contrastLevel, specVersion, platform)
        }
    }

    return ColorScheme(
        primary = scheme.primary.toComposeColor().animate(animationSpec),
        onPrimary = scheme.onPrimary.toComposeColor().animate(animationSpec),
        primaryContainer = scheme.primaryContainer.toComposeColor().animate(animationSpec),
        onPrimaryContainer = scheme.onPrimaryContainer.toComposeColor().animate(animationSpec),
        inversePrimary = scheme.inversePrimary.toComposeColor().animate(animationSpec),
        secondary = scheme.secondary.toComposeColor().animate(animationSpec),
        onSecondary = scheme.onSecondary.toComposeColor().animate(animationSpec),
        secondaryContainer = scheme.secondaryContainer.toComposeColor().animate(animationSpec),
        onSecondaryContainer = scheme.onSecondaryContainer.toComposeColor().animate(animationSpec),
        tertiary = scheme.tertiary.toComposeColor().animate(animationSpec),
        onTertiary = scheme.onTertiary.toComposeColor().animate(animationSpec),
        tertiaryContainer = scheme.tertiaryContainer.toComposeColor().animate(animationSpec),
        onTertiaryContainer = scheme.onTertiaryContainer.toComposeColor().animate(animationSpec),
        background = scheme.background.toComposeColor().animate(animationSpec),
        onBackground = scheme.onBackground.toComposeColor().animate(animationSpec),
        surface = scheme.surface.toComposeColor().animate(animationSpec),
        onSurface = scheme.onSurface.toComposeColor().animate(animationSpec),
        surfaceVariant = scheme.surfaceVariant.toComposeColor().animate(animationSpec),
        onSurfaceVariant = scheme.onSurfaceVariant.toComposeColor().animate(animationSpec),
        surfaceTint = scheme.surfaceTint.toComposeColor().animate(animationSpec),
        inverseSurface = scheme.inverseSurface.toComposeColor().animate(animationSpec),
        inverseOnSurface = scheme.inverseOnSurface.toComposeColor().animate(animationSpec),
        error = scheme.error.toComposeColor().animate(animationSpec),
        onError = scheme.onError.toComposeColor().animate(animationSpec),
        errorContainer = scheme.errorContainer.toComposeColor().animate(animationSpec),
        onErrorContainer = scheme.onErrorContainer.toComposeColor().animate(animationSpec),
        outline = scheme.outline.toComposeColor().animate(animationSpec),
        outlineVariant = scheme.outlineVariant.toComposeColor().animate(animationSpec),
        scrim = scheme.scrim.toComposeColor().animate(animationSpec),
        surfaceBright = scheme.surfaceBright.toComposeColor().animate(animationSpec),
        surfaceDim = scheme.surfaceDim.toComposeColor().animate(animationSpec),
        surfaceContainer = scheme.surfaceContainer.toComposeColor().animate(animationSpec),
        surfaceContainerHigh = scheme.surfaceContainerHigh.toComposeColor().animate(animationSpec),
        surfaceContainerHighest = scheme.surfaceContainerHighest.toComposeColor().animate(animationSpec),
        surfaceContainerLow = scheme.surfaceContainerLow.toComposeColor().animate(animationSpec),
        surfaceContainerLowest = scheme.surfaceContainerLowest.toComposeColor().animate(animationSpec),
        primaryFixed = scheme.primaryFixed.toComposeColor().animate(animationSpec),
        primaryFixedDim = scheme.primaryFixedDim.toComposeColor().animate(animationSpec),
        onPrimaryFixed = scheme.onPrimaryFixed.toComposeColor().animate(animationSpec),
        onPrimaryFixedVariant = scheme.onPrimaryFixedVariant.toComposeColor().animate(animationSpec),
        secondaryFixed = scheme.secondaryFixed.toComposeColor().animate(animationSpec),
        secondaryFixedDim = scheme.secondaryFixedDim.toComposeColor().animate(animationSpec),
        onSecondaryFixed = scheme.onSecondaryFixed.toComposeColor().animate(animationSpec),
        onSecondaryFixedVariant = scheme.onSecondaryFixedVariant.toComposeColor().animate(animationSpec),
        tertiaryFixed = scheme.tertiaryFixed.toComposeColor().animate(animationSpec),
        tertiaryFixedDim = scheme.tertiaryFixedDim.toComposeColor().animate(animationSpec),
        onTertiaryFixed = scheme.onTertiaryFixed.toComposeColor().animate(animationSpec),
        onTertiaryFixedVariant = scheme.onTertiaryFixedVariant.toComposeColor().animate(animationSpec),
    )
}

private fun Int.toComposeColor(): Color = Color(this)

@Composable
private fun Color.animate(animationSpec: AnimationSpec<Color>): Color =
    animateColorAsState(this, animationSpec, label = "theme-color").value

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
