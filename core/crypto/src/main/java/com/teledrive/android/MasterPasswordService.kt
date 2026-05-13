package com.teledrive.android

import android.content.Context
import android.util.Base64
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.teledrive.android.repository.KeystoreRepository
import com.teledrive.android.secure.SecureSettings
import com.teledrive.android.telegram.TelegramGateway
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class MasterPasswordService @Inject constructor(
    @Suppress("unused")
    private val context: Context,
    private val keystoreRepo: KeystoreRepository,
    private val prefs: SecureSettings,
    private val tdlib: TelegramGateway,
) {
    private val argon2 = Argon2Kt()
    private val secureRandom = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true }
    private val failedAttempts = AtomicInteger(0)
    private val lockoutUntil = AtomicLong(0L)

    suspend fun setMasterPassword(password: String) = withContext(Dispatchers.Default) {
        val salt = randomBytes(SALT_BYTES)
        val kek = deriveKek(password, salt)
        val allKeys = keystoreRepo.getAllKeys()
        val keysJson = json.encodeToString(allKeys)
        val plaintext = keysJson.toByteArray()
        val nonce = randomBytes(AES_GCM_NONCE_BYTES)
        val ciphertext = encryptKeystore(plaintext, kek, nonce)
        val blob = salt + nonce + ciphertext
        val blobB64 = Base64.encodeToString(blob, Base64.NO_WRAP)
        prefs.ghostPrefs.edit().putString("ghost_keystore_enc", blobB64).apply()
        prefs.ghostPrefs.edit().putString("ghost_kek_salt", Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
        if (prefs.keystoreBackupEnabled()) {
            uploadKeystoreBackup(blobB64)
        }
        prefs.setKeystoreBackupEnabled(true)
        prefs.ghostPrefs.edit().putBoolean("ghost_master_password_set", true).apply()
    }

    suspend fun verifyMasterPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        if (System.currentTimeMillis() < lockoutUntil.get()) return@withContext false
        val blobB64 = prefs.ghostPrefs.getString("ghost_keystore_enc", null) ?: return@withContext recordFailedAttempt()
        val saltB64 = prefs.ghostPrefs.getString("ghost_kek_salt", null) ?: return@withContext recordFailedAttempt()
        val blob = Base64.decode(blobB64, Base64.NO_WRAP)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        if (blob.size < SALT_BYTES + AES_GCM_NONCE_BYTES + AES_GCM_TAG_BYTES) {
            return@withContext recordFailedAttempt()
        }
        val nonce = blob.copyOfRange(SALT_BYTES, SALT_BYTES + AES_GCM_NONCE_BYTES)
        val encryptedData = blob.copyOfRange(SALT_BYTES + AES_GCM_NONCE_BYTES, blob.size)
        val kek = deriveKek(password, salt)
        val decrypted = decryptKeystore(encryptedData, kek, nonce) ?: return@withContext recordFailedAttempt()
        val valid = runCatching {
            json.decodeFromString<List<com.teledrive.android.data.KeyEntry>>(decrypted.decodeToString())
            true
        }.getOrElse { false }
        return@withContext if (valid) {
            failedAttempts.set(0)
            lockoutUntil.set(0L)
            true
        } else {
            recordFailedAttempt()
        }
    }

    suspend fun isMasterPasswordSet(): Boolean = prefs.ghostPrefs.getBoolean("ghost_master_password_set", false)

    suspend fun syncKeystore() {
        if (!isMasterPasswordSet()) return
        if (!prefs.keystoreBackupEnabled()) return
        val blobB64 = prefs.ghostPrefs.getString("ghost_keystore_enc", null) ?: return
        uploadKeystoreBackup(blobB64)
    }

    fun exportKeystoreBase64(): String = prefs.ghostPrefs.getString("ghost_keystore_enc", "") ?: ""

    suspend fun restoreKeystoreFromTelegram(): Boolean = withContext(Dispatchers.IO) {
        val reflection = com.teledrive.android.telegram.TdLibReflection.availableOrNull() ?: return@withContext false
        val pinnedIds = runCatching { tdlib.getPinnedMessages(null) }.getOrNull() ?: return@withContext false
        for (id in pinnedIds) {
            val message = runCatching { tdlib.getMessage(id, null) }.getOrNull() ?: continue
            val content = reflection.field(message, "content") ?: continue
            if (reflection.simpleName(content) != "MessageText") continue
            val text = reflection.stringField(content, "text") ?: continue
            if (!text.startsWith("#ghost_keystore")) continue
            val blobB64 = text.substringAfter("\n\n").trim()
            if (blobB64.isBlank()) continue
            val blob = Base64.decode(blobB64, Base64.NO_WRAP)
            if (blob.size < SALT_BYTES + AES_GCM_NONCE_BYTES + AES_GCM_TAG_BYTES) continue
            val salt = blob.copyOfRange(0, SALT_BYTES)
            prefs.ghostPrefs.edit().putString("ghost_keystore_enc", blobB64).apply()
            prefs.ghostPrefs.edit().putString("ghost_kek_salt", Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
        prefs.setKeystoreBackupEnabled(true)
        prefs.ghostPrefs.edit().putBoolean("ghost_master_password_set", true).apply()
            return@withContext true
        }
        false
    }

    suspend fun isMasterPasswordCorrect(password: String): Boolean = verifyMasterPassword(password)

    private suspend fun uploadKeystoreBackup(blobB64: String) = withContext(Dispatchers.IO) {
        val msgId = runCatching {
            tdlib.sendMessage("#ghost_keystore - do not delete\n\n$blobB64", null)
        }.getOrNull()
        if (msgId != null) {
            runCatching { tdlib.pinMessage(msgId, null) }
        }
    }

    private fun deriveKek(password: String, salt: ByteArray): ByteArray =
        argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password.toByteArray(),
            salt = salt,
            tCostInIterations = 3,
            mCostInKibibyte = 32768,
            parallelism = 1,
            hashLengthInBytes = 32,
        ).rawHashAsByteArray()

    private fun encryptKeystore(plaintext: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(AES_GCM_TAG_BITS, nonce),
        )
        return cipher.doFinal(plaintext)
    }

    private fun decryptKeystore(ciphertext: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray? =
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(AES_GCM_TAG_BITS, nonce),
            )
            cipher.doFinal(ciphertext)
        }.getOrNull()

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also(secureRandom::nextBytes)

    private fun recordFailedAttempt(): Boolean {
        val attempts = failedAttempts.incrementAndGet()
        if (attempts >= 5) {
            lockoutUntil.set(System.currentTimeMillis() + 60_000L * attempts)
        }
        return false
    }

    private companion object {
        const val SALT_BYTES = 16
        const val AES_GCM_NONCE_BYTES = 12
        const val AES_GCM_TAG_BITS = 128
        const val AES_GCM_TAG_BYTES = AES_GCM_TAG_BITS / 8
    }
}
