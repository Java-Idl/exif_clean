package com.example.exifclean.core.exif

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class PngChunkStripperTest {

    @Test
    fun testStripPngRemovesTextAndExifChunks() {
        val inputBos = ByteArrayOutputStream()
        // PNG signature
        inputBos.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        // Write tEXt chunk
        val typeBytes = "tEXt".toByteArray(Charsets.ISO_8859_1)
        val dataBytes = "Author\u0000John Doe".toByteArray(Charsets.ISO_8859_1)
        val crcCalc = CRC32()
        crcCalc.update(typeBytes)
        crcCalc.update(dataBytes)
        val crc = crcCalc.value.toInt()

        val len = dataBytes.size
        inputBos.write((len shr 24) and 0xFF)
        inputBos.write((len shr 16) and 0xFF)
        inputBos.write((len shr 8) and 0xFF)
        inputBos.write(len and 0xFF)
        inputBos.write(typeBytes)
        inputBos.write(dataBytes)
        inputBos.write((crc shr 24) and 0xFF)
        inputBos.write((crc shr 16) and 0xFF)
        inputBos.write((crc shr 8) and 0xFF)
        inputBos.write(crc and 0xFF)

        // Write IEND chunk
        val iendType = "IEND".toByteArray(Charsets.ISO_8859_1)
        val iendCrc = CRC32().apply { update(iendType) }.value.toInt()
        inputBos.write(0); inputBos.write(0); inputBos.write(0); inputBos.write(0)
        inputBos.write(iendType)
        inputBos.write((iendCrc shr 24) and 0xFF)
        inputBos.write((iendCrc shr 16) and 0xFF)
        inputBos.write((iendCrc shr 8) and 0xFF)
        inputBos.write(iendCrc and 0xFF)

        val inputBytes = inputBos.toByteArray()
        val outputBos = ByteArrayOutputStream()

        PngChunkStripper.strip(
            input = ByteArrayInputStream(inputBytes),
            output = outputBos
        )

        val outputBytes = outputBos.toByteArray()
        val outputStr = String(outputBytes, Charsets.ISO_8859_1)

        assertFalse(outputStr.contains("tEXt"))
        assertFalse(outputStr.contains("John Doe"))
        assertTrue(outputStr.contains("IEND"))
    }
}
