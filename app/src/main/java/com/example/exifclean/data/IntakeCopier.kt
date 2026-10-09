package com.example.exifclean.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class IntakeCopier(private val context: Context) {

    private val intakeDir: File
        get() = File(context.cacheDir, "intake").apply { if (!exists()) mkdirs() }

    suspend fun copyUriToIntakeCache(uri: Uri): IntakeItem = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: queryMimeFromExtension(uri) ?: "image/jpeg"
        val id = UUID.randomUUID().toString()
        val tempFile = File(intakeDir, "$id.tmp")

        resolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
        } ?: throw IllegalArgumentException("Cannot open stream for URI")

        IntakeItem(
            id = id,
            cachedFile = tempFile,
            originalMimeType = mimeType,
            sizeBytes = tempFile.length(),
            sourceUri = uri
        )
    }

    suspend fun copyUrisToIntakeCache(uris: List<Uri>): List<IntakeItem> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            try {
                copyUriToIntakeCache(uri)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun clearIntakeCache() {
        try {
            intakeDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    fun deleteItem(item: IntakeItem) {
        try {
            if (item.cachedFile.exists()) {
                item.cachedFile.delete()
            }
        } catch (_: Exception) {}
    }

    private fun queryMimeFromExtension(uri: Uri): String? {
        val path = uri.path?.lowercase() ?: return null
        return when {
            path.endsWith(".png") -> "image/png"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".heic") || path.endsWith(".heif") -> "image/heic"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            else -> null
        }
    }
}
