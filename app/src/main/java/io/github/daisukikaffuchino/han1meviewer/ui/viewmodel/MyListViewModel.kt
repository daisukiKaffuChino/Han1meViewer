package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist.FavSubViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist.LocalFavSubViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist.LocalWatchLaterSubViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist.WatchLaterSubViewModel

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/04 004 22:46
 */
class MyListViewModel : ViewModel() {

    val watchLater = WatchLaterSubViewModel(viewModelScope)
    val fav = FavSubViewModel(viewModelScope)
    val localWatchLater = LocalWatchLaterSubViewModel(viewModelScope)
    val localFav = LocalFavSubViewModel(viewModelScope)
}
