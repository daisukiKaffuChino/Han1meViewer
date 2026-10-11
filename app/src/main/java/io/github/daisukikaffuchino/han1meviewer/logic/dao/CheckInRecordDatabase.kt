package io.github.daisukikaffuchino.han1meviewer.logic.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.entity.CheckInRecordEntity

@Database(
    entities = [CheckInRecordEntity::class],
    version = 5,
    exportSchema = false
)
abstract class CheckInRecordDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInRecordDao

    companion object {
        @Volatile
        private var INSTANCE: CheckInRecordDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE check_in_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        type TEXT NOT NULL DEFAULT '自慰',
                        feeling TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                val cursor = db.query("SELECT date, count FROM check_in_records")
                while (cursor.moveToNext()) {
                    val date = cursor.getString(0)
                    val count = cursor.getInt(1)
                    repeat(count.coerceAtMost(20)) {
                        db.execSQL(
                            "INSERT INTO check_in_records_new (date, type, feeling) VALUES (?, '自慰', '')",
                            arrayOf(date)
                        )
                    }
                }
                cursor.close()
                db.execSQL("DROP TABLE check_in_records")
                db.execSQL("ALTER TABLE check_in_records_new RENAME TO check_in_records")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE check_in_records ADD COLUMN time TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE check_in_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        type TEXT NOT NULL,
                        feeling TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO check_in_records_new (id, date, time, type, feeling)
                    SELECT id, date, time, type, feeling FROM check_in_records
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE check_in_records")
                db.execSQL("ALTER TABLE check_in_records_new RENAME TO check_in_records")
                db.execSQL("DROP TABLE IF EXISTS sidedishes")
            }
        }

        fun getDatabase(context: Context): CheckInRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CheckInRecordDatabase::class.java,
                    "check_in_records"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                    )
                    // 不启用破坏性回退：缺迁移路径时宁可显式报错，也不要静默清空签到记录。
                    // 1→5 的显式迁移已全部提供，正常升级不会走到这里。
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
