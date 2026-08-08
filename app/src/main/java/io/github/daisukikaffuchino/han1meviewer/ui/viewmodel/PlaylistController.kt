package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.ModifiedPlaylistArgs
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.myplaylist.PlaylistUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放清单页在线/本地共用接口，使 [PlaylistScreen] 与 [PlaylistBottomSheet]
 * 可以按登录状态复用同一套 UI。
 */
interface PlaylistController {
    val myPlaylistsFlow: StateFlow<WebsiteState<Playlists>>
    val mainUiState: StateFlow<PlaylistUiState>
    val refreshCompleted: SharedFlow<Unit>
    val playlistStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>>
    val playlistFlow: StateFlow<List<HanimeInfo>>
    val playlistDesc: StateFlow<String?>
    val currentListInfo: StateFlow<Pair<String, String>?>
    val modifyPlaylistFlow: SharedFlow<WebsiteState<ModifiedPlaylistArgs>>
    val deleteFromPlaylistFlow: SharedFlow<WebsiteState<Int>>
    val createPlaylistFlow: SharedFlow<WebsiteState<Unit>>

    var currentPage: Int
    var playlistPage: Int
    val isLoadingMore: Boolean

    fun loadMyPlayList(page: Int = 1, forceReload: Boolean = false)
    fun setShowSheet(value: Boolean)
    fun setListInfo(code: String, title: String)
    fun clearCurrentList()
    fun getPlaylistItems(page: Int = 1, listCode: String, refresh: Boolean = false)
    fun getPlaylistSheetScrollState(listCode: String): PlaylistSheetScrollState
    fun updatePlaylistSheetScrollState(
        listCode: String,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    )
    fun modifyPlaylist(listCode: String, title: String, desc: String, delete: Boolean)
    fun deleteFromPlaylist(listCode: String, videoCode: String, position: Int)
    fun createPlaylist(title: String, description: String)
}
