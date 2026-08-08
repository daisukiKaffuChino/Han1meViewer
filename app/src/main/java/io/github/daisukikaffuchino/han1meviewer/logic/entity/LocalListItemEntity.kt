package io.github.daisukikaffuchino.han1meviewer.logic.entity

import androidx.room.Entity

/**
 * 本地列表中的单个视频元数据快照。
 *
 * listCode 约定：
 * - "likes"：我喜欢的影片
 * - "save"：稍后再看
 * - "local_<uuid>"：本地自定义播放清单
 */
@Entity(
    tableName = "LocalListItemEntity",
    primaryKeys = ["listCode", "videoCode"],
)
data class LocalListItemEntity(
    val listCode: String,
    val videoCode: String,
    val title: String,
    val coverUrl: String,
    val duration: String? = null,
    val views: String? = null,
    val uploadTime: String? = null,
    val genre: String? = null,
    val reviews: String? = null,
    val currentArtist: String? = null,
    val addedAt: Long,
)
