package com.teledrive.android.backup

import com.teledrive.android.telegram.TdLibReflection
import com.teledrive.android.telegram.TelegramGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class FileManifestEntry(
    val messageId: Long,
    val hash: String,
    val size: Long,
    val modifiedEpoch: Long,
)

data class BackupManifest(
    val version: Int = 1,
    val lastSync: String = "",
    val files: Map<String, FileManifestEntry> = emptyMap(),
)

class ManifestManager(private val gateway: TelegramGateway) {
    private val reflection = TdLibReflection.available()

    suspend fun getOrCreateManifest(): Triple<BackupManifest, Long, Boolean> = withContext(Dispatchers.IO) {
        val pinnedMessages = gateway.getPinnedMessages(null)
        val manifestMessageId = findManifestMessage(pinnedMessages)
        
        if (manifestMessageId != null) {
            val manifest = parseManifestFromMessageId(manifestMessageId)
            return@withContext Triple(manifest, manifestMessageId, false)
        } else {
            val newManifest = BackupManifest()
            val newMessageId = gateway.sendMessage(newManifest.toJson(), null)
            gateway.pinMessage(newMessageId, null)
            return@withContext Triple(newManifest, newMessageId, true)
        }
    }

    suspend fun updateManifest(manifest: BackupManifest, messageId: Long) {
        gateway.editMessage(messageId, manifest.toJson(), null)
    }

    private suspend fun findManifestMessage(pinnedIds: List<Long>): Long? {
        for (id in pinnedIds) {
            val message = gateway.getMessage(id, null) ?: continue
            val content = reflection.field(message, "content") ?: continue
            if (reflection.simpleName(content) == "MessageText") {
                val text = reflection.stringField(content, "text") ?: continue
                if (text.contains("\"version\"") && text.contains("\"files\"")) {
                    return id
                }
            }
        }
        return null
    }

    private suspend fun parseManifestFromMessageId(messageId: Long): BackupManifest {
        val message = gateway.getMessage(messageId, null) ?: return BackupManifest()
        val content = reflection.field(message, "content") ?: return BackupManifest()
        if (reflection.simpleName(content) != "MessageText") return BackupManifest()
        
        val text = reflection.stringField(content, "text") ?: return BackupManifest()
        return try {
            val json = JSONObject(text)
            val version = json.optInt("version", 1)
            val lastSync = json.optString("lastSync", "")
            val filesObj = json.optJSONObject("files") ?: JSONObject()
            
            val files = mutableMapOf<String, FileManifestEntry>()
            val keys = filesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = filesObj.getJSONObject(key)
                files[key] = FileManifestEntry(
                    messageId = entry.optLong("messageId"),
                    hash = entry.optString("hash"),
                    size = entry.optLong("size"),
                    modifiedEpoch = entry.optLong("modifiedEpoch"),
                )
            }
            
            BackupManifest(version = version, lastSync = lastSync, files = files)
        } catch (e: Exception) {
            BackupManifest()
        }
    }

    companion object {
        fun BackupManifest.toJson(): String {
            val json = JSONObject()
            json.put("version", version)
            json.put("lastSync", lastSync)
            
            val filesObj = JSONObject()
            files.forEach { (path, entry) ->
                val entryObj = JSONObject()
                entryObj.put("messageId", entry.messageId)
                entryObj.put("hash", entry.hash)
                entryObj.put("size", entry.size)
                entryObj.put("modifiedEpoch", entry.modifiedEpoch)
                filesObj.put(path, entryObj)
            }
            json.put("files", filesObj)
            
            return json.toString()
        }
    }
}