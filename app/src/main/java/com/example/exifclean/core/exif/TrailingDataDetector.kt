package com.example.exifclean.core.exif

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object TrailingDataDetector {

    fun detectJpegStructure(file: File): Pair<TrailingDataInfo, SegmentPresence> {
        if (!file.exists() || file.length() < 4) {
            return Pair(TrailingDataInfo(false), SegmentPresence())
        }
        return FileInputStream(file).use { detectJpegStructure(it, file.length()) }
    }

    fun detectJpegStructure(inputStream: InputStream, totalSize: Long): Pair<TrailingDataInfo, SegmentPresence> {
        val stream = BufferedInputStream(inputStream)
        var hasExif = false
        var hasXmp = false
        var hasIptc = false
        var hasMakerNote = false
        var hasIcc = false
        var hasThumbnail = false

        var bytesRead = 0L

        // Check SOI (0xFF 0xD8)
        val first = stream.read()
        val second = stream.read()
        if (first != 0xFF || second != 0xD8) {
            return Pair(TrailingDataInfo(false), SegmentPresence())
        }
        bytesRead += 2

        var eoiFound = false
        var eoiOffset = 0L

        while (true) {
            val b = stream.read()
            if (b == -1) break
            bytesRead++

            if (b != 0xFF) continue

            // Read marker code, handling fill bytes (0xFF 0xFF...)
            var marker = stream.read()
            bytesRead++
            while (marker == 0xFF) {
                marker = stream.read()
                bytesRead++
            }

            if (marker == -1) break

            // Standalone / variable markers
            if (marker == 0x00 || marker == 0x01 || (marker in 0xD0..0xD7)) {
                continue
            }

            if (marker == 0xD9) { // EOI
                eoiFound = true
                eoiOffset = bytesRead
                break
            }

            if (marker == 0xD8) { // Unexpected extra SOI
                continue
            }

            // Read 2-byte segment length
            val l1 = stream.read()
            val l2 = stream.read()
            if (l1 == -1 || l2 == -1) break
            bytesRead += 2

            val length = (l1 shl 8) or l2
            if (length < 2) break
            val payloadLen = length - 2

            val payload = ByteArray(minOf(payloadLen, 512))
            var readPayload = 0
            while (readPayload < payload.size) {
                val r = stream.read(payload, readPayload, payload.size - readPayload)
                if (r == -1) break
                readPayload += r
            }
            bytesRead += readPayload

            val skipAmount = payloadLen - readPayload
            if (skipAmount > 0) {
                var skipped = 0L
                while (skipped < skipAmount) {
                    val s = stream.skip(skipAmount - skipped)
                    if (s <= 0) break
                    skipped += s
                }
                bytesRead += skipped
            }

            // Inspect payload signatures
            val headerString = String(payload, 0, minOf(payload.size, 64), Charsets.ISO_8859_1)

            if (marker == 0xE1) { // APP1
                if (headerString.startsWith("Exif\u0000\u0000")) {
                    hasExif = true
                    // Scan payload for MakerNote or embedded thumbnail
                    if (headerString.contains("MakerNote", ignoreCase = true)) {
                        hasMakerNote = true
                    }
                } else if (headerString.startsWith("http://ns.adobe.com/xap/1.0/")) {
                    hasXmp = true
                }
            } else if (marker == 0xE2) { // APP2
                if (headerString.startsWith("ICC_PROFILE\u0000")) {
                    hasIcc = true
                }
            } else if (marker == 0xED) { // APP13
                if (headerString.startsWith("Photoshop 3.0\u0000")) {
                    hasIptc = true
                }
            } else if (marker in 0xE0..0xEF) {
                if (headerString.contains("MakerNote", ignoreCase = true)) {
                    hasMakerNote = true
                }
            }

            if (marker == 0xDA) { // SOS - Start of Scan
                // Scan entropy data byte-by-byte until EOI (0xFF 0xD9)
                var prev = 0
                while (true) {
                    val curr = stream.read()
                    if (curr == -1) break
                    bytesRead++

                    if (prev == 0xFF && curr == 0xD9) {
                        eoiFound = true
                        eoiOffset = bytesRead
                        break
                    }
                    prev = curr
                }
                if (eoiFound) break
            }
        }

        val trailingSize = if (eoiFound && totalSize > eoiOffset) totalSize - eoiOffset else 0L
        var isMotionPhoto = false
        var mp4Size = 0L

        if (trailingSize > 0) {
            // Check trailing data for MP4 box signature ("ftyp" or "moov" or "mdat")
            val trailingHeader = ByteArray(minOf(trailingSize.toInt(), 1024))
            var readTrailing = 0
            while (readTrailing < trailingHeader.size) {
                val r = stream.read(trailingHeader, readTrailing, trailingHeader.size - readTrailing)
                if (r <= 0) break
                readTrailing += r
            }

            val trailingStr = String(trailingHeader, 0, readTrailing, Charsets.ISO_8859_1)
            if (trailingStr.contains("ftyp") || trailingStr.contains("moov") || trailingStr.contains("mdat")) {
                isMotionPhoto = true
                mp4Size = trailingSize
            }
        }

        val trailingInfo = TrailingDataInfo(
            hasTrailingData = trailingSize > 0,
            trailingDataSize = trailingSize,
            isMotionPhoto = isMotionPhoto,
            embeddedMp4Size = mp4Size
        )

        val segments = SegmentPresence(
            hasExif = hasExif,
            hasXmp = hasXmp,
            hasIptc = hasIptc,
            hasMakerNote = hasMakerNote,
            hasIcc = hasIcc,
            hasThumbnail = hasThumbnail
        )

        return Pair(trailingInfo, segments)
    }
}
