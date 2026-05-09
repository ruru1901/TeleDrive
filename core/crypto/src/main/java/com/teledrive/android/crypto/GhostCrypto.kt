package com.teledrive.android.crypto

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.AEAD
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object GhostCrypto {

    private const val NONCE_BYTES = AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES
    private const val KEY_BYTES = AEAD.XCHACHA20POLY1305_IETF_KEYBYTES
    private const val MAC_BYTES = AEAD.XCHACHA20POLY1305_IETF_ABYTES
    private const val HEADER_BYTES = 6

    private val sodium by lazy { LazySodiumAndroid(SodiumAndroid()) }
    private val secureRandom = SecureRandom()

    fun encryptFile(bytes: ByteArray): EncryptResult {
        val keyBytes = randomBytes(KEY_BYTES)
        val nonceBytes = randomBytes(NONCE_BYTES)
        val ciphertext = ByteArray(bytes.size + MAC_BYTES)
        val encrypted = runCatching {
            sodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
                ciphertext,
                null,
                bytes,
                bytes.size.toLong(),
                null,
                0,
                null,
                nonceBytes,
                keyBytes,
            )
        }.getOrElse {
            encryptWithJvmFallback(bytes, keyBytes, nonceBytes).copyInto(ciphertext)
            true
        }
        check(encrypted) {
            "Encryption failed"
        }
        return EncryptResult(
            ciphertext = ciphertext,
            keyBase64 = Base64.getEncoder().encodeToString(keyBytes),
            nonce = nonceBytes,
        )
    }

    fun decryptFile(encryptedBytes: ByteArray, keyBase64: String): ByteArray? {
        return runCatching {
            val unpacked = unpackGhostFile(encryptedBytes) ?: return null
            val keyBytes = Base64.getDecoder().decode(keyBase64)
            val decrypted = ByteArray(unpacked.ciphertext.size - MAC_BYTES)
            val success = runCatching {
                sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
                    decrypted,
                    null,
                    null,
                    unpacked.ciphertext,
                    unpacked.ciphertext.size.toLong(),
                    null,
                    0,
                    unpacked.nonce,
                    keyBytes,
                )
            }.getOrElse {
                return@runCatching decryptWithJvmFallback(unpacked.ciphertext, keyBytes, unpacked.nonce)
            }
            if (!success) {
                return null
            }
            decrypted
        }.getOrNull()
    }

    fun packGhostFile(nonce: ByteArray, ciphertext: ByteArray): ByteArray {
        val result = ByteArray(HEADER_BYTES + nonce.size + ciphertext.size)
        result[0] = 0x47
        result[1] = 0x48
        result[2] = 0x4F
        result[3] = 0x53
        result[4] = 0x54
        result[5] = 0x01
        nonce.copyInto(result, HEADER_BYTES)
        ciphertext.copyInto(result, HEADER_BYTES + nonce.size)
        return result
    }

    fun unpackGhostFile(bytes: ByteArray): UnpackResult? {
        if (bytes.size < HEADER_BYTES + NONCE_BYTES) return null
        if (bytes[0] != 0x47.toByte() || bytes[1] != 0x48.toByte() || bytes[2] != 0x4F.toByte() ||
            bytes[3] != 0x53.toByte() || bytes[4] != 0x54.toByte() || bytes[5] != 0x01.toByte()
        ) {
            return null
        }
        return UnpackResult(
            nonce = bytes.copyOfRange(HEADER_BYTES, HEADER_BYTES + NONCE_BYTES),
            ciphertext = bytes.copyOfRange(HEADER_BYTES + NONCE_BYTES, bytes.size),
        )
    }

    data class EncryptResult(val ciphertext: ByteArray, val keyBase64: String, val nonce: ByteArray) {
        override fun equals(other: Any?) = other is EncryptResult && ciphertext.contentEquals(other.ciphertext) && keyBase64 == other.keyBase64 && nonce.contentEquals(other.nonce)
        override fun hashCode() = 31 * (31 * ciphertext.contentHashCode() + keyBase64.hashCode()) + nonce.contentHashCode()
    }

    data class UnpackResult(val nonce: ByteArray, val ciphertext: ByteArray) {
        override fun equals(other: Any?) = other is UnpackResult && nonce.contentEquals(other.nonce) && ciphertext.contentEquals(other.ciphertext)
        override fun hashCode() = 31 * nonce.contentHashCode() + ciphertext.contentHashCode()
    }

    private fun randomBytes(size: Int): ByteArray =
        runCatching { sodium.randomBytesBuf(size) }.getOrElse {
            ByteArray(size).also(secureRandom::nextBytes)
        }

    private fun encryptWithJvmFallback(bytes: ByteArray, keyBytes: ByteArray, nonceBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(MAC_BYTES * 8, nonceBytes.copyOfRange(0, 12)),
        )
        return cipher.doFinal(bytes)
    }

    private fun decryptWithJvmFallback(ciphertext: ByteArray, keyBytes: ByteArray, nonceBytes: ByteArray): ByteArray? =
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(MAC_BYTES * 8, nonceBytes.copyOfRange(0, 12)),
            )
            cipher.doFinal(ciphertext)
        }.getOrNull()
}
