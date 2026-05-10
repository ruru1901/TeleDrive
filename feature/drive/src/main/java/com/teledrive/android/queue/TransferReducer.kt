package com.teledrive.android.queue

import com.teledrive.android.data.TransferEntity
import com.teledrive.android.data.TransferStatus
import com.teledrive.android.telegram.TransferProgress

fun TransferEntity.applyProgress(progress: TransferProgress): TransferEntity =
    copy(
        status = when {
            progress.error != null -> TransferStatus.Error
            progress.done -> TransferStatus.Success
            else -> TransferStatus.Running
        },
        progress = progress.progress.coerceIn(0, 100),
        error = progress.error,
    )
