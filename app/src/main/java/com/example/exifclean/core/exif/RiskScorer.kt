package com.example.exifclean.core.exif

object RiskScorer {

    fun scoreTag(key: String): RiskLevel {
        val lowerKey = key.lowercase()

        // HIGH RISK: GPS coordinates, serial numbers, unique identifiers, personal info
        if (lowerKey.contains("gps") ||
            lowerKey.contains("serial") ||
            lowerKey.contains("unique") ||
            lowerKey.contains("documentid") ||
            lowerKey.contains("instanceid") ||
            lowerKey.contains("owner") ||
            lowerKey.contains("artist") ||
            lowerKey.contains("copyright") ||
            lowerKey.contains("body") ||
            lowerKey.contains("lensserial")
        ) {
            return RiskLevel.HIGH
        }

        // MEDIUM RISK: Timestamps, Make, Model, Software (excluding technical exposuretime)
        if (lowerKey != "exposuretime" && !lowerKey.contains("subsectime") && (
            lowerKey.contains("date") ||
            lowerKey.contains("time") ||
            lowerKey.contains("make") ||
            lowerKey.contains("model") ||
            lowerKey.contains("software")
        )) {
            return RiskLevel.MEDIUM
        }

        // LOW RISK: General technical camera settings
        return RiskLevel.LOW
    }

    fun calculateOverallRisk(tags: List<MetadataTag>, trailingDataInfo: TrailingDataInfo): RiskLevel {
        if (tags.any { it.riskLevel == RiskLevel.HIGH } || trailingDataInfo.isMotionPhoto) {
            return RiskLevel.HIGH
        }
        if (tags.any { it.riskLevel == RiskLevel.MEDIUM } || trailingDataInfo.hasTrailingData) {
            return RiskLevel.MEDIUM
        }
        return RiskLevel.LOW
    }
}
