package com.teledrive.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface KeyEntryDao {

    @Insert
    suspend fun insert(entry: KeyEntry)

    @Query("SELECT * FROM key_entries WHERE messageId = :messageId")
    suspend fun getByKey(messageId: Long): KeyEntry?

    @Query("SELECT * FROM key_entries ORDER BY uploadedAt DESC")
    suspend fun getAll(): List<KeyEntry>

    @Delete
    suspend fun delete(entry: KeyEntry)

    @Query("DELETE FROM key_entries WHERE messageId = :messageId")
    suspend fun deleteByKey(messageId: Long)

    @Query("DELETE FROM key_entries")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM key_entries")
    suspend fun count(): Int
}
