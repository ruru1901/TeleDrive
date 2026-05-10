package com.teledrive.android.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val markerSource: String,
    val updatedAt: Long,
)

@Stable
@Entity(
    tableName = "files",
    indices = [
        Index(value = ["folderId", "createdAt"]),
        Index(value = ["createdAt"]),
    ],
)
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
    val tdFileId: Int? = null,
    val tdRemoteUniqueId: String? = null,
    val tdThumbnailFileId: Int? = null,
    val tdThumbnailLocalPath: String? = null,
    val caption: String? = null,
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
        BackupFolder.WhatsAppImages,
        BackupFolder.WhatsAppVideo,
        BackupFolder.Documents,
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
}

@Stable
@Entity(
    tableName = "preview_cache",
    indices = [
        Index(value = ["updatedAt"]),
    ],
)
data class PreviewCacheEntity(
    @PrimaryKey val cacheKey: String,
    val thumbnailLocalPath: String? = null,
    val thumbnailReady: Boolean = false,
    /** thumbnail | badge | text_snippet | mixed */
    val previewKind: String = "badge",
    val badgeExt: String? = null,
    val badgeColor: Int? = null,
    val textSnippet: String? = null,
    val snippetBytes: Int? = null,
    val mimeType: String? = null,
    val sourceFileName: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
