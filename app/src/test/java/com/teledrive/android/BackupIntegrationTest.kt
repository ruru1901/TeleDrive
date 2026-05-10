package com.teledrive.android

import androidx.test.core.app.ApplicationProvider
import com.teledrive.android.backup.BackupPathResolver
import com.teledrive.android.data.BackupFolder
import com.teledrive.android.data.BackupSettingsEntity
import com.teledrive.android.data.TeleDriveConverters
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integrated tests for the Backup & Sync functions.
 * These tests verify path resolution and state logic for the backup engine.
 */
@RunWith(RobolectricTestRunner::class)
class BackupIntegrationTest {

    @Test
    fun testPathResolver_QueriesCommonFolders() {
        val foldersToTest = listOf(
            BackupFolder.Camera,
            BackupFolder.Downloads,
            BackupFolder.Screenshots,
            BackupFolder.Documents
        )
        
        foldersToTest.forEach { folder ->
            val uris = BackupPathResolver.resolveUris(
                ApplicationProvider.getApplicationContext(),
                BackupSettingsEntity(folders = setOf(folder)),
            )
            assertNotNull("Uris for ${folder.label} should not be null", uris)
        }
    }

    @Test
    fun testSyncStatus_EnumValues() {
        // Verify that the SyncStatus enum contains all required states
        val states = com.teledrive.android.data.SyncStatus.entries
        assert(states.contains(com.teledrive.android.data.SyncStatus.LocalOnly))
        assert(states.contains(com.teledrive.android.data.SyncStatus.CloudOnly))
        assert(states.contains(com.teledrive.android.data.SyncStatus.Synced))
        println("SyncStatus enum verified with ${states.size} states.")
    }

    @Test
    fun testBackupFolderConverter_IgnoresLegacyUnknownFolders() {
        val folders = TeleDriveConverters().backupFoldersFromString("Camera,CustomFolder,Downloads")
        assertEquals(setOf(BackupFolder.Camera, BackupFolder.Downloads), folders)
    }
}
