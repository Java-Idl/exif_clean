package com.example.exifclean.core.vault

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object StreamingAesGcm {

    private const val CHUNK_SIZE = 64 * 1024 // 64 KB
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    fun encryptStream(
        key: SecretKey,
        input: InputStream,
        output: OutputStream
    ) {
        val src = BufferedInputStream(input)
        val dst = BufferedOutputStream(output)
        val random = SecureRandom()

        val buffer = ByteArray(CHUNK_SIZE)
        var chunkIndex = 0

        while (true) {
            var bytesRead = 0
            while (bytesRead < CHUNK_SIZE) {
                val r = src.read(buffer, bytesRead, CHUNK_SIZE - bytesRead)
                if (r == -1) break
                bytesRead += r
            }

            if (bytesRead <= 0) break

            val iv = ByteArray(GCM_IV_LENGTH)
            random.nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)

            // Bind chunk index as AAD (Additional Authenticated Data)
            val aad = ByteBuffer.allocate(4).putInt(chunkIndex).array()
            cipher.updateAAD(aad)

            val ciphertext = cipher.doFinal(buffer, 0, bytesRead)

            // Write chunk header: 4-byte length, 12-byte IV, then ciphertext payload
            val lengthBytes = ByteBuffer.allocate(4).putInt(ciphertext.size).array()
            dst.write(lengthBytes)
            dst.write(iv)
            dst.write(ciphertext)

            chunkIndex++
        }

        dst.flush()
    }

    fun decryptStream(
        key: SecretKey,
        input: InputStream,
        output: OutputStream
    ) {
        val src = BufferedInputStream(input)
        val dst = BufferedOutputStream(output)

        var chunkIndex = 0

        while (true) {
            val lenBytes = ByteArray(4)
            val lRead = src.read(lenBytes)
            if (lRead < 4) break

            val cipherLength = ByteBuffer.wrap(lenBytes).int
            if (cipherLength <= 0 || cipherLength > CHUNK_SIZE + 32) {
                throw IllegalArgumentException("Invalid ciphertext chunk length: $cipherLength")
            }

            val iv = ByteArray(GCM_IV_LENGTH)
            val ivRead = src.read(iv)
            if (ivRead < GCM_IV_LENGTH) {
                throw IllegalArgumentException("Truncated ciphertext IV")
            }

            val cipherPayload = ByteArray(cipherLength)
            var pRead = 0
            while (pRead < cipherLength) {
                val r = src.read(cipherPayload, pRead, cipherLength - pRead)
                if (r == -1) break
                pRead += r
            }

            if (pRead < cipherLength) {
                throw IllegalArgumentException("Truncated ciphertext chunk payload")
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            // Bind chunk index as AAD
            val aad = ByteBuffer.allocate(4).putInt(chunkIndex).array()
            cipher.updateAAD(aad)

            val plaintext = cipher.doFinal(cipherPayload)
            dst.write(plaintext)

            chunkIndex++
        }

        dst.flush()
    }
}
