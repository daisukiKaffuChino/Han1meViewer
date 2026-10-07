package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daisukikaffuchino.han1meviewer.HanimeConstants
import io.github.daisukikaffuchino.han1meviewer.BuildConfig
import io.github.daisukikaffuchino.han1meviewer.HA1_GITHUB_URL
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.logic.AppUpdateState
import io.github.daisukikaffuchino.han1meviewer.logic.AppUpdateInfo
import io.github.daisukikaffuchino.han1meviewer.logic.state.dataOrNull
import io.github.daisukikaffuchino.han1meviewer.logic.state.isFirstPageEmpty
import io.github.daisukikaffuchino.han1meviewer.logic.state.isFirstPageError
import io.github.daisukikaffuchino.han1meviewer.logic.state.isFirstPageLoading
import io.github.daisukikaffuchino.han1meviewer.ui.component.PageContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.PullRefreshOverlay
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.HomePageTopBar
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.AppUpdateCard
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.AnnouncementCard
import io.github.daisukikaffuchino.han1meviewer.ui.screen.rememberRandomLoadingHint
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.util.toNetworkErrorMessageRes
import io.github.daisukikaffuchino.utils.SonnerToast

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
    showNavigationIcon: Boolean,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageState by viewModel.homePageFlow.collectAsStateWithLifecycle()
    val updateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val updateAnnouncement by viewModel.updateAnnouncement.collectAsStateWithLifecycle()
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val refreshState = rememberPullToRefreshState()
    val homeListState = rememberLazyListState()
    var wasRefreshing by remember { mutableStateOf(false) }
    val loadingHint = rememberRandomLoadingHint()
    val topBarScrollProgress by remember(homeListState) {
        derivedStateOf {
            when {
                homeListState.firstVisibleItemIndex > 0 -> 1f
                else -> (homeListState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
            }
        }
    }
    val topBarContainerColor by animateColorAsState(
        targetValue = HanimeDefaults.Colors.pageSurface.copy(
            alpha = 0.68f + (0.28f * topBarScrollProgress)
        ),
        animationSpec = tween(durationMillis = 150),
        label = "HomeTopBarContainerColor",
    )
    val density = LocalDensity.current
    val contentTopPadding = with(density) {
        WindowInsets.statusBars.getTop(this).toDp() + 72.dp
    }
    val isAVSite = SettingsRepository.baseUrl == HanimeConstants.HANIME_URL[3]
    LaunchedEffect(Unit) {
        viewModel.initializeHomePage()
    }

    BackHandler(enabled = !isDrawerOpen) {
        onEvent(HomeUiEvent.ShowExitDialog)
    }

    val isCurrentlyRefreshing = (pageState as? PagedUiState.Success)?.isRefreshing == true
    val simulatedUpdateDescription = stringResource(R.string.simulated_update_description)
    val simulatedUpdate = remember(simulatedUpdateDescription) {
        AppUpdateInfo(
            versionName = "Debug Preview",
            versionCode = Int.MAX_VALUE,
            downloadUrl = HA1_GITHUB_URL,
            updateDescription = simulatedUpdateDescription,
            forceUpdate = false,
        )
    }
    val showSimulatedUpdate = BuildConfig.DEBUG && settings.alwaysShowUpdateCard
    val availableUpdate = if (showSimulatedUpdate) {
        simulatedUpdate
    } else {
        (updateState as? AppUpdateState.Available)?.info
    }
    val forcedUpdate = availableUpdate?.takeIf { it.forceUpdate }

    LaunchedEffect(pageState) {
        val errorState = pageState as? PagedUiState.Error
        if (wasRefreshing && errorState?.cached != null) {
            SonnerToast.error(errorState.throwable.toNetworkErrorMessageRes())
        }
        wasRefreshing = isCurrentlyRefreshing
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (forcedUpdate != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = contentTopPadding,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp,
                    ),
                    verticalArrangement = Arrangement.Center,
                ) {
                    updateAnnouncement?.let { announcement ->
                        item(key = "forced_update_announcement") {
                            AnnouncementCard(
                                announcements = listOf(announcement),
                                onAnnouncementClick = { selectedAnnouncement ->
                                    onEvent(HomeUiEvent.ShowAnnouncementDialog(selectedAnnouncement))
                                },
                                onClose = null,
                            )
                        }
                    }
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
                            enabled = showSimulatedUpdate || updateState !is AppUpdateState.Checking,
                            onRefresh = {
                                viewModel.getHomePage(isRefresh = true)
                            }
                        )
                ) {
                    PageContent(
                        isLoading = (!showSimulatedUpdate && updateState is AppUpdateState.Checking) ||
                            pageState.isFirstPageLoading,
                        isError = pageState.isFirstPageError,
                        isEmpty = pageState.isFirstPageError || pageState.isFirstPageEmpty,
                        errorMessage = (pageState as? PagedUiState.Error)?.throwable
                            ?.toNetworkErrorMessageRes()
                            ?.let { stringResource(it) }
                            ?: "",
                        onRetry = { viewModel.getHomePage(isRefresh = false) },
                        loadingMessage = if (!showSimulatedUpdate && updateState is AppUpdateState.Checking) {
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
                                    updateAnnouncement = updateAnnouncement,
                                    isAVSite = isAVSite,
                                    onEvent = onEvent,
                                    onCloseAnnouncement = viewModel::dismissAnnouncements,
                                    contentTopPadding = contentTopPadding,
                                    listState = homeListState,
                                )
                            }
                        }
                    }

                    PullRefreshOverlay(
                        state = refreshState,
                        isRefreshing = isCurrentlyRefreshing,
                    )
                }
        }
        HomePageTopBar(
            onOpenDrawer = { onEvent(HomeUiEvent.OpenDrawer) },
            onSearchClick = { onEvent(HomeUiEvent.OpenSearchPage()) },
            onNavigateToPreview = { onEvent(HomeUiEvent.NavigateToPreview) },
            containerColor = topBarContainerColor,
            showNavigationIcon = showNavigationIcon,
            modifier = Modifier.zIndex(1f),
        )
    }
}
