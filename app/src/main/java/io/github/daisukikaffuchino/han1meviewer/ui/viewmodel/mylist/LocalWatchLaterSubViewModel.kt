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
 * 免登录本地"稍后再看"列表，数据来自 [LocalListRepository]。
 */
class LocalWatchLaterSubViewModel(
    private val scope: CoroutineScope,
) : WatchLaterListController {

    private val itemsStateFlow =
        MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(PageLoadingState.Loading)
    private val itemsFlow = MutableStateFlow(emptyList<HanimeInfo>())
    private val deleteFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    private val loadedPageCountFlow = MutableStateFlow(0)
    private val isLoadingMoreFlow = MutableStateFlow(false)
    private var loadJob: Job? = null

    override val watchLaterStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>> =
        itemsStateFlow.asStateFlow()
    override val watchLaterFlow: StateFlow<List<HanimeInfo>> = itemsFlow.asStateFlow()
    override val deleteMyWatchLaterFlow: SharedFlow<WebsiteState<Boolean>> = deleteFlow.asSharedFlow()
    override val loadedPageCount: StateFlow<Int> = loadedPageCountFlow.asStateFlow()
    override val isLoadingMore: StateFlow<Boolean> = isLoadingMoreFlow.asStateFlow()
    override var watchLaterPage = 1

    override fun getMyWatchLaterItems(page: Int) {
        loadJob?.cancel()
        loadJob = scope.launch {
            LocalListRepository.observeWatchLater().collect { list ->
                itemsFlow.value = list
                loadedPageCountFlow.value = 1
                isLoadingMoreFlow.value = false
                itemsStateFlow.value = PageLoadingState.NoMoreData
            }
        }
    }

    override fun deleteMyWatchLater(videoCode: String, position: Int) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                LocalListRepository.removeItem(LocalListRepository.WATCH_LATER_CODE, videoCode)
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
