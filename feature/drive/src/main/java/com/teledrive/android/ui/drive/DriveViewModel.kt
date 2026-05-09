package com.teledrive.android.ui.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
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
import com.teledrive.android.workers.DownloadWorker
import com.teledrive.android.workers.UploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DriveViewModel @Inject constructor(
    private val context: Context,
    database: TeleDriveDatabase,
    private val gateway: TelegramGateway,
    private val backupManager: com.teledrive.android.backup.BackupManager,
    val keystoreRepository: KeystoreRepository? = null,
    val masterPasswordService: MasterPasswordService? = null,
) : ViewModel() {
    private val dao = database.dao()
    private val workManager by lazy { WorkManager.getInstance(context) }
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
                val uploadUri: Uri
                val uploadName: String
                val cryptoResult = if (encrypt) {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } ?: throw IllegalStateException("Unable to open selected file.")
                    GhostCrypto.encryptFile(bytes).also { result ->
                        val packed = GhostCrypto.packGhostFile(result.nonce, result.ciphertext)
                        val encryptedFile = File(context.cacheDir, "${displayName}.ghost")
                        withContext(Dispatchers.IO) {
                            FileOutputStream(encryptedFile).use { it.write(packed) }
                        }
                        uploadUri = Uri.fromFile(encryptedFile)
                        uploadName = "${displayName}.ghost"
                    }
                } else {
                    uploadUri = uri
                    uploadName = displayName
                    null
                }

                dao.upsertTransfer(
                    TransferEntity(
                        id = transferId,
                        type = TransferType.Upload,
                        fileName = displayName,
                        folderId = folderId,
                        messageId = null,
                        status = TransferStatus.Running,
                        progress = 0,
                        error = null,
                    ),
                )
                val msgId = enqueueUploadWork(
                    transferId = transferId,
                    uri = uploadUri,
                    displayName = uploadName,
                    folderId = folderId,
                )
                if (cryptoResult != null) {
                    keystoreRepository?.saveKey(
                        KeyEntry(
                            messageId = msgId,
                            keyBase64 = cryptoResult.keyBase64,
                            originalFilename = displayName,
                            originalMime = context.contentResolver.getType(uri) ?: "application/octet-stream",
                            uploadedAt = System.currentTimeMillis(),
                            isEncrypted = true,
                        ),
                    )
                    masterPasswordService?.syncKeystore()
                }
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
                dao.upsertTransfer(
                    TransferEntity(
                        id = transferId,
                        type = TransferType.Download,
                        fileName = file.name,
                        folderId = file.folderId,
                        messageId = file.messageId,
                        status = TransferStatus.Running,
                        progress = 0,
                        error = null,
                    ),
                )
                lastLocalPath = enqueueDownloadWork(
                    transferId = transferId,
                    file = file,
                    destination = destination,
                )
                if (lastLocalPath != null || destination != null) {
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

    private suspend fun enqueueUploadWork(
        transferId: String,
        uri: Uri,
        displayName: String,
        folderId: Long?,
    ): Long {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(
                workDataOf(
                    UploadWorker.KEY_URI to uri.toString(),
                    UploadWorker.KEY_NAME to displayName,
                    UploadWorker.KEY_FOLDER_ID to (folderId ?: UploadWorker.NULL_FOLDER_ID),
                ),
            )
            .build()
        workManager.enqueueUniqueWork("upload_$transferId", ExistingWorkPolicy.KEEP, request)
        val info = awaitTransferWork(
            request.id,
            transferId,
            TransferType.Upload,
            displayName,
            folderId,
            null,
        )
        if (info.state != WorkInfo.State.SUCCEEDED) {
            throw IllegalStateException(info.outputData.getString(UploadWorker.KEY_ERROR) ?: "Upload failed")
        }
        val messageId = info.outputData.getLong(UploadWorker.KEY_MESSAGE_ID, UploadWorker.NULL_MESSAGE_ID)
        if (messageId == UploadWorker.NULL_MESSAGE_ID) throw IllegalStateException("Upload failed")
        return messageId
    }

    private suspend fun enqueueDownloadWork(
        transferId: String,
        file: FileEntity,
        destination: Uri?,
    ): String? {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_MESSAGE_ID to file.messageId,
                    DownloadWorker.KEY_FOLDER_ID to (file.folderId ?: UploadWorker.NULL_FOLDER_ID),
                    DownloadWorker.KEY_DESTINATION_URI to destination?.toString(),
                ),
            )
            .build()
        workManager.enqueueUniqueWork("download_$transferId", ExistingWorkPolicy.KEEP, request)
        val info = awaitTransferWork(
            request.id,
            transferId,
            TransferType.Download,
            file.name,
            file.folderId,
            file.messageId,
        )
        if (info.state != WorkInfo.State.SUCCEEDED) {
            throw IllegalStateException(info.outputData.getString(DownloadWorker.KEY_ERROR) ?: "Download failed")
        }
        return info.outputData.getString(DownloadWorker.KEY_LOCAL_PATH)
    }

    private suspend fun awaitTransferWork(
        workId: UUID,
        transferId: String,
        type: TransferType,
        fileName: String,
        folderId: Long?,
        messageId: Long?,
    ): WorkInfo {
        while (true) {
            val info = withContext(Dispatchers.IO) { workManager.getWorkInfoById(workId).get() }
            if (info == null) {
                delay(250)
                continue
            }
            val progress = info.progress.getInt(UploadWorker.KEY_PROGRESS, 0)
            val error = info.progress.getString(UploadWorker.KEY_ERROR)
            dao.upsertTransfer(
                TransferEntity(
                    id = transferId,
                    type = type,
                    fileName = fileName,
                    folderId = folderId,
                    messageId = messageId,
                    status = when {
                        info.state == WorkInfo.State.SUCCEEDED -> TransferStatus.Success
                        info.state == WorkInfo.State.FAILED || info.state == WorkInfo.State.CANCELLED -> TransferStatus.Error
                        else -> TransferStatus.Running
                    },
                    progress = if (info.state == WorkInfo.State.SUCCEEDED) 100 else progress,
                    error = error ?: info.outputData.getString(UploadWorker.KEY_ERROR),
                ),
            )
            if (info.state.isFinished) return info
            delay(250)
        }
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
