package com.teledrive.android.backup

import android.os.Environment
import com.teledrive.android.data.BackupFolder
import java.io.File

object BackupPathResolver {
    fun getPathForFolder(folder: BackupFolder): File? {
        return when (folder) {
            BackupFolder.Camera -> File(publicDirectory(DIRECTORY_DCIM), "Camera")
            BackupFolder.Screenshots -> {
                val dcim = File(publicDirectory(DIRECTORY_DCIM), "Screenshots")
                if (dcim.exists()) dcim else File(publicDirectory(DIRECTORY_PICTURES), "Screenshots")
            }
            BackupFolder.Downloads -> publicDirectory(DIRECTORY_DOWNLOADS)
            BackupFolder.WhatsAppImages -> File(externalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images")
            BackupFolder.WhatsAppVideo -> File(externalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video")
            BackupFolder.Documents -> publicDirectory(DIRECTORY_DOCUMENTS)
            BackupFolder.CustomFolder -> null // Managed via picker in the future
        }
    }

    private fun publicDirectory(type: String): File =
        runCatching { Environment.getExternalStoragePublicDirectory(type) }
            .getOrElse { File(testStorageRoot(), type) }

    private fun externalStorageDirectory(): File =
        runCatching { Environment.getExternalStorageDirectory() }
            .getOrElse { testStorageRoot() }

    private fun testStorageRoot(): File =
        File(
            System.getProperty("user.home")
                ?: System.getProperty("java.io.tmpdir")
                ?: ".",
            "TeleDriveStorage",
        )

    private const val DIRECTORY_DCIM = "DCIM"
    private const val DIRECTORY_DOWNLOADS = "Download"
    private const val DIRECTORY_DOCUMENTS = "Documents"
    private const val DIRECTORY_PICTURES = "Pictures"
}
