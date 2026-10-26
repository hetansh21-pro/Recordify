package com.audiovault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_history",
    indices = [
        Index(value = ["songId"]),
        Index(value = ["artist"]),
        Index(value = ["timestamp"]),
        Index(value = ["eventType"])
    ]
)
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArtPath: String? = null,
    val streamUrl: String? = null,
    val eventType: String, // PLAY_STARTED, PLAY_25, PLAY_50, PLAY_75, PLAY_COMPLETED, SONG_SKIPPED, SONG_LIKED, etc.
    val eventWeight: Float = 1.0f,
    val durationPlayedMs: Long = 0L,
    val songDurationMs: Long = 0L,
    val completionPercentage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String = "",
    val isOnline: Boolean = true,
    val genre: String? = null,
    val language: String? = null
)
