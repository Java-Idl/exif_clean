package com.example.exifclean.core.shred

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OriginalDeleter {

    suspend fun createBatchDeleteRequest(
        resolver: ContentResolver,
        uris: List<Uri>
    ): PendingIntent? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(resolver, uris)
        } else {
            null
        }
    }

    suspend fun deleteSafDocument(
        resolver: ContentResolver,
        uri: Uri
    ): ResultStatus = withContext(Dispatchers.IO) {
        try {
            val deleted = DocumentsContract.deleteDocument(resolver, uri)
            if (deleted) ResultStatus.UNLINKED_ONLY else ResultStatus.FAILED("Failed to delete document via SAF contract")
        } catch (e: Exception) {
            ResultStatus.FAILED("SAF document deletion error: ${e.localizedMessage ?: "Unknown"}")
        }
    }
}
