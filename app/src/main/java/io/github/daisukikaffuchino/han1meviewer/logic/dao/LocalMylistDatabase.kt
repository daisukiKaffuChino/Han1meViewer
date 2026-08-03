package io.github.daisukikaffuchino.han1meviewer.logic.dao

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalMylistTombstoneEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalPlaylistEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalPlaylistItemEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.mylist.LocalVideoEntity
import io.github.daisukikaffuchino.utils.applicationContext

/**
 * 本地收藏 / 稍后观看 / 播放清单数据库。
 *
 * @project Han1meViewer
 */
@Database(
    entities = [
        LocalVideoEntity::class,
        LocalPlaylistEntity::class,
        LocalPlaylistItemEntity::class,
        LocalMylistTombstoneEntity::class,
    ],
    version = 1, exportSchema = false
)
abstract class LocalMylistDatabase : RoomDatabase() {

    abstract val localMylistDao: LocalMylistDao

    companion object {
        val instance by lazy {
            Room.databaseBuilder(
                applicationContext,
                LocalMylistDatabase::class.java,
                "mylist.db"
            ).build()
        }
    }
}
