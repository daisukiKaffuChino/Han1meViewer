package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.logic.dao.LocalListDao
import io.github.daisukikaffuchino.han1meviewer.logic.dao.LocalListDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.dao.LocalPlaylistRow
import io.github.daisukikaffuchino.han1meviewer.logic.entity.LocalListEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.LocalListItemEntity
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 免登录本地列表仓库：稍后再看 / 我喜欢的影片 / 自定义播放清单。
 */
object LocalListRepository {

    const val FAVORITE_CODE = "likes"
    const val WATCH_LATER_CODE = "save"
    const val PLAYLIST_KIND = "playlist"

    private val dao: LocalListDao = LocalListDatabase.instance.localListDao

    fun observeWatchLater(): Flow<List<HanimeInfo>> =
        dao.observeItems(WATCH_LATER_CODE).map { rows -> rows.map { it.toHanimeInfo() } }

    fun observeFavorites(): Flow<List<HanimeInfo>> =
        dao.observeItems(FAVORITE_CODE).map { rows -> rows.map { it.toHanimeInfo() } }

    fun observePlaylistItems(listCode: String): Flow<List<HanimeInfo>> =
        dao.observeItems(listCode).map { rows -> rows.map { it.toHanimeInfo() } }

    fun observePlaylists(): Flow<List<Playlists.Playlist>> =
        dao.observePlaylists().map { rows -> rows.map { it.toPlaylist() } }

    fun observeIsFavorite(videoCode: String): Flow<Boolean> =
        dao.observeIsFavorite(videoCode)

    fun observeIsWatchLater(videoCode: String): Flow<Boolean> =
        dao.observeIsWatchLater(videoCode)

    fun observeListCodes(videoCode: String): Flow<List<String>> =
        dao.observeListCodes(videoCode)

    suspend fun isFavorite(videoCode: String): Boolean =
        dao.findItem(FAVORITE_CODE, videoCode) != null

    suspend fun isWatchLater(videoCode: String): Boolean =
        dao.findItem(WATCH_LATER_CODE, videoCode) != null

    suspend fun setFavorite(videoCode: String, video: HanimeVideo, add: Boolean) =
        setItem(FAVORITE_CODE, videoCode, video, add)

    suspend fun setWatchLater(videoCode: String, video: HanimeVideo, add: Boolean) =
        setItem(WATCH_LATER_CODE, videoCode, video, add)

    suspend fun setPlaylistContains(
        listCode: String,
        videoCode: String,
        video: HanimeVideo,
        add: Boolean,
    ) = setItem(listCode, videoCode, video, add)

    suspend fun removeItem(listCode: String, videoCode: String) =
        dao.deleteItem(listCode, videoCode)

    suspend fun createPlaylist(title: String, desc: String): Playlists.Playlist {
        val now = System.currentTimeMillis()
        val code = "local_" + UUID.randomUUID().toString().replace("-", "")
        dao.upsertPlaylist(
            LocalListEntity(
                listCode = code,
                kind = PLAYLIST_KIND,
                title = title,
                desc = desc,
                createdAt = now,
                updatedAt = now,
            )
        )
        return Playlists.Playlist(listCode = code, title = title, total = 0, coverUrl = null)
    }

    suspend fun updatePlaylist(listCode: String, title: String, desc: String) {
        val current = dao.getPlaylist(listCode) ?: return
        dao.upsertPlaylist(
            current.copy(
                title = title,
                desc = desc,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deletePlaylist(listCode: String) {
        dao.deletePlaylistItems(listCode)
        dao.deletePlaylist(listCode)
    }

    suspend fun getPlaylistDesc(listCode: String): String? =
        dao.getPlaylist(listCode)?.desc

    suspend fun getPlaylistsOnce(): List<Playlists.Playlist> =
        dao.getPlaylistsOnce().map { it.toPlaylist() }

    suspend fun getPlaylistItemsOnce(listCode: String): List<HanimeInfo> =
        dao.getItems(listCode).map { it.toHanimeInfo() }

    private suspend fun setItem(
        listCode: String,
        videoCode: String,
        video: HanimeVideo,
        add: Boolean,
    ) {
        if (add) {
            dao.upsertItem(video.toLocalListItem(listCode, videoCode))
        } else {
            dao.deleteItem(listCode, videoCode)
        }
    }

    private fun LocalListItemEntity.toHanimeInfo(): HanimeInfo =
        HanimeInfo(
            title = title,
            coverUrl = coverUrl,
            videoCode = videoCode,
            duration = duration,
            views = views,
            uploadTime = uploadTime,
            genre = genre,
            isPlaying = false,
            itemType = itemType,
            reviews = reviews ?: "",
            currentArtist = currentArtist ?: "",
            watched = false,
        )

    private fun HanimeVideo.toLocalListItem(
        listCode: String,
        videoCode: String,
    ): LocalListItemEntity =
        LocalListItemEntity(
            listCode = listCode,
            videoCode = videoCode,
            title = title,
            coverUrl = coverUrl,
            duration = null,
            views = views,
            uploadTime = uploadTime?.toString(),
            genre = null,
            reviews = null,
            currentArtist = artist?.name,
            itemType = HanimeInfo.NORMAL,
            addedAt = System.currentTimeMillis(),
        )

    private fun LocalPlaylistRow.toPlaylist(): Playlists.Playlist =
        Playlists.Playlist(
            listCode = listCode,
            title = title,
            total = total,
            coverUrl = coverUrl,
        )
}
