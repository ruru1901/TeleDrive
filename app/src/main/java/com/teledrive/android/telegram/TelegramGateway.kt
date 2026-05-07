package com.teledrive.android.telegram

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TelegramGateway {
    val authState: StateFlow<AuthState>
    val downloadDestinationLabel: StateFlow<String>

    suspend fun configure(apiId: Int, apiHash: String)
    suspend fun submitPhone(phone: String)
    suspend fun submitCode(code: String)
    suspend fun submitPassword(password: String)
    suspend fun logout()

    suspend fun scanFolders(): List<TelegramFolder>
    suspend fun createFolder(name: String): TelegramFolder
    suspend fun deleteFolder(folderId: Long)

    suspend fun listFiles(folderId: Long?): List<TelegramFile>
    suspend fun searchFiles(query: String): List<TelegramFile>
    fun downloadFile(messageId: Long, folderId: Long?): Flow<TransferProgress>
    fun downloadFileTo(messageId: Long, folderId: Long?, destination: Uri): Flow<TransferProgress>
    fun setDownloadDestination(uri: Uri)
    fun clearDownloadDestination()
    suspend fun deleteFiles(messageIds: List<Long>, folderId: Long?)
    suspend fun moveFiles(messageIds: List<Long>, sourceFolderId: Long?, targetFolderId: Long?)

    // Backup-specific operations
    suspend fun sendMessage(text: String, folderId: Long? = null): Long
    suspend fun editMessage(messageId: Long, text: String, folderId: Long? = null)
    suspend fun pinMessage(messageId: Long, folderId: Long? = null)
    suspend fun unpinMessage(messageId: Long, folderId: Long? = null)
    suspend fun getPinnedMessages(folderId: Long? = null): List<Long>
    suspend fun getMessage(messageId: Long, folderId: Long? = null): Any?

    // Upload with optional backupPath for caption; backupPath forms "backup/<path>" caption
    fun uploadFile(
        source: Uri,
        displayName: String,
        folderId: Long?,
        backupPath: String?,
    ): Flow<TransferProgress>

    // Download a thumbnail file by its TDLib file ID and return the local path when ready
    suspend fun downloadThumbnail(thumbFileId: Int): String?
}

sealed interface AuthState {
    /** Credentials are saved and TDLib is re-initialising — show a spinner. */
    data object Initializing : AuthState
    data object NeedsApiCredentials : AuthState
    data object NeedsPhone : AuthState
    data object NeedsCode : AuthState
    data object NeedsPassword : AuthState
    data object Ready : AuthState
    data class Error(val message: String) : AuthState
}

data class TelegramFolder(
    val id: Long,
    val name: String,
    val markerSource: String,
)

data class TelegramFile(
    val messageId: Long,
    val folderId: Long?,
    val name: String,
    val size: Long,
    val mimeType: String?,
    val extension: String?,
    val createdAt: Long,
    val localCachePath: String? = null,
    val thumbnailBase64: String? = null,
    val tdFileId: Int? = null,
    val tdRemoteUniqueId: String? = null,
    val tdThumbnailFileId: Int? = null,
    val tdThumbnailLocalPath: String? = null,
)

data class TransferProgress(
    val progress: Int,
    val done: Boolean = false,
    val error: String? = null,
    val localPath: String? = null,
    val downloadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val messageId: Long? = null,
)
