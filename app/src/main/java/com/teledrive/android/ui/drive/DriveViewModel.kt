package com.teledrive.android.ui.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teledrive.android.crypto.GhostCrypto
import com.teledrive.android.data.FileEntity
import com.teledrive.android.data.FolderEntity
import com.teledrive.android.data.BackupSettingsEntity
import com.teledrive.android.data.TeleDriveDatabase
import com.teledrive.android.data.TransferEntity
import com.teledrive.android.data.TransferStatus
import com.teledrive.android.data.TransferType
import com.teledrive.android.data.PreviewCacheEntity
import com.teledrive.android.data.KeyEntry
import com.teledrive.android.drive.toEntity
import com.teledrive.android.telegram.TelegramGateway
import com.teledrive.android.repository.KeystoreRepository
import com.teledrive.android.MasterPasswordService
import com.teledrive.android.secure.SecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class DriveViewModel @Inject constructor(
    private val context: Context,
    database: TeleDriveDatabase,
    private val gateway: TelegramGateway,
    private val backupManager: com.teledrive.android.backup.BackupManager,
    val keystoreRepository: KeystoreRepository? = null,
    val masterPasswordService: MasterPasswordService? = null,
) : ViewModel() {
    private val dao = database.dao()
    private val activeFolderId = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")
    private val busy = MutableStateFlow(false)
    private val notificationMessage = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    private val filesFlow = combine(activeFolderId, query) { folderId, search ->
        folderId to search
    }.flatMapLatest { (folderId, search) ->
        if (search.length > 2) dao.searchFiles(search) else dao.observeFiles(folderId)
    }.distinctUntilChanged()

    private val thumbnailSemaphore = Semaphore(8)

    /** Thumbnail ImageBitmap cache keyed by previewCacheKey — decoded once, never on main thread. */
    val thumbnailBitmapCache = mutableStateMapOf<String, ImageBitmap>()

    private val LIST_FILES_LIMIT = 100
    private var isFirstRefresh = true
    private val pendingThumbnailDownloads = mutableSetOf<Long>()

    fun loadThumbnail(cacheKey: String, base64: String) {
        if (thumbnailBitmapCache.containsKey(cacheKey)) return
        viewModelScope.launch(Dispatchers.IO) {
            thumbnailSemaphore.withPermit {
                if (thumbnailBitmapCache.containsKey(cacheKey)) return@withPermit
                val bmp = decodeThumbnailBase64(base64)
                if (bmp != null) thumbnailBitmapCache[cacheKey] = bmp
            }
        }
    }

    val sortOrder = MutableStateFlow(FileSort.Created)

    val sortedFiles: StateFlow<List<com.teledrive.android.data.FileEntity>> = combine(
        filesFlow, sortOrder
    ) { files, order ->
        files.sortedWith(order.comparator)
    }.distinctUntilChanged()
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val backupSettingsFlow = dao.observeBackupSettings().map { it ?: BackupSettingsEntity() }

    private fun previewCacheKeyFor(messageId: Long, folderId: Long?, remoteUniqueId: String?): String =
        remoteUniqueId?.takeIf { it.isNotBlank() } ?: "${folderId ?: 0L}:$messageId"

    private val previewCacheFlow = filesFlow
        .map { files -> files.map { previewCacheKeyFor(it.messageId, it.folderId, it.tdRemoteUniqueId) } }
        .distinctUntilChanged()
        .flatMapLatest { keys ->
            if (keys.isEmpty()) flowOf(emptyList()) else dao.observePreviewCache(keys)
        }

    val uiState: StateFlow<DriveUiState> = combine(
        dao.observeFolders(),
        filesFlow,
        dao.observeTransfers(),
        backupSettingsFlow,
        gateway.downloadDestinationLabel,
        previewCacheFlow,
        activeFolderId,
        query,
        busy,
        message,
        notificationMessage,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DriveUiState(
            folders = values[0] as List<FolderEntity>,
            files = values[1] as List<FileEntity>,
            transfers = values[2] as List<TransferEntity>,
            backupSettings = values[3] as BackupSettingsEntity,
            downloadDestinationLabel = values[4] as String,
            previewCache = (values[5] as List<PreviewCacheEntity>).associateBy { it.cacheKey },
            activeFolderId = values[6] as Long?,
            query = values[7] as String,
            busy = values[8] as Boolean,
            message = values[9] as String?,
            notificationMessage = values[10] as String?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DriveUiState(),
    )

    init {
        viewModelScope.launch {
            gateway.authState.collect { state ->
                if (state == com.teledrive.android.telegram.AuthState.Ready) {
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            dao.prunePreviewCache(weekAgo)
        }
    }

    fun selectFolder(folderId: Long?) {
        activeFolderId.value = folderId
        refreshFiles(folderId)
    }

    fun setQuery(value: String) {
        query.value = value
    }

    private fun scheduleThumbnailDownloads() {
        val files = uiState.value.files
        val needing = files.filter {
            it.tdThumbnailFileId != null &&
            it.tdThumbnailLocalPath.isNullOrBlank() &&
            it.messageId !in pendingThumbnailDownloads
        }
        if (needing.isEmpty()) return

        viewModelScope.launch {
            for (file in needing) {
                thumbnailSemaphore.withPermit {
                    if (file.messageId in pendingThumbnailDownloads) return@withPermit
                    pendingThumbnailDownloads += file.messageId
                    try {
                        val path = gateway.downloadThumbnail(file.tdThumbnailFileId!!)
                        if (path != null) {
                            val updated = file.copy(tdThumbnailLocalPath = path)
                            dao.upsertFiles(listOf(updated))
                            val cacheKey = previewCacheKeyFor(file.messageId, file.folderId, file.tdRemoteUniqueId)
                            dao.upsertPreviewCache(
                                listOf(
                                    PreviewCacheEntity(
                                        cacheKey = cacheKey,
                                        thumbnailLocalPath = path,
                                        thumbnailReady = true,
                                        previewKind = "thumbnail",
                                        badgeExt = file.extension,
                                        mimeType = file.mimeType,
                                        sourceFileName = file.name,
                                        updatedAt = System.currentTimeMillis(),
                                    )
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Log but don't crash
                    } finally {
                        pendingThumbnailDownloads -= file.messageId
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runStep {
                dao.upsertFolders(gateway.scanFolders().map { it.toEntity() })
                refreshFilesInternal(activeFolderId.value)
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            runStep {
                val folder = gateway.createFolder(name)
                dao.upsertFolders(listOf(folder.toEntity()))
            }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            runStep {
                gateway.deleteFolder(folderId)
                dao.deleteFolder(folderId)
                if (activeFolderId.value == folderId) activeFolderId.value = null
            }
        }
    }

    fun upload(uri: Uri, displayName: String, encrypt: Boolean = false) {
        val transferId = UUID.randomUUID().toString()
        val folderId = activeFolderId.value
        viewModelScope.launch {
            dao.upsertTransfer(
                TransferEntity(
                    id = transferId,
                    type = TransferType.Upload,
                    fileName = displayName,
                    folderId = folderId,
                    messageId = null,
                    status = TransferStatus.Pending,
                    progress = 0,
                    error = null,
                ),
            )
            try {
                var uploadedMessageId: Long? = null
                withContext(Dispatchers.IO) {
                    if (encrypt) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw IllegalStateException("Unable to open selected file.")
                        val cryptoResult = GhostCrypto.encryptFile(bytes)
                        val packed = GhostCrypto.packGhostFile(cryptoResult.nonce, cryptoResult.ciphertext)
                        val encryptedFile = File(context.cacheDir, "${displayName}.ghost")
                        FileOutputStream(encryptedFile).use { it.write(packed) }
                        val encryptedUri = Uri.fromFile(encryptedFile)
                        gateway.uploadFile(encryptedUri, "${displayName}.ghost", folderId, null).collect { progress ->
                            if (progress.done && progress.error == null) {
                                uploadedMessageId = progress.messageId
                            }
                        }
                        val msgId = uploadedMessageId ?: throw IllegalStateException("Upload failed")
                        keystoreRepository?.saveKey(KeyEntry(
                            messageId = msgId,
                            keyBase64 = cryptoResult.keyBase64,
                            originalFilename = displayName,
                            originalMime = context.contentResolver.getType(uri) ?: "application/octet-stream",
                            uploadedAt = System.currentTimeMillis(),
                            isEncrypted = true,
                        ))
                        masterPasswordService?.syncKeystore()
                    } else {
                        gateway.uploadFile(uri, displayName, folderId, null).collect { progress ->
                            if (progress.done && progress.error == null) {
                                uploadedMessageId = progress.messageId
                            }
                        }
                        if (uploadedMessageId == null) throw IllegalStateException("Upload failed")
                    }
                }
                val msgId = uploadedMessageId ?: throw IllegalStateException("Upload failed")
                dao.upsertTransfer(
                    TransferEntity(
                        id = transferId,
                        type = TransferType.Upload,
                        fileName = displayName,
                        folderId = folderId,
                        messageId = msgId,
                        status = TransferStatus.Success,
                        progress = 100,
                        error = null,
                    ),
                )
                notificationMessage.value = if (encrypt) "Encrypted & uploaded" else "File $displayName uploaded"
                viewModelScope.launch { delay(2000); notificationMessage.value = null }
            } catch (e: Throwable) {
                dao.upsertTransfer(
                    TransferEntity(
                        id = transferId,
                        type = TransferType.Upload,
                        fileName = displayName,
                        folderId = folderId,
                        messageId = null,
                        status = TransferStatus.Error,
                        progress = 0,
                        error = e.message ?: "Upload failed",
                    ),
                )
                message.value = e.message ?: "Upload failed."
            }
            refreshFilesInternal(folderId)
        }
    }

    fun download(file: FileEntity) { startDownload(file, destination = null) }
    fun downloadTo(file: FileEntity, destination: Uri) { startDownload(file, destination) }

    private fun startDownload(file: FileEntity, destination: Uri?) {
        val transferId = UUID.randomUUID().toString()
        viewModelScope.launch {
            dao.upsertTransfer(
                TransferEntity(
                    id = transferId,
                    type = TransferType.Download,
                    fileName = file.name,
                    folderId = file.folderId,
                    messageId = file.messageId,
                    status = TransferStatus.Pending,
                    progress = 0,
                    error = null,
                ),
            )
            try {
                var lastLocalPath: String? = null
                val flow = if (destination == null) gateway.downloadFile(file.messageId, file.folderId)
                           else gateway.downloadFileTo(file.messageId, file.folderId, destination)
                var completed = false
                flow.collect { progress ->
                    dao.upsertTransfer(
                        TransferEntity(
                            id = transferId,
                            type = TransferType.Download,
                            fileName = file.name,
                            folderId = file.folderId,
                            messageId = file.messageId,
                            status = when {
                                progress.error != null -> TransferStatus.Error
                                progress.done -> TransferStatus.Success
                                else -> TransferStatus.Running
                            },
                            progress = progress.progress,
                            error = progress.error,
                        ),
                    )
                    if (progress.localPath != null) lastLocalPath = progress.localPath
                    completed = progress.done && progress.error == null
                }
                if (completed) {
                    val isEncrypted = file.name.endsWith(".ghost") && file.caption?.contains("#ghost_enc") == true
                    if (isEncrypted && lastLocalPath != null) {
                        val keyEntry = keystoreRepository?.getKey(file.messageId)
                        if (keyEntry != null) {
                            val encryptedBytes = File(lastLocalPath!!).readBytes()
                            val decryptedBytes = GhostCrypto.decryptFile(encryptedBytes, keyEntry.keyBase64)
                            val decryptedFileName = file.name.removeSuffix(".ghost")
                            if (decryptedBytes != null) {
                                val decryptedFile = File(context.cacheDir, decryptedFileName)
                                FileOutputStream(decryptedFile).use { it.write(decryptedBytes) }
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", decryptedFile)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, file.mimeType ?: "application/octet-stream")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open $decryptedFileName"))
                                notificationMessage.value = "Decrypted & opened: $decryptedFileName"
                            } else {
                                notificationMessage.value = "Decryption failed."
                            }
                        } else {
                            notificationMessage.value = "Key not found. Check Security settings."
                        }
                    } else {
                        refreshFilesInternal(activeFolderId.value)
                        notificationMessage.value = "File ${file.name} downloaded"
                    }
                    viewModelScope.launch { delay(2000); notificationMessage.value = null }
                }
            } catch (e: Throwable) {
                dao.upsertTransfer(
                    TransferEntity(
                        id = transferId,
                        type = TransferType.Download,
                        fileName = file.name,
                        folderId = file.folderId,
                        messageId = file.messageId,
                        status = TransferStatus.Error,
                        progress = 0,
                        error = e.message ?: "Download failed",
                    ),
                )
                message.value = e.message ?: "Download failed."
            }
        }
    }

    fun setDownloadDestination(uri: Uri) {
        gateway.setDownloadDestination(uri)
        message.value = "Download folder set."
    }

    fun clearDownloadDestination() {
        gateway.clearDownloadDestination()
        message.value = "Download folder reset."
    }

    fun deleteFile(file: FileEntity) {
        viewModelScope.launch { runStep { gateway.deleteFiles(listOf(file.messageId), file.folderId); dao.deleteFiles(listOf(file.messageId)) } }
    }

    fun deleteFiles(files: List<FileEntity>) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            runStep {
                files.groupBy { it.folderId }.forEach { (folderId, folderFiles) ->
                    val ids = folderFiles.map { it.messageId }
                    gateway.deleteFiles(ids, folderId)
                    dao.deleteFiles(ids)
                }
            }
        }
    }

    fun moveFiles(files: List<FileEntity>, targetFolderId: Long?) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            runStep {
                files.groupBy { it.folderId }.forEach { (sourceFolderId, folderFiles) ->
                    gateway.moveFiles(folderFiles.map { it.messageId }, sourceFolderId, targetFolderId)
                }
                refreshFilesInternal(activeFolderId.value)
            }
        }
    }

    fun updateBackupSettings(settings: BackupSettingsEntity) {
        viewModelScope.launch { dao.upsertBackupSettings(settings); backupManager.scheduleBackup(settings) }
    }

    fun recordBackupRun() {
        viewModelScope.launch {
            val current = uiState.value.backupSettings
            dao.upsertBackupSettings(current.copy(lastBackupAt = System.currentTimeMillis()))
            message.value = "Backup started."
            backupManager.scheduleBackup(current.copy(instantBackup = true))
        }
    }

    fun cancelTransfer(id: String) {
        viewModelScope.launch {
            val allTransfers = dao.observeTransfers().first()
            val existing = allTransfers.firstOrNull { it.id == id }
            dao.upsertTransfer(TransferEntity(
                id = id,
                type = existing?.type ?: TransferType.Upload,
                fileName = existing?.fileName ?: "",
                folderId = existing?.folderId,
                messageId = existing?.messageId,
                status = TransferStatus.Cancelled,
                progress = 0,
                error = null,
            ))
        }
    }

    fun restoreAllFiles() {
        val files = uiState.value.files
        if (files.isEmpty()) { message.value = "There are no files to restore."; return }
        files.forEach { download(it) }
        message.value = "Restore started for ${files.size} files."
    }

    private fun refreshFiles(folderId: Long?) {
        viewModelScope.launch { runStep { refreshFilesInternal(folderId) } }
    }

    private suspend fun refreshFilesInternal(folderId: Long?) {
        val gatewayFiles = gateway.listFiles(folderId)
        val files = gatewayFiles.map { it.toEntity() }

        // If gateway returns empty, only clear if this is NOT the first refresh.
        if (files.isEmpty()) {
            if (!isFirstRefresh) {
                dao.clearFilesForFolder(folderId)
            }
            isFirstRefresh = false
            return
        }

        dao.upsertFiles(files)

        val previewEntries = gatewayFiles.mapNotNull { tf ->
            val key = previewCacheKeyFor(tf.messageId, tf.folderId, tf.tdRemoteUniqueId)
            val thumbPath = tf.tdThumbnailLocalPath?.takeIf { java.io.File(it).exists() }
            PreviewCacheEntity(
                cacheKey = key,
                thumbnailLocalPath = thumbPath,
                thumbnailReady = thumbPath != null,
                previewKind = if (thumbPath != null) "thumbnail" else "badge",
                badgeExt = tf.extension,
                mimeType = tf.mimeType,
                sourceFileName = tf.name,
                updatedAt = System.currentTimeMillis(),
            )
        }
        if (previewEntries.isNotEmpty()) dao.upsertPreviewCache(previewEntries)

        // Only delete missing files if we likely have the complete list (size < limit).
        if (!isFirstRefresh && files.size < LIST_FILES_LIMIT) {
            val messageIds = files.map { it.messageId }
            if (folderId == null) dao.deleteMissingRootFiles(messageIds) else dao.deleteMissingFilesInFolder(folderId, messageIds)
        }

        isFirstRefresh = false

        // Trigger async thumbnail downloads for files that need them
        scheduleThumbnailDownloads()
    }

    private suspend fun runStep(block: suspend () -> Unit) {
        busy.value = true
        message.value = null
        try { block() } catch (t: Throwable) { message.value = t.message ?: "Operation failed." } finally { busy.value = false }
    }
}

@androidx.compose.runtime.Immutable
data class DriveUiState(
    val folders: List<FolderEntity> = emptyList(),
    val files: List<FileEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList(),
    val backupSettings: BackupSettingsEntity = BackupSettingsEntity(),
    val downloadDestinationLabel: String = "Downloads/TeleDrive",
    val previewCache: Map<String, PreviewCacheEntity> = emptyMap(),
    val activeFolderId: Long? = null,
    val query: String = "",
    val busy: Boolean = false,
    val message: String? = null,
    val notificationMessage: String? = null,
)

enum class FileSort(val label: String, val comparator: Comparator<com.teledrive.android.data.FileEntity>) {
    NameAsc("Name A-Z", compareBy { it.name.lowercase(java.util.Locale.getDefault()) }),
    NameDesc("Name Z-A", compareByDescending { it.name.lowercase(java.util.Locale.getDefault()) }),
    Size("Size", compareByDescending<com.teledrive.android.data.FileEntity> { it.size }.thenBy { it.name.lowercase(java.util.Locale.getDefault()) }),
    Created("Created date", compareByDescending<com.teledrive.android.data.FileEntity> { it.createdAt }.thenBy { it.name.lowercase(java.util.Locale.getDefault()) }),
}

private fun decodeThumbnailBase64(encoded: String): ImageBitmap? = runCatching {
    val bytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
    val bitmap: android.graphics.Bitmap? = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    bitmap?.asImageBitmap()
}.getOrNull()

class DriveViewModelFactory(
    private val database: TeleDriveDatabase,
    private val gateway: TelegramGateway,
    private val backupManager: com.teledrive.android.backup.BackupManager,
    private val secureSettings: SecureSettings? = null,
    private val context: Context? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val ctx = context ?: throw IllegalStateException("Context required")
        val keystoreRepo = KeystoreRepository(database.keyEntryDao())
        val masterPwService = secureSettings?.let {
            MasterPasswordService(ctx, keystoreRepo, it, gateway)
        }
        return DriveViewModel(ctx, database, gateway, backupManager, keystoreRepo, masterPwService) as T
    }
}
