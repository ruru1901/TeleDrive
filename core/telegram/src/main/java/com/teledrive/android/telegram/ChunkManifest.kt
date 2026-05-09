package com.teledrive.android.telegram

import org.json.JSONArray
import org.json.JSONObject

data class ChunkManifest(
    val transferId: String,
    val originalFileName: String,
    val originalSize: Long,
    val mimeType: String?,
    val chunkSize: Long,
    val sha256: String,
    val chunks: List<Long>,
) {
    val chunkCount: Int
        get() = chunks.size

    fun toJson(): String =
        JSONObject()
            .put(TYPE_KEY, TYPE_VALUE)
            .put("transferId", transferId)
            .put("originalFileName", originalFileName)
            .put("originalSize", originalSize)
            .put("mimeType", mimeType.orEmpty())
            .put("chunkSize", chunkSize)
            .put("sha256", sha256)
            .put("chunks", JSONArray(chunks))
            .toString()

    companion object {
        const val TYPE_KEY = "td_type"
        const val TYPE_VALUE = "chunk_manifest_v1"
        const val CHUNK_CAPTION_TYPE = "chunk_part_v1"

        fun fromJson(text: String): ChunkManifest? =
            runCatching {
                val json = JSONObject(text)
                if (json.optString(TYPE_KEY) != TYPE_VALUE) return null
                val chunksJson = json.optJSONArray("chunks") ?: return null
                val chunks = buildList {
                    for (index in 0 until chunksJson.length()) {
                        add(chunksJson.getLong(index))
                    }
                }
                ChunkManifest(
                    transferId = json.getString("transferId"),
                    originalFileName = json.getString("originalFileName"),
                    originalSize = json.getLong("originalSize"),
                    mimeType = json.optString("mimeType").takeIf { it.isNotBlank() },
                    chunkSize = json.getLong("chunkSize"),
                    sha256 = json.getString("sha256"),
                    chunks = chunks,
                )
            }.getOrNull()

        fun chunkCaption(transferId: String, index: Int, originalFileName: String): String =
            JSONObject()
                .put(TYPE_KEY, CHUNK_CAPTION_TYPE)
                .put("transferId", transferId)
                .put("index", index)
                .put("originalFileName", originalFileName)
                .toString()

        fun isChunkCaption(text: String): Boolean =
            runCatching { JSONObject(text).optString(TYPE_KEY) == CHUNK_CAPTION_TYPE }
                .getOrDefault(false)
    }
}
