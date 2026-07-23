package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.myplaylist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.ui.component.LoadMoreFooter
import io.github.daisukikaffuchino.han1meviewer.ui.component.PageContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.EmptyContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.ErrorContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyVerticalGrid
import io.github.daisukikaffuchino.han1meviewer.ui.screen.getColumnCount

/**
 * 播放列表页 Content 层。纯 UI，不持有 ViewModel。
 *
 * 接收 [PlaylistUiState] + [PlaylistEvent] 回调，负责网格展示和动画切换。
 *
 * @param uiState 页面 UI 状态
 * @param onEvent 用户事件回调
 * @param rawState 原始网络状态（用于 Error 的重试和空状态判断）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistContent(
    uiState: PlaylistUiState,
    onEvent: (PlaylistEvent) -> Unit,
    rawState: WebsiteState<Playlists>,
) {
    val gridState = rememberLazyGridState()
    val noMore = uiState.noMorePlaylists
    val loadingMore = uiState.isLoadingMore

    LaunchedEffect(gridState, noMore, loadingMore) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 3 && uiState.playlists.isNotEmpty()
        }.collect { shouldLoad ->
            if (shouldLoad && !loadingMore && !noMore) {
                onEvent(PlaylistEvent.OnLoadMore)
            }
        }
    }

    AnimatedContent(
        targetState = rawState,
        label = "playlist-content-animation",
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
        }
    ) {
        PageContent(
            isLoading = rawState is WebsiteState.Loading,
            isError = rawState is WebsiteState.Error,
            isEmpty = rawState is WebsiteState.Success &&
                rawState.info.playlists.isEmpty() && uiState.playlists.isEmpty(),
            onRetry = { onEvent(PlaylistEvent.OnRefresh) },
            error = {
                ErrorContent(
                    message = stringResource(
                        R.string.load_failed_with_reason,
                        (rawState as WebsiteState.Error).throwable.message.orEmpty(),
                    ),
                    onRetry = { onEvent(PlaylistEvent.OnRefresh) },
                )
            },
            empty = { EmptyContent(stringResource(R.string.empty_content)) },
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(getColumnCount(180)),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.playlists) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        modifier = Modifier.height(140.dp)
                    ) {
                        onEvent(
                            PlaylistEvent.OnPlaylistClick(
                                playlist.listCode,
                                playlist.title
                            )
                        )
                    }
                }
                if (uiState.playlists.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LoadMoreFooter(
                            state = if (uiState.noMorePlaylists) PageLoadingState.NoMoreData
                            else if (uiState.isLoadingMore) PageLoadingState.Loading
                            else PageLoadingState.Success(Unit),
                            loadedPage = uiState.playlistPage - 1,
                            isLoadingMore = uiState.isLoadingMore
                        )
                    }
                }
            }
        }
    }
}
