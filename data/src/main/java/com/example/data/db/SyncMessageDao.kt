package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SyncMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<SyncMessageEntity>)

    @Update
    suspend fun update(message: SyncMessageEntity)

    @Delete
    suspend fun delete(message: SyncMessageEntity)

    @Query("SELECT * FROM sync_messages WHERE id = :id")
    suspend fun getById(id: String): SyncMessageEntity?

    @Query("SELECT * FROM sync_messages ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SyncMessageEntity>>

    @Query("SELECT * FROM sync_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatest(limit: Int = 50): List<SyncMessageEntity>

    @Query("SELECT * FROM sync_messages WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getByStatus(status: String): List<SyncMessageEntity>

    @Query("SELECT * FROM sync_messages WHERE direction = :direction ORDER BY timestamp DESC")
    suspend fun getByDirection(direction: String): List<SyncMessageEntity>

    @Query("SELECT * FROM sync_messages WHERE status = :status AND direction = :direction ORDER BY timestamp DESC")
    suspend fun getByStatusAndDirection(status: String, direction: String): List<SyncMessageEntity>

    @Query("DELETE FROM sync_messages WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM sync_messages")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sync_messages")
    suspend fun getCount(): Int
}
