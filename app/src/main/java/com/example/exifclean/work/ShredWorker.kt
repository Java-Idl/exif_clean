package com.example.exifclean.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import com.example.exifclean.core.vault.VaultRepository
import java.io.File

class ShredWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): WorkResult {
        val vaultRepository = VaultRepository(applicationContext)
        val fileId = inputData.getString("file_id")

        return try {
            if (fileId == "SHRED_ALL") {
                vaultRepository.shredEntireVault()
            } else if (fileId != null) {
                val items = vaultRepository.getVaultItems()
                val target = items.find { it.id == fileId }
                if (target != null) {
                    vaultRepository.shredVaultItem(target)
                }
            }

            // Also clean up intake cache
            val intakeDir = File(applicationContext.cacheDir, "intake")
            if (intakeDir.exists()) {
                intakeDir.listFiles()?.forEach { it.delete() }
            }

            WorkResult.success()
        } catch (_: Exception) {
            WorkResult.failure()
        }
    }
}
