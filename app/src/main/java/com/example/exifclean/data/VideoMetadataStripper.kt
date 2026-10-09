package com.example.exifclean.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.exifclean.core.shred.ResultStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class VideoMetadataStripper(private val context: Context) {

    suspend fun stripVideoMetadata(
        inputFile: File,
        outputFile: File
    ): ResultStatus = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(inputFile.absolutePath)
            val trackCount = extractor.trackCount

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Never call muxer.setLocation(...)!

            val trackIndexMap = HashMap<Int, Int>()
            var maxRotation = 0

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                        maxRotation = format.getInteger(MediaFormat.KEY_ROTATION)
                    }
                    val newTrackIndex = muxer.addTrack(format)
                    trackIndexMap[i] = newTrackIndex
                }
            }

            if (maxRotation != 0) {
                muxer.setOrientationHint(maxRotation)
            }

            muxer.start()

            val bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            for (i in 0 until trackCount) {
                if (!trackIndexMap.containsKey(i)) continue
                val muxerTrackIndex = trackIndexMap[i]!!

                extractor.selectTrack(i)
                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)

                    if (bufferInfo.size < 0) {
                        break
                    }

                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    val sampleFlags = extractor.sampleFlags
                    var flags = 0
                    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                        flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                    }
                    bufferInfo.flags = flags

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }
                extractor.unselectTrack(i)
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            ResultStatus.LOSSLESS_CLEANED
        } catch (e: Exception) {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            if (outputFile.exists()) outputFile.delete()
            ResultStatus.FAILED("Video metadata remux error: ${e.localizedMessage ?: "Unknown"}")
        }
    }
}
