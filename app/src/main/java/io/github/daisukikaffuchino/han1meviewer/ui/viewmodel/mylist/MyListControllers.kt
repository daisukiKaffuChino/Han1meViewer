package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 稍后再看列表的在线/本地共用接口，供路由层按登录状态切换实现。
 */
interface WatchLaterListController {
    val watchLaterStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>>
    val watchLaterFlow: StateFlow<List<HanimeInfo>>
    val deleteMyWatchLaterFlow: SharedFlow<WebsiteState<Boolean>>
    val loadedPageCount: StateFlow<Int>
    val isLoadingMore: StateFlow<Boolean>
    var watchLaterPage: Int

    fun clearMyListItems()
    fun getMyWatchLaterItems(page: Int)
    fun deleteMyWatchLater(videoCode: String, position: Int)
}

/**
 * 我喜欢的影片列表的在线/本地共用接口，供路由层按登录状态切换实现。
 */
interface FavVideoListController {
    val favVideoStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>>
    val favVideoFlow: StateFlow<List<HanimeInfo>>
    val deleteMyFavVideoFlow: SharedFlow<WebsiteState<Boolean>>
    val loadedPageCount: StateFlow<Int>
    val isLoadingMore: StateFlow<Boolean>
    var favVideoPage: Int

    fun clearMyListItems()
    fun getMyFavVideoItems(userId: String, page: Int)
    fun deleteMyFavVideo(videoCode: String, position: Int)
}
