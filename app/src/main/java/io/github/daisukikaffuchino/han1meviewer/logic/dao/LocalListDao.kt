package io.github.daisukikaffuchino.han1meviewer.logic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.daisukikaffuchino.han1meviewer.logic.entity.LocalListEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.LocalListItemEntity
import kotlinx.coroutines.flow.Flow

data class LocalPlaylistRow(
    val listCode: String,
    val title: String,
    val desc: String,
    val createdAt: Long,
    val updatedAt: Long,
    val total: Int,
    val coverUrl: String?,
)

@Dao
interface LocalListDao {

    @Query(
        """
        SELECT l.listCode, l.title, l.desc, l.createdAt, l.updatedAt,
               (SELECT COUNT(*) FROM LocalListItemEntity i WHERE i.listCode = l.listCode) AS total,
               (SELECT coverUrl FROM LocalListItemEntity i WHERE i.listCode = l.listCode
                ORDER BY addedAt DESC LIMIT 1) AS coverUrl
        FROM LocalListEntity l
        WHERE l.kind = 'playlist'
        ORDER BY l.createdAt DESC
        """
    )
    fun observePlaylists(): Flow<List<LocalPlaylistRow>>

    @Query(
        """
        SELECT l.listCode, l.title, l.desc, l.createdAt, l.updatedAt,
               (SELECT COUNT(*) FROM LocalListItemEntity i WHERE i.listCode = l.listCode) AS total,
               (SELECT coverUrl FROM LocalListItemEntity i WHERE i.listCode = l.listCode
                ORDER BY addedAt DESC LIMIT 1) AS coverUrl
        FROM LocalListEntity l
        WHERE l.kind = 'playlist'
        ORDER BY l.createdAt DESC
        """
    )
    suspend fun getPlaylistsOnce(): List<LocalPlaylistRow>

    @Query("SELECT * FROM LocalListEntity WHERE listCode = :listCode LIMIT 1")
    suspend fun getPlaylist(listCode: String): LocalListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(entity: LocalListEntity)

    @Query("DELETE FROM LocalListEntity WHERE listCode = :listCode")
    suspend fun deletePlaylist(listCode: String)

    @Query("DELETE FROM LocalListItemEntity WHERE listCode = :listCode")
    suspend fun deletePlaylistItems(listCode: String)

    @Query("DELETE FROM LocalListItemEntity")
    suspend fun deleteAllItems()

    @Query("DELETE FROM LocalListEntity")
    suspend fun deleteAllPlaylists()

    @Query("SELECT * FROM LocalListItemEntity WHERE listCode = :listCode ORDER BY addedAt DESC")
    fun observeItems(listCode: String): Flow<List<LocalListItemEntity>>

    @Query("SELECT * FROM LocalListItemEntity WHERE listCode = :listCode ORDER BY addedAt DESC")
    suspend fun getItems(listCode: String): List<LocalListItemEntity>

    @Query(
        "SELECT * FROM LocalListItemEntity WHERE listCode = :listCode AND videoCode = :videoCode LIMIT 1"
    )
    suspend fun findItem(listCode: String, videoCode: String): LocalListItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(entity: LocalListItemEntity)

    @Query("DELETE FROM LocalListItemEntity WHERE listCode = :listCode AND videoCode = :videoCode")
    suspend fun deleteItem(listCode: String, videoCode: String)

    @Query("SELECT DISTINCT listCode FROM LocalListItemEntity WHERE videoCode = :videoCode")
    fun observeListCodes(videoCode: String): Flow<List<String>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM LocalListItemEntity WHERE listCode = 'likes' AND videoCode = :videoCode)"
    )
    fun observeIsFavorite(videoCode: String): Flow<Boolean>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM LocalListItemEntity WHERE listCode = 'save' AND videoCode = :videoCode)"
    )
    fun observeIsWatchLater(videoCode: String): Flow<Boolean>
}
