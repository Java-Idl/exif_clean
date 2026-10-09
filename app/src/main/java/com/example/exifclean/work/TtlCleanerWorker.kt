package com.example.exifclean.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import com.example.exifclean.core.shred.BestEffortOverwriter
import java.io.File

class TtlCleanerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): WorkResult {
        val ttlMillis = inputData.getLong("ttl_millis", 24 * 60 * 60 * 1000L)
        val cutoffTime = System.currentTimeMillis() - ttlMillis

        try {
            // Clean intake cache
            val intakeDir = File(applicationContext.cacheDir, "intake")
            cleanDirectory(intakeDir, cutoffTime)

            // Clean app-private cleaned outputs
            val cleanedDir = File(applicationContext.filesDir, "cleaned")
            cleanDirectory(cleanedDir, cutoffTime)

            return WorkResult.success()
        } catch (_: Exception) {
            return WorkResult.failure()
        }
    }

    private suspend fun cleanDirectory(dir: File, cutoffTime: Long) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                BestEffortOverwriter.overwriteAndDelete(file)
            }
        }
    }
}
