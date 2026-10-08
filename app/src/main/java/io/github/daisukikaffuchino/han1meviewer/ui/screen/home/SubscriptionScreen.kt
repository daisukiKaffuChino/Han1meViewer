package io.github.daisukikaffuchino.han1meviewer.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.model.SubscriptionItem
import io.github.daisukikaffuchino.han1meviewer.logic.model.SubscriptionVideosItem
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.ui.component.ChoiceDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.IconButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.PullRefreshOverlay
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.EmptyContent
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.subscription.SubscriptionContent
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.subscription.SubscriptionEvent
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.subscription.SubscriptionUiState
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import kotlinx.coroutines.launch

/**
 * 订阅页面 Screen 层。
 *
 * 持有 [MySubscriptionsViewModel]，管理缓存、下拉刷新、加载更多等状态编排。
 * 渲染委托给 [SubscriptionContent]。
 *
 * @param navigateBack 返回回调
 * @param viewModel 订阅 ViewModel
 * @param onClickArtist 点击作者 → 跳转搜索
 * @param onLongClickArtist 长按作者 → 复制分享文本
 * @param onClickVideosItem 点击视频 → 跳转详情
 * @param onLongClickVideosItem 长按视频 → 复制分享文本
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionScreen(
    navigateBack: () -> Unit,
    viewModel: MySubscriptionsViewModel,
    onClickArtist: (String) -> Unit,
    onLongClickArtist: (String) -> Unit,
    onClickVideosItem: (String) -> Unit,
    onLongClickVideosItem: (String, String) -> Unit,
) {
    val state by viewModel.subscriptionsState.collectAsStateWithLifecycle()
    val loadedPage by viewModel.loadedPage.collectAsStateWithLifecycle()
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val cachedArtists = rememberSaveable { mutableStateOf<List<SubscriptionItem>>(emptyList()) }
    val cachedVideos =
        rememberSaveable { mutableStateOf<List<SubscriptionVideosItem>>(emptyList()) }
    val scrollBehavior = pinnedScrollBehavior(rememberTopAppBarState())
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var showArtistRowsDialog by rememberSaveable { mutableStateOf(false) }

    val refreshState = rememberPullToRefreshState()
    var isRefreshing by rememberSaveable { mutableStateOf(false) }

    val canLoadMore = viewModel.canLoadMore()
    val maxPage = (state as? UiState.Success)?.data?.maxPage ?: 1

    val showCached = state is UiState.Loading && cachedArtists.value.isNotEmpty() ||
            state is UiState.Error && cachedArtists.value.isNotEmpty()

    LaunchedEffect(state) {
        when (val s = state) {
            is UiState.Success -> {
                cachedArtists.value = s.data.subscriptions.toList()
                cachedVideos.value = s.data.subscriptionsVideos.toList()
            }

            is UiState.Loading -> {
                if (cachedArtists.value.isEmpty()) viewModel.loadMySubscriptions()
            }

            else -> Unit
        }
        if (state !is UiState.Loading) {
            isRefreshing = false
        }
    }

    val uiState = SubscriptionUiState(
        artists = cachedArtists.value,
        videos = cachedVideos.value,
        isRefreshing = isRefreshing,
        canLoadMore = canLoadMore,
        currentPage = loadedPage,
        maxPage = maxPage,
        error = (state as? UiState.Error)?.throwable,
        showCached = showCached,
    )

    ChoiceDialog(
        visible = showArtistRowsDialog,
        title = stringResource(R.string.subscription_artist_rows),
        options = (1..3).map { rows ->
            stringResource(R.string.subscription_artist_rows_option, rows) to rows.toString()
        },
        selectedValue = settings.subscriptionArtistRows.toString(),
        onDismiss = { showArtistRowsDialog = false },
        onSelect = { value ->
            showArtistRowsDialog = false
            scope.launch {
                SettingsRepository.setSubscriptionArtistRows(value.toInt())
            }
        },
    )

    val handleEvent: (SubscriptionEvent) -> Unit = { event ->
        when (event) {
            SubscriptionEvent.OnBack -> navigateBack()
            is SubscriptionEvent.OnClickArtist -> onClickArtist(event.artistName)
            is SubscriptionEvent.OnLongClickArtist -> onLongClickArtist(event.artistName)
            is SubscriptionEvent.OnClickVideo -> onClickVideosItem(event.videoCode)
            is SubscriptionEvent.OnLongClickVideo -> onLongClickVideosItem(
                event.videoCode,
                event.title
            )

            SubscriptionEvent.OnRefresh -> {
                isRefreshing = true
                viewModel.loadMySubscriptions(forceReload = true)
            }

            SubscriptionEvent.OnLoadMore -> viewModel.loadMySubscriptions()
            is SubscriptionEvent.OnGoToPage -> viewModel.goToPage(event.page)
        }
    }

    HanimeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = stringResource(R.string.my_subscribe),
        onBack = navigateBack,
        actions = {
            IconButton(onClick = { showArtistRowsDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_table_rows),
                    contentDescription = stringResource(R.string.subscription_artist_rows),
                )
            }
        },
        scrollBehavior = scrollBehavior,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = { handleEvent(SubscriptionEvent.OnRefresh) }
                )
        ) {
            when (state) {
                is UiState.Loading -> {
                    if (cachedArtists.value.isEmpty() || cachedVideos.value.isEmpty()) {
                        LoadingIndicator(Modifier.align(Alignment.Center))
                    } else {
                        SubscriptionContent(
                            uiState = uiState,
                            onEvent = handleEvent,
                            gridState = gridState,
                            artistRows = settings.subscriptionArtistRows,
                        )
                    }
                }

                is UiState.Error -> {
                    if (cachedArtists.value.isEmpty()) {
                        EmptyContent(
                            hint = stringResource(
                                R.string.load_failed_with_reason,
                                (state as UiState.Error).throwable.message.orEmpty()
                            ),
                            picRes = R.drawable.h_chan_sad
                        )
                    } else {
                        SubscriptionContent(
                            uiState = uiState,
                            onEvent = handleEvent,
                            gridState = gridState,
                            artistRows = settings.subscriptionArtistRows,
                        )
                    }
                }

                is UiState.Empty -> {
                    SubscriptionContent(
                        uiState = uiState,
                        onEvent = handleEvent,
                        gridState = gridState,
                        artistRows = settings.subscriptionArtistRows,
                    )
                }

                is UiState.Success -> {
                    SubscriptionContent(
                        uiState = uiState,
                        onEvent = handleEvent,
                        gridState = gridState,
                        artistRows = settings.subscriptionArtistRows,
                    )
                }
            }

            PullRefreshOverlay(
                state = refreshState,
                isRefreshing = isRefreshing,
            )
        }
    }
}
