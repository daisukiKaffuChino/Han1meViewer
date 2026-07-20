package io.github.daisukikaffuchino.han1meviewer.logic.dao

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.entity.HKeyframeEntity
import io.github.daisukikaffuchino.utils.applicationContext

/**
 * 这是各种 有数据库需求的小功能 的聚集地，
 * 如果这个功能需要数据库就放到这里。
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/11/12 012 12:28
 */
@Database(
    entities = [HKeyframeEntity::class],
    version = 1, exportSchema = false
)
abstract class MiscellanyDatabase : RoomDatabase() {

    abstract val hKeyframeDao: HKeyframeDao

    companion object {
        val instance by lazy {
            Room.databaseBuilder(
                applicationContext,
                MiscellanyDatabase::class.java,
                "miscellany.db"
            ).build()
        }
    }
}
