package com.audiovault.app

import com.audiovault.app.core.audio.AudioActivityDetector
import com.audiovault.app.core.audio.AudioCaptureConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioActivityDetectorTest {

    private val config = AudioCaptureConfig(
        sampleRate = 44100,
        channelCount = 2,
        silenceTimeoutSeconds = 1, // short timeout for testing
        minRecordingDurationSeconds = 1,
        startThresholdDb = -40f,
        stopThresholdDb = -50f
    )

    private fun generateSinePcmChunk(
        amplitude: Short,
        durationMs: Int = 100,
        sampleRate: Int = 44100,
        channels: Int = 2
    ): ByteArray {
        val totalSamples = (sampleRate * (durationMs / 1000.0) * channels).toInt()
        val buffer = ByteArray(totalSamples * 2)
        val byteBuf = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            byteBuf.putShort(amplitude)
        }
        return buffer
    }

    @Test
    fun `calculateRmsDb on full scale sine returns near zero dBFS`() {
        val detector = AudioActivityDetector(config, {}, {})
        val maxAmplitudeChunk = generateSinePcmChunk(amplitude = 30000, durationMs = 100)

        val rmsDb = detector.calculateRmsDb(maxAmplitudeChunk, maxAmplitudeChunk.size)
        assertThat(rmsDb).isGreaterThan(-3.0f)
        assertThat(rmsDb).isAtMost(0.0f)
    }

    @Test
    fun `calculateRmsDb on silence returns low dBFS`() {
        val detector = AudioActivityDetector(config, {}, {})
        val silentChunk = generateSinePcmChunk(amplitude = 0, durationMs = 100)

        val rmsDb = detector.calculateRmsDb(silentChunk, silentChunk.size)
        assertThat(rmsDb).isLessThan(-80f)
    }

    @Test
    fun `active audio triggers onRecordingStart callback`() {
        var startTriggered = false
        val detector = AudioActivityDetector(
            config = config,
            onRecordingStart = { startTriggered = true },
            onRecordingStop = {}
        )

        // Send loud audio chunk (-10 dBFS > -40 dBFS)
        val loudChunk = generateSinePcmChunk(amplitude = 15000, durationMs = 200)
        val (isRecording, _) = detector.processPcmChunk(loudChunk, loudChunk.size)

        assertThat(startTriggered).isTrue()
        assertThat(isRecording).isTrue()
    }

    @Test
    fun `silence after active audio triggers onRecordingStop callback`() {
        var stopTriggered = false
        var eligible = false

        val detector = AudioActivityDetector(
            config = config,
            onRecordingStart = {},
            onRecordingStop = { isEligible ->
                stopTriggered = true
                eligible = isEligible
            }
        )

        // 1. Send loud audio for 2000ms (exceeds min duration 1s)
        val loudChunk = generateSinePcmChunk(amplitude = 15000, durationMs = 500)
        for (i in 0 until 4) {
            detector.processPcmChunk(loudChunk, loudChunk.size)
        }

        // 2. Send silence for 1500ms (exceeds silence timeout 1s)
        val silentChunk = generateSinePcmChunk(amplitude = 0, durationMs = 500)
        for (i in 0 until 3) {
            detector.processPcmChunk(silentChunk, silentChunk.size)
        }

        assertThat(stopTriggered).isTrue()
        assertThat(eligible).isTrue()
    }
}
