package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

interface HanimeScreen : NavKey

@Serializable
object HomeRoute : HanimeScreen

@Serializable
object WatchHistoryRoute : HanimeScreen

@Serializable
object MyFavVideoRoute : HanimeScreen

@Serializable
object MyWatchLaterRoute : HanimeScreen

@Serializable
object MyPlaylistRoute : HanimeScreen

@Serializable
object SubscriptionRoute : HanimeScreen

@Serializable
object DailyCheckInRoute : HanimeScreen

@Serializable
object DownloadRoute : HanimeScreen

@Serializable
object AccountRoute : HanimeScreen

@Serializable
object LoginRoute : HanimeScreen

@Serializable
object ManualCookiesRoute : HanimeScreen

@Serializable
data class CloudflareRoute(
    val url: String,
    val host: String,
) : HanimeScreen

@Serializable
data class AvatarCropRoute(
    val sourceUri: String,
) : HanimeScreen

@Serializable
data class SearchRoute(
    val query: String? = null,
    val advancedSearchJson: String? = null,
) : HanimeScreen

@Serializable
object PreviewRoute : HanimeScreen

@Serializable
object GetchuPreviewRoute : HanimeScreen

@Serializable
data class GetchuPreviewDetailRoute(
    val id: String,
) : HanimeScreen

@Serializable
data class PreviewCommentRoute(
    val date: String,
    val dateCode: String,
) : HanimeScreen

@Serializable
data class VideoRoute(
    val videoCode: String,
    val localUri: String? = null,
    val autoPlay: Boolean = false,
) : HanimeScreen
