package com.example.exifclean.data

import android.content.Context
import com.example.exifclean.core.exif.*
import com.example.exifclean.core.shred.ResultStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class SanitizationMode {
    LOSSLESS,
    PARANOID
}

data class SanitizationOptions(
    val mode: SanitizationMode = SanitizationMode.LOSSLESS,
    val keepIcc: Boolean = true,
    val cutTrailingData: Boolean = true,
    val orientationValue: Int? = 1, // Minimal EXIF Orientation tag value or null
    val paranoidQuality: Int = 85
)

data class SanitizationResult(
    val status: ResultStatus,
    val outputFile: File?,
    val beforeReport: MetadataReport,
    val afterReport: MetadataReport?
)

class SanitizerEngine(private val context: Context) {

    private val metadataReader = MetadataReader(context)

    suspend fun sanitize(
        item: IntakeItem,
        outputDir: File,
        options: SanitizationOptions = SanitizationOptions()
    ): SanitizationResult = withContext(Dispatchers.IO) {
        val beforeReport = metadataReader.readMetadata(item)
        val tempOutputFile = File(outputDir, "clean_${item.id}.tmp")

        var isLosslessUsed = false
        val isHeic = item.originalMimeType == "image/heic" || item.originalMimeType == "image/heif"

        var inputHash = ""

        try {
            if (options.mode == SanitizationMode.LOSSLESS && !isHeic) {
                isLosslessUsed = true
                FileInputStream(item.cachedFile).use { input ->
                    FileOutputStream(tempOutputFile).use { output ->
                        when (item.originalMimeType) {
                            "image/png" -> {
                                inputHash = ScanDataHasher.hashPngIdatData(item.cachedFile.inputStream())
                                PngChunkStripper.strip(input, output, keepIcc = options.keepIcc)
                            }
                            "image/webp" -> {
                                WebpChunkStripper.strip(input, output)
                            }
                            else -> { // Default JPEG
                                inputHash = ScanDataHasher.hashJpegScanData(item.cachedFile.inputStream())
                                JpegSegmentStripper.strip(
                                    input = input,
                                    output = output,
                                    keepIcc = options.keepIcc,
                                    cutTrailingData = options.cutTrailingData,
                                    orientationValue = options.orientationValue
                                )
                            }
                        }
                    }
                }
            } else {
                // Paranoid re-encoding
                ParanoidReencoder.reencode(
                    inputFile = item.cachedFile,
                    outputFile = tempOutputFile,
                    targetMimeType = item.originalMimeType,
                    quality = options.paranoidQuality
                )
            }

            // MANDATORY VERIFICATION
            val tempItem = IntakeItem(
                id = "temp_${item.id}",
                cachedFile = tempOutputFile,
                originalMimeType = item.originalMimeType,
                sizeBytes = tempOutputFile.length(),
                sourceUri = null
            )
            val afterReport = metadataReader.readMetadata(tempItem)

            // Verify metadata zeroed out (except structural dimensions and minimal orientation tag)
            val metadataTags = afterReport.tags.filterNot {
                it.key == "Orientation" || it.key == "Image Width" || it.key == "Image Height"
            }
            if (metadataTags.isNotEmpty()) {
                tempOutputFile.delete()
                return@withContext SanitizationResult(
                    status = ResultStatus.FAILED("Verification failed: Metadata tags still present after sanitization (${metadataTags.size} tags)"),
                    outputFile = null,
                    beforeReport = beforeReport,
                    afterReport = afterReport
                )
            }

            // Verify trailing data removed if cut trailing data was requested
            if (options.cutTrailingData && afterReport.trailingData.hasTrailingData) {
                tempOutputFile.delete()
                return@withContext SanitizationResult(
                    status = ResultStatus.FAILED("Verification failed: Trailing bytes or Motion Photo payload still present after sanitization"),
                    outputFile = null,
                    beforeReport = beforeReport,
                    afterReport = afterReport
                )
            }

            // Verify Lossless payload hash assertion
            if (isLosslessUsed) {
                val outputHash = when (item.originalMimeType) {
                    "image/png" -> ScanDataHasher.hashPngIdatData(tempOutputFile.inputStream())
                    "image/jpeg" -> ScanDataHasher.hashJpegScanData(tempOutputFile.inputStream())
                    else -> ""
                }
                if (inputHash.isNotEmpty() && outputHash.isNotEmpty() && inputHash != outputHash) {
                    tempOutputFile.delete()
                    return@withContext SanitizationResult(
                        status = ResultStatus.FAILED("Verification failed: Lossless scan data payload hash mismatch"),
                        outputFile = null,
                        beforeReport = beforeReport,
                        afterReport = afterReport
                    )
                }
            }

            val finalStatus = if (isLosslessUsed) ResultStatus.LOSSLESS_CLEANED else ResultStatus.REENCODED_CLEANED
            SanitizationResult(
                status = finalStatus,
                outputFile = tempOutputFile,
                beforeReport = beforeReport,
                afterReport = afterReport
            )

        } catch (e: Exception) {
            if (tempOutputFile.exists()) {
                tempOutputFile.delete()
            }
            SanitizationResult(
                status = ResultStatus.FAILED("Sanitization error: ${e.localizedMessage ?: "Unknown failure"}"),
                outputFile = null,
                beforeReport = beforeReport,
                afterReport = null
            )
        }
    }
}
