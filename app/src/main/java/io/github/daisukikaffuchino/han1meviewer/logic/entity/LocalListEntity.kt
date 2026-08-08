package io.github.daisukikaffuchino.han1meviewer.logic.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地自定义播放清单元数据。
 *
 * 内置的"稍后再看"和"我喜欢的影片"不在此表保存，
 * 只使用 [LocalListItemEntity] 中保留的 listCode："save" 与 "likes"。
 */
@Entity(tableName = "LocalListEntity")
data class LocalListEntity(
    @PrimaryKey
    val listCode: String,
    val kind: String,
    val title: String,
    val desc: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)
