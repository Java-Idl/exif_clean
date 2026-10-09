package com.example.exifclean.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.exifclean.core.exif.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

class MetadataReader(private val context: Context) {

    suspend fun readMetadata(item: IntakeItem, hasLocationPermission: Boolean = false): MetadataReport = withContext(Dispatchers.IO) {
        val tags = mutableListOf<MetadataTag>()

        var inputStream: InputStream? = null
        val exifInterface = try {
            if (item.sourceUri != null && hasLocationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val requireOriginalUri = MediaStore.setRequireOriginal(item.sourceUri)
                    inputStream = context.contentResolver.openInputStream(requireOriginalUri)
                } catch (_: Exception) {}
            }
            if (inputStream == null) {
                inputStream = item.cachedFile.inputStream()
            }
            ExifInterface(inputStream!!)
        } catch (_: Exception) {
            null
        } finally {
            inputStream?.close()
        }

        var gpsLat: Double? = null
        var gpsLong: Double? = null
        var gpsDisplayString: String? = null
        var gpsExplanation: String? = null

        if (exifInterface != null) {
            val latLong = exifInterface.latLong
            if (latLong != null && latLong.size >= 2) {
                gpsLat = latLong[0]
                gpsLong = latLong[1]
                gpsDisplayString = formatGpsToDms(latLong[0], latLong[1])
                tags.add(
                    MetadataTag(
                        category = "GPS",
                        key = "GPS Coordinates",
                        displayValue = gpsDisplayString,
                        riskLevel = RiskLevel.HIGH
                    )
                )
            } else {
                val hasPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
                gpsExplanation = if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && item.sourceUri != null) {
                    "Redacted (Location permission ACCESS_MEDIA_LOCATION required to read GPS)"
                } else {
                    "Absent (No GPS tags found)"
                }
            }

            // Extract standard EXIF attributes
            readTag(exifInterface, ExifInterface.TAG_DATETIME_ORIGINAL, "Timestamps", "Date/Time Original", tags)
            readTag(exifInterface, ExifInterface.TAG_DATETIME_DIGITIZED, "Timestamps", "Date/Time Digitized", tags)
            readTag(exifInterface, ExifInterface.TAG_DATETIME, "Timestamps", "Date/Time Modified", tags)

            readTag(exifInterface, ExifInterface.TAG_MAKE, "Device", "Camera Make", tags)
            readTag(exifInterface, ExifInterface.TAG_MODEL, "Device", "Camera Model", tags)
            readTag(exifInterface, ExifInterface.TAG_SOFTWARE, "Device", "Software", tags)
            readTag(exifInterface, ExifInterface.TAG_BODY_SERIAL_NUMBER, "Device", "Body Serial Number", tags)

            readTag(exifInterface, ExifInterface.TAG_LENS_MAKE, "Lens", "Lens Make", tags)
            readTag(exifInterface, ExifInterface.TAG_LENS_MODEL, "Lens", "Lens Model", tags)
            readTag(exifInterface, ExifInterface.TAG_LENS_SERIAL_NUMBER, "Lens", "Lens Serial Number", tags)

            readTag(exifInterface, ExifInterface.TAG_ORIENTATION, "Image", "Orientation", tags)
            readTag(exifInterface, ExifInterface.TAG_IMAGE_WIDTH, "Image", "Image Width", tags)
            readTag(exifInterface, ExifInterface.TAG_IMAGE_LENGTH, "Image", "Image Height", tags)

            if (exifInterface.hasThumbnail()) {
                tags.add(
                    MetadataTag(
                        category = "Thumbnail",
                        key = "Embedded Thumbnail",
                        displayValue = "Present (${exifInterface.thumbnailBytes?.size ?: 0} bytes)",
                        riskLevel = RiskLevel.MEDIUM
                    )
                )
            }
        } else {
            gpsExplanation = "Absent (File cannot be parsed for EXIF)"
        }

        // Run segment analysis for trailing data & segment presence
        val (trailingInfo, segments) = if (item.originalMimeType == "image/jpeg" || item.cachedFile.name.lowercase().endsWith(".jpg")) {
            TrailingDataDetector.detectJpegStructure(item.cachedFile)
        } else {
            Pair(TrailingDataInfo(false), SegmentPresence())
        }

        val overallRisk = RiskScorer.calculateOverallRisk(tags, trailingInfo)

        MetadataReport(
            tags = tags,
            overallRisk = overallRisk,
            gpsLatitude = gpsLat,
            gpsLongitude = gpsLong,
            gpsDisplayString = gpsDisplayString,
            gpsExplanation = gpsExplanation,
            trailingData = trailingInfo,
            segments = segments
        )
    }

    private fun readTag(
        exif: ExifInterface,
        tagKey: String,
        category: String,
        label: String,
        tags: MutableList<MetadataTag>
    ) {
        val value: String? = exif.getAttribute(tagKey)
        if (value != null && value.trim { it <= ' ' }.isNotEmpty()) {
            tags.add(
                MetadataTag(
                    category = category,
                    key = label,
                    displayValue = value,
                    riskLevel = RiskScorer.scoreTag(label)
                )
            )
        }
    }

    companion object {
        fun formatGpsToDms(lat: Double, lng: Double): String {
            val latStr = convertToDms(lat, isLatitude = true)
            val lngStr = convertToDms(lng, isLatitude = false)
            return "$latStr $lngStr"
        }

        private fun convertToDms(coordinate: Double, isLatitude: Boolean): String {
            val absCoord = Math.abs(coordinate)
            val degrees = absCoord.toInt()
            val minutesDouble = (absCoord - degrees) * 60.0
            val minutes = minutesDouble.toInt()
            val seconds = (minutesDouble - minutes) * 60.0

            val hemisphere = if (isLatitude) {
                if (coordinate >= 0) "N" else "S"
            } else {
                if (coordinate >= 0) "E" else "W"
            }

            return String.format(Locale.US, "%d°%02d'%04.1f\"%s", degrees, minutes, seconds, hemisphere)
        }
    }
}
