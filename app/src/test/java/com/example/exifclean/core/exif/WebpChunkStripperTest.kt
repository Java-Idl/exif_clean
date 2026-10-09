package com.example.exifclean.core.exif

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WebpChunkStripperTest {

    @Test
    fun testStripWebpRemovesExifChunk() {
        val inputBos = ByteArrayOutputStream()
        // Write RIFF header placeholder
        inputBos.write("RIFF".toByteArray(Charsets.ISO_8859_1))
        inputBos.write(byteArrayOf(0, 0, 0, 0)) // Size placeholder
        inputBos.write("WEBP".toByteArray(Charsets.ISO_8859_1))

        // Write EXIF chunk
        val exifData = "EXIFPayloadData".toByteArray(Charsets.ISO_8859_1)
        inputBos.write("EXIF".toByteArray(Charsets.ISO_8859_1))
        val len = exifData.size
        inputBos.write(len and 0xFF)
        inputBos.write((len shr 8) and 0xFF)
        inputBos.write((len shr 16) and 0xFF)
        inputBos.write((len shr 24) and 0xFF)
        inputBos.write(exifData)
        if (len % 2 != 0) inputBos.write(0)

        // Write dummy VP8 chunk
        val vp8Data = byteArrayOf(1, 2, 3, 4)
        inputBos.write("VP8 ".toByteArray(Charsets.ISO_8859_1))
        inputBos.write(4)
        inputBos.write(0); inputBos.write(0); inputBos.write(0)
        inputBos.write(vp8Data)

        val inputBytes = inputBos.toByteArray()
        val outputBos = ByteArrayOutputStream()

        WebpChunkStripper.strip(
            input = ByteArrayInputStream(inputBytes),
            output = outputBos
        )

        val outputBytes = outputBos.toByteArray()
        val outputStr = String(outputBytes, Charsets.ISO_8859_1)

        assertFalse(outputStr.contains("EXIFPayloadData"))
        assertTrue(outputStr.startsWith("RIFF"))
        assertTrue(outputStr.contains("WEBP"))
        assertTrue(outputStr.contains("VP8 "))
    }
}
