package com.example.exifclean.core.exif

import java.io.BufferedInputStream
import java.io.InputStream
import java.security.MessageDigest

object ScanDataHasher {

    fun hashJpegScanData(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val stream = BufferedInputStream(input)

        val first = stream.read()
        val second = stream.read()
        if (first != 0xFF || second != 0xD8) {
            return ""
        }

        while (true) {
            val b = stream.read()
            if (b == -1) break
            if (b != 0xFF) continue

            var marker = stream.read()
            while (marker == 0xFF) {
                marker = stream.read()
            }
            if (marker == -1) break

            if (marker == 0xDA) { // SOS
                // Hash SOS marker and entropy payload up to EOI
                digest.update(0xFF.toByte())
                digest.update(0xDA.toByte())

                var prev = 0
                while (true) {
                    val curr = stream.read()
                    if (curr == -1) break
                    digest.update(curr.toByte())

                    if (prev == 0xFF && curr == 0xD9) { // EOI
                        break
                    }
                    prev = curr
                }
                break
            } else {
                if (marker == 0x00 || marker == 0x01 || (marker in 0xD0..0xD7)) continue
                if (marker == 0xD9) break

                val l1 = stream.read()
                val l2 = stream.read()
                if (l1 == -1 || l2 == -1) break
                val length = (l1 shl 8) or l2
                if (length < 2) break

                var skipped = 0L
                val skipTarget = (length - 2).toLong()
                while (skipped < skipTarget) {
                    val s = stream.skip(skipTarget - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }
        }

        return digest.digest().toHexString()
    }

    fun hashPngIdatData(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val stream = BufferedInputStream(input)

        val sig = ByteArray(8)
        val readSig = stream.read(sig)
        if (readSig < 8) return ""

        while (true) {
            val lengthBytes = ByteArray(4)
            if (stream.read(lengthBytes) < 4) break

            val length = ((lengthBytes[0].toInt() and 0xFF) shl 24) or
                    ((lengthBytes[1].toInt() and 0xFF) shl 16) or
                    ((lengthBytes[2].toInt() and 0xFF) shl 8) or
                    (lengthBytes[3].toInt() and 0xFF)

            val typeBytes = ByteArray(4)
            if (stream.read(typeBytes) < 4) break

            val typeStr = String(typeBytes, Charsets.ISO_8859_1)

            if (typeStr == "IDAT") {
                val buffer = ByteArray(8192)
                var remaining = length
                while (remaining > 0) {
                    val toRead = minOf(remaining, buffer.size)
                    val r = stream.read(buffer, 0, toRead)
                    if (r <= 0) break
                    digest.update(buffer, 0, r)
                    remaining -= r
                }
            } else {
                var skipped = 0L
                val skipTarget = length.toLong()
                while (skipped < skipTarget) {
                    val s = stream.skip(skipTarget - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }

            // Skip CRC (4 bytes)
            stream.skip(4)

            if (typeStr == "IEND") break
        }

        return digest.digest().toHexString()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}
