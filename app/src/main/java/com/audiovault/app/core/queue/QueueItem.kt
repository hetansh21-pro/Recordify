package com.audiovault.app.core.queue

import com.audiovault.app.core.identity.CanonicalTrackIdentity
import com.audiovault.app.core.identity.TrackVersionResolver
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.vibeflow.SongMoodProfile
import com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer
import com.audiovault.app.data.local.entity.RecordingEntity

enum class PlaybackRepeatMode {
    OFF,
    ALL,
    ONE
}

enum class QueueMode {
    ALBUM,
    PLAYLIST,
    USER_QUEUE,
    VIBE_RADIO,
    SHUFFLE,
    SINGLE_TRACK
}

/**
 * Authoritative, immutable queue item with stable unique ID.
 * Unifies both online streams and local recordings.
 */
data class QueueItem(
    val id: String,                         // Stable globally unique ID
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0L,
    val streamUrl: String = "",
    val albumArtPath: String? = null,
    val lyrics: String? = null,
    val isOnline: Boolean = true,
    val source: String = "online",
    val youtubeVideoId: String? = null,
    val localRecordingId: Long? = null,
    val recommendationReason: String? = null,
    val moodProfile: SongMoodProfile? = null,
    val language: String? = null,
    val canonicalTrackId: String = TrackVersionResolver.generateCanonicalId(title, artist),
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toRecordingEntity(): RecordingEntity {
        // Derive stable positive/negative ID from stable string ID
        val stableId = if (localRecordingId != null && localRecordingId > 0) {
            localRecordingId
        } else {
            // Consistent stable hash instead of random millisecond!
            val hash = id.hashCode().toLong()
            if (hash > 0) -hash else hash
        }

        return RecordingEntity(
            id = stableId,
            fileName = if (isOnline) "${id}.m4a" else streamUrl.substringAfterLast("/"),
            filePath = streamUrl,
            title = title,
            artist = artist,
            album = album,
            lyrics = lyrics,
            albumArtPath = albumArtPath,
            createdAt = addedAt,
            durationMs = durationMs,
            fileSizeBytes = 0L,
            bitrateKbps = if (streamUrl.contains("_96.mp4")) 96 else 320,
            mimeType = "audio/mp4",
            sourcePackage = if (isOnline) "com.audiovault.online" else "com.audiovault.local",
            isIdentifiedSong = true,
            isProcessing = false
        )
    }

    fun toOnlineSongModel(): OnlineSongModel {
        return OnlineSongModel(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            streamUrl = streamUrl,
            highResArtUrl = albumArtPath,
            lyrics = lyrics,
            source = source,
            youtubeVideoId = youtubeVideoId
        )
    }

    companion object {
        fun fromOnlineSong(song: OnlineSongModel, reason: String? = null): QueueItem {
            val stableId = if (song.id.isNotBlank()) {
                if (song.id.startsWith("online_") || song.id.startsWith("sp_") || song.id.startsWith("yt_") || song.id.startsWith("saavn_")) song.id else "online_${song.id}"
            } else {
                "online_${(song.title + song.artist).hashCode()}"
            }

            return QueueItem(
                id = stableId,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                streamUrl = song.streamUrl,
                albumArtPath = song.highResArtUrl,
                lyrics = song.lyrics,
                isOnline = true,
                source = song.source,
                youtubeVideoId = song.youtubeVideoId,
                recommendationReason = reason,
                language = song.language,
                moodProfile = VibeFlowMoodAnalyzer.analyze(song)
            )
        }

        fun fromRecording(recording: RecordingEntity, reason: String? = null): QueueItem {
            val isOnline = recording.filePath.startsWith("http://") || recording.filePath.startsWith("https://")
            val stableId = if (isOnline) "online_${recording.id}" else "local_${recording.id}"

            return QueueItem(
                id = stableId,
                title = recording.title,
                artist = recording.artist ?: "Unknown Artist",
                album = recording.album,
                durationMs = recording.durationMs,
                streamUrl = recording.filePath,
                albumArtPath = recording.albumArtPath,
                lyrics = recording.lyrics,
                isOnline = isOnline,
                source = if (isOnline) "online" else "local",
                localRecordingId = if (!isOnline) recording.id else null,
                recommendationReason = reason,
                language = VibeFlowMoodAnalyzer.inferLanguage("${recording.title} ${recording.artist ?: ""}"),
                moodProfile = VibeFlowMoodAnalyzer.analyze(recording)
            )
        }

        fun fromCanonicalTrack(canonical: CanonicalTrackIdentity, reason: String? = null): QueueItem {
            return QueueItem(
                id = canonical.providerTrackId,
                title = canonical.normalizedTitle,
                artist = canonical.normalizedPrimaryArtist,
                album = canonical.albumTitle,
                durationMs = canonical.durationMs,
                streamUrl = canonical.playableSource,
                albumArtPath = canonical.highResArtUrl,
                lyrics = canonical.lyrics,
                isOnline = canonical.providerName != "local",
                source = canonical.providerName,
                youtubeVideoId = canonical.youtubeVideoId,
                localRecordingId = canonical.localRecordingId,
                recommendationReason = reason,
                language = canonical.language,
                moodProfile = VibeFlowMoodAnalyzer.analyzeFromMetadata(
                    trackId = canonical.providerTrackId,
                    title = canonical.normalizedTitle,
                    artist = canonical.normalizedPrimaryArtist,
                    album = canonical.albumTitle,
                    lyrics = canonical.lyrics,
                    year = canonical.releaseYear?.toString(),
                    durationMs = canonical.durationMs
                ),
                canonicalTrackId = canonical.canonicalTrackId
            )
        }
    }
}

data class PlaybackQueueState(
    val currentTrack: QueueItem? = null,
    val upcomingQueue: List<QueueItem> = emptyList(),
    val playbackHistory: List<QueueItem> = emptyList(),
    val recentCanonicalIds: List<String> = emptyList(), // Bounded last 50 unique canonical track IDs
    val queueMode: QueueMode = QueueMode.VIBE_RADIO,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val isAutoQueueEnabled: Boolean = true,
    val isPersonalizationEnabled: Boolean = true,
    val queueGenerationId: Long = 0L,
    val isResolvingStream: Boolean = false,
    val activeVibeDescription: String = "VibeFlow Dynamic"
)
