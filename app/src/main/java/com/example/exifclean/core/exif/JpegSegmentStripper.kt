package com.example.exifclean.core.exif

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

object JpegSegmentStripper {

    fun strip(
        input: InputStream,
        output: OutputStream,
        keepIcc: Boolean = true,
        cutTrailingData: Boolean = true,
        orientationValue: Int? = null
    ) {
        val src = BufferedInputStream(input)
        val dst = BufferedOutputStream(output)

        val b1 = src.read()
        val b2 = src.read()
        if (b1 != 0xFF || b2 != 0xD8) {
            throw IllegalArgumentException("Not a valid JPEG file (missing SOI marker)")
        }

        // Write SOI
        dst.write(0xFF)
        dst.write(0xD8)

        // If orientation value specified, write minimal EXIF Orientation APP1 block
        if (orientationValue != null && orientationValue in 1..8) {
            val app1Block = createMinimalOrientationApp1(orientationValue)
            dst.write(app1Block)
        }

        while (true) {
            val markerStart = src.read()
            if (markerStart == -1) break
            if (markerStart != 0xFF) continue

            var marker = src.read()
            while (marker == 0xFF) {
                marker = src.read()
            }
            if (marker == -1) break

            // Standalone markers
            if (marker == 0x00 || marker == 0x01 || (marker in 0xD0..0xD7)) {
                dst.write(0xFF)
                dst.write(marker)
                continue
            }

            if (marker == 0xD9) { // EOI
                dst.write(0xFF)
                dst.write(0xD9)
                break
            }

            // Read segment length
            val l1 = src.read()
            val l2 = src.read()
            if (l1 == -1 || l2 == -1) break

            val length = (l1 shl 8) or l2
            if (length < 2) break
            val payloadLen = length - 2

            val payload = ByteArray(payloadLen)
            var readTotal = 0
            while (readTotal < payloadLen) {
                val r = src.read(payload, readTotal, payloadLen - readTotal)
                if (r == -1) break
                readTotal += r
            }

            // Decide whether to keep segment
            val keep = shouldKeepSegment(marker, payload, keepIcc)

            if (keep) {
                dst.write(0xFF)
                dst.write(marker)
                dst.write(l1)
                dst.write(l2)
                dst.write(payload)
            }

            if (marker == 0xDA) { // SOS (Start of Scan)
                // Copy scan payload entropy data up to and including EOI (0xFF 0xD9)
                var prev = 0
                while (true) {
                    val curr = src.read()
                    if (curr == -1) break
                    dst.write(curr)

                    if (prev == 0xFF && curr == 0xD9) {
                        break // EOI reached
                    }
                    prev = curr
                }

                if (!cutTrailingData) {
                    // Copy remaining trailing data if requested
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (src.read(buffer).also { count = it } != -1) {
                        dst.write(buffer, 0, count)
                    }
                }
                break
            }
        }

        dst.flush()
    }

    private fun shouldKeepSegment(marker: Int, payload: ByteArray, keepIcc: Boolean): Boolean {
        // APP1 (EXIF / XMP) -> DROP
        if (marker == 0xE1) return false

        // APP2 (ICC Profile) -> Keep if keepIcc toggle enabled and payload starts with "ICC_PROFILE"
        if (marker == 0xE2) {
            if (keepIcc && payload.size >= 12) {
                val header = String(payload, 0, 12, Charsets.ISO_8859_1)
                if (header.startsWith("ICC_PROFILE\u0000")) {
                    return true
                }
            }
            return false
        }

        // APP13 (IPTC / Photoshop 3.0) -> DROP
        if (marker == 0xED) return false

        // COM (Comment) -> DROP
        if (marker == 0xFE) return false

        // Drop all other APPn markers (0xE0..0xEF)
        if (marker in 0xE0..0xEF) return false

        // Keep structural segments: DQT (0xDB), SOF0..15 (0xC0..0xCF), DHT (0xC4), DRI (0xDD), etc.
        return true
    }

    fun createMinimalOrientationApp1(orientation: Int): ByteArray {
        val payloadLen = 32
        val segLen = payloadLen + 2 // 34
        val result = ByteArray(segLen + 2) // includes 0xFF 0xE1

        var idx = 0
        result[idx++] = 0xFF.toByte()
        result[idx++] = 0xE1.toByte()
        result[idx++] = ((segLen shr 8) and 0xFF).toByte()
        result[idx++] = (segLen and 0xFF).toByte()

        // "Exif\x00\x00"
        result[idx++] = 'E'.code.toByte()
        result[idx++] = 'x'.code.toByte()
        result[idx++] = 'i'.code.toByte()
        result[idx++] = 'f'.code.toByte()
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()

        // TIFF Header Little Endian "II"
        result[idx++] = 'I'.code.toByte()
        result[idx++] = 'I'.code.toByte()
        result[idx++] = 0x2A.toByte() // 42
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x08.toByte() // IFD offset 8
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()

        // 0th IFD field count = 1
        result[idx++] = 0x01.toByte()
        result[idx++] = 0x00.toByte()

        // Tag Entry: Orientation (0x0112)
        result[idx++] = 0x12.toByte()
        result[idx++] = 0x01.toByte()
        result[idx++] = 0x03.toByte() // SHORT = 2
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x01.toByte() // Count = 1
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()
        result[idx++] = (orientation and 0xFF).toByte()
        result[idx++] = ((orientation shr 8) and 0xFF).toByte()
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()

        // Next IFD offset = 0
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()
        result[idx++] = 0x00.toByte()

        return result
    }
}
