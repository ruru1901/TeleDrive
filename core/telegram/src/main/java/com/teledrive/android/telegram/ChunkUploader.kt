package com.teledrive.android.telegram

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class ChunkUploader(
    private val context: Context,
    private val chunkSize: Long = DEFAULT_CHUNK_SIZE,
    private val bufferSize: Int = ChunkDownloader.BUFFER_SIZE,
    private val uploadChunk: suspend (file: File, displayName: String, caption: String) -> Long,
    private val sendManifest: suspend (manifest: ChunkManifest) -> Long,
) {
    fun upload(
        source: Uri,
        displayName: String,
        originalSize: Long,
        mimeType: String?,
    ): Flow<TransferProgress> = flow {
        val transferId = UUID.randomUUID().toString()
        val chunkDir = File(context.cacheDir, "chunks").also { it.mkdirs() }
        val chunkMessageIds = mutableListOf<Long>()
        val digest = MessageDigest.getInstance("SHA-256")
        var bytesWrittenSoFar = 0L
        var chunkIndex = 0
        var currentChunk: File? = null

        try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw IllegalStateException("Unable to open selected file.")
            input.use {
                while (true) {
                    currentChunk = File(chunkDir, "${transferId}_${chunkIndex}.part")
                    var chunkBytes = 0L
                    currentChunk.outputStream().use { output ->
                        val buffer = ByteArray(bufferSize)
                        while (chunkBytes < chunkSize) {
                            val maxRead = minOf(buffer.size.toLong(), chunkSize - chunkBytes).toInt()
                            val read = input.read(buffer, 0, maxRead)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            chunkBytes += read
                            bytesWrittenSoFar += read
                            val percent = ((bytesWrittenSoFar.toFloat() / originalSize) * 100)
                                .toInt()
                                .coerceIn(0, 99)
                            emit(TransferProgress(progress = percent))
                        }
                    }
                    if (chunkBytes == 0L) {
                        currentChunk.delete()
                        currentChunk = null
                        break
                    }
                    val chunkMessageId = uploadChunk(
                        currentChunk,
                        "${displayName.ifBlank { "file" }}.part$chunkIndex",
                        ChunkManifest.chunkCaption(transferId, chunkIndex, displayName),
                    )
                    chunkMessageIds += chunkMessageId
                    currentChunk.delete()
                    currentChunk = null
                    chunkIndex++
                }
            }

            val manifest = ChunkManifest(
                transferId = transferId,
                originalFileName = displayName.ifBlank { source.lastPathSegment ?: "Uploaded file" },
                originalSize = originalSize,
                mimeType = mimeType,
                chunkSize = chunkSize,
                sha256 = digest.digest().joinToString("") { "%02x".format(it) },
                chunks = chunkMessageIds,
            )
            val manifestMessageId = sendManifest(manifest)
            emit(TransferProgress(progress = 100, done = true, messageId = manifestMessageId))
        } catch (throwable: Throwable) {
            currentChunk?.delete()
            throw throwable
        }
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 1_900_000_000L
    }
}
