package com.audiovault.app.core.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.audiovault.app.core.intelligence.SongMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Intelligent background audio boundary refiner.
 * Analyzes track transition boundaries in the background, detects acoustic onsets,
 * and cleanly trims any previous song bleed without interrupting active recording.
 */
object AudioTransitionRefiner {

    private const val TAG = "AudioVault/Refiner"
    private const val MAX_SCAN_DURATION_US = 6_000_000L // Scan first 6 seconds for boundary
    private const val BUFFER_SIZE = 65536

    data class RefineResult(
        val file: File,
        val durationMs: Long,
        val fileSizeBytes: Long
    )

    /**
     * Refines and trims the audio boundary of a newly captured track in the background.
     */
    suspend fun refineAndTrimBoundary(
        sourceFile: File,
        initialDurationMs: Long,
        metadata: SongMetadata?
    ): RefineResult = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() < 4096L) {
            return@withContext RefineResult(sourceFile, initialDurationMs, sourceFile.length())
        }

        try {
            Log.d(TAG, "Analyzing transition boundary for '${metadata?.displayTitle ?: sourceFile.name}'...")
            val trimOffsetUs = detectTrimOffsetUs(sourceFile)

            if (trimOffsetUs > 250_000L) { // If > 250ms of bleed detected
                Log.i(TAG, "Acoustic boundary detected at ${trimOffsetUs / 1000}ms. Trimming previous song bleed...")
                val trimmedFile = File(sourceFile.parentFile, "trimmed_${sourceFile.name}")
                val success = performLosslessTrim(sourceFile, trimmedFile, trimOffsetUs)

                if (success && trimmedFile.exists() && trimmedFile.length() > 4096L) {
                    val originalPath = sourceFile.absolutePath
                    sourceFile.delete()
                    trimmedFile.renameTo(File(originalPath))

                    val trimmedDurationMs = (initialDurationMs - (trimOffsetUs / 1000L)).coerceAtLeast(1000L)
                    val finalFile = File(originalPath)
                    Log.i(TAG, "Boundary refinement complete: trimmed to ${trimmedDurationMs}ms (${finalFile.length()} bytes)")
                    return@withContext RefineResult(finalFile, trimmedDurationMs, finalFile.length())
                } else if (trimmedFile.exists()) {
                    trimmedFile.delete()
                }
            } else {
                Log.d(TAG, "Track start is already clean (bleed < 250ms). Keeping original audio.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Boundary refiner encountered exception (keeping original file): ${e.message}")
        }

        RefineResult(sourceFile, initialDurationMs, sourceFile.length())
    }

    /**
     * Decodes the initial 6 seconds of audio to detect the exact energy dip / silence boundary
     * between the previous song's outro and the new song's true intro.
     */
    private fun detectTrimOffsetUs(sourceFile: File): Long {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        try {
            extractor.setDataSource(sourceFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) return 0L
            extractor.selectTrack(audioTrackIndex)

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return 0L
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            val energyProfiles = mutableListOf<Pair<Long, Float>>() // Pair<TimestampUs, RMS dB>
            var presentationTimeUs = 0L

            while (!isEos && presentationTimeUs < MAX_SCAN_DURATION_US) {
                val inputIndex = decoder.dequeueInputBuffer(5000L)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            presentationTimeUs = extractor.sampleTime
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                var outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 5000L)
                while (outputIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val pcmBytes = ByteArray(bufferInfo.size)
                        outputBuffer.get(pcmBytes)

                        val rmsDb = calculatePcmRms(pcmBytes, bufferInfo.size)
                        energyProfiles.add(Pair(bufferInfo.presentationTimeUs, rmsDb))
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                        break
                    }
                    outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 0L)
                }
            }

            // Find transition boundary:
            // Look for a distinct silence dip / acoustic trough (< -42dB) within first ~4.5 seconds
            if (energyProfiles.size >= 6) {
                var lowestEnergyUs = 0L
                var minRms = 0.0f
                var hasDip = false

                for (i in 0 until minOf(energyProfiles.size, 100)) {
                    val (timeUs, rms) = energyProfiles[i]
                    if (timeUs in 300_000L..4_500_000L) {
                        if (rms < -42.0f && (lowestEnergyUs == 0L || rms < minRms)) {
                            minRms = rms
                            lowestEnergyUs = timeUs
                            hasDip = true
                        }
                    }
                }

                if (hasDip && lowestEnergyUs > 0L) {
                    return lowestEnergyUs
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Notice: boundary scan skipped: ${e.message}")
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
        }

        return 0L
    }

    /**
     * Losslessly trims the leading audio packets before `trimOffsetUs` using MediaExtractor & MediaMuxer.
     */
    private fun performLosslessTrim(sourceFile: File, destFile: File, trimOffsetUs: Long): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(sourceFile.absolutePath)
            var trackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (trackIndex < 0 || audioFormat == null) return false
            extractor.selectTrack(trackIndex)

            extractor.seekTo(trimOffsetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val actualStartUs = extractor.sampleTime

            muxer = MediaMuxer(destFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime >= actualStartUs) {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = (sampleTime - actualStartUs).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                }

                extractor.advance()
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing lossless trim", e)
            return false
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    private fun calculatePcmRms(buffer: ByteArray, length: Int): Float {
        val sampleCount = length / 2
        if (sampleCount == 0) return -100.0f

        val byteBuffer = ByteBuffer.wrap(buffer, 0, length).order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0

        for (i in 0 until sampleCount) {
            val sample = byteBuffer.short.toDouble()
            sumSquares += sample * sample
        }

        val rms = sqrt(sumSquares / sampleCount)
        if (rms <= 0.0001) return -100.0f

        val db = 20.0 * log10(rms / 32768.0)
        return max(-100.0f, db.toFloat())
    }
}
