package io.github.daisukikaffuchino.han1meviewer.logic.model

/** [Local] 请求参数枚举（我的列表类型 / 评论位置）。 */
enum class MyListType(val value: String) {
    FAV_VIDEO("likes"), // 喜欢的视频
    WATCH_LATER("saves"), // 稍后再看
}

enum class CommentPlace(val value: String) {
    COMMENT("comment"), // 主評論
    CHILD_COMMENT("reply") // 子評論
}
