package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.logic.DatabaseRepo
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalMylistTombstoneEntity
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 本地收藏 / 稍后观看列表 ViewModel（未登录模式）。
 *
 * 数据源为本地数据库 mylist.db，删除时按登录状态
 * 实时同步云端或记录删除墓碑（登录后由同步引擎推送）。
 *
 * @project Han1meViewer
 */
class LocalVideoListViewModel(
    private val isFavoriteMode: Boolean,
) : ViewModel() {

    private val _itemsStateFlow = MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(
        PageLoadingState.Loading
    )
    val itemsStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>> =
        _itemsStateFlow.asStateFlow()

    private val itemsFlow = (if (isFavoriteMode) {
        DatabaseRepo.LocalMylist.observeFavorites()
    } else {
        DatabaseRepo.LocalMylist.observeWatchLater()
    }).map { entities ->
        entities.map { it.toHanimeInfo() }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val items: StateFlow<List<HanimeInfo>> = itemsFlow

    val loadedPageCount: StateFlow<Int> =
        MutableStateFlow(1).asStateFlow()

    val isLoadingMore: StateFlow<Boolean> =
        MutableStateFlow(false).asStateFlow()

    private val _deleteFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val deleteFlow = _deleteFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            itemsFlow.collect { items ->
                _itemsStateFlow.value = if (items.isEmpty()) {
                    PageLoadingState.NoMoreData
                } else {
                    PageLoadingState.Success(MyListItems(items))
                }
            }
        }
    }

    fun refresh() = Unit

    fun loadMore() = Unit

    fun deleteItem(videoCode: String, position: Int) {
        viewModelScope.launch {
            val entity = DatabaseRepo.LocalMylist.findBy(videoCode)
            if (entity == null) {
                _deleteFlow.emit(WebsiteState.Error(IllegalStateException("not found")))
                return@launch
            }
            val loggedIn = SettingsRepository.isAlreadyLogin
            val synced = if (isFavoriteMode) entity.favSynced else entity.watchLaterSynced
            if (loggedIn && synced) {
                val succeeded = runCatching {
                    val state = if (isFavoriteMode) {
                        NetworkRepo.addToMyFavVideo(
                            videoCode = videoCode,
                            likeStatus = true,
                            currentUserId = SettingsRepository.savedUserId,
                            token = csrfToken,
                        ).first()
                    } else {
                        NetworkRepo.addToMyList(
                            listCode = "save",
                            videoCode = videoCode,
                            isChecked = false,
                            position = position,
                            csrfToken = csrfToken,
                        ).first()
                    }
                    state is WebsiteState.Success
                }.getOrElse { false }
                if (succeeded) {
                    removeLocalVideo(entity)
                    _deleteFlow.emit(WebsiteState.Success(true))
                } else {
                    _deleteFlow.emit(WebsiteState.Error(IllegalStateException("delete failed")))
                }
            } else {
                val tombstone = DatabaseRepo.LocalMylist.findTombstone(videoCode)
                val merged = if (isFavoriteMode) {
                    tombstone?.copy(isFav = true) ?: LocalMylistTombstoneEntity(
                        videoCode = videoCode,
                        isFav = true,
                        deletedTime = System.currentTimeMillis(),
                    )
                } else {
                    tombstone?.copy(isWatchLater = true) ?: LocalMylistTombstoneEntity(
                        videoCode = videoCode,
                        isWatchLater = true,
                        deletedTime = System.currentTimeMillis(),
                    )
                }
                DatabaseRepo.LocalMylist.upsertTombstone(merged)
                removeLocalVideo(entity)
                _deleteFlow.emit(WebsiteState.Success(true))
            }
        }
    }

    /**
     * 删除本地行，但若视频仍属于另一个列表（收藏 / 稍后观看并存），
     * 仅清除当前列表的标志而不是整行删除。
     */
    private suspend fun removeLocalVideo(entity: io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalVideoEntity) {
        val remaining = if (isFavoriteMode) {
            entity.copy(isFav = false, favSynced = false)
        } else {
            entity.copy(isWatchLater = false, watchLaterSynced = false)
        }
        if (remaining.isFav || remaining.isWatchLater) {
            DatabaseRepo.LocalMylist.upsertVideo(remaining)
        } else {
            DatabaseRepo.LocalMylist.deleteVideo(entity.videoCode)
        }
    }
}
