package com.teledrive.android.backup

import android.content.Context
import androidx.work.*
import com.teledrive.android.data.BackupSettingsEntity
import java.util.concurrent.TimeUnit

class BackupManager(private val context: Context) {

    fun scheduleBackup(settings: BackupSettingsEntity) {
        val workManager = WorkManager.getInstance(context)

        if (!settings.enabled) {
            workManager.cancelUniqueWork(BACKUP_WORK_NAME)
            workManager.cancelUniqueWork(BACKUP_WORK_NAME + "_immediate")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(settings.chargingOnly)
            .build()

        if (settings.instantBackup) {
            val immediateRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(
                BACKUP_WORK_NAME + "_immediate",
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
        }

        val periodicRequest = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    companion object {
        private const val BACKUP_WORK_NAME = "teledrive_backup_sync"
    }
}