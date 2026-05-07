package com.teledrive.android.secure

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.teledrive.android.data.TeleDriveDatabase
import java.security.SecureRandom

class SecureSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "teledrive_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    
    val ghostPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "ghost_secure",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private var _ghostDb: TeleDriveDatabase? = null
    val ghostDb: TeleDriveDatabase?
        get() {
            if (_ghostDb == null) {
                _ghostDb = try {
                    Room.databaseBuilder(
                        appContext,
                        TeleDriveDatabase::class.java,
                        "teledrive_ghost.db",
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                } catch (e: Exception) {
                    null
                }
            }
            return _ghostDb
        }

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

    fun setHasCompletedLogin(completed: Boolean) {
        prefs.edit { putBoolean(KEY_LOGGED_IN, completed) }
    }

    fun hasCompletedLogin(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)
    
    fun getDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    
    fun setDarkMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }
    
    fun setMasterPasswordSet(set: Boolean) {
        ghostPrefs.edit { putBoolean(PREF_MASTER_PASSWORD_SET, set) }
    }
    
    fun isMasterPasswordSet(): Boolean = ghostPrefs.getBoolean(PREF_MASTER_PASSWORD_SET, false)

    fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val KEY_API_ID = "telegram_api_id"
        const val KEY_API_HASH = "telegram_api_hash"
        const val KEY_TDLIB_DATABASE_KEY = "tdlib_database_key"
        const val KEY_LOGGED_IN = "logged_in"
        const val KEY_DARK_MODE = "dark_mode"
        const val PREF_MASTER_PASSWORD_SET = "ghost_master_password_set"
    }
}

data class ApiCredentials(
    val apiId: Int,
    val apiHash: String,
)
