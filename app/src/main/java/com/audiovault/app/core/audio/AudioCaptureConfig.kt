package com.audiovault.app.core.audio

import android.media.AudioFormat

/**
 * Configuration parameters for audio capture and AAC encoding.
 */
data class AudioCaptureConfig(
    val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val channelCount: Int = 2,
    val bitrateKbps: Int = DEFAULT_BITRATE_KBPS,
    val silenceTimeoutSeconds: Int = DEFAULT_SILENCE_TIMEOUT_SECONDS,
    val minRecordingDurationSeconds: Int = DEFAULT_MIN_DURATION_SECONDS,
    val startThresholdDb: Float = DEFAULT_START_THRESHOLD_DB,
    val stopThresholdDb: Float = DEFAULT_STOP_THRESHOLD_DB,
    val preRollMs: Int = DEFAULT_PRE_ROLL_MS,
    val isSmartSongSplitEnabled: Boolean = true,
    val isNoiseAndAdFilterEnabled: Boolean = true
) {
    val bytesPerSample: Int get() = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 2 else 1
    val frameSizeBytes: Int get() = channelCount * bytesPerSample
    val bytesPerSecond: Int get() = sampleRate * frameSizeBytes

    companion object {
        const val DEFAULT_SAMPLE_RATE = 48000 // Android native media mixer standard
        const val FALLBACK_SAMPLE_RATE = 44100
        const val DEFAULT_BITRATE_KBPS = 256 // Studio High-Fidelity AAC default
        const val DEFAULT_SILENCE_TIMEOUT_SECONDS = 7
        const val DEFAULT_MIN_DURATION_SECONDS = 50 // Minimum 50s required to save recording
        const val DEFAULT_START_THRESHOLD_DB = -42.0f
        const val DEFAULT_STOP_THRESHOLD_DB = -52.0f
        const val DEFAULT_PRE_ROLL_MS = 6000 // 6 seconds rolling pre-roll history to prevent any song intro cuts
        const val INTER_TRACK_SILENCE_MS = 800L
    }
}
