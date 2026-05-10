package com.teledrive.android.telegram

// STORAGE: peak = originalSize + chunkSize

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest

class ChunkDownloader(
    private val cacheDir: File,
    private val gateway: TelegramGateway,
    private val folderId: Long?,
    private val bufferSize: Int = BUFFER_SIZE,
    private val onChunkPiped: ((Int, File) -> Unit)? = null,
) {
    fun download(manifest: ChunkManifest): Flow<TransferProgress> = flow {
        val outputFile = outputFileFor(cacheDir, manifest)
        var currentChunk: File? = null
        var randomAccessFile: RandomAccessFile? = null
        var bytesWrittenSoFar = 0L

        try {
            withContext(Dispatchers.IO) {
                outputFile.parentFile?.mkdirs()
                if (outputFile.exists()) outputFile.delete()
                randomAccessFile = RandomAccessFile(outputFile, "rw").also { it.setLength(0L) }
            }

            for ((index, chunkMessageId) in manifest.chunks.withIndex()) {
                val chunkPath = downloadChunkPath(chunkMessageId)
                currentChunk = File(chunkPath)
                currentChunk.inputStream().use { input ->
                    val buffer = ByteArray(bufferSize)
                    randomAccessFile?.seek(index * manifest.chunkSize)
                    while (true) {
                        val read = withContext(Dispatchers.IO) { input.read(buffer) }
                        if (read == -1) break
                        withContext(Dispatchers.IO) { randomAccessFile?.write(buffer, 0, read) }
                        bytesWrittenSoFar += read
                        emit(progress(bytesWrittenSoFar, manifest.originalSize))
                    }
                }
                withContext(Dispatchers.IO) { currentChunk.delete() }
                onChunkPiped?.invoke(index, currentChunk)
                currentChunk = null
            }

            withContext(Dispatchers.IO) {
                randomAccessFile?.close()
                randomAccessFile = null
            }

            val actualSha256 = withContext(Dispatchers.IO) { sha256(outputFile) }
            if (!actualSha256.equals(manifest.sha256, ignoreCase = true)) {
                outputFile.delete()
                emit(TransferProgress(progress = 0, error = "SHA-256 verification failed — file may be corrupted"))
                return@flow
            }

            emit(
                TransferProgress(
                    progress = 100,
                    done = true,
                    localPath = outputFile.absolutePath,
                    downloadedBytes = manifest.originalSize,
                    totalBytes = manifest.originalSize,
                ),
            )
        } catch (exception: MissingChunkException) {
            cleanup(outputFile, currentChunk)
            emit(TransferProgress(progress = 0, error = "One or more chunks are missing from Telegram — file cannot be reassembled"))
        } catch (exception: IOException) {
            cleanup(outputFile, currentChunk)
            emit(TransferProgress(progress = 0, error = "Not enough storage space"))
        } catch (exception: Exception) {
            cleanup(outputFile, currentChunk)
            emit(TransferProgress(progress = 0, error = exception.message ?: "Download failed"))
        } finally {
            withContext(Dispatchers.IO) {
                runCatching { randomAccessFile?.close() }
            }
        }
    }

    private fun progress(bytesWritten: Long, totalBytes: Long): TransferProgress {
        val percent = if (totalBytes > 0) {
            ((bytesWritten.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 99)
        } else {
            0
        }
        return TransferProgress(progress = percent, downloadedBytes = bytesWritten, totalBytes = totalBytes)
    }

    private suspend fun downloadChunkPath(messageId: Long): String {
        var path: String? = null
        var error: String? = null
        gateway.downloadFile(messageId, folderId).collect { progress ->
            if (progress.localPath != null) path = progress.localPath
            if (progress.error != null) error = progress.error
        }
        val downloadedPath = path
        if (error != null || downloadedPath == null || !File(downloadedPath).exists()) {
            throw MissingChunkException()
        }
        return downloadedPath
    }

    private fun cleanup(outputFile: File, chunkFile: File?) {
        runCatching { outputFile.delete() }
        runCatching { chunkFile?.delete() }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(bufferSize)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private class MissingChunkException : Exception()

    companion object {
        const val BUFFER_SIZE = 8 * 1024 * 1024

        fun outputFileFor(cacheDir: File, manifest: ChunkManifest): File =
            File(File(cacheDir, "reassembled"), sanitize(manifest.originalFileName))

        fun deletePartialOutput(cacheDir: File, manifest: ChunkManifest) {
            outputFileFor(cacheDir, manifest).delete()
        }

        fun deleteAllPartialOutputs(cacheDir: File) {
            File(cacheDir, "reassembled").listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        }

        private fun sanitize(name: String): String =
            name.ifBlank { "download.bin" }.replace(Regex("""[\\/:*?"<>|]"""), "_")
    }
}
