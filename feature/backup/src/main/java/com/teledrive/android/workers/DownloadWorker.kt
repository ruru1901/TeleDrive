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
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForeground(createForegroundInfo("Downloading...", NOTIFICATION_ID))
        val messageId = inputData.getLong(KEY_MESSAGE_ID, UploadWorker.NULL_MESSAGE_ID)
            .takeUnless { it == UploadWorker.NULL_MESSAGE_ID }
            ?: return@withContext Result.failure()
        val folderIdInput = inputData.getLong(KEY_FOLDER_ID, UploadWorker.NULL_FOLDER_ID)
        val folderId = folderIdInput.takeUnless { it == UploadWorker.NULL_FOLDER_ID }
        val destination = inputData.getString(KEY_DESTINATION_URI)?.let(Uri::parse)
        val gateway = EntryPointAccessors.fromApplication(
            applicationContext,
            TransferWorkerEntryPoint::class.java,
        ).telegramGateway()

        var localPath: String? = null
        runCatching {
            val flow = if (destination == null) {
                gateway.downloadFile(messageId, folderId)
            } else {
                gateway.downloadFileTo(messageId, folderId, destination)
            }
            flow.collect { progress ->
                val progressError = progress.error
                setProgress(
                    workDataOf(
                        KEY_PROGRESS to progress.progress,
                        KEY_ERROR to progressError,
                        KEY_LOCAL_PATH to progress.localPath,
                    ),
                )
                if (progress.localPath != null) localPath = progress.localPath
                if (progressError != null) error(progressError)
            }
        }.fold(
            onSuccess = {
                Result.success(
                    workDataOf(
                        KEY_PROGRESS to 100,
                        KEY_LOCAL_PATH to localPath,
                    ),
                )
            },
            onFailure = {
                Result.failure(workDataOf(KEY_ERROR to (it.message ?: "Download failed")))
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
            .setSmallIcon(android.R.drawable.stat_sys_download)
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
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_FOLDER_ID = "folderId"
        const val KEY_DESTINATION_URI = "destinationUri"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_LOCAL_PATH = "localPath"
        private const val CHANNEL_ID = "transfer_channel"
        private const val NOTIFICATION_ID = 2002
    }
}
