package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageState
import io.github.daisukikaffuchino.han1meviewer.logic.AppUpdateState
import io.github.daisukikaffuchino.han1meviewer.logic.state.dataOrNull
import io.github.daisukikaffuchino.han1meviewer.ui.component.PageContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.PullRefreshOverlay
import io.github.daisukikaffuchino.han1meviewer.ui.component.isFirstPageEmpty
import io.github.daisukikaffuchino.han1meviewer.ui.component.isFirstPageError
import io.github.daisukikaffuchino.han1meviewer.ui.component.isFirstPageLoading
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.HomePageTopBar
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.AppUpdateCard
import io.github.daisukikaffuchino.han1meviewer.ui.screen.rememberRandomLoadingHint
import io.github.daisukikaffuchino.han1meviewer.util.toNetworkErrorMessageRes

/**
 * 首页容器屏幕，负责连接 ViewModel 状态与导航回调。
 *
 * @param viewModel 提供首页数据与公告数据的 ViewModel。
 * @param isDrawerOpen 侧边抽屉是否已打开。
 * @param modifier 作用于屏幕根布局的修饰符。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePageScreen(
    viewModel: HomePageViewModel,
    isDrawerOpen: Boolean,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pageState by viewModel.homePageFlow.collectAsStateWithLifecycle()
    val updateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val refreshState = rememberPullToRefreshState()
    var wasRefreshing by remember { mutableStateOf(false) }
    val loadingHint = rememberRandomLoadingHint()
    LaunchedEffect(Unit) {
        viewModel.initializeHomePage()
    }

    BackHandler(enabled = !isDrawerOpen) {
        onEvent(HomeUiEvent.ShowExitDialog)
    }

    val isCurrentlyRefreshing = (pageState as? PageState.Success)?.isRefreshing == true
    val availableUpdate = (updateState as? AppUpdateState.Available)?.info
    val forcedUpdate = availableUpdate?.takeIf { it.forceUpdate }

    LaunchedEffect(pageState) {
        val errorState = pageState as? PageState.Error
        if (wasRefreshing && errorState?.cachedInfo != null) {
            Toast.makeText(
                context,
                errorState.throwable.toNetworkErrorMessageRes(),
                Toast.LENGTH_SHORT
            ).show()
        }
        wasRefreshing = isCurrentlyRefreshing
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomePageTopBar(
                onOpenDrawer = { onEvent(HomeUiEvent.OpenDrawer) },
                onSearchClick = { onEvent(HomeUiEvent.OpenSearchPage()) },
                onNavigateToPreview = { onEvent(HomeUiEvent.NavigateToPreview) }
            )
            if (forcedUpdate != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    item(key = "forced_update_${forcedUpdate.versionCode}") {
                        AppUpdateCard(
                            updateInfo = forcedUpdate,
                            onUpdateClick = {
                                onEvent(HomeUiEvent.OpenUpdatePage(forcedUpdate.downloadUrl))
                            },
                            onIgnoreClick = {},
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullToRefresh(
                            state = refreshState,
                            isRefreshing = isCurrentlyRefreshing,
                            enabled = updateState !is AppUpdateState.Checking,
                            onRefresh = {
                                viewModel.getHomePage(isRefresh = true)
                            }
                        )
                ) {
                    PageContent(
                        isLoading = updateState is AppUpdateState.Checking || pageState.isFirstPageLoading,
                        isError = pageState.isFirstPageError,
                        isEmpty = pageState.isFirstPageError || pageState.isFirstPageEmpty,
                        errorMessage = (pageState as? PageState.Error)?.throwable
                            ?.toNetworkErrorMessageRes()
                            ?.let { stringResource(it) }
                            ?: "",
                        onRetry = { viewModel.getHomePage(isRefresh = false) },
                        loadingMessage = if (updateState is AppUpdateState.Checking) {
                            stringResource(R.string.checking_for_updates)
                        } else {
                            loadingHint
                        },
                    ) {
                        val homeData = pageState.dataOrNull

                        if (homeData != null) {
                            AnimatedContent(
                                targetState = homeData,
                                transitionSpec = {
                                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                                },
                                label = "HomeContentAnimation",
                            ) { data ->
                                HomePageContent(
                                    data = data,
                                    updateInfo = availableUpdate,
                                    onEvent = onEvent,
                                    onCloseAnnouncement = viewModel::dismissAnnouncements,
                                )
                            }
                        }
                    }

                    PullRefreshOverlay(
                        state = refreshState,
                        isRefreshing = isCurrentlyRefreshing,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
            }
        }
    }
}
