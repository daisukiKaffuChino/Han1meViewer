package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist

import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListType
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope

class WatchLaterSubViewModel(scope: CoroutineScope) :
    MyListSubViewModel(scope), WatchLaterListController {

    override var watchLaterPage = 1

    override val watchLaterStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>> = itemsStateFlow.asStateFlow()
    override val watchLaterFlow: StateFlow<List<HanimeInfo>> = itemsFlow.asStateFlow()

    override fun getMyWatchLaterItems(page: Int) {
        loadItems(MyListType.WATCH_LATER, SettingsRepository.savedUserId, page)
    }

    private val _deleteMyWatchLaterFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    override val deleteMyWatchLaterFlow = _deleteMyWatchLaterFlow.asSharedFlow()

    override fun deleteMyWatchLater(videoCode: String, position: Int) {
        deleteItem(
            deleteCall = {
                NetworkRepo.addToMyList(
                    listCode = "save",
                    videoCode = videoCode,
                    isChecked = false,
                    position = position,
                    csrfToken = csrfToken,
                )
            },
            emitTo = _deleteMyWatchLaterFlow,
            position = position,
            mapState = { state ->
                when (state) {
                    is WebsiteState.Error -> WebsiteState.Error(state.throwable)
                    WebsiteState.Loading -> WebsiteState.Loading
                    is WebsiteState.Success -> WebsiteState.Success(true)
                }
            },
        )
    }

    override fun clearMyListItems() {
        super.clearMyListItems()
        watchLaterPage = 1
    }
}
