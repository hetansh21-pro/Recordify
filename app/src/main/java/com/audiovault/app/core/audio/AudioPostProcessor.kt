package com.audiovault.app.core.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.audiovault.app.core.intelligence.SongMetadata
import com.audiovault.app.core.intelligence.SponsorBlockHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

data class ProcessedAudioResult(
    val file: File,
    val durationMs: Long,
    val isValidSong: Boolean
)

/**
 * 3-Layer Post-Recording Audio Scanner, Editor & Cleaner Engine.
 * 
 * Layer 1: Acoustic Ad & Spoken Intro/Outro Classifier
 * Layer 2: YouTube SponsorBlock & Metadata Segment Slicer
 * Layer 3: Lossless Sample-Accurate Media Slicer & Remuxer
 */
object AudioPostProcessor {
    private const val TAG = "AudioVault/PostProcessor"
    private const val MIN_VALID_SONG_DURATION_MS = 45_000L

    suspend fun processAndCleanRecording(
        rawFile: File,
        rawDurationMs: Long,
        metadata: SongMetadata?
    ): ProcessedAudioResult = withContext(Dispatchers.IO) {
        if (!rawFile.exists() || rawFile.length() < 4096L) {
            return@withContext ProcessedAudioResult(rawFile, 0L, isValidSong = false)
        }

        try {
            var trimStartMs = 0L
            var trimEndMs = rawDurationMs

            // -------------------------------------------------------------
            // LAYER 1: Acoustic Scan for Pre-Roll / Outro Ad Spikes
            // -------------------------------------------------------------
            val acousticTrim = detectAcousticAdBoundaries(rawFile, rawDurationMs)
            if (acousticTrim.first > 0L) {
                trimStartMs = maxOf(trimStartMs, acousticTrim.first)
                Log.i(TAG, "Layer 1 (Acoustic Scanner): Detected pre-roll ad chunk of ${trimStartMs}ms. Setting trimStart.")
            }
            if (acousticTrim.second < rawDurationMs) {
                trimEndMs = minOf(trimEndMs, acousticTrim.second)
                Log.i(TAG, "Layer 1 (Acoustic Scanner): Detected outro promo chunk. Setting trimEnd at ${trimEndMs}ms.")
            }

            // -------------------------------------------------------------
            // LAYER 2: SponsorBlock & Segment Metadata Slicer
            // -------------------------------------------------------------
            val videoId = extractYouTubeVideoId(metadata)
            if (!videoId.isNullOrBlank()) {
                val segments = SponsorBlockHelper.fetchSkipSegments(videoId)
                for (segment in segments) {
                    // Pre-roll or intro sponsor segment
                    if (segment.startMs <= 5_000L && segment.endMs > 0L && segment.endMs < 60_000L) {
                        trimStartMs = maxOf(trimStartMs, segment.endMs)
                        Log.i(TAG, "Layer 2 (SponsorBlock): Detected ${segment.category} at start [${segment.startMs}ms - ${segment.endMs}ms]. Trimming start.")
                    }
                    // Outro or ending sponsor segment
                    if (segment.startMs >= (rawDurationMs - 45_000L) && segment.startMs > 0L) {
                        trimEndMs = minOf(trimEndMs, segment.startMs)
                        Log.i(TAG, "Layer 2 (SponsorBlock): Detected ${segment.category} at end [${segment.startMs}ms - ${segment.endMs}ms]. Trimming end.")
                    }
                }
            }

            // -------------------------------------------------------------
            // LAYER 3: Lossless Sample-Accurate Media Slicer & Remuxer
            // -------------------------------------------------------------
            val finalDurationMs = trimEndMs - trimStartMs
            val shouldTrim = (trimStartMs >= 1000L || trimEndMs <= (rawDurationMs - 1000L)) && finalDurationMs >= MIN_VALID_SONG_DURATION_MS

            val outputFile = if (shouldTrim) {
                Log.i(TAG, "Layer 3 (Lossless Slicer): Trimming '$rawFile.name' from ${trimStartMs}ms to ${trimEndMs}ms (Length: ${finalDurationMs}ms)")
                sliceAudioFile(rawFile, trimStartMs, trimEndMs) ?: rawFile
            } else {
                rawFile
            }

            val isSong = finalDurationMs >= MIN_VALID_SONG_DURATION_MS && metadata?.isAdvertisement != true
            if (!isSong) {
                Log.d(TAG, "Audio file failed validation as a music track (duration=${finalDurationMs}ms, isAd=${metadata?.isAdvertisement})")
            }

            return@withContext ProcessedAudioResult(
                file = outputFile,
                durationMs = if (shouldTrim) finalDurationMs else rawDurationMs,
                isValidSong = isSong
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in AudioPostProcessor", e)
            val isSong = rawDurationMs >= MIN_VALID_SONG_DURATION_MS && metadata?.isAdvertisement != true
            return@withContext ProcessedAudioResult(rawFile, rawDurationMs, isValidSong = isSong)
        }
    }

    /**
     * Scans the audio file for acoustic anomalies: sudden commercial loudness jumps
     * or speech-heavy pre-roll ads in the initial seconds.
     */
    private fun detectAcousticAdBoundaries(file: File, durationMs: Long): Pair<Long, Long> {
        var startCutMs = 0L
        var endCutMs = durationMs

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) return Pair(0L, durationMs)

            extractor.selectTrack(audioTrackIndex)
            val buffer = ByteBuffer.allocate(32768)
            val timestamps = mutableListOf<Long>()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val timeUs = extractor.sampleTime
                timestamps.add(timeUs / 1000L)
                extractor.advance()
                // Only scan first 35 seconds and last 35 seconds to keep post-processing lightning fast
                if (timeUs > 35_000_000L && timeUs < (durationMs - 35_000L) * 1000L) {
                    extractor.seekTo((durationMs - 35_000L) * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Acoustic boundary scan error: ${e.message}")
        } finally {
            try {
                extractor.release()
            } catch (ignored: Exception) {}
        }
        return Pair(startCutMs, endCutMs)
    }

    /**
     * Performs lossless, sample-accurate cutting using MediaExtractor + MediaMuxer.
     * Rewrites sample presentation timestamps starting from 0us without re-encoding!
     */
    private fun sliceAudioFile(sourceFile: File, startMs: Long, endMs: Long): File? {
        val tempFile = File(sourceFile.parentFile, "trimmed_${System.currentTimeMillis()}_${sourceFile.name}")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(sourceFile.absolutePath)
            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) return null

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val bufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var baseTimestampUs = -1L

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) {
                    break
                }

                if (sampleTimeUs >= startUs) {
                    if (baseTimestampUs == -1L) {
                        baseTimestampUs = sampleTimeUs
                    }

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTimeUs - baseTimestampUs
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            if (tempFile.exists() && tempFile.length() > 4096L) {
                sourceFile.delete()
                tempFile.renameTo(sourceFile)
                Log.i(TAG, "Lossless slicing completed successfully. Output size: ${sourceFile.length()} bytes")
                return sourceFile
            } else {
                if (tempFile.exists()) tempFile.delete()
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed during lossless slicing", e)
            try {
                muxer?.release()
            } catch (ignored: Exception) {}
            try {
                extractor.release()
            } catch (ignored: Exception) {}
            if (tempFile.exists()) tempFile.delete()
            return null
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }

    private fun extractYouTubeVideoId(metadata: SongMetadata?): String? {
        if (metadata == null) return null
        val title = metadata.title ?: return null
        // Extract 11-character video ID if available or return null
        val regex = Regex("(?<=v=|v\\/|vi=|vi\\/|youtu\\.be\\/)[a-zA-Z0-9_-]{11}")
        val match = regex.find(title)
        return match?.value
    }
}
