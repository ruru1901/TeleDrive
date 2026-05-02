package com.teledrive.android.drive

import com.teledrive.android.data.FileEntity
import com.teledrive.android.data.FolderEntity
import com.teledrive.android.telegram.TelegramFile
import com.teledrive.android.telegram.TelegramFolder

fun TelegramFolder.toEntity(updatedAt: Long = System.currentTimeMillis()): FolderEntity =
    FolderEntity(
        id = id,
        name = name,
        markerSource = markerSource,
        updatedAt = updatedAt,
    )

fun TelegramFile.toEntity(): FileEntity =
    FileEntity(
        messageId = messageId,
        folderId = folderId,
        name = name,
        size = size,
        mimeType = mimeType,
        extension = extension,
        createdAt = createdAt,
        localCachePath = localCachePath,
        thumbnailBase64 = thumbnailBase64,
    )

fun folderDisplayName(rawTitle: String): String =
    rawTitle
        .replace(" [TD]", "")
        .replace(" [td]", "")
        .replace("[TD]", "")
        .replace("[td]", "")
        .trim()

fun isTeleDriveFolder(title: String, about: String?): Boolean =
    title.contains("[TD]", ignoreCase = true) ||
        about?.contains("[telegram-drive-folder]", ignoreCase = true) == true
