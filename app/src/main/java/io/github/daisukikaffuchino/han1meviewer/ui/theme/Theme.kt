package io.github.daisukikaffuchino.han1meviewer.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.kyant.m3color.dynamiccolor.ColorSpec
import com.kyant.m3color.dynamiccolor.DynamicScheme
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeTonalSpot
import io.github.daisukikaffuchino.han1meviewer.Preferences

private val MomoPink = Color(0xFFF596AA)
private val MomoGreen = Color(0xFF8BC34A)
private val MomoYellow = Color(0xFFFFF59D)
private val MomoBlue = Color(0xFF03A9F4)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HanimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val preset = ThemeColorPreset.fromKey(Preferences.themeColor)
    val keyColor = if (
        preset == ThemeColorPreset.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        colorResource(android.R.color.system_accent1_500)
    } else {
        preset.expressiveSeedColor()
    }
    val colorScheme = expressiveColorScheme(
        keyColor = keyColor,
        isDark = darkTheme,
        contrastLevel = if (preset == ThemeColorPreset.HIGH_CONTRAST) 1.0 else 0.0,
    )
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
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

private fun ThemeColorPreset.expressiveSeedColor(): Color = when (this) {
    ThemeColorPreset.SYSTEM, ThemeColorPreset.DEFAULT, ThemeColorPreset.PINK,
    ThemeColorPreset.HIGH_CONTRAST -> MomoPink
    ThemeColorPreset.BLUE -> MomoBlue
    ThemeColorPreset.LIGHT_GREEN -> MomoGreen
    ThemeColorPreset.PURPLE -> Color(0xFF9C6ADE)
    ThemeColorPreset.ORANGE -> Color(0xFFE76F3C)
    ThemeColorPreset.TEAL -> Color(0xFF009688)
    ThemeColorPreset.YELLOW -> MomoYellow
}

@Composable
@Stable
private fun expressiveColorScheme(
    keyColor: Color,
    isDark: Boolean,
    contrastLevel: Double,
    animationSpec: AnimationSpec<Color> = spring(),
): ColorScheme {
    val scheme = remember(keyColor, isDark, contrastLevel) {
        SchemeTonalSpot(
            Hct.fromInt(keyColor.toArgb()),
            isDark,
            contrastLevel,
            ColorSpec.SpecVersion.SPEC_2021,
            DynamicScheme.Platform.PHONE,
        )
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
