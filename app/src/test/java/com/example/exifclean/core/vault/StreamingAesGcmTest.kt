package com.example.exifclean.core.vault

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.KeyGenerator

class StreamingAesGcmTest {

    private fun generateTestKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun testEncryptionAndDecryptionRoundTrip() {
        val key = generateTestKey()
        val plainText = "The quick brown fox jumps over the lazy dog! ".repeat(5000).toByteArray()

        val cipherBos = ByteArrayOutputStream()
        StreamingAesGcm.encryptStream(
            key = key,
            input = ByteArrayInputStream(plainText),
            output = cipherBos
        )

        val cipherBytes = cipherBos.toByteArray()
        val decryptedBos = ByteArrayOutputStream()

        StreamingAesGcm.decryptStream(
            key = key,
            input = ByteArrayInputStream(cipherBytes),
            output = decryptedBos
        )

        assertArrayEquals(plainText, decryptedBos.toByteArray())
    }

    @Test
    fun testTamperedCiphertextFailsDecryption() {
        val key = generateTestKey()
        val plainText = "Secret Vault Data".toByteArray()

        val cipherBos = ByteArrayOutputStream()
        StreamingAesGcm.encryptStream(
            key = key,
            input = ByteArrayInputStream(plainText),
            output = cipherBos
        )

        val cipherBytes = cipherBos.toByteArray()
        // Flip one byte in the ciphertext payload
        cipherBytes[cipherBytes.size - 2] = (cipherBytes[cipherBytes.size - 2].toInt() xor 0xFF).toByte()

        val decryptedBos = ByteArrayOutputStream()
        try {
            StreamingAesGcm.decryptStream(
                key = key,
                input = ByteArrayInputStream(cipherBytes),
                output = decryptedBos
            )
            fail("Expected exception on tampered ciphertext")
        } catch (_: Exception) {
            // Success: tamper detected
        }
    }
}
