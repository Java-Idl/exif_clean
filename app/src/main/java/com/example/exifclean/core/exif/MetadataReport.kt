package com.example.exifclean.core.exif

data class MetadataTag(
    val category: String,
    val key: String,
    val displayValue: String,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    HIGH,
    MEDIUM,
    LOW
}

data class TrailingDataInfo(
    val hasTrailingData: Boolean,
    val trailingDataSize: Long = 0L,
    val isMotionPhoto: Boolean = false,
    val embeddedMp4Size: Long = 0L
)

data class SegmentPresence(
    val hasExif: Boolean = false,
    val hasXmp: Boolean = false,
    val hasIptc: Boolean = false,
    val hasMakerNote: Boolean = false,
    val hasIcc: Boolean = false,
    val hasThumbnail: Boolean = false
)

data class MetadataReport(
    val tags: List<MetadataTag> = emptyList(),
    val overallRisk: RiskLevel = RiskLevel.LOW,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsDisplayString: String? = null,
    val gpsExplanation: String? = null,
    val trailingData: TrailingDataInfo = TrailingDataInfo(hasTrailingData = false),
    val segments: SegmentPresence = SegmentPresence()
)
