package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.subscription

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.model.SubscriptionItem
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.ui.component.ArtistItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.LoadMoreFooter
import io.github.daisukikaffuchino.han1meviewer.ui.component.PaginationPager
import io.github.daisukikaffuchino.han1meviewer.ui.component.VideoCardItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyVerticalGrid
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakeArtists
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakeVideos
import io.github.daisukikaffuchino.han1meviewer.ui.screen.rememberVideoGridColumns
import io.github.daisukikaffuchino.han1meviewer.ui.theme.ArtistIconSize
import io.github.daisukikaffuchino.han1meviewer.ui.theme.SpacingNormal
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 订阅页面 Content 层。纯 UI，不持有 ViewModel。
 *
 * 接收 [SubscriptionUiState] + [SubscriptionEvent] 回调，
 * 负责顶部作者横向列表、视频网格、加载更多触发等 UI 渲染。
 *
 * @param uiState 页面 UI 状态
 * @param onEvent 用户事件回调
 * @param gridState LazyGrid 滚动状态（UI 框架层）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionContent(
    uiState: SubscriptionUiState,
    onEvent: (SubscriptionEvent) -> Unit,
    gridState: LazyGridState,
    artistRows: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthPx = windowInfo.containerSize.width
    val screenWidthDp = with(density) { screenWidthPx.toDp() }
    val videoColumns = rememberVideoGridColumns()
    val artistColumns = maxOf(
        3,
        ((screenWidthDp + SpacingNormal) / (ArtistIconSize + SpacingNormal)).toInt()
    )
    val pageSize = 60
    val searchPagination = SettingsRepository.searchPagination
    val scope = rememberCoroutineScope()

    LaunchedEffect(gridState, uiState.videos.size, searchPagination) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .map { it.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (!searchPagination &&
                    lastVisibleIndex != null &&
                    lastVisibleIndex >= uiState.videos.size - 4 &&
                    uiState.videos.size >= uiState.currentPage * pageSize &&
                    uiState.canLoadMore
                ) {
                    onEvent(SubscriptionEvent.OnLoadMore)
                }
            }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(videoColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = SpacingNormal),
            horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            item(span = { GridItemSpan(videoColumns) }) {
                AnimatedContent(
                    targetState = uiState.artists,
                    label = "artist-animation",
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    }
                ) { artists ->
                    ArtistListSection(
                        artists = artists,
                        artistRows = artistRows,
                        artistColumns = artistColumns,
                        onClickArtist = {
                            onEvent(SubscriptionEvent.OnClickArtist(it))
                        },
                        onLongClickArtist = {
                            onEvent(SubscriptionEvent.OnLongClickArtist(it))
                        },
                    )
                }
            }

            item(span = { GridItemSpan(videoColumns) }) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            items(
                items = uiState.videos,
                key = { it.videoCode }
            ) { video ->
                Box(
                    modifier = Modifier.animateItem()
                ) {
                    VideoCardItem(
                        videoItem = video,
                        onClickVideosItem = {
                            onEvent(
                                SubscriptionEvent.OnClickVideo(video.videoCode)
                            )
                        },
                        onLongClickVideosItem = { _, _ -> },
                    )
                }
            }
            if (uiState.videos.isNotEmpty()) {
                item(span = { GridItemSpan(videoColumns) }) {
                    if (searchPagination) {
                        PaginationPager(
                            currentPage = uiState.currentPage,
                            totalPages = uiState.maxPage,
                            onPageSelected = { page ->
                                onEvent(SubscriptionEvent.OnGoToPage(page))
                                scope.launch { gridState.scrollToItem(0) }
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    } else {
                        LoadMoreFooter(
                            state = PagedUiState.Success(emptyList<String>()),
                            isLoadingMore = uiState.canLoadMore,
                            loadedPage = uiState.currentPage
                        )
                    }
                }
            }
        }
    }
}

/**
 * 已订阅作者横向列表区域。
 */
@Composable
private fun ArtistListSection(
    artists: List<SubscriptionItem>,
    artistRows: Int,
    artistColumns: Int,
    onClickArtist: (String) -> Unit,
    onLongClickArtist: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val artistColumnCount = maxOf(1, (artists.size + artistRows - 1) / artistRows)
    val showArtistOverflowHint by remember(scrollState, artistColumnCount, artistColumns) {
        derivedStateOf {
            artistColumnCount > artistColumns && scrollState.value < scrollState.maxValue
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.subscribed_artists_count, artists.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AnimatedVisibility(visible = showArtistOverflowHint) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.swipe_more),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(16.dp)
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(end = 28.dp, bottom = 8.dp)
            ) {
                repeat(artistColumnCount) { columnIndex ->
                    Column(
                        modifier = Modifier.width(ArtistIconSize),
                        verticalArrangement = Arrangement.spacedBy(SpacingNormal)
                    ) {
                        repeat(artistRows) { rowIndex ->
                            val itemIndex = columnIndex * artistRows + rowIndex
                            val artist = artists.getOrNull(itemIndex)
                            if (artist != null) {
                                ArtistItem(
                                    artist = artist,
                                    onClickArtist = { onClickArtist(artist.artistName) },
                                    onLongClickArtist = { onLongClickArtist(artist.artistName) },
                                )
                            } else {
                                Spacer(modifier = Modifier.width(ArtistIconSize))
                            }
                        }
                    }
                }
            }
            if (showArtistOverflowHint) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(32.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background,
                                )
                            )
                        )
                )
            }
        }
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showBackground = true)
@Composable
private fun PreviewSubscriptionContent() {
    MaterialTheme {
        SubscriptionContent(
            uiState = SubscriptionUiState(
                artists = fakeArtists,
                videos = fakeVideos,
            ),
            onEvent = {},
            gridState = LazyGridState(),
            artistRows = 1,
        )
    }
}
