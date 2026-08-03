package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalMylistTombstoneEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalPlaylistEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalPlaylistItemEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalVideoEntity
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListType
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.AppViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow

/**
 * 本地收藏 / 稍后观看 / 播放清单 与云端的同步引擎。
 *
 * 未登录时的操作全部写入本地数据库（mylist.db），
 * 登录成功后调用 [syncOnLogin] 执行双向合并：
 * - 推送：本地删除墓碑（云端删除）→ 本地脏数据（收藏 / 稍后观看 / 播放清单）推送到云端
 * - 拉取：云端全量列表拉取后合并到本地，云端优先（server-wins）
 *
 * @project Han1meViewer
 */
object MylistSyncManager {

    @Volatile
    private var isSyncing = false

    /**
     * 登录成功后调用，失败不影响登录流程。
     */
    suspend fun syncOnLogin() {
        if (isSyncing) return
        isSyncing = true
        try {
            if (!SettingsRepository.isAlreadyLogin) return
            val userId = SettingsRepository.savedUserId
            if (userId.isBlank()) return
            val token = obtainCsrfToken() ?: return
            pushTombstones(userId, token)
            pullFavorites(userId)
            pullWatchLater(userId)
            pushDirtyFavorites(userId, token)
            pushDirtyWatchLater(userId, token)
            syncPlaylists(userId, token)
        } finally {
            isSyncing = false
        }
    }

    private suspend fun obtainCsrfToken(): String? {
        var token = AppViewModel.csrfToken
        if (token.isNullOrBlank()) {
            runCatching {
                NetworkRepo.getHomePage().first { state ->
                    if (state is WebsiteState.Success) {
                        token = state.info.csrfToken
                        true
                    } else state !is WebsiteState.Loading
                }
            }
        }
        return token
    }

    //<editor-fold desc="推送：删除墓碑">

