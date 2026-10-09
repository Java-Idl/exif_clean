package com.example.exifclean.core.exif

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32

object PngChunkStripper {

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    fun strip(
        input: InputStream,
        output: OutputStream,
        keepIcc: Boolean = true
    ) {
        val src = BufferedInputStream(input)
        val dst = BufferedOutputStream(output)

        val sig = ByteArray(8)
        val readSig = src.read(sig)
        if (readSig < 8 || !sig.contentEquals(PNG_SIGNATURE)) {
            throw IllegalArgumentException("Not a valid PNG file (invalid signature)")
        }

        dst.write(PNG_SIGNATURE)

        while (true) {
            val lengthBytes = ByteArray(4)
            val lRead = src.read(lengthBytes)
            if (lRead < 4) break

            val length = ((lengthBytes[0].toInt() and 0xFF) shl 24) or
                    ((lengthBytes[1].toInt() and 0xFF) shl 16) or
                    ((lengthBytes[2].toInt() and 0xFF) shl 8) or
                    (lengthBytes[3].toInt() and 0xFF)

            val typeBytes = ByteArray(4)
            val tRead = src.read(typeBytes)
            if (tRead < 4) break

            val typeStr = String(typeBytes, Charsets.ISO_8859_1)

            val data = ByteArray(length)
            var dRead = 0
            while (dRead < length) {
                val r = src.read(data, dRead, length - dRead)
                if (r == -1) break
                dRead += r
            }

            val crcBytes = ByteArray(4)
            val cRead = src.read(crcBytes)
            if (cRead < 4) break

            // Verify input CRC
            val crcCalc = CRC32()
            crcCalc.update(typeBytes)
            crcCalc.update(data)
            val expectedCrc = crcCalc.value.toInt()

            val actualCrc = ((crcBytes[0].toInt() and 0xFF) shl 24) or
                    ((crcBytes[1].toInt() and 0xFF) shl 16) or
                    ((crcBytes[2].toInt() and 0xFF) shl 8) or
                    (crcBytes[3].toInt() and 0xFF)

            if (expectedCrc != actualCrc) {
                // Warning / invalid chunk CRC
            }

            val keep = shouldKeepChunk(typeStr, keepIcc)

            if (keep) {
                dst.write(lengthBytes)
                dst.write(typeBytes)
                dst.write(data)

                // Write re-calculated CRC32
                val outCrc = ((expectedCrc shr 24) and 0xFF).toByte()
                val outCrc2 = ((expectedCrc shr 16) and 0xFF).toByte()
                val outCrc3 = ((expectedCrc shr 8) and 0xFF).toByte()
                val outCrc4 = (expectedCrc and 0xFF).toByte()

                dst.write(byteArrayOf(outCrc, outCrc2, outCrc3, outCrc4))
            }

            if (typeStr == "IEND") break
        }

        dst.flush()
    }

    private fun shouldKeepChunk(type: String, keepIcc: Boolean): Boolean {
        return when (type) {
            // Drop metadata chunks
            "tEXt", "iTXt", "zTXt", "eXIf", "tIME", "dSIG" -> false
            "iCCP" -> keepIcc
            // Keep critical and rendering chunks
            "IHDR", "PLTE", "IDAT", "IEND", "gAMA", "cHRM", "sRGB", "bKGD", "pHYs", "tRNS" -> true
            else -> {
                // Ancillary chunks (type starts with lowercase)
                val isAncillary = type.isNotEmpty() && type[0].isLowerCase()
                !isAncillary
            }
        }
    }
}
