package com.example.exifclean.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ParanoidReencoder {

    suspend fun reencode(
        inputFile: File,
        outputFile: File,
        targetMimeType: String = "image/jpeg",
        quality: Int = 85
    ) = withContext(Dispatchers.IO) {
        // Read orientation from source file
        val orientation = try {
            val exif = ExifInterface(inputFile.absolutePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        // Calculate subsampling inSampleSize to avoid memory pressure on large images
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(inputFile.absolutePath, boundsOptions)

        val maxDimension = 4096
        var sampleSize = 1
        val w = boundsOptions.outWidth
        val h = boundsOptions.outHeight

        while (w / sampleSize > maxDimension || h / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions)
            ?: throw IllegalArgumentException("Failed to decode image bitmap")

        val rotatedBitmap = applyOrientationMatrix(bitmap, orientation)

        val format = when (targetMimeType) {
            "image/png" -> Bitmap.CompressFormat.PNG
            "image/webp" -> Bitmap.CompressFormat.WEBP_LOSSY
            else -> Bitmap.CompressFormat.JPEG
        }

        FileOutputStream(outputFile).use { out ->
            rotatedBitmap.compress(format, quality, out)
            out.flush()
        }

        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        rotatedBitmap.recycle()
    }

    private fun applyOrientationMatrix(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
