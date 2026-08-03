package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import io.github.daisukikaffuchino.utils.LogUtil
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.HanimeResolution
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.DatabaseRepo
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.entity.HKeyframeEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.WatchHistoryEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.download.HanimeDownloadEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalMylistTombstoneEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalPlaylistItemEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalVideoEntity
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo
import io.github.daisukikaffuchino.han1meviewer.logic.state.VideoLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import io.github.daisukikaffuchino.han1meviewer.util.TagLocalizer
import androidx.lifecycle.ViewModel
import io.github.daisukikaffuchino.han1meviewer.logic.platform.AndroidVideoCacheStore
import io.github.daisukikaffuchino.han1meviewer.logic.platform.VideoCacheStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/17 017 19:01
 */
class VideoViewModel(
    private val videoCacheStore: VideoCacheStore = AndroidVideoCacheStore,
) : ViewModel() {

    data class IntroScrollState(
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0,
    )

    data class VideoHostUiState(
        val selectedTabIndex: Int = 0,
        val commentBadgeCount: Int = 0,
        val isScrollDisabled: Boolean = false,
        val isInPipMode: Boolean = false,
        val playerHeightDp: Dp? = 250.dp,
    )

    private data class VideoIntroUiState(
        val playlistFirstVisibleIndex: Int? = null,
        val cachedVideo: HanimeVideo? = null,
        val introRestored: Boolean = false,
        val scrollState: IntroScrollState = IntroScrollState(),
        val selectedTabIndex: Int = 0,
    )

    companion object {
        /**
         * 最小的 HKeyframe 保存間隔，暫定 5s
         */
        const val MIN_H_KEYFRAME_SAVE_INTERVAL = 5_000 // ms
    }
    private val videoIntroUiStateMap = mutableMapOf<String, VideoIntroUiState>()
    var videoCode: String = EMPTY_STRING
        set(value) {
            field = value
        }

    var fromDownload = false

    // 平板横屏模式下，左栏不显示相关视频（右栏已显示）
    var hideRelatedInIntro by mutableStateOf(false)
    var hKeyframes: HKeyframeEntity? = null
    private val _videoList = MutableLiveData<List<HanimeInfo>>()
    val videoList: LiveData<List<HanimeInfo>> = _videoList
    private val _hanimeVideoStateFlow =
        MutableStateFlow<VideoLoadingState<HanimeVideo>>(VideoLoadingState.Loading)
    val hanimeVideoStateFlow = _hanimeVideoStateFlow.asStateFlow()

    private val _hanimeVideoFlow = MutableStateFlow<HanimeVideo?>(null)
    val hanimeVideoFlow = _hanimeVideoFlow.asStateFlow()
    private val _videoHostUiStateFlow = MutableStateFlow(VideoHostUiState())
    val videoHostUiStateFlow = _videoHostUiStateFlow.asStateFlow()

    fun setVideoList(list: List<HanimeInfo>) {
        _videoList.value = list
    }

    fun getPlaylistFirstVisibleIndex(videoCode: String): Int? {
        return videoIntroUiStateMap[videoCode]?.playlistFirstVisibleIndex
    }

    fun setPlaylistFirstVisibleIndex(videoCode: String, index: Int) {
        updateVideoIntroUiState(videoCode) { copy(playlistFirstVisibleIndex = index) }
    }

    fun setVideoIntroCachedData(videoCode: String, video: HanimeVideo?) {
        updateVideoIntroUiState(videoCode) {
            copy(
                cachedVideo = video,
                introRestored = video != null,
            )
        }
    }

    fun clearVideoIntroRestoredFlag(videoCode: String) {
        updateVideoIntroUiState(videoCode) { copy(introRestored = false) }
    }

    fun getIntroScrollState(videoCode: String): IntroScrollState {
        return videoIntroUiStateMap[videoCode]?.scrollState ?: IntroScrollState()
    }

    fun getSelectedTabIndex(videoCode: String): Int {
        return _videoHostUiStateFlow.value.selectedTabIndex
    }

    fun setSelectedTabIndex(videoCode: String, selectedTabIndex: Int) {
        _videoHostUiStateFlow.update { it.copy(selectedTabIndex = selectedTabIndex) }
        updateVideoIntroUiState(videoCode) { copy(selectedTabIndex = selectedTabIndex) }
    }

    fun setCommentBadgeCount(commentBadgeCount: Int) {
        _videoHostUiStateFlow.update { it.copy(commentBadgeCount = commentBadgeCount) }
    }

    fun setScrollDisabled(isScrollDisabled: Boolean) {
        _videoHostUiStateFlow.update { it.copy(isScrollDisabled = isScrollDisabled) }
    }

    fun setPipMode(isInPipMode: Boolean) {
        _videoHostUiStateFlow.update { it.copy(isInPipMode = isInPipMode) }
    }

    fun setPlayerHeightDp(playerHeightDp: Dp?) {
        _videoHostUiStateFlow.update { it.copy(playerHeightDp = playerHeightDp) }
    }

    fun setIntroScrollState(
        videoCode: String,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ) {
        updateVideoIntroUiState(videoCode) {
            copy(
                scrollState = IntroScrollState(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                )
            )
        }
    }

    private inline fun updateVideoIntroUiState(
        videoCode: String,
        transform: VideoIntroUiState.() -> VideoIntroUiState,
    ) {
        val current = videoIntroUiStateMap[videoCode] ?: VideoIntroUiState()
        videoIntroUiStateMap[videoCode] = current.transform()
    }

    fun resolveTagSearchKey(tag: String): String = TagLocalizer.resolveSearchKey(tag)

    private fun HanimeVideo.withLocalizedLabels(): HanimeVideo {
        return copy(
            tags = TagLocalizer.localizeTags(tags),
            artist = artist?.copy(genre = TagLocalizer.localizeTag(artist.genre)),
        )
    }

    fun buildLocalPlayInfo(localPath: String? = null): HanimeVideo {
        val resolution = HanimeResolution()
        resolution.parseResolution(
            HanimeResolution.RES_1080P,
            resLink = localPath?:"",
            type = "video/mp4"
        )
        return HanimeVideo(
            title = "",
            coverUrl = "",
            chineseTitle = localPath?.toUri()?.lastPathSegment,
            introduction = "",
            uploadTime = null,
            views = "0",
            videoUrls = resolution.toResolutionLinkMap(),
            tags = emptyList(),
        )
    }
    fun getHanimeVideo(videoCode: String,localUri: String? = null) {
        if (videoCode == "-1"){
            val localPlayInfo = buildLocalPlayInfo(localUri)
            _hanimeVideoStateFlow.value = VideoLoadingState.Success(localPlayInfo)
            _hanimeVideoFlow.value = localPlayInfo
            return
        }
        if (videoIntroUiStateMap[videoCode]?.introRestored == true) return
        viewModelScope.launch {
            val flow = if (fromDownload) {
                videoCacheStore.load(videoCode).map { hv ->
                    if (hv == null) {
                        VideoLoadingState.NoContent
                    } else {
                        VideoLoadingState.Success(hv)
                    }
                }
            } else {
                NetworkRepo.getHanimeVideo(videoCode)
            }
            flow.collect { state ->
                val emitState = when {
                    localUri != null && state is VideoLoadingState.Success -> {
                        val resolution = HanimeResolution()
                        resolution.parseResolution(
                            HanimeResolution.RES_1080P,
                            resLink = localUri,
                            type = "video/mp4"
                        )
                        VideoLoadingState.Success(
                            state.info.copy(videoUrls = resolution.toResolutionLinkMap())
                                .withLocalizedLabels()
                        )
                    }

                    state is VideoLoadingState.Success -> {
                        VideoLoadingState.Success(state.info.withLocalizedLabels())
                    }

                    else -> state
                }
                _hanimeVideoStateFlow.value = emitState
                if (emitState is VideoLoadingState.Success) {
                    _hanimeVideoFlow.update { emitState.info }
                    csrfToken = emitState.info.csrfToken
                    applyLocalMylistState(videoCode)
                }
            }
        }
    }

    fun restoreFromCacheIfExists(code: String): Boolean {
        val cached = videoIntroUiStateMap[code]?.cachedVideo?.withLocalizedLabels() ?: return false
        updateVideoIntroUiState(code) { copy(introRestored = true) }
        _hanimeVideoFlow.value = cached
        _hanimeVideoStateFlow.value = VideoLoadingState.Success(cached)
        return true
    }



    private val _addToFavVideoFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val addToFavVideoFlow = _addToFavVideoFlow.asSharedFlow()

    private val _loadDownloadedFlow = MutableSharedFlow<HanimeDownloadEntity?>()
    val loadDownloadedFlow = _loadDownloadedFlow.asSharedFlow()

    fun addToFavVideo(
        videoCode: String,
        currentUserId: String?,
    ) = modifyFavVideoInternal(videoCode, likeStatus = false, currentUserId)

    fun removeFromFavVideo(
        videoCode: String,
        currentUserId: String?,
    ) = modifyFavVideoInternal(videoCode, likeStatus = true, currentUserId)

    /**
     * 收藏 / 取消收藏（本地优先）。
     *
     * 未登录时只写本地（sync 标记为 false，登录后由同步引擎推送云端）；
     * 已登录时写本地镜像并同步调用云端，云端失败则回滚 sync 标记等待下次同步。
     */
    private fun modifyFavVideoInternal(
        videoCode: String,
        likeStatus: Boolean,
        currentUserId: String?,
    ) {
        val newFav = !likeStatus
        viewModelScope.launch {
            val video = _hanimeVideoFlow.value
            val local = DatabaseRepo.LocalMylist.findBy(videoCode)
            val base = local ?: LocalVideoEntity(
                videoCode = videoCode,
                title = video?.title.orEmpty(),
                coverUrl = video?.coverUrl.orEmpty(),
                duration = video?.views,
                views = video?.views,
                reviews = video?.ratingCount?.toString(),
                currentArtist = video?.artist?.name,
                uploadTime = video?.uploadTime?.toString(),
            )
            DatabaseRepo.LocalMylist.upsertVideo(
                base.copy(
                    isFav = newFav,
                    favTime = System.currentTimeMillis(),
                    favSynced = SettingsRepository.isAlreadyLogin,
                )
            )
            _hanimeVideoFlow.update { it?.copy(isFav = newFav) }
            if (SettingsRepository.isAlreadyLogin) {
                NetworkRepo.addToMyFavVideo(
                    videoCode, likeStatus, currentUserId, csrfToken
                ).collect { state ->
                    _addToFavVideoFlow.emit(state)
                    DatabaseRepo.LocalMylist.findBy(videoCode)?.let { entity ->
                        DatabaseRepo.LocalMylist.upsertVideo(
                            entity.copy(favSynced = state is WebsiteState.Success)
                        )
                    }
                    // 云端删除失败时记录墓碑，登录同步时补偿，避免被云端数据「复活」
                    if (likeStatus && state is WebsiteState.Error) {
                        upsertTombstone(videoCode, isFav = true)
                    }
                }
            } else {
                // 未登录的取消收藏：记录墓碑，登录后推送到云端删除
                if (likeStatus) {
                    upsertTombstone(videoCode, isFav = true)
                }
                _addToFavVideoFlow.emit(WebsiteState.Success(true))
            }
        }
    }

    private suspend fun upsertTombstone(videoCode: String, isFav: Boolean, isWatchLater: Boolean = false) {
        val existing = DatabaseRepo.LocalMylist.findTombstone(videoCode)
        DatabaseRepo.LocalMylist.upsertTombstone(
            existing?.copy(
                isFav = existing.isFav || isFav,
                isWatchLater = existing.isWatchLater || isWatchLater,
            ) ?: LocalMylistTombstoneEntity(
                videoCode = videoCode,
                isFav = isFav,
                isWatchLater = isWatchLater,
                deletedTime = System.currentTimeMillis(),
            )
        )
    }

    fun rateVideo(video: HanimeVideo, isPositive: Boolean) {
        viewModelScope.launch {
            NetworkRepo.rateVideo(
                videoCode = videoCode,
                isPositive = isPositive,
                likeStatus = video.isFav,
                unlikeStatus = video.isUnlike,
                likesCount = video.favTimes ?: 0,
                unlikesCount = video.unlikesCount ?: 0,
                currentUserId = video.currentUserId,
                token = csrfToken,
            ).collect { state ->
                _addToFavVideoFlow.emit(state)
                if (state is WebsiteState.Success) {
                    _hanimeVideoFlow.update { it?.rateVideo(isPositive) }
                }
            }
        }
    }

    private val _modifyMyListFlow = MutableSharedFlow<WebsiteState<Int>>()
    val modifyMyListFlow = _modifyMyListFlow.asSharedFlow()

    /**
     * 修改视频的清单勾选状态（本地优先，兼容未登录）。
     *
     * listCode 为 "save" 时代表稍后观看，否则为播放清单 code。
     * 已登录时同步调用云端并写本地镜像；未登录时只写本地，
     * 登录后由 MylistSyncManager 推送云端。
     */
    fun modifyMyList(
        listCode: String,
        videoCode: String,
        isChecked: Boolean,
        position: Int,
    ) {
        viewModelScope.launch {
            val video = _hanimeVideoFlow.value
            val local = DatabaseRepo.LocalMylist.findBy(videoCode)
            if (listCode == "save") {
                val base = local ?: LocalVideoEntity(
                    videoCode = videoCode,
                    title = video?.title.orEmpty(),
                    coverUrl = video?.coverUrl.orEmpty(),
                    views = video?.views,
                    currentArtist = video?.artist?.name,
                )
                DatabaseRepo.LocalMylist.upsertVideo(
                    base.copy(
                        isWatchLater = isChecked,
                        watchLaterTime = System.currentTimeMillis(),
                        watchLaterSynced = SettingsRepository.isAlreadyLogin,
                    )
                )
                _hanimeVideoFlow.update { prev ->
                    prev?.copy(myList = prev.myList?.copy(isWatchLater = isChecked))
                }
                if (SettingsRepository.isAlreadyLogin) {
                    NetworkRepo.addToMyList(listCode, videoCode, isChecked, position, csrfToken).collect { state ->
                        _modifyMyListFlow.emit(state)
                        DatabaseRepo.LocalMylist.findBy(videoCode)?.let { entity ->
                            DatabaseRepo.LocalMylist.upsertVideo(
                                entity.copy(watchLaterSynced = state is WebsiteState.Success)
                            )
                        }
                        if (!isChecked && state is WebsiteState.Error) {
                            upsertTombstone(videoCode, isWatchLater = true)
                        }
                    }
                } else {
                    if (!isChecked) {
                        upsertTombstone(videoCode, isWatchLater = true)
                    }
                    _modifyMyListFlow.emit(WebsiteState.Success(position))
                }
            } else {
                modifyLocalPlaylistItem(listCode, videoCode, isChecked, video)
                if (SettingsRepository.isAlreadyLogin) {
                    NetworkRepo.addToMyList(listCode, videoCode, isChecked, position, csrfToken).collect { state ->
                        _modifyMyListFlow.emit(state)
                        DatabaseRepo.LocalMylist.getPlaylistItemCodes(
                            listCode, listOf(videoCode)
                        ).firstOrNull()?.let { item ->
                            DatabaseRepo.LocalMylist.upsertPlaylistItem(
                                item.copy(synced = state is WebsiteState.Success)
                            )
                        }
                    }
                } else {
                    _modifyMyListFlow.emit(WebsiteState.Success(position))
                }
                updateMyListSelectionUi(listCode, isChecked)
            }
        }
    }

    private suspend fun modifyLocalPlaylistItem(
        listCode: String,
        videoCode: String,
        isChecked: Boolean,
        video: HanimeVideo?,
    ) {
        val existing = DatabaseRepo.LocalMylist.getPlaylistItemCodes(listCode, listOf(videoCode))
        if (isChecked && existing.isEmpty()) {
            val count = DatabaseRepo.LocalMylist.getPlaylistItems(listCode).size
            DatabaseRepo.LocalMylist.upsertPlaylistItem(
                LocalPlaylistItemEntity(
                    playlistCode = listCode,
                    videoCode = videoCode,
                    title = video?.title.orEmpty(),
                    coverUrl = video?.coverUrl.orEmpty(),
                    views = video?.views,
                    currentArtist = video?.artist?.name,
                    position = count,
                    addedTime = System.currentTimeMillis(),
                    synced = SettingsRepository.isAlreadyLogin,
                )
            )
        } else if (!isChecked) {
            DatabaseRepo.LocalMylist.deletePlaylistItem(listCode, videoCode)
        }
    }

    private fun updateMyListSelectionUi(listCode: String, isChecked: Boolean) {
        _hanimeVideoFlow.update { prev ->
            val myListInfo = prev?.myList?.myListInfo?.map { info ->
                if (info.code == listCode) info.copy(isSelected = isChecked) else info
            }.orEmpty()
            prev?.copy(myList = prev.myList?.copy(myListInfo = myListInfo))
        }
    }

    /**
     * 视频加载成功后，用本地数据合并收藏 / 稍后观看状态。
     * 未登录时（服务端没有 myList 数据）从本地数据库构造清单列表。
     */
    private suspend fun applyLocalMylistState(code: String) {
        val local = DatabaseRepo.LocalMylist.findBy(code)
        _hanimeVideoFlow.update { prev ->
            prev ?: return@update null
            val myList = when {
                prev.myList != null -> prev.myList.copy(
                    isWatchLater = prev.myList.isWatchLater || (local?.isWatchLater == true)
                )
                else -> buildLocalMyList(code, local)
            }
            prev.copy(
                isFav = prev.isFav || (local?.isFav == true),
                myList = myList,
            )
        }
    }

    private suspend fun buildLocalMyList(
        code: String,
        local: LocalVideoEntity?,
    ): HanimeVideo.MyList {
        val playlists = DatabaseRepo.LocalMylist.getAllPlaylists()
        val savedInLists = playlists.map { playlist ->
            val selected = DatabaseRepo.LocalMylist
                .getPlaylistItemCodes(playlist.code, listOf(code))
                .isNotEmpty()
            HanimeVideo.MyList.MyListInfo(
                code = playlist.code,
                title = playlist.name,
                isSelected = selected,
            )
        }
        return HanimeVideo.MyList(
            isWatchLater = local?.isWatchLater ?: false,
            myListInfo = savedInLists + HanimeVideo.MyList.MyListInfo(
                code = "save",
                title = "save",
                isSelected = local?.isWatchLater ?: false,
            ),
        )
    }

    fun insertWatchHistory(history: WatchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.insert(history)
            LogUtil.d("insert_watch_hty", "$history DONE!")
        }
    }

    fun insertWatchHistoryWithCover(history: WatchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.insert(history)
        }
    }

    fun findDownloadedHanime(videoCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val info = DatabaseRepo.HanimeDownload.find(videoCode)
            _loadDownloadedFlow.emit(info)
        }
    }

    // true代表已关注成功，false代表取消关注成功
    private val _subscribeArtistFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val subscribeArtistFlow = _subscribeArtistFlow.asSharedFlow()

    fun subscribeArtist(
        userId: String,
        artistId: String,
    ) {
        viewModelScope.launch {
            NetworkRepo.subscribeArtist(csrfToken, userId, artistId, true).collect { state ->
                _subscribeArtistFlow.emit(state)
                if (state is WebsiteState.Success) {
                    _hanimeVideoFlow.update {
                        it?.copy(artist = it.artist?.copy(post = it.artist.post?.copy(isSubscribed = true)))
                    }
                }
            }
        }
    }

    fun unsubscribeArtist(
        userId: String,
        artistId: String,
    ) {
        viewModelScope.launch {
            NetworkRepo.subscribeArtist(csrfToken, userId, artistId, false).collect { state ->
                _subscribeArtistFlow.emit(state)
                if (state is WebsiteState.Success) {
                    _hanimeVideoFlow.update {
                        it?.copy(artist = it.artist?.copy(post = it.artist.post?.copy(isSubscribed = false)))
                    }
                }
            }
        }
    }

    // boolean: 成功 or 失敗，String: 提示信息
    data class HKeyframeResult(
        val succeeded: Boolean,
        val messageResId: Int,
        val args: List<Any> = emptyList(),
    )

    private val _modifyHKeyframeFlow = MutableSharedFlow<HKeyframeResult>()
    val modifyHKeyframeFlow = _modifyHKeyframeFlow.asSharedFlow()
    private val _forceRefresh = MutableSharedFlow<Unit>(replay = 1)
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeKeyframe(videoCode: String): Flow<HKeyframeEntity?> {
        return _forceRefresh
            .onStart { emit(Unit) }
            .flatMapLatest {
                DatabaseRepo.HKeyframe.observe(videoCode).flowOn(Dispatchers.IO)
            }
    }
    fun appendHKeyframe(videoCode: String, title: String, hKeyframe: HKeyframeEntity.Keyframe) {
        viewModelScope.launch(Dispatchers.IO) {
            run {
                this@VideoViewModel.hKeyframes?.keyframes?.forEach { keyframeInDb ->
                    if (abs(keyframeInDb.position - hKeyframe.position) < MIN_H_KEYFRAME_SAVE_INTERVAL) {
                        LogUtil.d("HKeyframe", "append_hkeyframe:time conflict: $keyframeInDb")
                        _modifyHKeyframeFlow.emit(
                            HKeyframeResult(
                                succeeded = false,
                                messageResId = R.string.interval_must_greater_than_d,
                                args = listOf(MIN_H_KEYFRAME_SAVE_INTERVAL / 1_000L),
                            )
                        )
                        return@run
                    }
                }
                DatabaseRepo.HKeyframe.appendKeyframe(videoCode, title, hKeyframe)
                LogUtil.d("HKeyframe", "append_hkeyframe:$hKeyframe DONE!")
                _modifyHKeyframeFlow.emit(HKeyframeResult(true, R.string.add_success))
                _forceRefresh.emit(Unit)
            }
        }
    }

    fun modifyHKeyframe(
        videoCode: String,
        oldKeyframe: HKeyframeEntity.Keyframe,
        newKeyframe: HKeyframeEntity.Keyframe,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.modifyKeyframe(videoCode, oldKeyframe, newKeyframe)
            _modifyHKeyframeFlow.emit(HKeyframeResult(true, R.string.modify_success))
            _forceRefresh.emit(Unit)
        }
    }

    fun removeHKeyframe(videoCode: String, hKeyframe: HKeyframeEntity.Keyframe) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.removeKeyframe(videoCode, hKeyframe)
            _modifyHKeyframeFlow.emit(HKeyframeResult(true, R.string.delete_success))
            _forceRefresh.emit(Unit)
        }
    }
}
