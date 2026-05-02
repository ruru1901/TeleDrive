package com.teledrive.android.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teledrive.android.AppContainer
import com.teledrive.android.data.BackupFolder
import com.teledrive.android.data.FileEntity
import com.teledrive.android.data.SyncStatus
import com.teledrive.android.drive.toEntity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.io.File

class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer.create(applicationContext as android.app.Application)
        val dao = container.database.dao()
        val gateway = container.telegramGateway

        val settings = dao.observeBackupSettings().first() ?: return Result.success()
        if (!settings.enabled) return Result.success()

        // 1. Scan selected folders
        for (folder in settings.folders) {
            val localDir = BackupPathResolver.getPathForFolder(folder) ?: continue
            if (!localDir.exists() || !localDir.isDirectory) continue

            val localFiles = localDir.listFiles()?.filter { it.isFile } ?: continue
            
            // 2. Check if folder exists in Telegram
            val remoteFolders = gateway.scanFolders()
            val remoteFolder = remoteFolders.find { it.name == folder.label } 
                ?: gateway.createFolder(folder.label)
            
            val remoteFiles = gateway.listFiles(remoteFolder.id)
            
            for (file in localFiles) {
                // Skip if already backed up (simple check by name and size)
                val isBackedUp = remoteFiles.any { it.name == file.name && it.size == file.length() }
                if (isBackedUp) continue

                // 3. Upload
                try {
                    val uri = Uri.fromFile(file)
                    gateway.uploadFile(uri, file.name, remoteFolder.id).collect { progress ->
                        if (progress.done && progress.error == null) {
                            // Successfully uploaded
                            // We could insert into DAO here if we want immediate local visibility
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue with other files even if one fails
                }
            }
        }

        // Update last backup time
        dao.upsertBackupSettings(settings.copy(lastBackupAt = System.currentTimeMillis()))

        return Result.success()
    }
}
