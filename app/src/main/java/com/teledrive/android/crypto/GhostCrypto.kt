package com.teledrive.android.crypto

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.SecretBox
import android.util.Base64

object GhostCrypto {

    private val sodium = LazySodiumAndroid(SodiumAndroid())

    fun encryptFile(bytes: ByteArray): EncryptResult {
        val keyBytes = sodium.randomBytesBuf(SecretBox.KEYBYTES)
        val nonceBytes = sodium.randomBytesBuf(SecretBox.NONCEBYTES)
        val ciphertext = ByteArray(bytes.size + SecretBox.MACBYTES)
        check(sodium.cryptoSecretBoxEasy(ciphertext, bytes, bytes.size.toLong(), nonceBytes, keyBytes)) {
            "Encryption failed"
        }
        return EncryptResult(
            ciphertext = ciphertext,
            keyBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP),
            nonce = nonceBytes,
        )
    }

    fun decryptFile(encryptedBytes: ByteArray, keyBase64: String): ByteArray? {
        return runCatching {
            val unpacked = unpackGhostFile(encryptedBytes) ?: return null
            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
            val decrypted = ByteArray(unpacked.ciphertext.size - SecretBox.MACBYTES)
            if (!sodium.cryptoSecretBoxOpenEasy(decrypted, unpacked.ciphertext, unpacked.ciphertext.size.toLong(), unpacked.nonce, keyBytes)) return null
            decrypted
        }.getOrNull()
    }

    fun packGhostFile(nonce: ByteArray, ciphertext: ByteArray): ByteArray {
        val result = ByteArray(30 + ciphertext.size)
        result[0] = 0x47; result[1] = 0x48; result[2] = 0x4F; result[3] = 0x53; result[4] = 0x54
        result[5] = 0x01
        nonce.copyInto(result, 6)
        ciphertext.copyInto(result, 30)
        return result
    }

    fun unpackGhostFile(bytes: ByteArray): UnpackResult? {
        if (bytes.size < 30) return null
        if (bytes[0] != 0x47.toByte() || bytes[1] != 0x48.toByte() || bytes[2] != 0x4F.toByte() ||
            bytes[3] != 0x53.toByte() || bytes[4] != 0x54.toByte() || bytes[5] != 0x01.toByte()) return null
        return UnpackResult(nonce = bytes.copyOfRange(6, 30), ciphertext = bytes.copyOfRange(30, bytes.size))
    }

    data class EncryptResult(val ciphertext: ByteArray, val keyBase64: String, val nonce: ByteArray) {
        override fun equals(other: Any?) = other is EncryptResult && ciphertext.contentEquals(other.ciphertext) && keyBase64 == other.keyBase64 && nonce.contentEquals(other.nonce)
        override fun hashCode() = 31 * (31 * ciphertext.contentHashCode() + keyBase64.hashCode()) + nonce.contentHashCode()
    }

    data class UnpackResult(val nonce: ByteArray, val ciphertext: ByteArray) {
        override fun equals(other: Any?) = other is UnpackResult && nonce.contentEquals(other.nonce) && ciphertext.contentEquals(other.ciphertext)
        override fun hashCode() = 31 * nonce.contentHashCode() + ciphertext.contentHashCode()
    }
}
