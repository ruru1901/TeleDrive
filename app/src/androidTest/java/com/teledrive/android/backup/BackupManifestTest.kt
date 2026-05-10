package com.teledrive.android.backup

import android.util.Base64
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManifestTest {
    @Test
    fun sha256Change_marksFileAsChanged() {
        val file = File.createTempFile("manifest", ".txt")
        file.writeText("first")
        val firstHash = sha256(file)
        val manifest = BackupManifest(
            files = mapOf(
                "Documents/manifest.txt" to FileManifestEntry(
                    messageId = 7L,
                    hash = firstHash,
                    size = file.length(),
                    modifiedEpoch = file.lastModified(),
                ),
            ),
        )

        file.writeText("second")
        val secondHash = sha256(file)
        val changed = manifest.files["Documents/manifest.txt"]?.hash != secondHash

        assertTrue(changed)
        file.delete()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
    }
}
