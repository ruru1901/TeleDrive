package com.teledrive.android

import com.teledrive.android.backup.BackupPathResolver
import com.teledrive.android.data.BackupFolder
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/**
 * Integrated tests for the Backup & Sync functions.
 * These tests verify path resolution and state logic for the backup engine.
 */
class BackupIntegrationTest {

    @Test
    fun testPathResolver_ResolvesCommonFolders() {
        val foldersToTest = listOf(
            BackupFolder.Camera,
            BackupFolder.Downloads,
            BackupFolder.Screenshots,
            BackupFolder.Documents
        )
        
        foldersToTest.forEach { folder ->
            val path = BackupPathResolver.getPathForFolder(folder)
            println("Testing ${folder.label} -> Resolved to: $path")
            
            // In a headless test environment, the path might not exist physically,
            // but the resolver should return a valid File object.
            assertNotNull("Path for ${folder.label} should not be null", path)
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
}
