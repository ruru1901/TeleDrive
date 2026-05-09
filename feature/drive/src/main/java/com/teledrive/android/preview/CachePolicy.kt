package com.teledrive.android.preview

data class CacheEntry(
    val path: String,
    val modifiedAt: Long,
    val bytes: Long,
)

fun entriesToPrune(
    entries: List<CacheEntry>,
    maxFiles: Int = 30,
    maxBytes: Long = 80L * 1024L * 1024L,
): List<CacheEntry> {
    val sorted = entries.sortedBy { it.modifiedAt }.toMutableList()
    val prune = mutableListOf<CacheEntry>()
    var totalBytes = sorted.sumOf { it.bytes }

    while (sorted.size > maxFiles || totalBytes > maxBytes) {
        val oldest = sorted.removeFirstOrNull() ?: break
        prune += oldest
        totalBytes -= oldest.bytes
    }

    return prune
}
