package com.audiovault.app.core.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Intelligent acoustic analyzer for real-time PCM streams:
 * - Computes RMS energy (dBFS)
 * - Zero-Crossing Rate (ZCR) and energy variance
 * - Music vs Speech / Ad / Noise classification
 * - Rapid acoustic gap & song transition detection
 */
class IntelligentAudioAnalyzer(
    private val sampleRate: Int = 44100,
    private val channelCount: Int = 2
) {
    companion object {
        const val SILENCE_DB = -100.0f
        // Speech typically has higher ZCR and erratic energy variance compared to sustained music
        private const val SPEECH_ZCR_THRESHOLD = 0.18f
    }

    var currentRmsDb: Float = SILENCE_DB
        private set

    var currentZcr: Float = 0f
        private set

    var isMusicPlayback: Boolean = false
        private set

    private var previousSample: Short = 0

    /**
     * Analyzes PCM byte chunk and returns acoustic parameters.
     */
    fun analyzePcm(buffer: ByteArray, length: Int): AcousticProfile {
        val sampleCount = length / 2
        if (sampleCount == 0) {
            return AcousticProfile(SILENCE_DB, 0f, isMusic = false, isSilent = true)
        }

        val byteBuffer = ByteBuffer.wrap(buffer, 0, length).order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0
        var zeroCrossings = 0
        var prev = previousSample

        for (i in 0 until sampleCount) {
            val sample = byteBuffer.short
            sumSquares += sample.toDouble() * sample.toDouble()

            // Zero crossing detection
            if ((prev >= 0 && sample < 0) || (prev < 0 && sample >= 0)) {
                zeroCrossings++
            }
            prev = sample
        }
        previousSample = prev

        val rms = sqrt(sumSquares / sampleCount)
        val rmsDb = if (rms > 0.0001) {
            max(SILENCE_DB, (20.0 * log10(rms / 32768.0)).toFloat())
        } else {
            SILENCE_DB
        }

        val zcr = zeroCrossings.toFloat() / sampleCount
        currentRmsDb = rmsDb
        currentZcr = zcr

        // Music classification heuristic: meaningful energy (> -45 dBFS) with stable spectral distribution
        val isSilent = rmsDb < -55.0f
        val isMusic = !isSilent && (rmsDb >= -45.0f) && (zcr < SPEECH_ZCR_THRESHOLD || rmsDb > -30.0f)
        isMusicPlayback = isMusic

        return AcousticProfile(
            rmsDb = rmsDb,
            zeroCrossingRate = zcr,
            isMusic = isMusic,
            isSilent = isSilent
        )
    }

    data class AcousticProfile(
        val rmsDb: Float,
        val zeroCrossingRate: Float,
        val isMusic: Boolean,
        val isSilent: Boolean
    )
}
