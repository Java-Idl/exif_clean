package com.example.exifclean.core.exif

import org.junit.Assert.assertEquals
import org.junit.Test

class RiskScorerTest {

    @Test
    fun testGPSKeyIsHighRisk() {
        assertEquals(RiskLevel.HIGH, RiskScorer.scoreTag("GPSLatitude"))
        assertEquals(RiskLevel.HIGH, RiskScorer.scoreTag("SerialNumber"))
        assertEquals(RiskLevel.HIGH, RiskScorer.scoreTag("DocumentID"))
    }

    @Test
    fun testDateAndModelAreMediumRisk() {
        assertEquals(RiskLevel.MEDIUM, RiskScorer.scoreTag("DateTimeOriginal"))
        assertEquals(RiskLevel.MEDIUM, RiskScorer.scoreTag("Make"))
        assertEquals(RiskLevel.MEDIUM, RiskScorer.scoreTag("Model"))
        assertEquals(RiskLevel.MEDIUM, RiskScorer.scoreTag("Software"))
    }

    @Test
    fun testCameraSettingsAreLowRisk() {
        assertEquals(RiskLevel.LOW, RiskScorer.scoreTag("FNumber"))
        assertEquals(RiskLevel.LOW, RiskScorer.scoreTag("ExposureTime"))
        assertEquals(RiskLevel.LOW, RiskScorer.scoreTag("ISOSpeedRatings"))
    }

    @Test
    fun testCalculateOverallRisk() {
        val highTag = MetadataTag("GPS", "GPSLatitude", "37.7749", RiskLevel.HIGH)
        val medTag = MetadataTag("Camera", "Make", "Canon", RiskLevel.MEDIUM)
        val lowTag = MetadataTag("Camera", "FNumber", "f/2.8", RiskLevel.LOW)

        val noTrailing = TrailingDataInfo(hasTrailingData = false)
        val withMotionPhoto = TrailingDataInfo(hasTrailingData = true, isMotionPhoto = true)

        assertEquals(RiskLevel.HIGH, RiskScorer.calculateOverallRisk(listOf(highTag, lowTag), noTrailing))
        assertEquals(RiskLevel.HIGH, RiskScorer.calculateOverallRisk(listOf(lowTag), withMotionPhoto))
        assertEquals(RiskLevel.MEDIUM, RiskScorer.calculateOverallRisk(listOf(medTag, lowTag), noTrailing))
        assertEquals(RiskLevel.LOW, RiskScorer.calculateOverallRisk(listOf(lowTag), noTrailing))
    }
}
