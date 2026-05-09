package com.teledrive.android.drive

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.teledrive.android.backup.BackupManager
import com.teledrive.android.data.TeleDriveDatabase
import com.teledrive.android.telegram.InMemoryTelegramGateway
import com.teledrive.android.ui.drive.DriveViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DriveViewModelIntegrationTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: TeleDriveDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TeleDriveDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun uploadFile_increasesSortedFiles() = runTest(dispatcher) {
        val gateway = InMemoryTelegramGateway(context)
        val viewModel = DriveViewModel(
            context = context,
            database = database,
            gateway = gateway,
            backupManager = BackupManager(context),
        )
        backgroundScope.launch { viewModel.sortedFiles.collect {} }
        dispatcher.scheduler.advanceUntilIdle()
        val before = viewModel.sortedFiles.value.size
        val source = File(context.cacheDir, "integration-upload.txt").apply {
            writeText("hello from the integration test")
        }

        gateway.uploadFile(Uri.fromFile(source), "integration-upload.txt", null, null).collect {}
        viewModel.refresh()

        repeat(30) {
            dispatcher.scheduler.advanceUntilIdle()
            if (viewModel.sortedFiles.value.size > before) return@runTest
            Thread.sleep(100)
        }

        assertTrue(
            "Expected upload to add a file; message=${viewModel.uiState.value.message}, transfers=${viewModel.uiState.value.transfers}",
            viewModel.sortedFiles.value.size > before,
        )
    }
}
