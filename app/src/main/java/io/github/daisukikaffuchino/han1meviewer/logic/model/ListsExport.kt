package io.github.daisukikaffuchino.han1meviewer.logic.model

import kotlinx.serialization.Serializable

/**
 * 列表导入/导出文件格式，本地与在线数据共用。
 */
@Serializable
data class ListsExport(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val watchLater: List<ListItemExport> = emptyList(),
    val favorites: List<ListItemExport> = emptyList(),
    val playlists: List<PlaylistExport> = emptyList(),
)

@Serializable
data class PlaylistExport(
    val title: String,
    val desc: String = "",
    val items: List<ListItemExport> = emptyList(),
)

@Serializable
data class ListItemExport(
    val videoCode: String,
    val title: String,
    val coverUrl: String,
    val duration: String? = null,
    val views: String? = null,
    val uploadTime: String? = null,
    val genre: String? = null,
    val reviews: String? = null,
    val currentArtist: String? = null,
    val addedAt: Long = 0,
)
