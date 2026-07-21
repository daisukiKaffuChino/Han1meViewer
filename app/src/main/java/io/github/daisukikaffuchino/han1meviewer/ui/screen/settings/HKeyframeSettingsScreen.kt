package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSliderItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingSwitchItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsAnimatedVisibility
import io.github.daisukikaffuchino.han1meviewer.ui.component.animatedSegmentedSection
import io.github.daisukikaffuchino.han1meviewer.ui.component.segmentedGroup
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
) {
    LazyColumn(
        enableItemAnimation = false,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segmentedGroup {
            SettingSwitchItem(
                title = stringResource(R.string.h_keyframes_enable),
                summary = state.hKeyframesSummary,
                checked = state.hKeyframesEnable,
                iconRes = R.drawable.baseline_h_24,
                onCheckedChange = onHKeyframesEnableChange,
            )
        }

        animatedSegmentedSection(
            visible = state.hKeyframesEnable,
            titleRes = R.string.manage,
        ) {
            SettingNavigationItem(
                title = stringResource(R.string.h_keyframe_manage),
                iconRes = R.drawable.baseline_format_list_bulleted_24,
                onClick = onOpenHKeyframeManage,
            )
        }

        animatedSegmentedSection(
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

        animatedSegmentedSection(
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
