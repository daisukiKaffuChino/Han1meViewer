package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.logic.LocalListRepository
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.ModifiedPlaylistArgs
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.myplaylist.PlaylistUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 免登录本地播放清单 ViewModel，实现与在线版相同的 [PlaylistController] 接口。
 */
class LocalPlayListViewModel : ViewModel(), PlaylistController {

    private val _myPlaylistsFlow = MutableStateFlow<WebsiteState<Playlists>>(WebsiteState.Loading)
    override val myPlaylistsFlow: StateFlow<WebsiteState<Playlists>> = _myPlaylistsFlow.asStateFlow()

    private val _cachedMyPlayList = MutableStateFlow<List<Playlists.Playlist>>(emptyList())
    private val _showSheet = MutableStateFlow(false)
    private val _currentListInfo = MutableStateFlow<Pair<String, String>?>(null)
    override val currentListInfo: StateFlow<Pair<String, String>?> = _currentListInfo.asStateFlow()

    private val _playlistStateFlow =
        MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(PageLoadingState.Loading)
    override val playlistStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>> =
        _playlistStateFlow.asStateFlow()

    private val _playlistDesc = MutableStateFlow<String?>(null)
    override val playlistDesc: StateFlow<String?> = _playlistDesc.asStateFlow()

    private val _playlistFlow = MutableStateFlow(emptyList<HanimeInfo>())
    override val playlistFlow: StateFlow<List<HanimeInfo>> = _playlistFlow.asStateFlow()

    private val _playlistSheetScrollStates =
        MutableStateFlow<Map<String, PlaylistSheetScrollState>>(emptyMap())
    private val _refreshCompleted = MutableSharedFlow<Unit>()
    override val refreshCompleted: SharedFlow<Unit> = _refreshCompleted.asSharedFlow()
    private val _modifyPlaylistFlow = MutableSharedFlow<WebsiteState<ModifiedPlaylistArgs>>()
    override val modifyPlaylistFlow: SharedFlow<WebsiteState<ModifiedPlaylistArgs>> =
        _modifyPlaylistFlow.asSharedFlow()
    private val _deleteFromPlaylistFlow = MutableSharedFlow<WebsiteState<Int>>()
    override val deleteFromPlaylistFlow: SharedFlow<WebsiteState<Int>> =
        _deleteFromPlaylistFlow.asSharedFlow()
    private val _createPlaylistFlow = MutableSharedFlow<WebsiteState<Unit>>()
    override val createPlaylistFlow: SharedFlow<WebsiteState<Unit>> =
        _createPlaylistFlow.asSharedFlow()

    private val _isLoadingMorePlaylists = MutableStateFlow(false)
    private val _noMorePlaylists = MutableStateFlow(true)

    override var currentPage = 1
    override var playlistPage = 1
    override var isLoadingMore = false
        private set

    override val mainUiState: StateFlow<PlaylistUiState> = combine(
        _cachedMyPlayList,
        _showSheet,
        _currentListInfo,
        _isLoadingMorePlaylists,
        _noMorePlaylists,
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        PlaylistUiState(
            playlists = array[0] as List<Playlists.Playlist>,
            showSheet = array[1] as Boolean,
            selectedListCode = (array[2] as Pair<String, String>?)?.first ?: "",
            selectedListTitle = (array[2] as Pair<String, String>?)?.second ?: "",
            isLoadingMore = array[3] as Boolean,
            noMorePlaylists = array[4] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistUiState())

    init {
        viewModelScope.launch {
            LocalListRepository.observePlaylists().collect { playlists ->
                _cachedMyPlayList.value = playlists
                _myPlaylistsFlow.value = WebsiteState.Success(Playlists(playlists))
                _noMorePlaylists.value = true
                _isLoadingMorePlaylists.value = false
                _refreshCompleted.emit(Unit)
            }
        }
    }

    override fun loadMyPlayList(page: Int, forceReload: Boolean) {
        viewModelScope.launch {
            if (page == 1 || forceReload) {
                _myPlaylistsFlow.value = WebsiteState.Loading
            }
            val playlists = LocalListRepository.getPlaylistsOnce()
            _cachedMyPlayList.value = playlists
            _myPlaylistsFlow.value = WebsiteState.Success(Playlists(playlists))
            _noMorePlaylists.value = true
            _isLoadingMorePlaylists.value = false
            _refreshCompleted.emit(Unit)
        }
    }

    override fun setShowSheet(value: Boolean) {
        _showSheet.value = value
    }

    override fun setListInfo(code: String, title: String) {
        _currentListInfo.value = code to title
    }

    override fun clearCurrentList() {
        _playlistFlow.value = emptyList()
    }

    override fun getPlaylistItems(page: Int, listCode: String, refresh: Boolean) {
        if (isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            try {
                if (page == 1 || refresh) {
                    _playlistFlow.value = emptyList()
                    _playlistDesc.value = null
                    _playlistStateFlow.value = PageLoadingState.Loading
                }
                val desc = LocalListRepository.getPlaylistDesc(listCode)
                val items = LocalListRepository.getPlaylistItemsOnce(listCode)
                _playlistDesc.value = desc
                _playlistFlow.value = items
                _playlistStateFlow.value = if (items.isEmpty()) {
                    PageLoadingState.NoMoreData
                } else {
                    PageLoadingState.Success(MyListItems(items, desc))
                }
                currentPage = 2
            } finally {
                isLoadingMore = false
            }
        }
    }

    override fun getPlaylistSheetScrollState(listCode: String): PlaylistSheetScrollState =
        _playlistSheetScrollStates.value[listCode] ?: PlaylistSheetScrollState()

    override fun updatePlaylistSheetScrollState(
        listCode: String,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ) {
        if (listCode.isBlank()) return
        _playlistSheetScrollStates.update { prev ->
            prev + (
                listCode to PlaylistSheetScrollState(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                )
            )
        }
    }

    override fun modifyPlaylist(listCode: String, title: String, desc: String, delete: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (delete) {
                    LocalListRepository.deletePlaylist(listCode)
                } else {
                    LocalListRepository.updatePlaylist(listCode, title, desc)
                }
            }.onSuccess {
                _modifyPlaylistFlow.emit(WebsiteState.Success(ModifiedPlaylistArgs(title, desc, delete)))
                if (delete) {
                    _playlistStateFlow.value = PageLoadingState.Loading
                    _playlistFlow.value = emptyList()
                }
            }.onFailure {
                _modifyPlaylistFlow.emit(WebsiteState.Error(it))
            }
        }
    }

    override fun deleteFromPlaylist(listCode: String, videoCode: String, position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                LocalListRepository.removeItem(listCode, videoCode)
            }.onSuccess {
                _deleteFromPlaylistFlow.emit(WebsiteState.Success(position))
                _playlistFlow.update { prev ->
                    prev.toMutableList().apply {
                        if (position in indices) removeAt(position)
                    }
                }
            }.onFailure {
                _deleteFromPlaylistFlow.emit(WebsiteState.Error(it))
            }
        }
    }

    override fun createPlaylist(title: String, description: String) {
        viewModelScope.launch {
            runCatching {
                LocalListRepository.createPlaylist(title, description)
            }.onSuccess {
                _createPlaylistFlow.emit(WebsiteState.Success(Unit))
            }.onFailure {
                _createPlaylistFlow.emit(WebsiteState.Error(it))
            }
        }
    }
}
