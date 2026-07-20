@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.immediateClickable
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.AppPaletteStyle
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.ui.theme.ThemeAccentColor
import io.github.daisukikaffuchino.han1meviewer.ui.theme.animatedShape
import io.github.daisukikaffuchino.han1meviewer.ui.theme.expressiveColorScheme

@Composable
fun ThemeAccentColorPicker(
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember { ThemeAccentColor.entries.toList() }
    PickerContainer(
        title = stringResource(R.string.preset_color_scheme),
        description = stringResource(R.string.preset_color_scheme_summary),
        modifier = modifier,
    ) {
        items(items = options, key = { it.id }) { option ->
            AccentColorItem(
                option = option,
                selected = selectedId == option.id,
                onClick = { onSelect(option.id) },
            )
        }
    }
}

@Composable
fun DarkModePicker(
    selectedValue: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemDark = isSystemInDarkTheme()
    val options = listOf(
        DarkModeOption(
            value = "follow_system",
            title = stringResource(R.string.follow_system),
            icon = Icons.Outlined.SettingsBrightness,
            dark = systemDark,
        ),
        DarkModeOption(
            value = "always_off",
            title = stringResource(R.string.always_off),
            icon = Icons.Outlined.LightMode,
            dark = false,
        ),
        DarkModeOption(
            value = "always_on",
            title = stringResource(R.string.always_on),
            icon = Icons.Outlined.DarkMode,
            dark = true,
        ),
    )
    PickerContainer(
        title = stringResource(R.string.dark_theme),
        description = stringResource(R.string.dark_mode_picker_summary),
        modifier = modifier,
    ) {
        items(options, key = DarkModeOption::value) { option ->
            DarkModeItem(
                option = option,
                selected = selectedValue == option.value,
                onClick = { onSelect(option.value) },
            )
        }
    }
}

@Composable
fun AppPalettePicker(
    selectedId: Int,
    accentColorId: Int,
    dynamicColor: Boolean,
    darkMode: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember { AppPaletteStyle.entries.toList() }
    val isDark = when (darkMode) {
        "always_off" -> false
        "always_on" -> true
        else -> isSystemInDarkTheme()
    }
    val accentColor = ThemeAccentColor.fromId(accentColorId)
    val keyColor = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(android.R.color.system_accent1_500)
    } else {
        accentColor.colors.first()
    }
    PickerContainer(
        title = stringResource(R.string.palette_style),
        description = stringResource(R.string.palette_style_summary),
        modifier = modifier,
    ) {
        items(items = options, key = { it.id }) { style ->
            val previewScheme = expressiveColorScheme(
                keyColor = keyColor,
                isDark = isDark,
                style = style,
            )
            PaletteStyleItem(
                style = style,
                colors = listOf(
                    previewScheme.primary,
                    previewScheme.secondary,
                    previewScheme.tertiary,
                    previewScheme.tertiaryContainer,
                    previewScheme.secondaryContainer,
                    previewScheme.primaryContainer,
                ),
                selected = selectedId == style.id,
                onClick = { onSelect(style.id) },
            )
        }
    }
}

@Composable
private fun PickerContainer(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = HanimeDefaults.Colors.Container,
        shape = HanimeDefaults.defaultShape,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = HanimeDefaults.settingsItemHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawFadedEdge(16.dp, leftEdge = true)
                        drawFadedEdge(16.dp, leftEdge = false)
                    },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun AccentColorItem(
    option: ThemeAccentColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else (-1).dp,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "accent-color-border",
    )
    PickerOption(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = borderWidth,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large,
                ),
        ) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(58.dp)
                    .clip(CircleShape),
            ) {
                drawArc(option.colors[1], 180f, 180f, true)
                drawArc(option.colors[2], 0f, 90f, true)
                drawArc(option.colors[3], 90f, 90f, true)
            }
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DarkModeItem(
    option: DarkModeOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else (-1).dp,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "dark-mode-border",
    )
    PickerOption(onClick = onClick) {
        val background = if (option.dark) Color(0xFF1B1B1F) else Color(0xFFFFFBFF)
        val foreground = if (option.dark) Color(0xFFE5E1E6) else Color(0xFF1B1B1F)
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(MaterialTheme.shapes.large)
                .background(background)
                .border(
                    width = borderWidth,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large,
                ),
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Text(
            text = option.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PaletteStyleItem(
    style: AppPaletteStyle,
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else (-1).dp,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "palette-style-border",
    )
    PickerOption(onClick = onClick) {
        Column(
            modifier = Modifier
                .width(70.dp)
                .clip(MaterialTheme.shapes.large)
                .border(
                    width = borderWidth,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large,
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            colors.forEach { color ->
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(color),
                )
            }
        }
        Text(
            text = style.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PickerOption(
    onClick: () -> Unit,
    shapes: ButtonShapes = HanimeDefaults.shapes(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = animatedShape(shapes, interactionSource)
    Column(
        modifier = Modifier
            .width(106.dp)
            .clip(shape)
            .immediateClickable(
                interactionSource = interactionSource,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onClick()
                },
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

private data class DarkModeOption(
    val value: String,
    val title: String,
    val icon: ImageVector,
    val dark: Boolean,
)

private fun ContentDrawScope.drawFadedEdge(edgeWidth: androidx.compose.ui.unit.Dp, leftEdge: Boolean) {
    val edgeWidthPx = edgeWidth.toPx()
    drawRect(
        topLeft = Offset(if (leftEdge) 0f else size.width - edgeWidthPx, 0f),
        size = Size(edgeWidthPx, size.height),
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startX = if (leftEdge) 0f else size.width,
            endX = if (leftEdge) edgeWidthPx else size.width - edgeWidthPx,
        ),
        blendMode = BlendMode.DstIn,
    )
}

@Preview(showBackground = true)
@Composable
private fun AppearancePickersPreview() {
    ComponentPreview {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeAccentColorPicker(0, {})
            DarkModePicker("follow_system", {})
            AppPalettePicker(1, 0, false, "follow_system", {})
        }
    }
}
