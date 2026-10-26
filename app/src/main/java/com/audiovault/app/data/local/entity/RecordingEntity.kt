package com.audiovault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a captured audio recording with rich song metadata.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val lyrics: String? = null,
    val albumArtPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val fileSizeBytes: Long,
    val bitrateKbps: Int = 256,
    val mimeType: String = "audio/mp4",
    val sourcePackage: String? = null,
    val isIdentifiedSong: Boolean = false,
    val isProcessing: Boolean = false
) {
    val displayArtistTitle: String
        get() = if (!artist.isNullOrBlank()) "$artist - $title" else title
}
