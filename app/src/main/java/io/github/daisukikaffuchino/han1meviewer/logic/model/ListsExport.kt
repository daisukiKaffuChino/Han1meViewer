package io.github.daisukikaffuchino.han1meviewer.logic.model

import kotlinx.serialization.Serializable

/**
 * [Local] 本地 / 在线列表的导入导出文件格式。
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
