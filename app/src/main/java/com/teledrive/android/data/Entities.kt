package com.teledrive.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val markerSource: String,
    val updatedAt: Long,
)

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val messageId: Long,
    val folderId: Long?,
    val name: String,
    val size: Long,
    val mimeType: String?,
    val extension: String?,
    val createdAt: Long,
    val localCachePath: String?,
    val thumbnailBase64: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val backupSourcePath: String? = null,
)

enum class SyncStatus {
    LocalOnly,
    CloudOnly,
    Synced,
}

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val id: String,
    val type: TransferType,
    val fileName: String,
    val folderId: Long?,
    val messageId: Long?,
    val status: TransferStatus,
    val progress: Int,
    val error: String?,
)

enum class TransferType {
    Upload,
    Download,
}

enum class TransferStatus {
    Pending,
    Running,
    Success,
    Error,
    Cancelled,
}

@Entity(tableName = "backup_settings")
data class BackupSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val scope: BackupScope = BackupScope.CommonFolders,
    val mode: BackupMode = BackupMode.OneWay,
    val folders: Set<BackupFolder> = setOf(
        BackupFolder.Camera,
        BackupFolder.Screenshots,
        BackupFolder.Downloads,
    ),
    val wifiOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val instantBackup: Boolean = true,
    val dailyBackup: Boolean = false,
    val lastBackupAt: Long? = null,
)

enum class BackupScope(val label: String) {
    CommonFolders("Common folders"),
    EntireStorage("Entire storage"),
}

enum class BackupMode(val label: String) {
    OneWay("One-way backup"),
    TwoWay("Two-way sync"),
}

enum class BackupFolder(val label: String) {
    Camera("Camera"),
    Screenshots("Screenshots"),
    Downloads("Downloads"),
    WhatsAppImages("WhatsApp Images"),
    WhatsAppVideo("WhatsApp Video"),
    Documents("Documents"),
    CustomFolder("Custom folder"),
}
