package com.teledrive.android.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [FolderEntity::class, FileEntity::class, TransferEntity::class, BackupSettingsEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(TeleDriveConverters::class)
abstract class TeleDriveDatabase : RoomDatabase() {
    abstract fun dao(): TeleDriveDao
}

class TeleDriveConverters {
    @TypeConverter
    fun transferTypeToString(value: TransferType): String = value.name

    @TypeConverter
    fun transferTypeFromString(value: String): TransferType = TransferType.valueOf(value)

    @TypeConverter
    fun transferStatusToString(value: TransferStatus): String = value.name

    @TypeConverter
    fun transferStatusFromString(value: String): TransferStatus = TransferStatus.valueOf(value)

    @TypeConverter
    fun backupScopeToString(value: BackupScope): String = value.name

    @TypeConverter
    fun backupScopeFromString(value: String): BackupScope = BackupScope.valueOf(value)

    @TypeConverter
    fun backupModeToString(value: BackupMode): String = value.name

    @TypeConverter
    fun backupModeFromString(value: String): BackupMode = BackupMode.valueOf(value)

    @TypeConverter
    fun backupFoldersToString(value: Set<BackupFolder>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun backupFoldersFromString(value: String): Set<BackupFolder> =
        value.split(',')
            .filter { it.isNotBlank() }
            .map { BackupFolder.valueOf(it) }
            .toSet()

    @TypeConverter
    fun syncStatusToString(value: SyncStatus): String = value.name

    @TypeConverter
    fun syncStatusFromString(value: String): SyncStatus = SyncStatus.valueOf(value)
}
