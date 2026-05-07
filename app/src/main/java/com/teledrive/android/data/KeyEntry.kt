package com.teledrive.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "key_entries")
data class KeyEntry(
    @PrimaryKey val messageId: Long,
    val keyBase64: String,
    val originalFilename: String,
    val originalMime: String,
    val uploadedAt: Long,
    val isEncrypted: Boolean = true
)