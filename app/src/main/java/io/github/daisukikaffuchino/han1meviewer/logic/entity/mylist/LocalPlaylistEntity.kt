package io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 本地播放清单。
 *
 * 未登录时清单保存在本地（code 为本地生成的 id），
 * 登录后由同步引擎推送到云端并覆盖为云端 code。
 *
 * @project Han1meViewer
 */
@Serializable
@Entity(tableName = "LocalPlaylistEntity")
data class LocalPlaylistEntity(
    @PrimaryKey val code: String,
    val name: String,
    val description: String? = null,
    val createdTime: Long,
    val synced: Boolean = false,
)
