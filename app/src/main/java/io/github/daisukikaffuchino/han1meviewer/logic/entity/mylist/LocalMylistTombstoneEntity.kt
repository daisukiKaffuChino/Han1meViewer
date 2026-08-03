package io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 本地删除墓碑。
 *
 * 未登录时删除的收藏 / 稍后观看条目记录在这里，
 * 登录后由同步引擎先推送到云端（删除），再拉取合并，避免被云端数据「复活」。
 *
 * @project Han1meViewer
 */
@Serializable
@Entity(tableName = "LocalMylistTombstoneEntity")
data class LocalMylistTombstoneEntity(
    @PrimaryKey val videoCode: String,
    val isFav: Boolean = false,
    val isWatchLater: Boolean = false,
    val deletedTime: Long,
)
