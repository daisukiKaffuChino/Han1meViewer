package io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoItemType
import kotlinx.serialization.Serializable

/**
 * 本地收藏 / 稍后观看条目。
 *
 * 未登录时收藏和稍后观看保存在本地，
 * 登录后由同步引擎与云端双向合并（见 MylistSyncManager）。
 *
 * @project Han1meViewer
 */
@Serializable
@Entity(tableName = "LocalVideoEntity")
data class LocalVideoEntity(
    @PrimaryKey val videoCode: String,
    val title: String,
    val coverUrl: String,
    val duration: String? = null,
    val views: String? = null,
    val reviews: String? = null,
    val currentArtist: String? = null,
    val uploadTime: String? = null,
    val isFav: Boolean = false,
    val isWatchLater: Boolean = false,
    val favSynced: Boolean = false,
    val watchLaterSynced: Boolean = false,
    val favTime: Long = 0L,
    val watchLaterTime: Long = 0L,
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
