package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist

import io.github.daisukikaffuchino.han1meviewer.logic.LocalListRepository
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 免登录本地"我喜欢的影片"列表，数据来自 [LocalListRepository]。
 */
class LocalFavSubViewModel(
    private val scope: CoroutineScope,
) : FavVideoListController {

    private val itemsStateFlow =
        MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(PageLoadingState.Loading)
    private val itemsFlow = MutableStateFlow(emptyList<HanimeInfo>())
    private val deleteFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    private val loadedPageCountFlow = MutableStateFlow(0)
    private val isLoadingMoreFlow = MutableStateFlow(false)
    private var loadJob: Job? = null

    override val favVideoStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>> =
        itemsStateFlow.asStateFlow()
    override val favVideoFlow: StateFlow<List<HanimeInfo>> = itemsFlow.asStateFlow()
    override val deleteMyFavVideoFlow: SharedFlow<WebsiteState<Boolean>> = deleteFlow.asSharedFlow()
    override val loadedPageCount: StateFlow<Int> = loadedPageCountFlow.asStateFlow()
    override val isLoadingMore: StateFlow<Boolean> = isLoadingMoreFlow.asStateFlow()
    override var favVideoPage = 1

    override fun getMyFavVideoItems(userId: String, page: Int) {
        loadJob?.cancel()
        loadJob = scope.launch {
            LocalListRepository.observeFavorites().collect { list ->
                itemsFlow.value = list
                loadedPageCountFlow.value = 1
                isLoadingMoreFlow.value = false
                itemsStateFlow.value = PageLoadingState.NoMoreData
            }
        }
    }

    override fun deleteMyFavVideo(videoCode: String, position: Int) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                LocalListRepository.removeItem(LocalListRepository.FAVORITE_CODE, videoCode)
            }.onSuccess {
                deleteFlow.emit(WebsiteState.Success(true))
            }.onFailure {
                deleteFlow.emit(WebsiteState.Error(it))
            }
        }
    }

    override fun clearMyListItems() {
        loadJob?.cancel()
        loadJob = null
        itemsFlow.value = emptyList()
        itemsStateFlow.value = PageLoadingState.Loading
        loadedPageCountFlow.value = 0
        isLoadingMoreFlow.value = false
    }
}
