package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.getHanimeShareText
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoItemType
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.SearchRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.navigateSafely
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakeVideosItem
import io.github.daisukikaffuchino.han1meviewer.ui.screen.RetryableImage
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.ui.theme.shapeByInteraction
import io.github.daisukikaffuchino.han1meviewer.ui.util.rememberCopyTextToClipboard
import io.github.daisukikaffuchino.han1meviewer.util.DisplayTextLocalizer
import io.github.daisukikaffuchino.utils.SonnerToast


/**
 * 标准视频卡片项组件。
 *
 * 展示视频封面、标题等信息，支持水平和垂直两种布局。
 * 长按会弹出上下文菜单。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun VideoCardItem(
    modifier: Modifier = Modifier,
    videoItem: VideoItemType,
    isHorizontalCard: Boolean = true,
    isWatched: Boolean = false,
    isPlaying: Boolean = false,
    onClickVideosItem: (String) -> Unit,
    onLongClickVideosItem: (String, String) -> Unit,
) {
    val textFontSize = dimensionResource(id = R.dimen.video_view_and_time_and_duration).value.sp
    val iconSize = dimensionResource(id = R.dimen.view_view_and_time_icon_size)
    val imageAspectRatio = if (isHorizontalCard) 16f / 9f else 3f / 4f
    val context = LocalContext.current
    val copyTextToClipboard = rememberCopyTextToClipboard()
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val pressed by interactionSource.collectIsPressedAsState()
    var showContextMenu by remember { mutableStateOf(false) }
    val currentArtist = videoItem.currentArtist?.takeIf { it.isNotBlank() }
    val cardShape = shapeByInteraction(
        shapes = HanimeDefaults.largerShapes(),
        pressed = pressed,
        animationSpec = HanimeDefaults.shapesDefaultAnimationSpec,
    )
    val tonalElevation by animateDpAsState(
        targetValue = if (pressed) 0.dp else 1.dp,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "video-card-elevation",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = cardShape,
        tonalElevation = tonalElevation,
        color = HanimeDefaults.Colors.Container,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        enabled = !isPlaying,
                        interactionSource = interactionSource,
                        indication = indication,
                        onClick = { onClickVideosItem(videoItem.videoCode) },
                        onLongClick = { showContextMenu = true },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageAspectRatio),
                ) {
                    RetryableImage(
                        model = videoItem.coverUrl,
                        contentDescription = videoItem.title,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(R.drawable.h_chan_loading),
                        error = painterResource(R.drawable.h_chan_load_failed),
                        contentScale = ContentScale.FillWidth,
                    )

                    if (isWatched) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.played),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // 底部半透明遮罩（播放量和时长）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x9F000000)
                                    ),
                                ),
                            )
                            .padding(horizontal = 6.dp),
                    ) {
                        videoItem.views?.let {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_play_circle_outline_24),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(iconSize),
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 1.dp),
                                text = DisplayTextLocalizer.localizeViews(it),
                                color = Color.White,
                                fontSize = textFontSize,
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        videoItem.duration?.let {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_access_time_24),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(iconSize),
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 1.dp),
                                text = it,
                                color = Color.White,
                                fontSize = textFontSize,
                            )
                        }
                    }
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_baseline_play_circle_outline_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.now_playing),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Text(
                    text = videoItem.title,
                    maxLines = 2,
                    minLines = 2,
                    style = MaterialTheme.typography.titleSmall,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                if (currentArtist != null) {
                    Text(
                        text = currentArtist,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                ) {
                    videoItem.reviews?.takeIf { it.isNotEmpty() }?.let { reviewsText ->
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_thumb_up_off_alt_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(iconSize),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = reviewsText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (!videoItem.uploadTime.isNullOrEmpty()) {
                        Text(
                            text = DisplayTextLocalizer.localizeRelativeTime(videoItem.uploadTime!!),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("复制视频信息") },
                    onClick = {
                        showContextMenu = false
                        copyTextToClipboard(
                            getHanimeShareText(
                                videoItem.title,
                                videoItem.videoCode
                            )
                        )
                        SonnerToast.success(R.string.copy_to_clipboard)
                    },
                )
                if (currentArtist != null) {
                    DropdownMenuItem(
                        text = { Text("搜索该作者所有作品") },
                        onClick = {
                            showContextMenu = false
                            (context as? MainActivity)?.navController?.navigateSafely(
                                SearchRoute(query = currentArtist)
                            )
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoCardItemPreview() {
    ComponentPreview {
        VideoCardItem(
            videoItem = fakeVideosItem,
            onClickVideosItem = {},
            onLongClickVideosItem = { _, _ -> },
            isWatched = true,
            isPlaying = true
        )
    }
}
