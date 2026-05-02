package com.teledrive.android.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TeleDriveDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM files WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId ORDER BY createdAt DESC")
    fun observeFiles(folderId: Long?): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM transfers ORDER BY rowid DESC")
    fun observeTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM backup_settings WHERE id = 1")
    fun observeBackupSettings(): Flow<BackupSettingsEntity?>

    @Upsert
    suspend fun upsertFolders(folders: List<FolderEntity>)

    @Upsert
    suspend fun upsertFiles(files: List<FileEntity>)

    @Upsert
    suspend fun upsertTransfer(transfer: TransferEntity)

    @Upsert
    suspend fun upsertBackupSettings(settings: BackupSettingsEntity)

    @Query("DELETE FROM files WHERE folderId IS :folderId")
    suspend fun clearFilesForFolder(folderId: Long?)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    @Query("DELETE FROM files WHERE messageId IN (:messageIds)")
    suspend fun deleteFiles(messageIds: List<Long>)
}
