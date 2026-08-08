package io.github.daisukikaffuchino.han1meviewer.logic.dao

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.entity.LocalListEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.LocalListItemEntity
import io.github.daisukikaffuchino.utils.applicationContext

@Database(
    entities = [LocalListEntity::class, LocalListItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LocalListDatabase : RoomDatabase() {

    abstract val localListDao: LocalListDao

    companion object {
        val instance by lazy {
            Room.databaseBuilder(
                applicationContext,
                LocalListDatabase::class.java,
                "local_list.db",
            ).build()
        }
    }
}
