package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 今日打卡卡片。
 *
 * @param today 今天的日期
 * @param count 今日已打卡次数
 * @param maxCount 每日最大打卡数
 * @param onCheckIn 打卡按钮回调
 * @param onClear 清除按钮回调
 * @param modifier 修饰符
 */
@Composable
fun TodayCheckInCard(
    modifier: Modifier = Modifier,
    today: LocalDate,
    count: Int,
    maxCount: Int = 20,
    onCheckIn: () -> Unit,
    onClear: () -> Unit,
) {
    val isMaxed = count >= maxCount

    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardDefaults.shape)
        ) {

            // 左下角装饰图标
            Icon(
                painter = painterResource(R.drawable.ic_baseline_thumb_up_off_alt_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomStart)
                    .offset(
                        x = (-18).dp,
                        y = 18.dp
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = today.format(
                            DateTimeFormatter.ofPattern("MM月dd日 EEEE")
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = if (count > 0) {
                            "${stringResource(R.string.today_checked)} $count/$maxCount ${
                                stringResource(R.string.times)
                            }"
                        } else {
                            stringResource(R.string.not_checked_yet)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                            alpha = 0.7f
                        )
                    )
                }


                // Split Button
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(50)),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        onClick = onCheckIn,
                        enabled = !isMaxed,
                        shape = RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            Modifier.width(4.dp)
                        )

                        Text(
                            if (count > 0)
                                stringResource(R.string.view_checkin)
                            else
                                stringResource(R.string.checkin)
                        )
                    }


                    if (count > 0) {

                        VerticalDivider(
                            modifier = Modifier.height(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(
                                alpha = 0.3f
                            )
                        )

                        FilledTonalIconButton(
                            onClick = onClear,
                            modifier = Modifier
                                .size(40.dp),
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                bottomStart = 4.dp,
                                topEnd = 50.dp,
                                bottomEnd = 50.dp
                            ),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_delete_24),
                                modifier = Modifier.size(width = 20.dp, height = 20.dp),
                                contentDescription = stringResource(
                                    R.string.clear_checkin
                                ),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class StatsItem(
    val icon: Int,
    val label: String,
    val value: String,
)

@Composable
fun StatsCard(
    items: List<StatsItem>,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                StatItem(
                    icon = item.icon,
                    label = item.label,
                    value = item.value,
                )
                if (index < items.lastIndex) {
                    VerticalDivider(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

/**
 * 统计项组件，在 [StatsCard] 和报表中复用。
 *
 * @param icon 图标
 * @param label 标签文字
 * @param value 统计值
 */
@Composable
fun RowScope.StatItem(
    icon: Int,
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun PreviewTodayCheckInCard() {
    TodayCheckInCard(
        today = LocalDate.now(),
        count = 3,
        onCheckIn = {},
        onClear = {},
    )
}

@Preview
@Composable
private fun PreviewStatsCard() {
    StatsCard(
        items = listOf(
            StatsItem(R.drawable.calendar_month_24px, "Monthly", "15 days"),
            StatsItem(R.drawable.alarm_24px, "Total", "42 times"),
            StatsItem(R.drawable.calendar_view_week_24px, "Streak", "7 days"),
        ),
    )
}
