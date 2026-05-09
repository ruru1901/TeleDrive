package com.teledrive.android.backup

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import com.teledrive.android.data.BackupFolder
import com.teledrive.android.data.BackupScope
import com.teledrive.android.data.BackupSettingsEntity
import java.io.File

object BackupPathResolver {
    fun resolveBackupFiles(context: Context, settings: BackupSettingsEntity): List<BackupSourceFile> {
        val resolver = context.contentResolver
        return if (settings.scope == BackupScope.EntireStorage) {
            queryMedia(
                resolver = resolver,
                collection = MediaStore.Files.getContentUri("external"),
                relativePrefix = "storage",
            )
        } else {
            settings.folders.flatMap { folder -> queryFolder(resolver, folder) }
        }
    }

    fun resolveUris(context: Context, settings: BackupSettingsEntity): List<Uri> =
        resolveBackupFiles(context, settings).map { it.uri }

    private fun queryFolder(resolver: ContentResolver, folder: BackupFolder): List<BackupSourceFile> =
        when (folder) {
            BackupFolder.Camera -> queryMedia(
                resolver = resolver,
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                relativePathLike = "%DCIM/Camera%",
                relativePrefix = folder.label,
            )
            BackupFolder.Screenshots -> queryMedia(
                resolver = resolver,
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                relativePathLike = "%Screenshots%",
                relativePrefix = folder.label,
            )
            BackupFolder.Downloads -> queryMedia(
                resolver = resolver,
                collection = MediaStore.Files.getContentUri("external"),
                relativePathLike = "%Download%",
                relativePrefix = folder.label,
            )
            BackupFolder.WhatsAppImages -> queryMedia(
                resolver = resolver,
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                relativePathLike = "%WhatsApp Images%",
                relativePrefix = folder.label,
            )
            BackupFolder.WhatsAppVideo -> queryMedia(
                resolver = resolver,
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                relativePathLike = "%WhatsApp Video%",
                relativePrefix = folder.label,
            )
            BackupFolder.Documents -> queryMedia(
                resolver = resolver,
                collection = MediaStore.Files.getContentUri("external"),
                mimeTypeLike = "%document%",
                relativePathLike = "%Documents%",
                relativePrefix = folder.label,
            )
        }

    private fun queryMedia(
        resolver: ContentResolver,
        collection: Uri,
        relativePathLike: String? = null,
        mimeTypeLike: String? = null,
        relativePrefix: String,
    ): List<BackupSourceFile> {
        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val selections = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (relativePathLike != null) {
            selections += "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            args += relativePathLike
        }
        if (mimeTypeLike != null) {
            selections += "${MediaStore.MediaColumns.MIME_TYPE} LIKE ?"
            args += mimeTypeLike
        }

        val files = mutableListOf<BackupSourceFile>()
        resolver.query(
            collection,
            projection,
            selections.takeIf { it.isNotEmpty() }?.joinToString(" AND "),
            args.takeIf { it.isNotEmpty() }?.toTypedArray(),
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val relativeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: "backup-file-$id"
                val relativePath = cursor.getString(relativeIndex).orEmpty().trim('/')
                val path = listOf(relativePrefix, relativePath, name)
                    .filter { it.isNotBlank() }
                    .joinToString("/")
                    .replace("//", "/")
                files += BackupSourceFile(
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = name,
                    relativePath = path,
                    size = cursor.getLong(sizeIndex),
                    modifiedEpoch = cursor.getLong(modifiedIndex) * 1000L,
                )
            }
        }
        return files
    }

    fun getStorageRoot(): File = runCatching { Environment.getExternalStorageDirectory() }
        .getOrElse {
            File(
                System.getProperty("user.home")
                    ?: System.getProperty("java.io.tmpdir")
                    ?: ".",
                "TeleDriveStorage",
            )
        }
}

data class BackupSourceFile(
    val uri: Uri,
    val displayName: String,
    val relativePath: String,
    val size: Long,
    val modifiedEpoch: Long,
)
