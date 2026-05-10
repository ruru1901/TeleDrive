package com.teledrive.android.backup

import android.app.Notification
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.teledrive.android.data.TeleDriveDatabase
import com.teledrive.android.data.BackupMode
import com.teledrive.android.data.BackupScope
import com.teledrive.android.data.BackupSettingsEntity
import com.teledrive.android.telegram.TelegramGateway
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val NOTIF_ID = 1001
    private val COMPLETION_NOTIF_ID = 1002
    private val CHANNEL_ID = "backup_channel"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            BackupWorkerEntryPoint::class.java,
        )
        val dao = dependencies.database().dao()
        val gateway = dependencies.telegramGateway()
        val manifestManager = ManifestManager(gateway)

        val settings = dao.observeBackupSettings().first() ?: return@withContext Result.success()
        if (!settings.enabled) return@withContext Result.success()

        val allFiles = BackupPathResolver.resolveBackupFiles(applicationContext, settings)
        val totalFiles = allFiles.size

        if (totalFiles == 0) {
            return@withContext Result.success()
        }

        // Start foreground with initial notification
        val startNotif = createProgressNotification("Backup starting...", 0, totalFiles)
        setForegroundAsync(createForegroundInfo(startNotif))

        var newCount = 0
        var updatedCount = 0
        var skippedCount = 0
        var processed = 0

        // Get or create manifest
        val (manifest, manifestMessageId, _) = manifestManager.getOrCreateManifest()
        val mergedFiles = manifest.files.toMutableMap()

        // Process each file
        for (file in allFiles) {
            try {
                val hash = computeFileHash(file)
                val existing = mergedFiles[file.relativePath]
                val needsUpload = existing == null || existing.hash != hash

                if (needsUpload) {
                    var uploadedMessageId: Long? = null

                    gateway.uploadFile(file.uri, file.displayName, null, file.relativePath).collect { progress ->
                        if (progress.done) {
                            uploadedMessageId = progress.messageId
                        }
                    }

                    val finalMessageId = uploadedMessageId
                    if (finalMessageId != null) {
                        mergedFiles[file.relativePath] = FileManifestEntry(
                            messageId = finalMessageId,
                            hash = hash,
                            size = file.size,
                            modifiedEpoch = file.modifiedEpoch
                        )
                        if (existing == null) newCount++ else updatedCount++
                    } else {
                        skippedCount++
                    }
                } else {
                    skippedCount++
                }
            } catch (e: SecurityException) {
                skippedCount++
            } catch (e: Exception) {
                e.printStackTrace()
                skippedCount++
            } finally {
                processed++
                // Update foreground notification
                val progressText = "Processed $processed/$totalFiles files"
                val progNotif = createProgressNotification(progressText, processed, totalFiles)
                setForegroundAsync(createForegroundInfo(progNotif))
            }

            // Rate limiting between uploads (skip after last)
            if (processed < allFiles.size) {
                delay(1500)
            }
        }

        // Update manifest if any changes
        if (mergedFiles != manifest.files) {
            val finalManifest = manifest.copy(
                version = 1,
                lastSync = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()),
                files = mergedFiles
            )
            manifestManager.updateManifest(finalManifest, manifestMessageId)

            // Two-way sync: download files from cloud that are missing or outdated locally
            if (settings.mode == BackupMode.TwoWay) {
                var restored = 0
                val entries = finalManifest.files.entries.toList()
                for ((relPath, entry) in entries) {
                    val localFile = resolveLocalPath(settings, relPath)
                    if (localFile != null && (!localFile.exists() || localFile.lastModified() < entry.modifiedEpoch)) {
                        gateway.downloadFile(entry.messageId, null).collect { progress ->
                            if (progress.done && progress.localPath != null) {
                                val downloaded = File(progress.localPath)
                                if (downloaded.exists()) {
                                    localFile.parentFile?.mkdirs()
                                    downloaded.copyTo(localFile, overwrite = true)
                                    localFile.setLastModified(entry.modifiedEpoch)
                                    restored++
                                }
                            }
                        }
                    }
                }
                if (restored > 0) {
                    val progressText = "Restored $restored files from cloud"
                    val restoreNotif = createProgressNotification(progressText, restored, entries.size)
                    setForegroundAsync(createForegroundInfo(restoreNotif))
                }
            }
        }

        // Update backup timestamp
        dao.upsertBackupSettings(settings.copy(lastBackupAt = System.currentTimeMillis()))

        // Post completion notification (separate from foreground)
        val finalStats = "$newCount new, $updatedCount updated, $skippedCount skipped"
        val finalNotif = createCompletionNotification(finalStats)
        NotificationManagerCompat.from(applicationContext).notify(COMPLETION_NOTIF_ID, finalNotif)

        // Brief delay to allow user to see completion notification
        delay(3000)

        Result.success()
    }

    private fun resolveLocalPath(settings: BackupSettingsEntity, relativePath: String): File? {
        if (settings.scope == BackupScope.EntireStorage) {
            val prefix = "storage/"
            val subPath = if (relativePath.startsWith(prefix)) relativePath.removePrefix(prefix) else relativePath
            return File(BackupPathResolver.getStorageRoot(), subPath)
        }
        val separatorIndex = relativePath.indexOf('/')
        if (separatorIndex < 0) return null
        val folderLabel = relativePath.substring(0, separatorIndex)
        val subPath = relativePath.substring(separatorIndex + 1)
        return File(BackupPathResolver.getStorageRoot(), "$folderLabel/$subPath")
    }

    private suspend fun computeFileHash(file: BackupSourceFile): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        val input = applicationContext.contentResolver.openInputStream(file.uri)
            ?: throw IllegalStateException("Unable to open ${file.displayName}")
        input.use { `in` ->
            val buffer = ByteArray(8192)
            var read: Int
            while (true) {
                read = `in`.read(buffer)
                if (read == -1) break
                md.update(buffer, 0, read)
            }
        }
        Base64.encodeToString(md.digest(), Base64.NO_WRAP)
    }

    private fun createProgressNotification(contentText: String, processed: Int, total: Int): android.app.Notification {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Ghost Backup")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total, processed, false)
            .build()
    }

    private fun createCompletionNotification(stats: String): android.app.Notification {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Backup complete")
            .setContentText(stats)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setAutoCancel(true)
            .build()
    }

    private fun createForegroundInfo(notification: android.app.Notification): ForegroundInfo {
        return ForegroundInfo(NOTIF_ID, notification)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupWorkerEntryPoint {
    fun database(): TeleDriveDatabase
    fun telegramGateway(): TelegramGateway
}
