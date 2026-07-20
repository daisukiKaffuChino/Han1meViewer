package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.daisukikaffuchino.han1meviewer.R

enum class MainDrawerDestination(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    val requiresLogin: Boolean = false,
) {
    Home(
        iconRes = R.drawable.ic_baseline_home_24,
        titleRes = R.string.home_page,
    ),
    Settings(
        iconRes = R.drawable.ic_baseline_settings_24,
        titleRes = R.string.settings,
    ),
    WatchLater(
        iconRes = R.drawable.ic_baseline_watch_later_24,
        titleRes = R.string.watch_later,
        requiresLogin = true,
    ),
    FavVideo(
        iconRes = R.drawable.ic_baseline_favorite_24,
        titleRes = R.string.fav_video,
        requiresLogin = true,
    ),
    Playlist(
        iconRes = R.drawable.ic_baseline_list_24,
        titleRes = R.string.play_list,
        requiresLogin = true,
    ),
    Subscription(
        iconRes = R.drawable.ic_subscribtion,
        titleRes = R.string.my_subscribe,
        requiresLogin = true,
    ),
    WatchHistory(
        iconRes = R.drawable.ic_baseline_history_24,
        titleRes = R.string.watch_history,
    ),
    Download(
        iconRes = R.drawable.ic_baseline_download_24,
        titleRes = R.string.download,
    ),
}
