package com.teledrive.android.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class CachePolicyTest {
    @Test
    fun prunesOldestEntriesWhenFileLimitExceeded() {
        val entries = (1..4).map {
            CacheEntry(path = "$it", modifiedAt = it.toLong(), bytes = 10)
        }

        val prune = entriesToPrune(entries, maxFiles = 2, maxBytes = 100)

        assertEquals(listOf("1", "2"), prune.map { it.path })
    }

    @Test
    fun prunesOldestEntriesWhenByteLimitExceeded() {
        val entries = listOf(
            CacheEntry(path = "old", modifiedAt = 1, bytes = 70),
            CacheEntry(path = "new", modifiedAt = 2, bytes = 70),
        )

        val prune = entriesToPrune(entries, maxFiles = 30, maxBytes = 100)

        assertEquals(listOf("old"), prune.map { it.path })
    }
}
