package io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist

import androidx.room.Entity
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoItemType
import kotlinx.serialization.Serializable

/**
 * 本地播放清单内的视频条目。
 *
 * @project Han1meViewer
 */
@Serializable
@Entity(tableName = "LocalPlaylistItemEntity", primaryKeys = ["playlistCode", "videoCode"])
data class LocalPlaylistItemEntity(
    val playlistCode: String,
    val videoCode: String,
    val title: String,
    val coverUrl: String,
    val duration: String? = null,
    val views: String? = null,
    val reviews: String? = null,
    val currentArtist: String? = null,
    val uploadTime: String? = null,
    val position: Int,
    val addedTime: Long,
    val synced: Boolean = false,
) : VideoItemType {

    fun toHanimeInfo(itemType: Int = HanimeInfo.NORMAL): HanimeInfo = HanimeInfo(
        title = title,
        coverUrl = coverUrl,
        videoCode = videoCode,
        duration = duration,
        views = views,
        uploadTime = uploadTime,
        itemType = itemType,
        reviews = reviews,
        currentArtist = currentArtist,
    )
}
