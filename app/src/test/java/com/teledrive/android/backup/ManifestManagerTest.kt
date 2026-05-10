package com.teledrive.android.backup

import com.teledrive.android.backup.ManifestManager.Companion.toJson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ManifestManagerTest {

    @Test
    fun testManifestSerialization() {
        val manifest = BackupManifest(
            version = 1,
            lastSync = "2024-01-15T10:30:00",
            files = mapOf(
                "Camera/IMG_001.jpg" to FileManifestEntry(
                    messageId = 12345L,
                    hash = "abc123",
                    size = 1024000L,
                    modifiedEpoch = 1705315800000L
                ),
                "Downloads/document.pdf" to FileManifestEntry(
                    messageId = 67890L,
                    hash = "def456",
                    size = 2048000L,
                    modifiedEpoch = 1705315900000L
                )
            )
        )

        val json = manifest.toJson()
        assertNotNull("JSON should not be null", json)
        assertTrue("JSON should contain version", json.contains("\"version\""))
        assertTrue("JSON should contain lastSync", json.contains("\"lastSync\""))
        assertTrue("JSON should contain files", json.contains("\"files\""))
        assertTrue("JSON should contain Camera path", json.contains("Camera/IMG_001.jpg"))
        assertTrue("JSON should contain Downloads path", json.contains("Downloads/document.pdf"))

        val parsed = JSONObject(json)
        assertEquals("Version should match", 1, parsed.getInt("version"))
        assertEquals("LastSync should match", "2024-01-15T10:30:00", parsed.getString("lastSync"))
        
        val filesObj = parsed.getJSONObject("files")
        assertTrue("Files should contain Camera entry", filesObj.has("Camera/IMG_001.jpg"))
        
        val cameraEntry = filesObj.getJSONObject("Camera/IMG_001.jpg")
        assertEquals("MessageId should match", 12345L, cameraEntry.getLong("messageId"))
        assertEquals("Hash should match", "abc123", cameraEntry.getString("hash"))
        assertEquals("Size should match", 1024000L, cameraEntry.getLong("size"))
    }

    @Test
    fun testEmptyManifestSerialization() {
        val manifest = BackupManifest()
        val json = manifest.toJson()
        
        val parsed = JSONObject(json)
        assertEquals("Version should be 1", 1, parsed.getInt("version"))
        assertEquals("LastSync should be empty", "", parsed.getString("lastSync"))
        assertEquals("Files should be empty", 0, parsed.getJSONObject("files").length())
    }

    @Test
    fun testManifestWithSpecialCharacters() {
        val manifest = BackupManifest(
            version = 1,
            lastSync = "2024-01-15T10:30:00",
            files = mapOf(
                "WhatsApp/IMG (1).jpg" to FileManifestEntry(
                    messageId = 111L,
                    hash = "xyz789",
                    size = 500000L,
                    modifiedEpoch = 1705315800000L
                )
            )
        )

        val json = manifest.toJson()
        val parsed = JSONObject(json)
        val filesObj = parsed.getJSONObject("files")
        assertTrue("Should handle special chars in path", filesObj.has("WhatsApp/IMG (1).jpg"))
    }
}
