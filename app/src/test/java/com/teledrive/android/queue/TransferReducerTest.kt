package com.teledrive.android.queue

import com.teledrive.android.data.TransferEntity
import com.teledrive.android.data.TransferStatus
import com.teledrive.android.data.TransferType
import com.teledrive.android.telegram.TransferProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferReducerTest {
    @Test
    fun progressClampsAndMarksRunning() {
        val transfer = baseTransfer().applyProgress(TransferProgress(progress = 140))

        assertEquals(TransferStatus.Running, transfer.status)
        assertEquals(100, transfer.progress)
    }

    @Test
    fun doneMarksSuccess() {
        val transfer = baseTransfer().applyProgress(TransferProgress(progress = 100, done = true))

        assertEquals(TransferStatus.Success, transfer.status)
    }

    @Test
    fun errorMarksError() {
        val transfer = baseTransfer().applyProgress(TransferProgress(progress = 50, error = "Network"))

        assertEquals(TransferStatus.Error, transfer.status)
        assertEquals("Network", transfer.error)
    }

    private fun baseTransfer() = TransferEntity(
        id = "id",
        type = TransferType.Upload,
        fileName = "file.txt",
        folderId = null,
        messageId = null,
        status = TransferStatus.Pending,
        progress = 0,
        error = null,
    )
}