    private suspend fun pushTombstones(userId: String, token: String?) {
        val tombstones = DatabaseRepo.LocalMylist.getTombstones()
        for (tombstone in tombstones) {
            var isFav = tombstone.isFav
            var isWatchLater = tombstone.isWatchLater
            if (isFav) {
                runCatching {
                    NetworkRepo.addToMyFavVideo(
                        videoCode = tombstone.videoCode,
                        likeStatus = true,
                        currentUserId = userId,
                        token = token,
                    ).first()
                }.onSuccess { isFav = false }
            }
            if (isWatchLater) {
                runCatching {
                    NetworkRepo.addToMyList(
                        listCode = "save",
                        videoCode = tombstone.videoCode,
                        isChecked = false,
                        position = 0,
                        csrfToken = token,
                    ).first()
                }.onSuccess { isWatchLater = false }
            }
            if (!isFav && !isWatchLater) {
                DatabaseRepo.LocalMylist.deleteTombstone(tombstone.videoCode)
            } else if (isFav != tombstone.isFav || isWatchLater != tombstone.isWatchLater) {
                DatabaseRepo.LocalMylist.upsertTombstone(
                    tombstone.copy(isFav = isFav, isWatchLater = isWatchLater)
                )
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="拉取：收藏 / 稍后观看">

    private suspend fun pullFavorites(userId: String) {
        pullMyList(userId, MyListType.FAV_VIDEO) { hanimeInfo, local, index ->
            LocalVideoEntity(
                videoCode = hanimeInfo.videoCode,
                title = hanimeInfo.title,
                coverUrl = hanimeInfo.coverUrl,
                duration = hanimeInfo.duration,
                views = hanimeInfo.views,
                reviews = hanimeInfo.reviews,
                currentArtist = hanimeInfo.currentArtist,
                uploadTime = hanimeInfo.uploadTime,
                isFav = true,
                favSynced = true,
                favTime = System.currentTimeMillis() - index,
                isWatchLater = local?.isWatchLater ?: false,
                watchLaterSynced = local?.watchLaterSynced ?: false,
                watchLaterTime = local?.watchLaterTime ?: 0L,
            )
        }
    }

    private suspend fun pullWatchLater(userId: String) {
        pullMyList(userId, MyListType.WATCH_LATER) { hanimeInfo, local, index ->
            LocalVideoEntity(
                videoCode = hanimeInfo.videoCode,
                title = hanimeInfo.title,
                coverUrl = hanimeInfo.coverUrl,
                duration = hanimeInfo.duration,
                views = hanimeInfo.views,
                reviews = hanimeInfo.reviews,
                currentArtist = hanimeInfo.currentArtist,
                uploadTime = hanimeInfo.uploadTime,
                isWatchLater = true,
                watchLaterSynced = true,
                watchLaterTime = System.currentTimeMillis() - index,
                isFav = local?.isFav ?: false,
                favSynced = local?.favSynced ?: false,
                favTime = local?.favTime ?: 0L,
            )
        }
    }

    private suspend fun pullMyList(
        userId: String,
        listType: MyListType,
        toEntity: (HanimeInfo, LocalVideoEntity?, Int) -> LocalVideoEntity,
    ) {
        val localAll = DatabaseRepo.LocalMylist.getAll().associateBy { it.videoCode }
        val merged = mutableListOf<LocalVideoEntity>()
        var page = 1
        var index = 0
        while (page <= 100) {
            val items = runCatching {
                NetworkRepo.getMyListItems(userId, listType, page).first() as? PageLoadingState.Success
            }.getOrNull()?.info?.hanimeInfo ?: break
            if (items.isEmpty()) break
            merged += items.map { toEntity(it, localAll[it.videoCode], index++) }
            page++
        }
        if (merged.isNotEmpty()) {
            DatabaseRepo.LocalMylist.upsertVideos(merged)
        }
    }

    //</editor-fold>

    //<editor-fold desc="推送：本地脏数据">

    private suspend fun pushDirtyFavorites(userId: String, token: String?) {
        for (entity in DatabaseRepo.LocalMylist.getDirtyFavorites()) {
            runCatching {
                NetworkRepo.addToMyFavVideo(
                    videoCode = entity.videoCode,
                    likeStatus = false,
                    currentUserId = userId,
                    token = token,
                ).first()
            }.onSuccess {
                DatabaseRepo.LocalMylist.upsertVideo(entity.copy(favSynced = true))
            }
        }
    }

    private suspend fun pushDirtyWatchLater(userId: String, token: String?) {
        for (entity in DatabaseRepo.LocalMylist.getDirtyWatchLater()) {
            runCatching {
                NetworkRepo.addToMyList(
                    listCode = "save",
                    videoCode = entity.videoCode,
                    isChecked = true,
                    position = 0,
                    csrfToken = token,
                ).first()
            }.onSuccess {
                DatabaseRepo.LocalMylist.upsertVideo(entity.copy(watchLaterSynced = true))
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="播放清单同步">

    private suspend fun syncPlaylists(userId: String, token: String?) {
        val remotePlaylists = pullPlaylistList(userId)
        if (remotePlaylists.isNotEmpty()) {
            val localAll = DatabaseRepo.LocalMylist.getAllPlaylists().associateBy { it.code }
            DatabaseRepo.LocalMylist.upsertPlaylists(
                remotePlaylists.map { remote ->
                    localAll[remote.listCode]?.copy(
                        name = remote.title,
                        synced = true,
                    ) ?: LocalPlaylistEntity(
                        code = remote.listCode,
                        name = remote.title,
                        createdTime = System.currentTimeMillis(),
                        synced = true,
                    )
                }
            )
        }

        pushDirtyPlaylists(token)

        val remoteAfterPush = pullPlaylistList(userId)
        if (remoteAfterPush.isNotEmpty()) {
            val localAll = DatabaseRepo.LocalMylist.getAllPlaylists().associateBy { it.code }
            DatabaseRepo.LocalMylist.upsertPlaylists(
                remoteAfterPush.map { remote ->
                    localAll[remote.listCode]?.copy(
                        name = remote.title,
                        synced = true,
                    ) ?: LocalPlaylistEntity(
                        code = remote.listCode,
                        name = remote.title,
                        createdTime = System.currentTimeMillis(),
                        synced = true,
                    )
                }
            )
        }

        pushDirtyPlaylistItems(token)

        val finalPlaylists = DatabaseRepo.LocalMylist.getAllPlaylists()
        for (playlist in finalPlaylists) {
            if (playlist.synced) {
                pullPlaylistItems(playlist.code)
            }
        }
    }

    private suspend fun pullPlaylistList(userId: String): List<Playlists.Playlist> {
        val result = mutableListOf<Playlists.Playlist>()
        var page = 1
        while (page <= 100) {
            val playlists = runCatching {
                NetworkRepo.getPlaylists(page, userId).first() as? WebsiteState.Success
            }.getOrNull()?.info?.playlists ?: break
            if (playlists.isEmpty()) break
            result += playlists
            page++
        }
        return result
    }

    private suspend fun pullPlaylistItems(playlistCode: String) {
        val localItems = DatabaseRepo.LocalMylist
            .getPlaylistItems(playlistCode)
            .associateBy { it.videoCode }
        val merged = mutableListOf<LocalPlaylistItemEntity>()
        var page = 1
        while (page <= 100) {
            val state = runCatching {
                NetworkRepo.getMyPlayListItems(page, playlistCode).first()
            }.getOrNull() ?: break
            val items = (state as? PageLoadingState.Success)?.info
                ?: break
            if (items.hanimeInfo.isEmpty()) break
            items.desc?.let { desc ->
                DatabaseRepo.LocalMylist.upsertPlaylist(
                    DatabaseRepo.LocalMylist.findPlaylist(playlistCode)?.copy(description = desc)
                        ?: LocalPlaylistEntity(playlistCode, "", desc, System.currentTimeMillis(), true)
                )
            }
            merged += items.hanimeInfo.mapIndexed { index, info ->
                localItems[info.videoCode]?.let { local ->
                    if (!local.synced) local else local.updatedFromRemote(info, index)
                } ?: LocalPlaylistItemEntity(
                    playlistCode = playlistCode,
                    videoCode = info.videoCode,
                    title = info.title,
                    coverUrl = info.coverUrl,
                    duration = info.duration,
                    views = info.views,
                    reviews = info.reviews,
                    currentArtist = info.currentArtist,
                    uploadTime = info.uploadTime,
                    position = index,
                    addedTime = System.currentTimeMillis(),
                    synced = true,
                )
            }
            page++
        }
        if (merged.isNotEmpty()) {
            DatabaseRepo.LocalMylist.upsertPlaylistItems(merged)
        }
    }

    private fun LocalPlaylistItemEntity.updatedFromRemote(info: HanimeInfo, index: Int) =
        copy(
            title = info.title,
            coverUrl = info.coverUrl,
            duration = info.duration,
            views = info.views,
            reviews = info.reviews,
            currentArtist = info.currentArtist,
            uploadTime = info.uploadTime,
            position = index,
            synced = true,
        )

    private suspend fun pushDirtyPlaylists(token: String?) {
        val dirty = DatabaseRepo.LocalMylist.getDirtyPlaylists()
        if (dirty.isEmpty()) return
        for (playlist in dirty) {
            runCatching {
                NetworkRepo.createPlaylist(
                    videoCode = EMPTY_STRING,
                    title = playlist.name,
                    description = playlist.description.orEmpty(),
                    csrfToken = token,
                ).first()
            }
        }
        // 创建成功后云端清单 code 未知，重新拉取并按名称匹配以更新本地 code
        if (!SettingsRepository.isAlreadyLogin) return
        val userId = SettingsRepository.savedUserId
        if (userId.isBlank()) return
        val remote = pullPlaylistList(userId)
        val localAll = DatabaseRepo.LocalMylist.getAllPlaylists().associateBy { it.code }
        for (playlist in dirty) {
            val matched = remote.firstOrNull { it.title == playlist.name && !localAll.containsKey(it.listCode) }
                ?: continue
            val items = DatabaseRepo.LocalMylist.getPlaylistItems(playlist.code)
            if (items.isNotEmpty()) {
                DatabaseRepo.LocalMylist.upsertPlaylistItems(
                    items.map { it.copy(playlistCode = matched.listCode) }
                )
            }
            DatabaseRepo.LocalMylist.upsertPlaylist(
                playlist.copy(code = matched.listCode, synced = true)
            )
            DatabaseRepo.LocalMylist.deletePlaylist(playlist.code)
        }
    }

    private suspend fun pushDirtyPlaylistItems(token: String?) {
        for (item in DatabaseRepo.LocalMylist.getDirtyPlaylistItems()) {
            val parent = DatabaseRepo.LocalMylist.findPlaylist(item.playlistCode) ?: continue
            if (!parent.synced) continue
            runCatching {
                NetworkRepo.addToMyList(
                    listCode = parent.code,
                    videoCode = item.videoCode,
                    isChecked = true,
                    position = item.position,
                    csrfToken = token,
                ).first()
            }.onSuccess {
                DatabaseRepo.LocalMylist.upsertPlaylistItem(item.copy(synced = true))
            }
        }
    }

    //</editor-fold>
}
