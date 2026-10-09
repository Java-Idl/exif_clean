package com.example.exifclean.core.exif

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

object WebpChunkStripper {

    fun strip(
        input: InputStream,
        output: OutputStream
    ) {
        val src = BufferedInputStream(input)
        val dst = BufferedOutputStream(output)

        val header = ByteArray(12)
        val hRead = src.read(header)
        if (hRead < 12) {
            throw IllegalArgumentException("Not a valid WebP file (truncated header)")
        }

        val riffStr = String(header, 0, 4, Charsets.ISO_8859_1)
        val webpStr = String(header, 8, 4, Charsets.ISO_8859_1)

        if (riffStr != "RIFF" || webpStr != "WEBP") {
            throw IllegalArgumentException("Not a valid WebP RIFF file")
        }

        val outChunks = mutableListOf<ByteArray>()

        while (true) {
            val fourCc = ByteArray(4)
            val fRead = src.read(fourCc)
            if (fRead < 4) break

            val sizeBytes = ByteArray(4)
            val sRead = src.read(sizeBytes)
            if (sRead < 4) break

            val chunkSize = (sizeBytes[0].toInt() and 0xFF) or
                    ((sizeBytes[1].toInt() and 0xFF) shl 8) or
                    ((sizeBytes[2].toInt() and 0xFF) shl 16) or
                    ((sizeBytes[3].toInt() and 0xFF) shl 24)

            val data = ByteArray(chunkSize)
            var dRead = 0
            while (dRead < chunkSize) {
                val r = src.read(data, dRead, chunkSize - dRead)
                if (r == -1) break
                dRead += r
            }

            // Read padding byte if odd size
            val padByte = if (chunkSize % 2 != 0) src.read() else -1

            val chunkType = String(fourCc, Charsets.ISO_8859_1)

            if (chunkType == "EXIF" || chunkType == "XMP ") {
                // Drop EXIF and XMP chunks
                continue
            }

            if (chunkType == "VP8X" && data.isNotEmpty()) {
                // Clear EXIF (0x08) and XMP (0x04) flags in VP8X
                // Flag byte 0: Bit 3 = XMP (0x08), Bit 2 = EXIF (0x04)
                data[0] = (data[0].toInt() and 0x08.inv() and 0x04.inv()).toByte()
            }

            // Pack chunk for output
            val chunkBytes = ByteArray(8 + chunkSize + (if (chunkSize % 2 != 0) 1 else 0))
            System.arraycopy(fourCc, 0, chunkBytes, 0, 4)
            System.arraycopy(sizeBytes, 0, chunkBytes, 4, 4)
            System.arraycopy(data, 0, chunkBytes, 8, chunkSize)
            if (chunkSize % 2 != 0 && padByte != -1) {
                chunkBytes[8 + chunkSize] = padByte.toByte()
            }

            outChunks.add(chunkBytes)
        }

        var totalPayloadSize = 4 // "WEBP"
        for (chunk in outChunks) {
            totalPayloadSize += chunk.size
        }

        // Write "RIFF"
        dst.write("RIFF".toByteArray(Charsets.ISO_8859_1))
        // Write total size (little endian)
        dst.write(totalPayloadSize and 0xFF)
        dst.write((totalPayloadSize shr 8) and 0xFF)
        dst.write((totalPayloadSize shr 16) and 0xFF)
        dst.write((totalPayloadSize shr 24) and 0xFF)
        // Write "WEBP"
        dst.write("WEBP".toByteArray(Charsets.ISO_8859_1))

        // Write all chunks
        for (chunk in outChunks) {
            dst.write(chunk)
        }

        dst.flush()
    }
}
