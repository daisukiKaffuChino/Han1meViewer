package io.github.daisukikaffuchino.han1meviewer.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.ui.theme.animatedShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingSurface(
    modifier: Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor = animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            HanimeDefaults.Colors.Container
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "setting-container-color",
    ).value
    val shape = animatedShape(HanimeDefaults.shapes(), interactionSource)

    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            content = content,
        )
    } else {
        Surface(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            },
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            shape = shape,
            color = containerColor,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    summary: String?,
    iconRes: Int?,
    enabled: Boolean,
    applyContainerPadding: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    val rowModifier = if (applyContainerPadding) {
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(
                horizontal = HanimeDefaults.settingsItemHorizontalPadding,
                vertical = HanimeDefaults.settingsItemVerticalPadding,
            )
    } else {
        Modifier.fillMaxWidth().animateContentSize()
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                modifier = Modifier
                    .padding(end = HanimeDefaults.settingsItemHorizontalPadding)
                    .size(24.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    iconRes: Int? = null,
    enabled: Boolean = true,
) {
    SettingSurface(
        modifier = modifier,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
    ) {
        SettingRow(title, summary, iconRes, enabled) {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
                modifier = Modifier.padding(start = HanimeDefaults.settingsItemHorizontalPadding / 2),
            )
        }
    }
}

@Composable
fun SettingNavigationItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    valueText: String? = null,
    iconRes: Int? = null,
    enabled: Boolean = true,
) {
    SettingSurface(modifier, enabled, onClick = onClick) {
        SettingRow(title, summary, iconRes, enabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 12.dp),
            ) {
                if (!valueText.isNullOrBlank()) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
fun SettingInfoItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    valueText: String? = null,
    iconRes: Int? = null,
) {
    SettingSurface(modifier = modifier) {
        SettingRow(title, summary, iconRes, enabled = true) {
            valueText?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
fun SettingSliderItem(
    title: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    summary: String? = null,
    iconRes: Int? = null,
) {
    val totalSteps = if (step > 0) {
        ((valueRange.last - valueRange.first) / step) - 1
    } else 0
    SettingSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HanimeDefaults.settingsItemHorizontalPadding,
                    vertical = HanimeDefaults.settingsItemVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingRow(
                title = title,
                summary = summary,
                iconRes = iconRes,
                enabled = true,
                applyContainerPadding = false,
            )
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                steps = totalSteps.coerceAtLeast(0),
            )
        }
    }
}

@Composable
fun SettingChoiceItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    iconRes: Int? = null,
) {
    SettingSurface(modifier, selected = selected, onClick = onClick) {
        SettingRow(title, summary, iconRes, enabled = true) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingSwitchItemPreview() {
    ComponentPreview {
        SettingSwitchItem("启用动态取色", true, {}, summary = "Android 12 及以上可使用系统取色")
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingNavigationItemPreview() {
    ComponentPreview {
        SettingNavigationItem("播放器内核", {}, summary = "当前使用 ExoPlayer", valueText = "ExoPlayer")
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingInfoItemPreview() {
    ComponentPreview { SettingInfoItem("当前版本", summary = "Han1meViewer", valueText = "v1.0.0") }
}

@Preview(showBackground = true)
@Composable
private fun SettingSliderItemPreview() {
    ComponentPreview { SettingSliderItem("更新间隔", 7, 0..30, {}, summary = "7 天") }
}

@Preview(showBackground = true)
@Composable
private fun SettingChoiceItemPreview() {
    ComponentPreview { SettingChoiceItem("跟随系统", true, {}) }
}
