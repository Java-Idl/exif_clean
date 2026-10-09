package com.example.exifclean.data

import android.content.Context
import com.example.exifclean.core.shred.ResultStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

data class BatchProgressState(
    val totalCount: Int,
    val processedCount: Int,
    val currentFileName: String,
    val results: List<SanitizationResult>,
    val isCompleted: Boolean = false
)

class BatchProcessor(
    context: Context,
    private val maxConcurrency: Int = 4
) {
    private val sanitizerEngine = SanitizerEngine(context)

    fun processBatch(
        items: List<IntakeItem>,
        outputDir: File,
        options: SanitizationOptions
    ): Flow<BatchProgressState> = flow {
        val total = items.size
        var processed = 0
        val results = mutableListOf<SanitizationResult>()
        val semaphore = Semaphore(maxConcurrency)

        emit(BatchProgressState(total, 0, "", emptyList(), false))

        for (item in items) {
            semaphore.withPermit {
                emit(BatchProgressState(total, processed, item.cachedFile.name, results.toList(), false))
                val result = sanitizerEngine.sanitize(item, outputDir, options)
                results.add(result)
                processed++
                emit(BatchProgressState(total, processed, item.cachedFile.name, results.toList(), processed == total))
            }
        }
    }.flowOn(Dispatchers.IO)
}
