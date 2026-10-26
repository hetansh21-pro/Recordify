package com.audiovault.app.core.recommendation

enum class ListeningEventType {
    PLAY_STARTED,
    PLAY_25_PERCENT,
    PLAY_50_PERCENT,
    PLAY_75_PERCENT,
    PLAY_COMPLETED,
    SONG_SKIPPED,
    SONG_SHORT_SKIP,
    SONG_LIKED,
    SONG_UNLIKED,
    SONG_REPLAYED,
    ADDED_TO_PLAYLIST,
    REMOVED_FROM_PLAYLIST,
    SEARCHED,
    ARTIST_OPENED,
    ALBUM_OPENED,
    MANUAL_SONG_SELECTION,
    RECOMMENDATION_ACCEPTED,
    RECOMMENDATION_REJECTED,
    STREAM_FAILED
}

data class ListeningEvent(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArtPath: String? = null,
    val streamUrl: String? = null,
    val type: ListeningEventType,
    val durationPlayedMs: Long = 0L,
    val songDurationMs: Long = 0L,
    val completionPercentage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String = "",
    val isOnline: Boolean = true,
    val genre: String? = null,
    val language: String? = null
)
