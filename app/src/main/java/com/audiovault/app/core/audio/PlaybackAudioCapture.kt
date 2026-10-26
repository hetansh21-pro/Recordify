package com.audiovault.app.core.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import com.audiovault.app.core.intelligence.SongMetadata
import com.audiovault.app.core.intelligence.YouTubeAdAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * Handles official Android AudioPlaybackCapture with 3-layer precision ad cutting,
 * real-time acoustic crossfading, and ultra-smooth High-Fidelity AAC encoding.
 */
class PlaybackAudioCapture(
    private val mediaProjection: MediaProjection,
    private val config: AudioCaptureConfig,
    private val fileProvider: (metadata: SongMetadata?) -> File,
    private val currentSongProvider: () -> SongMetadata?,
    private val onRecordingSaved: (file: File, durationMs: Long, metadata: SongMetadata?) -> Unit,
    private val onStateChanged: (isRecording: Boolean, durationMs: Long, audioLevelDb: Float, isMusic: Boolean) -> Unit,
    private val onError: (message: String) -> Unit
) {
    companion object {
        private const val TAG = "AudioVault/Capture"
    }

    private var activeConfig: AudioCaptureConfig = config
    private var audioRecord: AudioRecord? = null
    private var detector: AudioActivityDetector? = null
    private var analyzer: IntelligentAudioAnalyzer? = null
    private var currentEncoder: AacAudioEncoder? = null
    private var currentRecordingFile: File? = null
    private var currentSessionStartTimeMs = 0L
    private var currentTrackMetadata: SongMetadata? = null

    private var captureJob: Job? = null
    private val captureScope = CoroutineScope(Dispatchers.IO)
    private var isCapturing = false

    private var isAdPaused = false
    private var isFadingInNext = false

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    fun start() {
        if (isCapturing) return

        try {
            val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            // Prioritize standard 48kHz Android mixer sample rate, with 44.1kHz fallback
            val sampleRates = listOf(AudioCaptureConfig.DEFAULT_SAMPLE_RATE, AudioCaptureConfig.FALLBACK_SAMPLE_RATE)
            val channelConfigs = listOf(AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_MONO)

            var selectedRecord: AudioRecord? = null
            var actualSampleRate = AudioCaptureConfig.DEFAULT_SAMPLE_RATE
            var actualChannelConfig = config.channelConfig
            var actualChannelCount = config.channelCount

            for (rate in sampleRates) {
                for (chConfig in channelConfigs) {
                    val minBuf = AudioRecord.getMinBufferSize(
                        rate,
                        chConfig,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (minBuf > 0) {
                        val audioFormat = AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(chConfig)
                            .build()

                        try {
                            val bufferSizeBytes = maxOf(minBuf * 8, 65536)
                            val record = AudioRecord.Builder()
                                .setAudioFormat(audioFormat)
                                .setBufferSizeInBytes(bufferSizeBytes)
                                .setAudioPlaybackCaptureConfig(playbackConfig)
                                .build()

                            if (record.state == AudioRecord.STATE_INITIALIZED) {
                                selectedRecord = record
                                actualSampleRate = rate
                                actualChannelConfig = chConfig
                                actualChannelCount = if (chConfig == AudioFormat.CHANNEL_IN_STEREO) 2 else 1
                                break
                            } else {
                                record.release()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed initializing AudioRecord with rate $rate, ch $chConfig: ${e.message}")
                        }
                    }
                }
                if (selectedRecord != null) break
            }

            if (selectedRecord == null) {
                throw IOException("Unable to initialize AudioRecord with any supported configuration.")
            }

            this.activeConfig = config.copy(
                sampleRate = actualSampleRate,
                channelConfig = actualChannelConfig,
                channelCount = actualChannelCount
            )

            audioRecord = selectedRecord
            analyzer = IntelligentAudioAnalyzer(activeConfig.sampleRate, activeConfig.channelCount)
            detector = AudioActivityDetector(
                config = activeConfig,
                onRecordingStart = { preRollBytes ->
                    handleRecordingStart(preRollBytes)
                },
                onRecordingStop = { isEligibleDuration ->
                    handleRecordingStop(isEligibleDuration)
                }
            )

            selectedRecord.startRecording()
            isCapturing = true
            isAdPaused = false
            isFadingInNext = false
            Log.i(TAG, "AudioPlaybackCapture started with pristine studio precision ($actualSampleRate Hz, $actualChannelCount ch, ${activeConfig.bitrateKbps} kbps)")

            captureJob = captureScope.launch {
                runCaptureLoop(selectedRecord, activeConfig)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioPlaybackCapture", e)
            onError(e.message ?: "Failed to initialize audio capture")
            stop()
        }
    }

    private fun runCaptureLoop(record: AudioRecord, activeConfig: AudioCaptureConfig) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val bufferSize = 4096
        val buffer = ByteArray(bufferSize)

        try {
            while (captureScope.isActive && isCapturing) {
                val bytesRead = record.read(buffer, 0, bufferSize)
                if (bytesRead > 0) {
                    val detector = this.detector ?: break
                    val acousticProfile = analyzer?.analyzePcm(buffer, bytesRead)
                    val isMusic = acousticProfile?.isMusic ?: true

                    val liveSong = currentSongProvider()
                    val isAccessibilityAd = YouTubeAdAccessibilityService.isAdPlaying.value
                    val isAdActive = isAccessibilityAd || liveSong?.isAdvertisement == true || currentTrackMetadata?.isAdvertisement == true

                    // Layer 1 & 2: Real-time Precision Ad Pause with 50ms Micro-Crossfades
                    if (isAdActive) {
                        if (!isAdPaused) {
                            Log.i(TAG, "YouTube Ad active: applying 50ms micro-fade out and pausing encoder queue.")
                            applyFadeOut(buffer, 0, bytesRead)
                            currentEncoder?.encodePcm(buffer, 0, bytesRead)
                            isAdPaused = true
                            isFadingInNext = true
                        }
                        onStateChanged(false, 0L, detector.currentRmsDb, false)
                        continue
                    } else {
                        if (isAdPaused) {
                            Log.i(TAG, "Ad finished: applying 50ms micro-fade in and resuming clean recording.")
                            isAdPaused = false
                        }
                    }

                    val (isRecording, durationMs) = detector.processPcmChunk(buffer, bytesRead)

                    if (isRecording) {
                        if (currentTrackMetadata == null || !currentTrackMetadata!!.isIdentified) {
                            if (liveSong != null && liveSong.isIdentified) {
                                currentTrackMetadata = liveSong
                            }
                        }

                        if (isFadingInNext) {
                            applyFadeIn(buffer, 0, bytesRead)
                            isFadingInNext = false
                        }

                        // Fast, non-blocking asynchronous queue push
                        currentEncoder?.encodePcm(buffer, 0, bytesRead)
                    }

                    onStateChanged(isRecording, durationMs, detector.currentRmsDb, isMusic)
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "AudioRecord read error: $bytesRead")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during capture loop", e)
        } finally {
            Log.d(TAG, "Exiting capture loop")
        }
    }

    private fun applyFadeOut(pcmData: ByteArray, offset: Int, length: Int) {
        val sampleCount = length / 2
        for (i in 0 until sampleCount) {
            val sampleIndex = offset + i * 2
            val low = pcmData[sampleIndex].toInt() and 0xFF
            val high = pcmData[sampleIndex + 1].toInt() shl 8
            val shortVal = (low or high).toShort()
            val factor = 1.0f - (i.toFloat() / sampleCount.toFloat())
            val faded = (shortVal * factor).toInt().coerceIn(-32768, 32767)
            pcmData[sampleIndex] = (faded and 0xFF).toByte()
            pcmData[sampleIndex + 1] = ((faded shr 8) and 0xFF).toByte()
        }
    }

    private fun applyFadeIn(pcmData: ByteArray, offset: Int, length: Int) {
        val sampleCount = length / 2
        for (i in 0 until sampleCount) {
            val sampleIndex = offset + i * 2
            val low = pcmData[sampleIndex].toInt() and 0xFF
            val high = pcmData[sampleIndex + 1].toInt() shl 8
            val shortVal = (low or high).toShort()
            val factor = (i.toFloat() / sampleCount.toFloat())
            val faded = (shortVal * factor).toInt().coerceIn(-32768, 32767)
            pcmData[sampleIndex] = (faded and 0xFF).toByte()
            pcmData[sampleIndex + 1] = ((faded shr 8) and 0xFF).toByte()
        }
    }

    private fun handleRecordingStart(preRollBytes: ByteArray) {
        try {
            val song = currentSongProvider()
            if (song != null && song.isAdvertisement) {
                Log.i(TAG, "Ad active ('${song.displayTitle}'): skipping recording session.")
                currentEncoder = null
                currentRecordingFile = null
                currentTrackMetadata = song
                return
            }

            currentTrackMetadata = song
            val file = fileProvider(song)
            currentRecordingFile = file
            currentSessionStartTimeMs = System.currentTimeMillis()

            val encoder = AacAudioEncoder(file, activeConfig, song)
            currentEncoder = encoder

            if (preRollBytes.isNotEmpty()) {
                encoder.encodePcm(preRollBytes, 0, preRollBytes.size)
            }
            Log.d(TAG, "Started recording session for '${song?.displayTitle ?: file.name}' at ${activeConfig.sampleRate}Hz")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording session", e)
            currentEncoder = null
            currentRecordingFile = null
        }
    }

    private fun handleRecordingStop(isEligibleDuration: Boolean) {
        val encoder = currentEncoder
        val file = currentRecordingFile
        val song = currentTrackMetadata ?: currentSongProvider()

        currentEncoder = null
        currentRecordingFile = null
        currentTrackMetadata = null

        if (encoder != null && file != null) {
            captureScope.launch(Dispatchers.IO) {
                val isValid = encoder.finishAndRelease()
                val exactDurationMs = encoder.recordedDurationMs.takeIf { it > 0L }
                    ?: (System.currentTimeMillis() - currentSessionStartTimeMs).coerceAtLeast(1000L)

                if (isValid && isEligibleDuration) {
                    // Layer 3: Super-Accurate Audio Post-Processor & Quality Validator
                    val processed = AudioPostProcessor.processAndCleanRecording(file, exactDurationMs, song)
                    if (processed.isValidSong) {
                        Log.i(TAG, "Clean recording saved: ${processed.file.name}, exact sample duration: ${processed.durationMs}ms for '${song?.displayTitle}'")
                        onRecordingSaved(processed.file, processed.durationMs, song)
                    } else if (processed.file.exists()) {
                        processed.file.delete()
                    }
                } else {
                    Log.d(TAG, "Discarded recording (eligible=$isEligibleDuration, duration=${exactDurationMs}ms, valid=$isValid)")
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * Seamlessly splits recording when a new song is detected.
     */
    @Synchronized
    fun onSongTransition(newSong: SongMetadata) {
        if (!isCapturing) return

        val prevTitle = currentTrackMetadata?.displayTitle ?: ""
        val newTitle = newSong.displayTitle
        if (prevTitle.isNotBlank() && prevTitle.equals(newTitle, ignoreCase = true)) {
            return
        }

        if (currentEncoder != null && currentRecordingFile != null) {
            val oldEncoder = currentEncoder
            val oldFile = currentRecordingFile
            val oldSong = currentTrackMetadata

            currentEncoder = null
            currentRecordingFile = null
            currentTrackMetadata = newSong

            if (newSong.isAdvertisement) {
                Log.i(TAG, "Track changed to Advertisement ('$newTitle'). Finalizing previous track.")
            } else {
                Log.i(TAG, "Track changed from '$prevTitle' to '$newTitle'. Splitting recording.")
            }

            // Finalize previous song in background and process cleanly
            if (oldEncoder != null && oldFile != null) {
                captureScope.launch(Dispatchers.IO) {
                    val isValid = oldEncoder.finishAndRelease()
                    val exactDurationMs = oldEncoder.recordedDurationMs.takeIf { it > 0L }
                        ?: (System.currentTimeMillis() - currentSessionStartTimeMs).coerceAtLeast(1000L)

                    if (isValid) {
                        val processed = AudioPostProcessor.processAndCleanRecording(oldFile, exactDurationMs, oldSong)
                        if (processed.isValidSong) {
                            onRecordingSaved(processed.file, processed.durationMs, oldSong)
                        } else if (oldFile.exists()) {
                            oldFile.delete()
                        }
                    } else if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }
            }

            // If the new track is a real song (not an ad), start recording it immediately with full pre-roll history
            if (!newSong.isAdvertisement) {
                val preRollHistory = detector?.getRecentHistoryBytes(activeConfig.preRollMs.toLong()) ?: ByteArray(0)
                handleRecordingStart(preRollHistory)
            }
        } else {
            currentTrackMetadata = newSong
            if (!newSong.isAdvertisement) {
                val preRollHistory = detector?.getRecentHistoryBytes(activeConfig.preRollMs.toLong()) ?: ByteArray(0)
                handleRecordingStart(preRollHistory)
            }
        }
    }

    @Synchronized
    fun stop() {
        if (!isCapturing && audioRecord == null) return
        isCapturing = false

        captureJob?.cancel()
        captureJob = null

        val encoder = currentEncoder
        val file = currentRecordingFile
        val song = currentTrackMetadata ?: currentSongProvider()

        currentEncoder = null
        currentRecordingFile = null
        currentTrackMetadata = null
        currentSessionStartTimeMs = 0L

        try {
            detector?.forceStop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping detector: ${e.message}")
        }
        detector = null

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        }
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord: ${e.message}")
        }
        audioRecord = null

        // If recording was in progress and user stopped in middle:
        // If duration is 50 seconds or up (>= 50_000ms), process and save the audio!
        if (encoder != null && file != null) {
            captureScope.launch(Dispatchers.IO) {
                try {
                    val isValid = encoder.finishAndRelease()
                    val exactDurationMs = encoder.recordedDurationMs.takeIf { it > 0L } ?: 0L
                    val isAtLeast50s = exactDurationMs >= 50_000L
                    val isAd = song?.isAdvertisement == true

                    if (isValid && isAtLeast50s && !isAd) {
                        Log.i(TAG, "Stopped ongoing recording in middle (exact duration: ${exactDurationMs}ms >= 50s). Processing & saving to library!")
                        val processed = AudioPostProcessor.processAndCleanRecording(file, exactDurationMs, song)
                        if (processed.isValidSong) {
                            onRecordingSaved(processed.file, processed.durationMs, song)
                        } else if (processed.file.exists()) {
                            processed.file.delete()
                        }
                    } else {
                        Log.d(TAG, "Stopped recording in middle with duration < 50s (${exactDurationMs}ms). Discarding short fragment.")
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving mid-recording stop", e)
                }
            }
        }

        Log.i(TAG, "PlaybackAudioCapture stopped and resources released.")
    }
}
