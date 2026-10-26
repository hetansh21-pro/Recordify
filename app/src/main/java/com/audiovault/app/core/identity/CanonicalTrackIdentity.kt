package com.audiovault.app.core.identity

import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.data.local.entity.RecordingEntity

/**
 * Authoritative, normalized music identity model.
 * Enables deduplication across providers and differentiates versions of recordings.
 */
data class CanonicalTrackIdentity(
    val canonicalTrackId: String,               // Deterministic hash of (normalizedTitle_stem + normalizedPrimaryArtist)
    val providerTrackId: String,                // Original provider ID (e.g. saavn_123, sp_456, yt_789)
    val providerName: String,                   // "saavn", "spotify", "itunes", "youtube", "local"
    val normalizedTitle: String,                // Clean display title without junk tags
    val normalizedPrimaryArtist: String,        // Clean primary artist
    val albumId: String? = null,
    val albumTitle: String? = null,
    val releaseYear: Int? = null,
    val durationMs: Long = 0L,
    val recordingVersion: RecordingVersionType = RecordingVersionType.ORIGINAL_STUDIO,
    val isrc: String? = null,
    val playableSource: String = "",
    val versionConfidence: Float = 0.90f,       // 0.0 to 1.0 confidence in version classification
    val officialStatus: Boolean = true,         // True if verified official studio/OST/single release
    val highResArtUrl: String? = null,
    val lyrics: String? = null,
    val language: String? = null,
    val bitrate: String = "320kbps",
    val youtubeVideoId: String? = null,
    val localRecordingId: Long? = null,
    val alternateSources: List<CanonicalTrackIdentity> = emptyList()
) {
    val isPlayable: Boolean get() = playableSource.isNotBlank() || !youtubeVideoId.isNullOrBlank() || localRecordingId != null

    fun toOnlineSongModel(): OnlineSongModel {
        return OnlineSongModel(
            id = providerTrackId,
            title = normalizedTitle,
            artist = normalizedPrimaryArtist,
            album = albumTitle,
            durationMs = durationMs,
            streamUrl = playableSource,
            highResArtUrl = highResArtUrl,
            lyrics = lyrics,
            year = releaseYear?.toString(),
            bitrate = bitrate,
            source = providerName,
            youtubeVideoId = youtubeVideoId,
            language = language
        )
    }

    fun toRecordingEntity(): RecordingEntity {
        val hash = canonicalTrackId.hashCode().toLong()
        val stableId = if (localRecordingId != null && localRecordingId > 0) {
            localRecordingId
        } else {
            if (hash > 0) -hash else hash
        }

        return RecordingEntity(
            id = stableId,
            fileName = "${providerTrackId}.m4a",
            filePath = playableSource,
            title = normalizedTitle,
            artist = normalizedPrimaryArtist,
            album = albumTitle,
            lyrics = lyrics,
            albumArtPath = highResArtUrl,
            createdAt = System.currentTimeMillis(),
            durationMs = durationMs,
            fileSizeBytes = 0L,
            bitrateKbps = if (playableSource.contains("_96.mp4")) 96 else 320,
            mimeType = "audio/mp4",
            sourcePackage = "com.audiovault.$providerName",
            isIdentifiedSong = true
        )
    }
}
