package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSliderItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSwitchItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsAnimatedVisibility
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsSectionTitle
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsSegmentedGroup
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

data class HKeyframeSettingsUiState(
    val hKeyframesEnable: Boolean,
    val hKeyframesSummary: String,
    val sharedHKeyframesEnable: Boolean,
    val sharedHKeyframesUseFirst: Boolean,
    val showCommentWhenCountdown: Boolean,
    val whenCountdownRemind: Int,
    val whenCountdownRemindSummary: String,
)

@Composable
fun HKeyframeSettingsScreen(
    state: HKeyframeSettingsUiState,
    onHKeyframesEnableChange: (Boolean) -> Unit,
    onOpenHKeyframeManage: () -> Unit,
    onSharedHKeyframesEnableChange: (Boolean) -> Unit,
    onSharedHKeyframesUseFirstChange: (Boolean) -> Unit,
    onOpenSharedHKeyframeManage: () -> Unit,
    onShowCommentWhenCountdownChange: (Boolean) -> Unit,
    onWhenCountdownRemindChange: (Int) -> Unit,
    embedded: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        HKeyframeSettingsContent(
            state = state,
            showTitle = embedded,
            onHKeyframesEnableChange = onHKeyframesEnableChange,
            onOpenHKeyframeManage = onOpenHKeyframeManage,
            onSharedHKeyframesEnableChange = onSharedHKeyframesEnableChange,
            onSharedHKeyframesUseFirstChange = onSharedHKeyframesUseFirstChange,
            onOpenSharedHKeyframeManage = onOpenSharedHKeyframeManage,
            onShowCommentWhenCountdownChange = onShowCommentWhenCountdownChange,
            onWhenCountdownRemindChange = onWhenCountdownRemindChange,
        )
    }
    if (embedded) {
        content()
    } else {
        LazyColumn(
            enableItemAnimation = false,
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item { content() }
        }
    }
}

@Composable
private fun HKeyframeSettingsContent(
    state: HKeyframeSettingsUiState,
    showTitle: Boolean,
    onHKeyframesEnableChange: (Boolean) -> Unit,
    onOpenHKeyframeManage: () -> Unit,
    onSharedHKeyframesEnableChange: (Boolean) -> Unit,
    onSharedHKeyframesUseFirstChange: (Boolean) -> Unit,
    onOpenSharedHKeyframeManage: () -> Unit,
    onShowCommentWhenCountdownChange: (Boolean) -> Unit,
    onWhenCountdownRemindChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTitle) {
            SettingsSectionTitle(titleRes = R.string.h_keyframe_settings)
        }
        SettingsSegmentedGroup {
            SettingSwitchItem(
                title = stringResource(R.string.h_keyframes_enable),
                summary = state.hKeyframesSummary,
                checked = state.hKeyframesEnable,
                iconRes = R.drawable.baseline_h_24,
                onCheckedChange = onHKeyframesEnableChange,
            )
        }
        Spacer(Modifier.size(HanimeDefaults.settingsItemPadding))

        HKeyframeAnimatedSection(
            visible = state.hKeyframesEnable,
            titleRes = R.string.manage,
        ) {
            SettingNavigationItem(
                title = stringResource(R.string.h_keyframe_manage),
                iconRes = R.drawable.baseline_format_list_bulleted_24,
                onClick = onOpenHKeyframeManage,
            )
        }

        HKeyframeAnimatedSection(
            visible = state.hKeyframesEnable,
            titleRes = R.string.shared,
        ) {
            SettingSwitchItem(
                title = stringResource(R.string.shared_h_keyframes_enable),
                summary = stringResource(R.string.shared_h_keyframes_enable_tip),
                checked = state.sharedHKeyframesEnable,
                iconRes = R.drawable.baseline_share_24,
                onCheckedChange = onSharedHKeyframesEnableChange,
            )
            SettingsAnimatedVisibility(visible = state.sharedHKeyframesEnable) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        HanimeDefaults.settingsSegmentedItemPadding,
                    ),
                ) {
                    SettingSwitchItem(
                        title = stringResource(R.string.shared_h_keyframes_use_first),
                        summary = stringResource(R.string.shared_h_keyframes_use_first_tip),
                        checked = state.sharedHKeyframesUseFirst,
                        iconRes = R.drawable.baseline_share_first_24,
                        onCheckedChange = onSharedHKeyframesUseFirstChange,
                    )
                    SettingNavigationItem(
                        title = stringResource(R.string.shared_h_keyframe_manage),
                        summary = stringResource(R.string.shared_h_keyframe_manage_tip),
                        iconRes = R.drawable.baseline_online_manage_24,
                        onClick = onOpenSharedHKeyframeManage,
                    )
                }
            }
        }

        HKeyframeAnimatedSection(
            visible = state.hKeyframesEnable,
            titleRes = R.string.custom,
        ) {
            SettingSwitchItem(
                title = stringResource(R.string.show_prompt_when_countdown),
                checked = state.showCommentWhenCountdown,
                iconRes = R.drawable.baseline_count_down_24,
                onCheckedChange = onShowCommentWhenCountdownChange,
            )
            SettingSliderItem(
                title = stringResource(R.string.when_countdown_remind),
                summary = state.whenCountdownRemindSummary,
                value = state.whenCountdownRemind,
                valueRange = 5..30,
                iconRes = R.drawable.ic_baseline_alert_24,
                onValueChange = onWhenCountdownRemindChange,
            )
        }
    }
}

@Composable
private fun HKeyframeAnimatedSection(
    visible: Boolean,
    titleRes: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsAnimatedVisibility(visible = visible) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            SettingsSectionTitle(titleRes = titleRes)
            SettingsSegmentedGroup(content = content)
            Spacer(Modifier.size(HanimeDefaults.settingsItemPadding))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HKeyframeSettingsScreenPreview() {
    ComponentPreview {
        HKeyframeSettingsScreen(
            state = HKeyframeSettingsUiState(
                hKeyframesEnable = true,
                hKeyframesSummary = "开启后，播放器顶部会显示🥵",
                sharedHKeyframesEnable = true,
                sharedHKeyframesUseFirst = false,
                showCommentWhenCountdown = false,
                whenCountdownRemind = 10,
                whenCountdownRemindSummary = "将会在 10 秒前倒数计时提醒 (預設)",
            ),
            onHKeyframesEnableChange = {},
            onOpenHKeyframeManage = {},
            onSharedHKeyframesEnableChange = {},
            onSharedHKeyframesUseFirstChange = {},
            onOpenSharedHKeyframeManage = {},
            onShowCommentWhenCountdownChange = {},
            onWhenCountdownRemindChange = {},
        )
    }
}
