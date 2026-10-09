package com.example.exifclean.core.shred

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

class FreeSpaceFiller(private val context: Context) {

    private val fillDir: File
        get() = File(context.filesDir, "free_space_fill").apply { if (!exists()) mkdirs() }

    private val markerFile: File
        get() = File(fillDir, "free_space_fill_in_progress.marker")

    suspend fun fillFreeSpace(
        onProgress: (bytesWritten: Long, freeRemaining: Long) -> Unit,
        isCancelled: () -> Boolean
    ): ResultStatus = withContext(Dispatchers.IO) {
        // Pre-check safety conditions
        if (!isDeviceCharging(context)) {
            return@withContext ResultStatus.FAILED("Safety Guard: Device must be connected to a charger")
        }

        if (isThermalThrottled(context)) {
            return@withContext ResultStatus.FAILED("Safety Guard: Device thermal state is elevated")
        }

        // Cleanup any previous leftovers
        cleanupLeftovers()

        markerFile.writeText("IN_PROGRESS_${System.currentTimeMillis()}")

        val statFs = StatFs(context.filesDir.path)
        val totalBytes = statFs.totalBytes
        val stopThresholdBytes = maxOf(1024L * 1024L * 1024L, (totalBytes * 0.08).toLong())

        val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
        val throwawayKey = keyGen.generateKey()
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, throwawayKey, IvParameterSpec(iv))

        val chunkSize = 10 * 1024 * 1024 // 10 MB chunks
        val zeros = ByteArray(64 * 1024)
        var totalWritten = 0L
        var chunkIndex = 0

        try {
            while (!isCancelled()) {
                val currentStat = StatFs(context.filesDir.path)
                val available = currentStat.availableBytes

                if (available <= stopThresholdBytes) {
                    break // Reached safety threshold (1 GB or 8% free)
                }

                val chunkFile = File(fillDir, "fill_chunk_$chunkIndex.bin")
                RandomAccessFile(chunkFile, "rws").use { raf ->
                    val channel = raf.channel
                    var chunkWritten = 0

                    while (chunkWritten < chunkSize && !isCancelled()) {
                        val currentAvail = StatFs(context.filesDir.path).availableBytes
                        if (currentAvail <= stopThresholdBytes) break

                        val toWrite = minOf(zeros.size, chunkSize - chunkWritten)
                        val keystream = cipher.update(zeros, 0, toWrite)
                        val buf = ByteBuffer.wrap(keystream ?: zeros, 0, toWrite)
                        while (buf.hasRemaining()) {
                            channel.write(buf)
                        }
                        chunkWritten += toWrite
                        totalWritten += toWrite

                        onProgress(totalWritten, currentAvail)
                    }
                    channel.force(true)
                }
                chunkIndex++
            }

            cleanupLeftovers()
            ResultStatus.OVERWRITTEN_BEST_EFFORT
        } catch (e: Exception) {
            cleanupLeftovers()
            ResultStatus.FAILED("Free space fill interrupted: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun cleanupLeftovers() {
        try {
            if (fillDir.exists()) {
                fillDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    companion object {
        fun isDeviceCharging(context: Context): Boolean {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            return batteryManager?.isCharging == true
        }

        fun isThermalThrottled(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
                val status = powerManager.currentThermalStatus
                return status >= PowerManager.THERMAL_STATUS_SEVERE
            }
            return false
        }
    }
}
