package com.example.exifclean.data

import android.net.Uri
import java.io.File

data class IntakeItem(
    val id: String,
    val cachedFile: File,
    val originalMimeType: String,
    val sizeBytes: Long,
    val sourceUri: Uri?
)
