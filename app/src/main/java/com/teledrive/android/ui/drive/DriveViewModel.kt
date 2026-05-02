package com.teledrive.android.ui.drive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teledrive.android.data.FileEntity
import com.teledrive.android.data.FolderEntity
import com.teledrive.android.data.BackupSettingsEntity
import com.teledrive.android.data.TeleDriveDatabase
import com.teledrive.android.data.TransferEntity
import com.teledrive.android.data.TransferStatus
import com.teledrive.android.data.TransferType
import com.teledrive.android.drive.toEntity
import com.teledrive.android.telegram.TelegramGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

class DriveViewModel(
    database: TeleDriveDatabase,
    private val gateway: TelegramGateway,
    private val backupManager: com.teledrive.android.backup.BackupManager,
) : ViewModel() {
    private val dao = database.dao()
    private val activeFolderId = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")
    private val busy = MutableStateFlow(false)
    private val notificationMessage = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    private val filesFlow = combine(activeFolderId, query) { folderId, search ->
        folderId to search
    }.flatMapLatest { (folderId, search) ->
        if (search.length > 2) dao.searchFiles(search) else dao.observeFiles(folderId)
    }

    private val backupSettingsFlow = dao.observeBackupSettings().map { it ?: BackupSettingsEntity() }

    val uiState: StateFlow<DriveUiState> = combine(
        dao.observeFolders(),
        filesFlow,
        dao.observeTransfers(),
        backupSettingsFlow,
        gateway.downloadDestinationLabel,
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
            activeFolderId = values[5] as Long?,
            query = values[6] as String,
            busy = values[7] as Boolean,
            message = values[8] as String?,
            notificationMessage = values[9] as String?,
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
    }

    fun selectFolder(folderId: Long?) {
        activeFolderId.value = folderId
        refreshFiles(folderId)
    }

    fun setQuery(value: String) {
        query.value = value
        searchJob?.cancel()
        val search = value.trim()
        if (search.length > 2) {
            searchJob = viewModelScope.launch {
                delay(350)
                runCatching {
                    dao.upsertFiles(gateway.searchFiles(search).map { it.toEntity() })
                }.onFailure { throwable ->
                    if (query.value.trim() == search) {
                        message.value = throwable.message ?: "Search failed."
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

    fun upload(uri: Uri, displayName: String) {
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
                gateway.uploadFile(uri, displayName, folderId).collect { progress ->
                    dao.upsertTransfer(
                        TransferEntity(
                            id = transferId,
                            type = TransferType.Upload,
                            fileName = displayName,
                            folderId = folderId,
                            messageId = null,
                            status = when {
                                progress.error != null -> TransferStatus.Error
                                progress.done -> TransferStatus.Success
                                else -> TransferStatus.Running
                            },
                            progress = progress.progress,
                            error = progress.error,
                        ),
                    )
                }
                notificationMessage.value = "File $displayName uploaded"
                viewModelScope.launch {
                    delay(2000)
                    if (notificationMessage.value == "File $displayName uploaded") notificationMessage.value = null
                }
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

    fun download(file: FileEntity) {
        startDownload(file, destination = null)
    }

    fun downloadTo(file: FileEntity, destination: Uri) {
        startDownload(file, destination)
    }

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
                var completed = false
                val flow = if (destination == null) {
                    gateway.downloadFile(file.messageId, file.folderId)
                } else {
                    gateway.downloadFileTo(file.messageId, file.folderId, destination)
                }
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
                    completed = progress.done && progress.error == null
                }
                if (completed) {
                    refreshFilesInternal(activeFolderId.value)
                    notificationMessage.value = "File ${file.name} downloaded"
                    viewModelScope.launch {
                        delay(2000)
                        if (notificationMessage.value == "File ${file.name} downloaded") {
                            notificationMessage.value = null
                        }
                    }
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
        message.value = "Download folder set to ${gateway.downloadDestinationLabel.value}."
    }

    fun clearDownloadDestination() {
        gateway.clearDownloadDestination()
        message.value = "Download folder reset to ${gateway.downloadDestinationLabel.value}."
    }

    fun deleteFile(file: FileEntity) {
        viewModelScope.launch {
            runStep {
                gateway.deleteFiles(listOf(file.messageId), file.folderId)
                dao.deleteFiles(listOf(file.messageId))
            }
        }
    }

    fun deleteFiles(files: List<FileEntity>) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            runStep {
                val grouped = files.groupBy { it.folderId }
                grouped.forEach { (folderId, folderFiles) ->
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
                val grouped = files.groupBy { it.folderId }
                grouped.forEach { (sourceFolderId, folderFiles) ->
                    gateway.moveFiles(
                        messageIds = folderFiles.map { it.messageId },
                        sourceFolderId = sourceFolderId,
                        targetFolderId = targetFolderId,
                    )
                }
                refreshFilesInternal(activeFolderId.value)
            }
        }
    }

    fun updateBackupSettings(settings: BackupSettingsEntity) {
        viewModelScope.launch {
            dao.upsertBackupSettings(settings)
            backupManager.scheduleBackup(settings)
        }
    }

    fun recordBackupRun() {
        viewModelScope.launch {
            val current = uiState.value.backupSettings
            dao.upsertBackupSettings(current.copy(lastBackupAt = System.currentTimeMillis()))
            message.value = "Backup options saved. Sync job marked as started."
        }
    }

    fun restoreAllFiles() {
        val files = uiState.value.files
        if (files.isEmpty()) {
            message.value = "There are no files to restore."
            return
        }
        files.forEach { download(it) }
        message.value = "Restore started for ${files.size} files."
    }

    private fun refreshFiles(folderId: Long?) {
        viewModelScope.launch {
            runStep { refreshFilesInternal(folderId) }
        }
    }

    private suspend fun refreshFilesInternal(folderId: Long?) {
        dao.clearFilesForFolder(folderId)
        dao.upsertFiles(gateway.listFiles(folderId).map { it.toEntity() })
    }

    private suspend fun runStep(block: suspend () -> Unit) {
        busy.value = true
        message.value = null
        try {
            block()
        } catch (throwable: Throwable) {
            message.value = throwable.message ?: "Operation failed."
        } finally {
            busy.value = false
        }
    }
}

data class DriveUiState(
    val folders: List<FolderEntity> = emptyList(),
    val files: List<FileEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList(),
    val backupSettings: BackupSettingsEntity = BackupSettingsEntity(),
    val downloadDestinationLabel: String = "Downloads/TeleDrive",
    val activeFolderId: Long? = null,
    val query: String = "",
    val busy: Boolean = false,
    val message: String? = null,
    val notificationMessage: String? = null,
)

class DriveViewModelFactory(
    private val database: TeleDriveDatabase,
    private val gateway: TelegramGateway,
    private val backupManager: com.teledrive.android.backup.BackupManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DriveViewModel(database, gateway, backupManager) as T
    }
}
