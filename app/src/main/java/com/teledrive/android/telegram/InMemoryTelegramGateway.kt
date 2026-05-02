package com.teledrive.android.telegram

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong

class InMemoryTelegramGateway(
    private val context: Context,
) : TelegramGateway {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NeedsApiCredentials)
    override val authState: StateFlow<AuthState> = _authState
    private val _downloadDestinationLabel = MutableStateFlow("Downloads/TeleDrive")
    override val downloadDestinationLabel: StateFlow<String> = _downloadDestinationLabel

    private val ids = AtomicLong(1000L)
    private val folders = mutableListOf<TelegramFolder>()
    private val files = mutableListOf<TelegramFile>()

    init {
        files += TelegramFile(
            messageId = ids.incrementAndGet(),
            folderId = null,
            name = "Welcome to TeleDrive.pdf",
            size = 384_000,
            mimeType = "application/pdf",
            extension = "pdf",
            createdAt = System.currentTimeMillis(),
            localCachePath = writePlaceholderFile("Welcome to TeleDrive.pdf", "TeleDrive starter document"),
        )
        val sampleImage = createSampleImageFile("Saved image.jpg")
        files += TelegramFile(
            messageId = ids.incrementAndGet(),
            folderId = null,
            name = "Saved image.jpg",
            size = sampleImage.length(),
            mimeType = "image/jpeg",
            extension = "jpg",
            createdAt = System.currentTimeMillis() - 86_400_000L,
            localCachePath = sampleImage.absolutePath,
            thumbnailBase64 = createThumbnailBase64(sampleImage),
        )
        files += TelegramFile(
            messageId = ids.incrementAndGet(),
            folderId = null,
            name = "Launch_Cut.mp4",
            size = 238_000_000,
            mimeType = "video/mp4",
            extension = "mp4",
            createdAt = System.currentTimeMillis() - 172_800_000L,
            thumbnailBase64 = createSolidThumbnailBase64(0xFF9B59FF.toInt()),
        )
        files += TelegramFile(
            messageId = ids.incrementAndGet(),
            folderId = null,
            name = "Project_Roadmap.xlsx",
            size = 524_000,
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            extension = "xlsx",
            createdAt = System.currentTimeMillis() - 259_200_000L,
            localCachePath = writePlaceholderFile("Project_Roadmap.xlsx", "Spreadsheet preview placeholder"),
        )
    }

    override suspend fun configure(apiId: Int, apiHash: String) {
        require(apiId > 0) { "API ID must be positive." }
        require(apiHash.isNotBlank()) { "API hash is required." }
        delay(250)
        _authState.value = AuthState.NeedsPhone
    }

    override suspend fun submitPhone(phone: String) {
        require(phone.isNotBlank()) { "Phone number is required." }
        delay(250)
        _authState.value = AuthState.NeedsCode
    }

    override suspend fun submitCode(code: String) {
        require(code.isNotBlank()) { "Login code is required." }
        delay(250)
        _authState.value = if (code == "2222") AuthState.NeedsPassword else AuthState.Ready
    }

    override suspend fun submitPassword(password: String) {
        require(password.isNotBlank()) { "Password is required." }
        delay(250)
        _authState.value = AuthState.Ready
    }

    override suspend fun logout() {
        delay(150)
        _authState.value = AuthState.NeedsApiCredentials
    }

    override suspend fun scanFolders(): List<TelegramFolder> {
        delay(200)
        return folders.toList()
    }

    override suspend fun createFolder(name: String): TelegramFolder {
        require(name.isNotBlank()) { "Folder name is required." }
        delay(200)
        return TelegramFolder(
            id = ids.incrementAndGet(),
            name = name.trim(),
            markerSource = "title",
        ).also(folders::add)
    }

    override suspend fun deleteFolder(folderId: Long) {
        delay(200)
        folders.removeAll { it.id == folderId }
        files.removeAll { it.folderId == folderId }
    }

    override suspend fun listFiles(folderId: Long?): List<TelegramFile> {
        delay(150)
        return files.filter { it.folderId == folderId }.sortedByDescending { it.createdAt }
    }

    override suspend fun searchFiles(query: String): List<TelegramFile> {
        delay(200)
        return files.filter { it.name.contains(query, ignoreCase = true) }
    }

    override fun uploadFile(source: Uri, displayName: String, folderId: Long?): Flow<TransferProgress> = flow {
        for (percent in 0..100 step 10) {
            delay(80)
            emit(TransferProgress(percent))
        }
        val copied = copyUriToCache(source, displayName)
        val mime = context.contentResolver.getType(source) ?: guessMime(displayName)
        files.add(
            TelegramFile(
                messageId = ids.incrementAndGet(),
                folderId = folderId,
                name = displayName.ifBlank { source.lastPathSegment ?: "Uploaded file" },
                size = copied.length(),
                mimeType = mime,
                extension = displayName.substringAfterLast('.', missingDelimiterValue = ""),
                createdAt = System.currentTimeMillis(),
                localCachePath = copied.absolutePath,
                thumbnailBase64 = if ((mime ?: "").startsWith("image/")) createThumbnailBase64(copied) else null,
            ),
        )
        emit(TransferProgress(progress = 100, done = true))
    }

    override fun downloadFile(messageId: Long, folderId: Long?): Flow<TransferProgress> = flow {
        val index = files.indexOfFirst { it.messageId == messageId && it.folderId == folderId }
        if (index == -1) {
            emit(TransferProgress(progress = 0, error = "File not found"))
            return@flow
        }
        for (percent in 0..100 step 20) {
            delay(90)
            emit(TransferProgress(percent, done = percent == 100))
        }
        val file = files[index]
        if (file.localCachePath == null || !File(file.localCachePath).exists()) {
            val restored = writePlaceholderFile(file.name, "Downloaded from TeleDrive")
            files[index] = file.copy(localCachePath = restored)
        }
    }

    override fun downloadFileTo(messageId: Long, folderId: Long?, destination: Uri): Flow<TransferProgress> {
        setDownloadDestination(destination)
        return downloadFile(messageId, folderId)
    }

    override fun setDownloadDestination(uri: Uri) {
        _downloadDestinationLabel.value = uri.lastPathSegment?.substringAfter(':')?.ifBlank { null } ?: "Selected folder"
    }

    override fun clearDownloadDestination() {
        _downloadDestinationLabel.value = "Downloads/TeleDrive"
    }

    override suspend fun deleteFiles(messageIds: List<Long>, folderId: Long?) {
        delay(150)
        files.removeAll { it.messageId in messageIds && it.folderId == folderId }
    }

    override suspend fun moveFiles(messageIds: List<Long>, sourceFolderId: Long?, targetFolderId: Long?) {
        delay(200)
        val moved = files.filter { it.messageId in messageIds && it.folderId == sourceFolderId }
        files.removeAll { it.messageId in messageIds && it.folderId == sourceFolderId }
        files.addAll(moved.map { it.copy(folderId = targetFolderId, messageId = ids.incrementAndGet()) })
    }

    private fun copyUriToCache(uri: Uri, displayName: String): File {
        val safeName = displayName.ifBlank { uri.lastPathSegment ?: "upload.bin" }.replace("/", "_")
        val target = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$safeName")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to read selected file." }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }

    private fun writePlaceholderFile(name: String, body: String): String {
        val target = File(context.cacheDir, "${System.currentTimeMillis()}_${name.replace("/", "_")}")
        target.writeText(body)
        return target.absolutePath
    }

    private fun createSampleImageFile(name: String): File {
        val bitmap = Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888)
        repeat(180) { y ->
            repeat(180) { x ->
                val red = 70 + (x % 80)
                val blue = 120 + (y % 100)
                bitmap.setPixel(x, y, android.graphics.Color.rgb(red, 120, blue.coerceAtMost(220)))
            }
        }
        val target = File(context.cacheDir, name)
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return target
    }

    private fun createThumbnailBase64(file: File): String? {
        return runCatching {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
            val bytes = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, bytes)
            Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun createSolidThumbnailBase64(color: Int): String? {
        return runCatching {
            val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
            repeat(48) { y ->
                repeat(48) { x -> bitmap.setPixel(x, y, color) }
            }
            val bytes = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes)
            Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun guessMime(name: String): String? = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "mp4" -> "video/mp4"
        "pdf" -> "application/pdf"
        "wav" -> "audio/wav"
        "mp3" -> "audio/mpeg"
        else -> null
    }
}
