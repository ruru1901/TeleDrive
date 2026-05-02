package com.teledrive.android.secure

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

class SecureSettings(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "teledrive_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveApiCredentials(apiId: Int, apiHash: String) {
        prefs.edit {
            putInt(KEY_API_ID, apiId)
            putString(KEY_API_HASH, apiHash)
        }
    }

    fun apiCredentials(): ApiCredentials? {
        val apiId = prefs.getInt(KEY_API_ID, 0)
        val apiHash = prefs.getString(KEY_API_HASH, null)
        return if (apiId > 0 && !apiHash.isNullOrBlank()) {
            ApiCredentials(apiId, apiHash)
        } else {
            null
        }
    }

    fun tdlibDatabaseKey(): ByteArray {
        val existing = prefs.getString(KEY_TDLIB_DATABASE_KEY, null)
        if (!existing.isNullOrBlank()) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }

        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        prefs.edit {
            putString(KEY_TDLIB_DATABASE_KEY, Base64.encodeToString(key, Base64.NO_WRAP))
        }
        return key
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val KEY_API_ID = "telegram_api_id"
        const val KEY_API_HASH = "telegram_api_hash"
        const val KEY_TDLIB_DATABASE_KEY = "tdlib_database_key"
    }
}

data class ApiCredentials(
    val apiId: Int,
    val apiHash: String,
)
