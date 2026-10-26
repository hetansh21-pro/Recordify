package com.audiovault.app.core.audio

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Detects active audio playback vs silence in continuous PCM streams.
 * Employs RMS energy calculation, hysteresis thresholds, pre-roll ring buffer,
 * and configurable silence timeout counters.
 */
class AudioActivityDetector(
    private val config: AudioCaptureConfig,
    private val onRecordingStart: (preRollBytes: ByteArray) -> Unit,
    private val onRecordingStop: (isEligibleDuration: Boolean) -> Unit
) {
    companion object {
        private const val TAG = "AudioVault/Detector"
        private const val SILENCE_DB = -100.0f
    }

    enum class DetectionState {
        IDLE_WAITING,
        RECORDING_ACTIVE,
        SILENCE_COUNTDOWN
    }

    private var currentState = DetectionState.IDLE_WAITING
    private var silenceElapsedMs = 0L
    private var currentSessionDurationMs = 0L
    private val preRollBuffer = CircularPcmBuffer(
        capacityBytes = (config.bytesPerSecond * (config.preRollMs / 1000.0)).toInt().coerceAtLeast(4096)
    )

    var currentRmsDb: Float = SILENCE_DB
        private set

    var normalizedEnergy: Float = 0f
        private set

    /**
     * Processes a chunk of 16-bit PCM bytes.
     * @param pcmData raw byte buffer of 16-bit PCM audio.
     * @param bytesRead number of valid bytes in pcmData.
     * @return pair of (isCurrentlyInRecordingSession, currentSessionDurationMs)
     */
    @Synchronized
    fun processPcmChunk(pcmData: ByteArray, bytesRead: Int): Pair<Boolean, Long> {
        if (bytesRead <= 0) return Pair(currentState != DetectionState.IDLE_WAITING, currentSessionDurationMs)

        // Always feed the rolling pre-roll circular buffer so audio history is never lost
        preRollBuffer.write(pcmData, 0, bytesRead)

        // Calculate chunk duration in milliseconds
        val chunkDurationMs = (bytesRead.toDouble() / config.bytesPerSecond * 1000.0).toLong()

        // Calculate RMS in dBFS
        val rmsDb = calculateRmsDb(pcmData, bytesRead)
        currentRmsDb = rmsDb
        normalizedEnergy = normalizeDb(rmsDb)

        val isAudioAboveStart = rmsDb >= config.startThresholdDb
        val isAudioBelowStop = rmsDb < config.stopThresholdDb

        when (currentState) {
            DetectionState.IDLE_WAITING -> {
                if (isAudioAboveStart) {
                    Log.d(TAG, "Audio activity detected ($rmsDb dBFS >= ${config.startThresholdDb} dBFS). Starting recording session.")
                    currentState = DetectionState.RECORDING_ACTIVE
                    silenceElapsedMs = 0L
                    currentSessionDurationMs = 0L

                    // Grab pre-roll data and notify start
                    val preRollBytes = preRollBuffer.snapshot()
                    onRecordingStart(preRollBytes)
                }
            }

            DetectionState.RECORDING_ACTIVE -> {
                currentSessionDurationMs += chunkDurationMs

                if (isAudioBelowStop) {
                    Log.d(TAG, "Audio fell below stop threshold ($rmsDb dBFS < ${config.stopThresholdDb} dBFS). Entering silence countdown.")
                    currentState = DetectionState.SILENCE_COUNTDOWN
                    silenceElapsedMs = chunkDurationMs
                }
            }

            DetectionState.SILENCE_COUNTDOWN -> {
                currentSessionDurationMs += chunkDurationMs
                silenceElapsedMs += chunkDurationMs

                if (isAudioAboveStart) {
                    val minDurationMs = config.minRecordingDurationSeconds * 1000L
                    val isInterTrackBoundary = currentSessionDurationMs >= 30_000L && silenceElapsedMs >= 800L

                    if (isInterTrackBoundary) {
                        Log.i(TAG, "Inter-track silence gap detected (${silenceElapsedMs}ms after ${currentSessionDurationMs}ms). Finalizing previous song and starting new track with pre-roll.")
                        val isEligible = (currentSessionDurationMs - silenceElapsedMs) >= minDurationMs
                        onRecordingStop(isEligible)

                        currentState = DetectionState.RECORDING_ACTIVE
                        silenceElapsedMs = 0L
                        currentSessionDurationMs = 0L

                        val preRollBytes = preRollBuffer.snapshot()
                        onRecordingStart(preRollBytes)
                    } else {
                        Log.d(TAG, "Audio resumed ($rmsDb dBFS). Returning to RECORDING_ACTIVE.")
                        currentState = DetectionState.RECORDING_ACTIVE
                        silenceElapsedMs = 0L
                    }
                } else {
                    val silenceTimeoutMs = config.silenceTimeoutSeconds * 1000L
                    if (silenceElapsedMs >= silenceTimeoutMs) {
                        Log.d(TAG, "Silence timeout reached ($silenceElapsedMs ms >= $silenceTimeoutMs ms). Finalizing recording.")
                        val minDurationMs = config.minRecordingDurationSeconds * 1000L
                        val isEligible = (currentSessionDurationMs - silenceElapsedMs) >= minDurationMs

                        currentState = DetectionState.IDLE_WAITING
                        silenceElapsedMs = 0L
                        onRecordingStop(isEligible)
                        currentSessionDurationMs = 0L
                    }
                }
            }
        }

        return Pair(currentState != DetectionState.IDLE_WAITING, currentSessionDurationMs)
    }

    /**
     * Gets a snapshot of the most recent PCM bytes from the continuous rolling buffer.
     */
    @Synchronized
    fun getRecentHistoryBytes(durationMs: Long = config.preRollMs.toLong()): ByteArray {
        val bytesNeeded = (config.bytesPerSecond * (durationMs / 1000.0)).toInt()
        return preRollBuffer.snapshot(bytesNeeded)
    }

    /**
     * Force stop detector (e.g. when service is stopping).
     */
    @Synchronized
    fun forceStop() {
        if (currentState != DetectionState.IDLE_WAITING) {
            val minDurationMs = config.minRecordingDurationSeconds * 1000L
            val isEligible = currentSessionDurationMs >= minDurationMs
            currentState = DetectionState.IDLE_WAITING
            silenceElapsedMs = 0L
            currentSessionDurationMs = 0L
            onRecordingStop(isEligible)
        }
    }

    /**
     * Calculates RMS (Root Mean Square) energy of 16-bit PCM in dBFS.
     */
    fun calculateRmsDb(buffer: ByteArray, length: Int): Float {
        val sampleCount = length / 2
        if (sampleCount == 0) return SILENCE_DB

        val byteBuffer = ByteBuffer.wrap(buffer, 0, length).order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0

        for (i in 0 until sampleCount) {
            val sample = byteBuffer.short.toDouble()
            sumSquares += sample * sample
        }

        val rms = sqrt(sumSquares / sampleCount)
        if (rms <= 0.0001) return SILENCE_DB

        // Full scale 16-bit max amplitude is 32768
        val db = 20.0 * log10(rms / 32768.0)
        return max(SILENCE_DB, db.toFloat())
    }

    /**
     * Normalizes dBFS (-60dB to 0dB) into 0.0f..1.0f range for UI bars.
     */
    fun normalizeDb(db: Float): Float {
        if (db <= -60f) return 0f
        if (db >= 0f) return 1f
        return ((db + 60f) / 60f).coerceIn(0f, 1f)
    }

    /**
     * Circular buffer for storing continuous rolling pre-roll PCM audio.
     */
    private class CircularPcmBuffer(private val capacityBytes: Int) {
        private val buffer = ByteArray(capacityBytes)
        private var writePos = 0
        private var size = 0

        @Synchronized
        fun write(data: ByteArray, offset: Int, length: Int) {
            for (i in 0 until length) {
                buffer[writePos] = data[offset + i]
                writePos = (writePos + 1) % capacityBytes
                if (size < capacityBytes) size++
            }
        }

        @Synchronized
        fun drain(): ByteArray {
            if (size == 0) return ByteArray(0)
            val result = ByteArray(size)
            val startPos = if (size < capacityBytes) 0 else writePos

            for (i in 0 until size) {
                result[i] = buffer[(startPos + i) % capacityBytes]
            }
            clear()
            return result
        }

        @Synchronized
        fun snapshot(bytesNeeded: Int = size): ByteArray {
            val count = bytesNeeded.coerceIn(0, size)
            if (count == 0) return ByteArray(0)
            val result = ByteArray(count)
            val startPos = if (size < capacityBytes) {
                (size - count).coerceAtLeast(0)
            } else {
                (writePos - count + capacityBytes) % capacityBytes
            }

            for (i in 0 until count) {
                result[i] = buffer[(startPos + i) % capacityBytes]
            }
            return result
        }

        @Synchronized
        fun clear() {
            writePos = 0
            size = 0
        }
    }
}
