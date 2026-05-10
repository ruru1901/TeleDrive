package com.teledrive.android.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TeleDriveDaoTest {
    private lateinit var database: TeleDriveDatabase
    private lateinit var dao: TeleDriveDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TeleDriveDatabase::class.java).build()
        dao = database.dao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertFileAndTransfer_readsThemBack() = runTest {
        val file = FileEntity(
            messageId = 42L,
            folderId = null,
            name = "photo.jpg",
            size = 128L,
            mimeType = "image/jpeg",
            extension = "jpg",
            createdAt = 1_700_000_000L,
            localCachePath = null,
        )
        dao.upsertFiles(listOf(file))

        val files = dao.observeFiles(null).first()
        assertTrue(files.any { it.messageId == 42L && it.name == "photo.jpg" })

        val transfer = TransferEntity(
            id = "transfer-1",
            type = TransferType.Upload,
            fileName = "photo.jpg",
            folderId = null,
            messageId = 42L,
            status = TransferStatus.Running,
            progress = 40,
            error = null,
        )
        dao.upsertTransfer(transfer)

        val transfers = dao.observeTransfers().first()
        assertEquals(transfer, transfers.single())
    }
}
