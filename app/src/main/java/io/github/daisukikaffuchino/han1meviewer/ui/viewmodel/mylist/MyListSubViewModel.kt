package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist

import io.github.daisukikaffuchino.han1meviewer.logic.repository.MyListRepository
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListType
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MyListSubViewModel(
    private val scope: CoroutineScope,
) {

    protected val itemsStateFlow: MutableStateFlow<PagedUiState<MyListItems<HanimeInfo>>> =
        MutableStateFlow(PagedUiState.Loading)

    protected val itemsFlow = MutableStateFlow(emptyList<HanimeInfo>())

    protected val mutableLoadedPageCount = MutableStateFlow(0)
    val loadedPageCount = mutableLoadedPageCount.asStateFlow()

    protected val mutableTotalPages = MutableStateFlow(1)
    val totalPages = mutableTotalPages.asStateFlow()

    protected val mutableIsLoadingMore = MutableStateFlow(false)
    val isLoadingMore = mutableIsLoadingMore.asStateFlow()

    protected var isRefreshing = true

    protected fun loadItems(
        listType: MyListType,
        userId: String,
        page: Int,
        onSuccess: (MyListItems<HanimeInfo>) -> Unit = {},
    ) {
        mutableIsLoadingMore.value = !isRefreshing && itemsFlow.value.isNotEmpty()
        scope.launch {
            MyListRepository.getMyListItems(userId, listType, page).collect { state ->
                itemsStateFlow.value = state
                itemsFlow.update { prevList ->
                    when (state) {
                        is PagedUiState.Success -> {
                            onSuccess(state.data)
                            mutableTotalPages.value = state.data.maxPage
                            if (state.data.hanimeInfo.isEmpty()) {
                                itemsStateFlow.update { PagedUiState.NoMoreData }
                            } else {
                                mutableLoadedPageCount.value = page
                            }
                            val baseList = if (isRefreshing) emptyList() else prevList
                            isRefreshing = false
                            mutableIsLoadingMore.value = false
                            (baseList + state.data.hanimeInfo).distinctBy(HanimeInfo::videoCode)
                        }

                        is PagedUiState.Loading -> prevList
                        else -> {
                            mutableIsLoadingMore.value = false
                            prevList
                        }
                    }
                }
            }
        }
    }

    protected fun <T, R> deleteItem(
        deleteCall: suspend () -> kotlinx.coroutines.flow.Flow<UiState<T>>,
        emitTo: MutableSharedFlow<UiState<R>>,
        position: Int,
        mapState: (UiState<T>) -> UiState<R>,
        isSuccess: (UiState<T>) -> Boolean = { it is UiState.Success },
    ) {
        scope.launch {
            deleteCall().collect { deleteState ->
                emitTo.emit(mapState(deleteState))
                itemsFlow.update { list ->
                    if (isSuccess(deleteState)) {
                        list.toMutableList().apply { removeAt(position) }
                    } else list
                }
            }
        }
    }

    open fun clearMyListItems() {
        isRefreshing = true
        mutableIsLoadingMore.value = false
        mutableLoadedPageCount.value = 0
        mutableTotalPages.value = 1
        itemsFlow.value = emptyList()
        itemsStateFlow.value = PagedUiState.Loading
    }
}
