package com.audiovault.app

import com.audiovault.app.core.audio.IntelligentAudioAnalyzer
import com.audiovault.app.core.intelligence.SongMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class IntelligentAudioAnalyzerTest {

    private val analyzer = IntelligentAudioAnalyzer(sampleRate = 44100, channelCount = 2)

    private fun generateSineWave(amplitude: Short, durationMs: Int = 100): ByteArray {
        val totalSamples = (44100 * (durationMs / 1000.0) * 2).toInt()
        val buffer = ByteArray(totalSamples * 2)
        val byteBuf = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            val freq = 440.0 // 440 Hz A tone
            val sample = (amplitude * kotlin.math.sin(2.0 * Math.PI * freq * (i / 44100.0))).toInt().toShort()
            byteBuf.putShort(sample)
        }
        return buffer
    }

    @Test
    fun `analyzePcm detects harmonic audio with accurate RMS and zero crossings`() {
        val audioChunk = generateSineWave(amplitude = 20000, durationMs = 200)
        val profile = analyzer.analyzePcm(audioChunk, audioChunk.size)

        assertThat(profile.isSilent).isFalse()
        assertThat(profile.rmsDb).isGreaterThan(-20.0f)
        assertThat(profile.zeroCrossingRate).isGreaterThan(0.005f)
        assertThat(profile.zeroCrossingRate).isLessThan(0.15f)
    }

    @Test
    fun `SongMetadata formats clean display title with artist`() {
        val song = SongMetadata(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours"
        )

        assertThat(song.displayTitle).isEqualTo("The Weeknd - Blinding Lights")
        assertThat(song.isIdentified).isTrue()
    }
}
