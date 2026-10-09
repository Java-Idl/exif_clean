package com.example.exifclean.core.shred

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

object BestEffortOverwriter {

    suspend fun overwriteAndDelete(file: File): ResultStatus = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ResultStatus.FAILED("File does not exist")

        try {
            val fileLength = file.length()
            if (fileLength > 0) {
                // Generate throwaway key for single-pass keystream overwrite
                val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
                val throwawayKey = keyGen.generateKey()
                val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }

                val cipher = Cipher.getInstance("AES/CTR/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, throwawayKey, IvParameterSpec(iv))

                RandomAccessFile(file, "rws").use { raf ->
                    val channel = raf.channel
                    val bufferSize = 64 * 1024
                    val zeros = ByteArray(bufferSize)
                    var written = 0L

                    while (written < fileLength) {
                        val toWrite = minOf((fileLength - written).toInt(), bufferSize)
                        val keystreamBytes = cipher.update(zeros, 0, toWrite)
                        val byteBuf = ByteBuffer.wrap(keystreamBytes ?: zeros, 0, toWrite)
                        while (byteBuf.hasRemaining()) {
                            channel.write(byteBuf)
                        }
                        written += toWrite
                    }

                    // Flush changes to underlying storage
                    channel.force(true)
                    channel.truncate(0)
                }
            }

            // Rename to random name before unlinking
            val randomRename = File(file.parentFile, "shred_${UUID.randomUUID().toString().take(8)}.tmp")
            if (file.renameTo(randomRename)) {
                randomRename.delete()
            } else {
                file.delete()
            }

            ResultStatus.OVERWRITTEN_BEST_EFFORT
        } catch (e: Exception) {
            ResultStatus.FAILED("Best-effort overwrite failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
