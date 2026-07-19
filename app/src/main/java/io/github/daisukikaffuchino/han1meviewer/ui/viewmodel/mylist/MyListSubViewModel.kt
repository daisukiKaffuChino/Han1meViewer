package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListType
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.utils.ApplicationViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MyListSubViewModel(application: Application) : ApplicationViewModel(application) {

    protected val itemsStateFlow: MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>> =
        MutableStateFlow(PageLoadingState.Loading)

    protected val itemsFlow = MutableStateFlow(emptyList<HanimeInfo>())

    protected val mutableLoadedPageCount = MutableStateFlow(0)
    val loadedPageCount = mutableLoadedPageCount.asStateFlow()

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
        viewModelScope.launch {
            NetworkRepo.getMyListItems(userId, listType, page).collect { state ->
                itemsStateFlow.value = state
                itemsFlow.update { prevList ->
                    when (state) {
                        is PageLoadingState.Success -> {
                            onSuccess(state.info)
                            if (state.info.hanimeInfo.isEmpty()) {
                                itemsStateFlow.update { PageLoadingState.NoMoreData }
                            } else {
                                mutableLoadedPageCount.value = page
                            }
                            val baseList = if (isRefreshing) emptyList() else prevList
                            isRefreshing = false
                            mutableIsLoadingMore.value = false
                            (baseList + state.info.hanimeInfo).distinctBy(HanimeInfo::videoCode)
                        }

                        is PageLoadingState.Loading -> prevList
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
        deleteCall: suspend () -> kotlinx.coroutines.flow.Flow<WebsiteState<T>>,
        emitTo: MutableSharedFlow<WebsiteState<R>>,
        position: Int,
        mapState: (WebsiteState<T>) -> WebsiteState<R>,
        isSuccess: (WebsiteState<T>) -> Boolean = { it is WebsiteState.Success },
    ) {
        viewModelScope.launch {
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
        itemsFlow.value = emptyList()
        itemsStateFlow.value = PageLoadingState.Loading
    }
}
