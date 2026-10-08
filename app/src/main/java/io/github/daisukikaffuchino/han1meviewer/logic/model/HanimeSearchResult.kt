package io.github.daisukikaffuchino.han1meviewer.logic.model

/**
 * 搜索结果：一页的影片列表 + 总页数。
 * [DTO] 网页解析产物（搜索结果页）。
 */
data class HanimeSearchResult(
    val list: List<HanimeInfo>,
    val totalPages: Int,
)
