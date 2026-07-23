package io.github.daisukikaffuchino.han1meviewer.ui.screen.video

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.github.daisukikaffuchino.han1meviewer.HAdvancedSearch
import io.github.daisukikaffuchino.han1meviewer.HCacheManager
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.getHanimeVideoDownloadLink
import io.github.daisukikaffuchino.han1meviewer.getHanimeVideoLink
import io.github.daisukikaffuchino.han1meviewer.logic.dao.CheckInRecordDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.entity.CheckInRecordEntity
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo
import io.github.daisukikaffuchino.han1meviewer.logic.model.SearchOption
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.navigateSafely
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.SearchRoute
import io.github.daisukikaffuchino.han1meviewer.ui.widget.CheckInWidget
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.VideoViewModel
import io.github.daisukikaffuchino.han1meviewer.worker.HanimeDownloadManager
import io.github.daisukikaffuchino.han1meviewer.worker.HanimeDownloadWorker
import io.github.daisukikaffuchino.utils.SonnerToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.Serializable

class VideoRouteActions(
    private val context: Context,
    private val scope: CoroutineScope,
    private val viewModel: VideoViewModel,
    private val genres: List<SearchOption>,
    private val onPendingDownloadPromptChange: (DownloadPromptState?) -> Unit,
    private val getCheckedQuality: () -> String?,
    private val setCheckedQuality: (String?) -> Unit,
    private val onOpenUri: (String) -> Unit,
    private val onCopyText: (String) -> Unit,
    private val onRequestUnsubscribe: (HanimeVideo.Artist) -> Unit,
    private val onRequestNotificationPermission: () -> Unit,
) {
    fun openArtistSearch(artist: HanimeVideo.Artist) {
        val searchKey = genres.firstOrNull { option ->
            option.lang?.let { lang ->
                artist.genre == lang.zhrCN ||
                        artist.genre == lang.zhrTW ||
                        artist.genre == lang.en
            } == true
        }?.searchKey ?: ""
        val map = buildMap<HAdvancedSearch, Serializable> {
            put(HAdvancedSearch.QUERY, artist.name)
            if (searchKey.isNotEmpty() && !Preferences.searchArtistIgnoreVideoType) {
                put(HAdvancedSearch.GENRE, searchKey)
            }
        }
        val bundleMap = HashMap<String, Serializable>().apply {
            map.forEach { (key, value) -> put(key.name, value) }
        }
        val routeMap = bundleMap.mapValues { it.value.toString() }
        (context as? MainActivity)?.navController?.navigateSafely(
            SearchRoute(query = artist.name, advancedSearchJson = Json.encodeToString(routeMap))
        )
    }

    fun openTagSearch(tag: String) {
        (context as? MainActivity)?.navController?.navigateSafely(SearchRoute(query = tag))
    }

    fun toggleArtistSubscription(artist: HanimeVideo.Artist) {
        val post = artist.post ?: return
        if (!Preferences.isAlreadyLogin) {
            SonnerToast.warning(R.string.login_first)
            return
        }
        if (artist.isSubscribed) {
            onRequestUnsubscribe(artist)
        } else {
            viewModel.subscribeArtist(post.userId, post.artistId)
        }
    }

    fun confirmUnsubscribe(artist: HanimeVideo.Artist) {
        val post = artist.post ?: return
        viewModel.unsubscribeArtist(post.userId, post.artistId)
    }

    fun toggleFavorite(video: HanimeVideo) {
        if (!Preferences.isAlreadyLogin) {
            SonnerToast.warning(R.string.login_first)
            return
        }
        if (video.isFav) {
            viewModel.removeFromFavVideo(viewModel.videoCode, video.currentUserId)
        } else {
            viewModel.addToFavVideo(viewModel.videoCode, video.currentUserId)
        }
    }

    fun rateVideo(video: HanimeVideo, isPositive: Boolean) {
        if (!Preferences.isAlreadyLogin) {
            SonnerToast.warning(R.string.login_first)
            return
        }
        viewModel.rateVideo(video, isPositive)
    }

    fun updateMyListSelection(
        myList: HanimeVideo.MyList?,
        selectedStates: List<Boolean>,
    ) {
        if (!Preferences.isAlreadyLogin || myList == null || myList.myListInfo.isEmpty()) {
            SonnerToast.warning(R.string.login_first)
            return
        }
        myList.myListInfo.forEachIndexed { index, info ->
            val newChecked = selectedStates.getOrNull(index) ?: return@forEachIndexed
            if (info.isSelected != newChecked) {
                viewModel.modifyMyList(
                    listCode = info.code,
                    videoCode = viewModel.videoCode,
                    isChecked = newChecked,
                    position = index,
                )
            }
        }
    }

    fun quickCheckIn(record: CheckInRecordEntity) {
        scope.launch(Dispatchers.IO) {
            CheckInRecordDatabase.getDatabase(context).checkInDao().insert(record)
            runCatching { CheckInWidget().updateAll(context) }
            withContext(Dispatchers.Main) {
                SonnerToast.success(R.string.checkin_success)
            }
        }
    }

    fun openIntroductionLink(link: String) {
        try {
            onOpenUri(link)
        } catch (_: Exception) {
            onCopyText(link)
            SonnerToast.success(R.string.copy_to_clipboard)
        }
    }

    fun openOriginalComic(comicLink: String) {
        runCatching { onOpenUri(comicLink) }
            .onFailure { SonnerToast.error(R.string.fault_prompt) }
    }

    fun openVideoWebPage() {
        onOpenUri(getHanimeVideoLink(viewModel.videoCode))
    }

    fun openOfficialDownloadPage() {
        onOpenUri(getHanimeVideoDownloadLink(viewModel.videoCode))
    }

    fun startDownloadFlow(videoData: HanimeVideo) {
        if (videoData.videoUrls.isEmpty()) {
            SonnerToast.warning(R.string.no_video_links_found)
            return
        }
        viewModel.findDownloadedHanime(viewModel.videoCode)
    }

    fun confirmPendingDownload(
        videoData: HanimeVideo,
        pendingDownloadPrompt: DownloadPromptState?
    ) {
        val redownload = pendingDownloadPrompt?.oldQuality != null
        onPendingDownloadPromptChange(null)
        scope.launch {
            enqueueDownloadWork(videoData, redownload = redownload)
        }
    }

    private suspend fun enqueueDownloadWork(videoData: HanimeVideo, redownload: Boolean = false) {
        onRequestNotificationPermission()
        val quality = getCheckedQuality()
        withContext(Dispatchers.IO) {
            HCacheManager.saveHanimeVideoInfo(context, viewModel.videoCode, videoData)
        }
        HanimeDownloadManager.addTask(
            HanimeDownloadWorker.Args(
                quality = quality,
                downloadUrl = videoData.videoUrls[quality]?.link,
                videoType = videoData.videoUrls[quality]?.suffix,
                hanimeName = videoData.title,
                videoCode = viewModel.videoCode,
                coverUrl = videoData.coverUrl,
            ),
            redownload = redownload,
        )
    }

}
