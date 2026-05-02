package com.teledrive.android.drive

import com.teledrive.android.telegram.TelegramFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {
    @Test
    fun folderDisplayNameRemovesMarkers() {
        assertEquals("Photos", folderDisplayName("Photos [TD]"))
        assertEquals("Docs", folderDisplayName("[td] Docs"))
    }

    @Test
    fun isTeleDriveFolderAcceptsTitleOrAboutMarker() {
        assertTrue(isTeleDriveFolder("Archive [TD]", null))
        assertTrue(isTeleDriveFolder("Archive", "Telegram Drive [telegram-drive-folder]"))
        assertFalse(isTeleDriveFolder("Archive", "ordinary channel"))
    }

    @Test
    fun telegramFileMapsToRoomEntity() {
        val file = TelegramFile(
            messageId = 42,
            folderId = 7,
            name = "clip.mp4",
            size = 128,
            mimeType = "video/mp4",
            extension = "mp4",
            createdAt = 1000,
        )

        val entity = file.toEntity()

        assertEquals(42L, entity.messageId)
        assertEquals(7L, entity.folderId)
        assertEquals("clip.mp4", entity.name)
        assertEquals("video/mp4", entity.mimeType)
    }
}
