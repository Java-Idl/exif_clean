package com.example.exifclean.core.exif

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TrailingDataDetectorTest {

    @Test
    fun testDetectsCleanJpegWithNoTrailingData() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) // SOI
        bos.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte())) // EOI

        val input = bos.toByteArray()
        val (trailingInfo, segments) = TrailingDataDetector.detectJpegStructure(
            ByteArrayInputStream(input),
            input.size.toLong()
        )

        assertFalse(trailingInfo.hasTrailingData)
        assertEquals(0L, trailingInfo.trailingDataSize)
        assertFalse(trailingInfo.isMotionPhoto)
        assertFalse(segments.hasExif)
    }

    @Test
    fun testDetectsExifSegmentAndMotionPhotoTrailingBytes() {
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) // SOI

        // Write APP1 EXIF segment
        val exifHeader = "Exif\u0000\u0000DummyExifData".toByteArray(Charsets.ISO_8859_1)
        val len = exifHeader.size + 2
        bos.write(0xFF)
        bos.write(0xE1)
        bos.write((len shr 8) and 0xFF)
        bos.write(len and 0xFF)
        bos.write(exifHeader)

        bos.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte())) // EOI

        // Trailing MP4 payload
        val mp4Payload = "ftypisom00000000moovDummyVideo".toByteArray(Charsets.ISO_8859_1)
        bos.write(mp4Payload)

        val input = bos.toByteArray()
        val (trailingInfo, segments) = TrailingDataDetector.detectJpegStructure(
            ByteArrayInputStream(input),
            input.size.toLong()
        )

        assertTrue(segments.hasExif)
        assertTrue(trailingInfo.hasTrailingData)
        assertTrue(trailingInfo.isMotionPhoto)
        assertEquals(mp4Payload.size.toLong(), trailingInfo.trailingDataSize)
    }
}
