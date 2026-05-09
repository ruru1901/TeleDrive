package com.teledrive.android.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GhostCryptoTest {
    @Test
    fun encryptDecrypt_roundTripsPlaintext() {
        val plaintext = "known plaintext".toByteArray()
        val encrypted = GhostCrypto.encryptFile(plaintext)
        val packed = GhostCrypto.packGhostFile(encrypted.nonce, encrypted.ciphertext)

        val decrypted = GhostCrypto.decryptFile(packed, encrypted.keyBase64)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun decryptFile_returnsNullForTamperedCiphertext() {
        val encrypted = GhostCrypto.encryptFile("known plaintext".toByteArray())
        val packed = GhostCrypto.packGhostFile(encrypted.nonce, encrypted.ciphertext)
        packed[packed.lastIndex] = (packed.last().toInt() xor 1).toByte()

        val decrypted = GhostCrypto.decryptFile(packed, encrypted.keyBase64)

        assertNull(decrypted)
    }
}
