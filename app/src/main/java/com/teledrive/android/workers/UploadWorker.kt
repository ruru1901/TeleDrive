package com.teledrive.android.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.teledrive.android.telegram.TelegramGateway
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForeground(createForegroundInfo("Uploading...", NOTIFICATION_ID))
        val uri = inputData.getString(KEY_URI)?.let(Uri::parse) ?: return@withContext Result.failure()
        val name = inputData.getString(KEY_NAME) ?: return@withContext Result.failure()
        val folderIdInput = inputData.getLong(KEY_FOLDER_ID, NULL_FOLDER_ID)
        val folderId = folderIdInput.takeUnless { it == NULL_FOLDER_ID }
        val backupPath = inputData.getString(KEY_BACKUP_PATH)
        val gateway = EntryPointAccessors.fromApplication(
            applicationContext,
            TransferWorkerEntryPoint::class.java,
        ).telegramGateway()

        var messageId: Long? = null
        runCatching {
            gateway.uploadFile(uri, name, folderId, backupPath).collect { progress ->
                setProgress(
                    workDataOf(
                        KEY_PROGRESS to progress.progress,
                        KEY_ERROR to progress.error,
                    ),
                )
                if (progress.messageId != null) messageId = progress.messageId
                if (progress.error != null) error(progress.error)
            }
        }.fold(
            onSuccess = {
                Result.success(
                    workDataOf(
                        KEY_PROGRESS to 100,
                        KEY_MESSAGE_ID to (messageId ?: NULL_MESSAGE_ID),
                    ),
                )
            },
            onFailure = {
                Result.failure(workDataOf(KEY_ERROR to (it.message ?: "Upload failed")))
            },
        )
    }

    private fun createForegroundInfo(text: String, notificationId: Int): ForegroundInfo {
        ensureChannel()
        return ForegroundInfo(notificationId, createNotification(text))
    }

    private fun createNotification(text: String): Notification =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("TeleDrive")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Transfers", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_URI = "uri"
        const val KEY_NAME = "name"
        const val KEY_FOLDER_ID = "folderId"
        const val KEY_BACKUP_PATH = "backupPath"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_MESSAGE_ID = "messageId"
        const val NULL_FOLDER_ID = -1L
        const val NULL_MESSAGE_ID = -1L
        private const val CHANNEL_ID = "transfer_channel"
        private const val NOTIFICATION_ID = 2001
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TransferWorkerEntryPoint {
    fun telegramGateway(): TelegramGateway
}
