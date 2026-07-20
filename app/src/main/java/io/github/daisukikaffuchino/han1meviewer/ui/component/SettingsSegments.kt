package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.AnimatedLazyListScope
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AnimatedLazyListScope.segmentedGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    item {
        SettingsSegmentedGroup(modifier = modifier, content = content)
    }
    item { Spacer(Modifier.size(HanimeDefaults.settingsItemPadding)) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSegmentedGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            HanimeDefaults.settingsSegmentedItemPadding,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.largeIncreased)
            .animateContentSize(),
        content = content,
    )
}

@Composable
fun SettingsAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        content()
    }
}

fun AnimatedLazyListScope.segmentedSection(
    @StringRes titleRes: Int? = null,
    title: String? = null,
    content: AnimatedLazyListScope.() -> Unit,
) {
    if (titleRes != null || title != null) {
        item { SettingsSectionTitle(titleRes = titleRes, title = title) }
    }
    content()
    item { Spacer(Modifier.size(HanimeDefaults.settingsItemPadding)) }
}

fun AnimatedLazyListScope.animatedSegmentedSection(
    visible: Boolean,
    @StringRes titleRes: Int? = null,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    item {
        SettingsAnimatedVisibility(visible = visible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            ) {
                if (titleRes != null || title != null) {
                    SettingsSectionTitle(titleRes = titleRes, title = title)
                }
                SettingsSegmentedGroup(content = content)
                Spacer(Modifier.size(HanimeDefaults.settingsItemPadding))
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(
    @StringRes titleRes: Int? = null,
    title: String? = null,
) {
    Text(
        text = titleRes?.let { stringResource(it) } ?: title.orEmpty(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(
                top = 12.dp,
                bottom = 8.dp,
            )
            .padding(horizontal = HanimeDefaults.screenVerticalPadding),
    )
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun SettingsSegmentedGroupPreview() {
    ComponentPreview {
        LazyColumn {
            segmentedSection(title = "显示") {
                segmentedGroup {
                    SettingSwitchItem(
                        title = "动态配色",
                        summary = "跟随系统主题颜色",
                        checked = true,
                        onCheckedChange = {},
                    )
                    SettingNavigationItem(
                        title = "深色模式",
                        valueText = "跟随系统",
                        onClick = {},
                    )
                }
            }
        }
    }
}
