package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
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
        Column(
            verticalArrangement = Arrangement.spacedBy(
                HanimeDefaults.settingsSegmentedItemPadding,
            ),
            modifier = modifier.clip(MaterialTheme.shapes.largeIncreased),
            content = content,
        )
    }
    item { Spacer(Modifier.size(HanimeDefaults.settingsItemPadding)) }
}

fun AnimatedLazyListScope.segmentedSection(
    title: String? = null,
    content: AnimatedLazyListScope.() -> Unit,
) {
    title?.let { sectionTitle ->
        item { SettingsSectionTitle(sectionTitle) }
    }
    content()
    item { Spacer(Modifier.size(HanimeDefaults.settingsItemPadding)) }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = HanimeDefaults.screenVerticalPadding,
            vertical = HanimeDefaults.settingsItemPadding,
        ),
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
