package com.teledrive.android.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FolderEntity::class,
        FileEntity::class,
        TransferEntity::class,
        BackupSettingsEntity::class,
        PreviewCacheEntity::class,
        KeyEntry::class,
    ],
    version = 8,
    exportSchema = false,
)
@TypeConverters(TeleDriveConverters::class)
abstract class TeleDriveDatabase : RoomDatabase() {
    abstract fun dao(): TeleDriveDao
    abstract fun keyEntryDao(): KeyEntryDao

    companion object {
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS preview_cache (
                        cacheKey TEXT NOT NULL,
                        thumbnailLocalPath TEXT,
                        thumbnailReady INTEGER NOT NULL,
                        previewKind TEXT NOT NULL,
                        badgeExt TEXT,
                        badgeColor INTEGER,
                        textSnippet TEXT,
                        snippetBytes INTEGER,
                        mimeType TEXT,
                        sourceFileName TEXT,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(cacheKey)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_preview_cache_updatedAt ON preview_cache(updatedAt)")

                // Indices for file listing performance
                db.execSQL("CREATE INDEX IF NOT EXISTS index_files_folderId_createdAt ON files(folderId, createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_files_createdAt ON files(createdAt)")
            }
        }

        // MIGRATION_5_6: intentionally empty — version 6 was a metadata-only bump with no schema changes.
        // This migration MUST stay registered in AppModule so users on v5 can upgrade without data loss.
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) { /* no-op: schema unchanged */ }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS key_entries (
                        messageId INTEGER NOT NULL,
                        keyBase64 TEXT NOT NULL,
                        originalFilename TEXT NOT NULL,
                        originalMime TEXT NOT NULL,
                        uploadedAt INTEGER NOT NULL,
                        isEncrypted INTEGER NOT NULL,
                        PRIMARY KEY(messageId)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_key_entries_uploadedAt ON key_entries(uploadedAt)")
                
                // Add caption column to files table
                db.execSQL("ALTER TABLE files ADD COLUMN caption TEXT")
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // syncStatus and backupSourcePath columns remain in SQLite
                // but are no longer mapped to FileEntity
            }
        }
    }
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
            .mapNotNull { name ->
                runCatching { BackupFolder.valueOf(name) }.getOrNull()
            }
            .toSet()

}
