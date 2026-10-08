package io.github.daisukikaffuchino.han1meviewer.logic.model

/** [DTO] 网页解析产物（Getchu）。 */
data class GetchuPreview(
    val dateCode: String,
    val groups: List<Group>,
) {
    data class Group(
        val releaseDate: String,
        val items: List<Item>,
    )

    data class Item(
        val id: String,
        val title: String,
        val brand: String?,
        val coverUrl: String?,
        val detailUrl: String,
        val price: String?,
    )
}

data class GetchuPreviewDetail(
    val id: String,
    val title: String,
    val brand: String?,
    val coverUrl: String?,
    val description: String?,
    val releaseDate: String?,
    val price: String?,
    val productUrl: String,
    val videoUrls: List<String>,
    val sections: List<TextSection>,
    val sampleImages: List<String>,
    val seriesItems: List<GetchuPreview.Item>,
    val relatedItems: List<GetchuPreview.Item>,
) {
    data class TextSection(
        val title: String,
        val body: String,
    )
}
