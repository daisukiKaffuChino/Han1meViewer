package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.VideoGridScreen
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.MyListViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist.LocalVideoListViewModel

@Composable
fun FavVideoRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val isLoggedIn by SettingsRepository.loginStateFlow.collectAsStateWithLifecycle()
    if (isLoggedIn) {
        val viewModel: MyListViewModel = viewModel()
        val fav = viewModel.fav
        val items = fav.favVideoFlow.collectAsStateWithLifecycle().value
        val state = fav.favVideoStateFlow.collectAsStateWithLifecycle().value
        val loadedPageCount = fav.loadedPageCount.collectAsStateWithLifecycle().value
        val isLoadingMore = fav.isLoadingMore.collectAsStateWithLifecycle().value

        VideoGridScreen(
            items = items,
            state = state,
            deleteStateFlow = fav.deleteMyFavVideoFlow,
            loadedPageCount = loadedPageCount,
            isLoadingMore = isLoadingMore,
            titleRes = R.string.fav_video,
            helpMessageRes = R.string.long_press_to_cancel_fav,
            deleteTitleRes = R.string.delete_fav,
            onBack = onBack,
            onOpenVideo = { onNavigateToVideo(it.videoCode) },
            onDeleteItem = { item ->
                val position = items.indexOfFirst { it.videoCode == item.videoCode }
                if (position >= 0) fav.deleteMyFavVideo(item.videoCode, position)
            },
            onRefresh = {
                fav.favVideoPage = 1
                fav.clearMyListItems()
                fav.getMyFavVideoItems(SettingsRepository.savedUserId, 1)
                fav.favVideoPage = 2
            },
            onLoadMore = {
                val page = fav.favVideoPage
                fav.getMyFavVideoItems(SettingsRepository.savedUserId, page)
                fav.favVideoPage = page + 1
            },
        )
    } else {
        LocalVideoListRoute(
            isFavoriteMode = true,
            onBack = onBack,
            onNavigateToVideo = onNavigateToVideo,
        )
    }
}

@Composable
fun WatchLaterRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val isLoggedIn by SettingsRepository.loginStateFlow.collectAsStateWithLifecycle()
    if (isLoggedIn) {
        val viewModel: MyListViewModel = viewModel()
        val wl = viewModel.watchLater
        val items = wl.watchLaterFlow.collectAsStateWithLifecycle().value
        val state = wl.watchLaterStateFlow.collectAsStateWithLifecycle().value
        val loadedPageCount = wl.loadedPageCount.collectAsStateWithLifecycle().value
        val isLoadingMore = wl.isLoadingMore.collectAsStateWithLifecycle().value

        VideoGridScreen(
            items = items,
            state = state,
            deleteStateFlow = wl.deleteMyWatchLaterFlow,
            loadedPageCount = loadedPageCount,
            isLoadingMore = isLoadingMore,
            titleRes = R.string.watch_later,
            helpMessageRes = R.string.long_press_to_cancel_watch_later,
            deleteTitleRes = R.string.delete_watch_later,
            onBack = onBack,
            onOpenVideo = { onNavigateToVideo(it.videoCode) },
            onDeleteItem = { item ->
                val position = items.indexOfFirst { it.videoCode == item.videoCode }
                if (position >= 0) wl.deleteMyWatchLater(item.videoCode, position)
            },
            onRefresh = {
                wl.watchLaterPage = 1
                wl.clearMyListItems()
                wl.getMyWatchLaterItems(1)
                wl.watchLaterPage = 2
            },
            onLoadMore = {
                val page = wl.watchLaterPage
                wl.getMyWatchLaterItems(page)
                wl.watchLaterPage = page + 1
            },
        )
    } else {
        LocalVideoListRoute(
            isFavoriteMode = false,
            onBack = onBack,
            onNavigateToVideo = onNavigateToVideo,
        )
    }
}

@Composable
private fun LocalVideoListRoute(
    isFavoriteMode: Boolean,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: LocalVideoListViewModel = viewModel {
        LocalVideoListViewModel(isFavoriteMode)
    }
    val items by viewModel.items.collectAsStateWithLifecycle()
    val state by viewModel.itemsStateFlow.collectAsStateWithLifecycle()
    val deleteStateFlow = viewModel.deleteFlow
    val loadedPageCount by viewModel.loadedPageCount.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

    VideoGridScreen(
        items = items,
        state = state,
        deleteStateFlow = deleteStateFlow,
        loadedPageCount = loadedPageCount,
        isLoadingMore = isLoadingMore,
        titleRes = if (isFavoriteMode) R.string.fav_video else R.string.watch_later,
        helpMessageRes = if (isFavoriteMode) {
            R.string.long_press_to_cancel_fav
        } else {
            R.string.long_press_to_cancel_watch_later
        },
        deleteTitleRes = if (isFavoriteMode) R.string.delete_fav else R.string.delete_watch_later,
        onBack = onBack,
        onOpenVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteItem = { item ->
            val position = items.indexOfFirst { it.videoCode == item.videoCode }
            if (position >= 0) viewModel.deleteItem(item.videoCode, position)
        },
        onRefresh = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() },
    )
}
