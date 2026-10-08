package io.github.daisukikaffuchino.han1meviewer.logic.model

/** [Domain] 领域模型的结构约束：列表项字段。 */
interface VideoItemType {
    val title: String
    val coverUrl: String
    val videoCode: String
    val duration: String?
    val views: String?
    val reviews: String?
    val currentArtist: String?
    val uploadTime: String?
}
