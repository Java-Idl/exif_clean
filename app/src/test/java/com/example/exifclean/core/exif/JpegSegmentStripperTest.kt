package com.example.exifclean.core.exif

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class JpegSegmentStripperTest {

    @Test
    fun testStripJpegRemovesApp1AndTrailingBytes() {
        val inputBos = ByteArrayOutputStream()
        inputBos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) // SOI

        // APP1 Exif segment
        val exifData = "Exif\u0000\u0000SomeSecretExifData".toByteArray(Charsets.ISO_8859_1)
        val len = exifData.size + 2
        inputBos.write(0xFF)
        inputBos.write(0xE1)
        inputBos.write((len shr 8) and 0xFF)
        inputBos.write(len and 0xFF)
        inputBos.write(exifData)

        // SOS segment
        inputBos.write(byteArrayOf(0xFF.toByte(), 0xDA.toByte(), 0x00, 0x02))
        inputBos.write("EntropyDataPayload".toByteArray(Charsets.ISO_8859_1))
        inputBos.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte())) // EOI

        // Trailing data
        inputBos.write("TrailingSecrets".toByteArray(Charsets.ISO_8859_1))

        val inputBytes = inputBos.toByteArray()
        val outputBos = ByteArrayOutputStream()

        JpegSegmentStripper.strip(
            input = ByteArrayInputStream(inputBytes),
            output = outputBos,
            keepIcc = true,
            cutTrailingData = true,
            orientationValue = 1
        )

        val outputBytes = outputBos.toByteArray()
        val (trailingInfo, segments) = TrailingDataDetector.detectJpegStructure(
            ByteArrayInputStream(outputBytes),
            outputBytes.size.toLong()
        )

        assertFalse(trailingInfo.hasTrailingData)
        // Verify minimal orientation tag block was added
        assertTrue(segments.hasExif)
    }
}
