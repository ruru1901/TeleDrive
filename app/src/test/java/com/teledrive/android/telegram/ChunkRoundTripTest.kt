package com.teledrive.android.telegram

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChunkRoundTripTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.cacheDir, "reassembled").deleteRecursively()
        File(context.cacheDir, "fake_chunks").deleteRecursively()
    }

    @Test
    fun reassemblyDeletesEachChunkBeforeNextDownloadAndKeepsPeakFileCountAtTwo() = runTest {
        val original = ByteArray(23) { index -> (index * 7).toByte() }
        val chunks = original.toList().chunked(8).map { values -> values.map { it }.toByteArray() }
        val chunkDir = File(context.cacheDir, "fake_chunks").also { it.mkdirs() }
        val outputDir = File(context.cacheDir, "reassembled")
        val gateway = FakeChunkGateway(
            chunkDir = chunkDir,
            outputDir = outputDir,
            chunks = chunks,
        )
        val manifest = ChunkManifest(
            transferId = "test-transfer",
            originalFileName = "large.bin",
            originalSize = original.size.toLong(),
            mimeType = "application/octet-stream",
            chunkSize = 8,
            sha256 = sha256(original),
            chunks = listOf(1L, 2L, 3L),
        )

        var finalPath: String? = null
        ChunkDownloader(
            cacheDir = context.cacheDir,
            gateway = gateway,
            folderId = null,
            bufferSize = 4,
        ).download(manifest).collect { progress ->
            if (progress.done) finalPath = progress.localPath
        }

        val outputFile = requireNotNull(finalPath).let(::File)
        assertTrue(outputFile.exists())
        assertArrayEquals(original, outputFile.readBytes())
        assertEquals(original.size.toLong(), outputFile.length())
        assertEquals("Peak file count should be output file + one downloaded chunk", 2, gateway.peakFileCount)
        assertEquals("Final disk usage should equal the original size", original.size.toLong(), outputDir.walkTopDown().filter { it.isFile }.sumOf { it.length() })
        assertTrue("The first chunk should have been deleted before the second download", gateway.deletedBeforeNextDownload)
        assertTrue("Each chunk temp file should be deleted after piping", gateway.chunkPaths.all { !it.exists() })
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private class FakeChunkGateway(
        private val chunkDir: File,
        private val outputDir: File,
        private val chunks: List<ByteArray>,
    ) : TelegramGateway {
        override val authState = MutableStateFlow<AuthState>(AuthState.Ready)
        override val downloadDestinationLabel = MutableStateFlow("Downloads/TeleDrive")
        val chunkPaths = mutableListOf<File>()
        var peakFileCount = 0
        var deletedBeforeNextDownload = true
        private var previousChunk: File? = null

        override fun downloadFile(messageId: Long, folderId: Long?): Flow<TransferProgress> = flow {
            previousChunk?.let { deletedBeforeNextDownload = deletedBeforeNextDownload && !it.exists() }
            val chunkFile = File(chunkDir, "chunk_$messageId.part")
            chunkFile.writeBytes(chunks[(messageId - 1).toInt()])
            chunkPaths += chunkFile
            previousChunk = chunkFile
            peakFileCount = maxOf(peakFileCount, fileCount())
            emit(TransferProgress(progress = 100, done = true, localPath = chunkFile.absolutePath))
        }

        private fun fileCount(): Int =
            listOf(chunkDir, outputDir)
                .sumOf { dir -> dir.walkTopDown().count { it.isFile } }

        override suspend fun configure(apiId: Int, apiHash: String) = Unit
        override suspend fun submitPhone(phone: String) = Unit
        override suspend fun submitCode(code: String) = Unit
        override suspend fun submitPassword(password: String) = Unit
        override suspend fun logout() = Unit
        override suspend fun scanFolders(): List<TelegramFolder> = emptyList()
        override suspend fun createFolder(name: String): TelegramFolder = TelegramFolder(1L, name, "test")
        override suspend fun deleteFolder(folderId: Long) = Unit
        override suspend fun listFiles(folderId: Long?): List<TelegramFile> = emptyList()
        override suspend fun searchFiles(query: String): List<TelegramFile> = emptyList()
        override fun downloadFileTo(messageId: Long, folderId: Long?, destination: Uri): Flow<TransferProgress> = downloadFile(messageId, folderId)
        override fun setDownloadDestination(uri: Uri) = Unit
        override fun clearDownloadDestination() = Unit
        override suspend fun deleteFiles(messageIds: List<Long>, folderId: Long?) = Unit
        override suspend fun moveFiles(messageIds: List<Long>, sourceFolderId: Long?, targetFolderId: Long?) = Unit
        override suspend fun sendMessage(text: String, folderId: Long?): Long = 0L
        override suspend fun editMessage(messageId: Long, text: String, folderId: Long?) = Unit
        override suspend fun pinMessage(messageId: Long, folderId: Long?) = Unit
        override suspend fun unpinMessage(messageId: Long, folderId: Long?) = Unit
        override suspend fun getPinnedMessages(folderId: Long?): List<Long> = emptyList()
        override suspend fun getMessage(messageId: Long, folderId: Long?): Any? = null
        override fun uploadFile(source: Uri, displayName: String, folderId: Long?, backupPath: String?): Flow<TransferProgress> = flow {
            emit(TransferProgress(100, done = true))
        }
        override suspend fun downloadThumbnail(thumbFileId: Int): String? = null
    }
}
