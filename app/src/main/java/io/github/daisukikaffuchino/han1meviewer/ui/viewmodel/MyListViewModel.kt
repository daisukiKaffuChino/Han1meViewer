package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import android.app.Application
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist.FavSubViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist.PlaylistSubViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.mylist.WatchLaterSubViewModel
import io.github.daisukikaffuchino.utils.ApplicationViewModel

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/04 004 22:46
 */
class MyListViewModel(application: Application) : ApplicationViewModel(application) {

    val playlist by sub<PlaylistSubViewModel>()
    val watchLater by sub<WatchLaterSubViewModel>()
    val fav by sub<FavSubViewModel>()
}
