package com.teledrive.android.repository

import com.teledrive.android.data.KeyEntry
import com.teledrive.android.data.KeyEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreRepository @Inject constructor(
    private val keyEntryDao: KeyEntryDao,
) {

    suspend fun saveKey(entry: KeyEntry) {
        withContext(Dispatchers.IO) {
            keyEntryDao.insert(entry)
        }
    }

    suspend fun getKey(messageId: Long): KeyEntry? {
        return withContext(Dispatchers.IO) {
            keyEntryDao.getByKey(messageId)
        }
    }

    suspend fun getAllKeys(): List<KeyEntry> {
        return withContext(Dispatchers.IO) {
            keyEntryDao.getAll()
        }
    }

    suspend fun deleteKey(messageId: Long) {
        withContext(Dispatchers.IO) {
            keyEntryDao.deleteByKey(messageId)
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            keyEntryDao.clearAll()
        }
    }
}
