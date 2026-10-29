package com.audiovault.app.core.player

/**
 * Controlled buffering parameters for Progressive & Range-Based Audio Streaming.
 * Enforces strict sliding window buffering so Recordify never downloads the entire song
 * upfront when the user presses Play.
 */
data class StreamingBufferConfig(
    val initialBufferSeconds: Int = 2,
    val targetBufferSeconds: Int = 15,
    val maximumBufferSeconds: Int = 30,
    val backBufferSeconds: Int = 10,
    val bufferForPlaybackAfterRebufferSeconds: Int = 4,
    val prefetchNextSong: Boolean = false
) {
    val initialBufferMs: Int get() = initialBufferSeconds * 1000
    val targetBufferMs: Int get() = targetBufferSeconds * 1000
    val maximumBufferMs: Int get() = maximumBufferSeconds * 1000
    val backBufferMs: Int get() = backBufferSeconds * 1000
    val bufferForPlaybackAfterRebufferMs: Int get() = bufferForPlaybackAfterRebufferSeconds * 1000

    companion object {
        /**
         * Normal Streaming Mode (Spotify-Style Progressive Sliding Window):
         * - 2s initial startup buffer (instant start)
         * - 25s target ahead buffer (prevents pausing on screen lock or network latency)
         * - 50s maximum sliding window (robust headroom without over-downloading)
         * - 15s retention behind playback for instant rewind
         */
        val NORMAL = StreamingBufferConfig(
            initialBufferSeconds = 2,
            targetBufferSeconds = 25,
            maximumBufferSeconds = 50,
            backBufferSeconds = 15,
            bufferForPlaybackAfterRebufferSeconds = 3,
            prefetchNextSong = false
        )

        /**
         * Ultra Data Saver Mode:
         * - 2s initial startup buffer
         * - 12s target ahead buffer
         * - 20s maximum sliding window
         * - 5s back buffer discard
         * - Strict 0 next-song audio prefetch (immediate socket tear-down on skip)
         */
        val DATA_SAVER = StreamingBufferConfig(
            initialBufferSeconds = 2,
            targetBufferSeconds = 12,
            maximumBufferSeconds = 20,
            backBufferSeconds = 5,
            bufferForPlaybackAfterRebufferSeconds = 2,
            prefetchNextSong = false
        )
    }
}

/**
 * Real-time network streaming metrics and data savings tracking.
 */
data class StreamingMetrics(
    val sessionBytesStreamed: Long = 0L,
    val currentSongBytesStreamed: Long = 0L,
    val currentSongFullSizeBytes: Long = 0L,
    val isDataSaverActive: Boolean = false,
    val currentBitrateKbps: Int = 320,
    val activeRangeRequestsCount: Long = 0L
) {
    val estimatedDataSavedBytes: Long
        get() = if (currentSongFullSizeBytes > currentSongBytesStreamed) {
            currentSongFullSizeBytes - currentSongBytesStreamed
        } else 0L

    val dataSavedPercentage: Float
        get() = if (currentSongFullSizeBytes > 0L) {
            ((estimatedDataSavedBytes.toFloat() / currentSongFullSizeBytes.toFloat()) * 100f).coerceIn(0f, 99f)
        } else 0f
}
