package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.myplaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.ui.component.CardContainerSurface
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakePlaylists
import io.github.daisukikaffuchino.han1meviewer.ui.screen.RetryableImage
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.ui.theme.shapeByInteraction

/**
 * 播放列表项组件。
 *
 * 以卡片形式展示播放列表封面、标题和视频数量，支持点击交互。
 *
 * @param playlist 播放列表数据
 * @param modifier 修饰符
 * @param onClick 点击回调，为 null 时不可点击
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistItem(
    playlist: Playlists.Playlist,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val pressed by interactionSource.collectIsPressedAsState()
    val cardShape = shapeByInteraction(
        shapes = HanimeDefaults.largerShapes(),
        pressed = pressed,
        animationSpec = HanimeDefaults.shapesDefaultAnimationSpec,
    )
    CardContainerSurface(
        modifier = modifier
            .width(180.dp),
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier.combinedClickable(
                enabled = onClick != null,
                interactionSource = interactionSource,
                indication = indication,
                onClick = { onClick?.invoke() },
                onLongClick = {},
            ),
        ) {
            Box(modifier = Modifier.height(100.dp)) {
                RetryableImage(
                    model = playlist.coverUrl ?: "",
                    contentDescription = playlist.title,
                    placeholder = painterResource(R.drawable.h_chan_loading),
                    error = painterResource(R.drawable.h_chan_load_failed),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.video_count, playlist.total),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistItemPreview() {
    ComponentPreview {
        PlaylistItem(
            playlist = fakePlaylists.first(),
            modifier = Modifier.height(140.dp),
        )
    }
}
