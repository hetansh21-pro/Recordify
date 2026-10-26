package com.audiovault.app.core.identity

import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer
import com.audiovault.app.data.local.entity.RecordingEntity
import java.util.Locale

/**
 * Production-quality version classification and metadata normalization engine.
 * Classifies tracks into studio originals, soundtracks, remixes, slowed/sped edits, acoustic, live, etc.
 */
object TrackVersionResolver {

    private val JUNK_SUFFIX_PATTERNS = listOf(
        "(?i)\\s*\\(\\s*official\\s+(audio|video|music\\s+video|hd\\s+video|lyric\\s+video|lyrics|visualizer)\\s*\\)",
        "(?i)\\s*\\[\\s*official\\s+(audio|video|music\\s+video|hd\\s+video|lyric\\s+video|lyrics|visualizer)\\s*\\]",
        "(?i)\\s*\\(\\s*(full\\s+song|full\\s+video\\s+song|audio\\s+song|video\\s+song|lyrics\\s+video|lyric\\s+video|lyrics|hd|4k|audio|4k\\s+audio)\\s*\\)",
        "(?i)\\s*\\[\\s*(full\\s+song|full\\s+video\\s+song|audio\\s+song|video\\s+song|lyrics\\s+video|lyric\\s+video|lyrics|hd|4k|audio|4k\\s+audio)\\s*\\]",
        "(?i)\\s*[-|:]\\s*(official\\s+audio|official\\s+video|official\\s+music\\s+video|full\\s+video\\s+song|full\\s+song|audio\\s+song|video\\s+song|lyrics\\s+video|lyric\\s+video|lyrics|audio|4k)\\b",
        "(?i)\\s*-\\s*\\d{4}\\s*$", // Trailing year like "- 2022"
        "(?i)\\s*\\(from\\s+[\"'].*?[\"']\\s*\\)", // "(From 'Brahmastra')"
        "(?i)\\s*\\[from\\s+[\"'].*?[\"']\\s*\\]"
    ).map { it.toRegex() }

    private val SLOWED_REVERB_PATTERNS = listOf(
        "(?i)\\b(slowed(\\s*\\+\\s*|\\s+and\\s+)reverb|slowed|reverb\\s+edit|chilled\\s+edit|8d\\s+audio)\\b".toRegex()
    )

    private val SPED_UP_PATTERNS = listOf(
        "(?i)\\b(sped\\s*up|speed\\s*up|nightcore|pitch\\s+shifted|hyperpop\\s+edit)\\b".toRegex()
    )

    private val REMIX_PATTERNS = listOf(
        "(?i)[\\(\\[\\-–—|]\\s*([^)\\]]*?\\b(remix|club\\s+mix|dance\\s+mix|dj\\s+mix|mashup|bootleg|extended\\s+mix|trap\\s+mix|lofi\\s+remix|lo-fi\\s+mix)\\b.*?)[]\\)]?".toRegex(),
        "(?i)\\s+(remix|dance\\s+mix|club\\s+mix|trap\\s+mix)\\s*$".toRegex()
    )

    private val ACOUSTIC_PATTERNS = listOf(
        "(?i)[\\(\\[\\-–—|]?\\s*\\b(acoustic(\\s+version)?|unplugged|stripped|piano\\s+version)\\b[]\\)]?".toRegex()
    )

    private val LIVE_PATTERNS = listOf(
        "(?i)[\\(\\[\\-–—|]?\\s*\\b(live(\\s+at|\\s+in|\\s+from|\\s+concert|\\s+version|\\s+performance)?)\\b[]\\)]?".toRegex()
    )

    private val INSTRUMENTAL_PATTERNS = listOf(
        "(?i)[\\(\\[\\-–—|]?\\s*\\b(karaoke|instrumental|backing\\s+track|minus\\s+one|without\\s+vocal)\\b[]\\)]?".toRegex()
    )

    private val COVER_PATTERNS = listOf(
        "(?i)[\\(\\[\\-–—|]?\\s*\\b(cover\\s+by|tribute\\s+to|acoustic\\s+cover|lofi\\s+cover)\\b[]\\)]?".toRegex()
    )

    private val REMASTERED_PATTERNS = listOf(
        "(?i)[\\(\\[\\-–—|]?\\s*\\b(remastered|remaster|\\d{4}\\s+remaster)\\b[]\\)]?".toRegex()
    )

    /**
     * Resolves an OnlineSongModel into its canonical identity and version.
     */
    fun resolve(song: OnlineSongModel): CanonicalTrackIdentity {
        val cleanTitle = cleanTitle(song.title)
        val primaryArtist = extractPrimaryArtist(song.artist)
        val versionInfo = classifyVersion(song.title, song.album, song.artist, song.durationMs)
        val canonicalId = generateCanonicalId(cleanTitle, primaryArtist)
        val year = song.year?.filter { it.isDigit() }?.take(4)?.toIntOrNull()

        return CanonicalTrackIdentity(
            canonicalTrackId = canonicalId,
            providerTrackId = song.id,
            providerName = song.source,
            normalizedTitle = cleanTitle,
            normalizedPrimaryArtist = primaryArtist,
            albumId = song.album,
            albumTitle = song.album,
            releaseYear = year,
            durationMs = song.durationMs,
            recordingVersion = versionInfo.first,
            versionConfidence = versionInfo.second,
            officialStatus = isOfficialVersion(versionInfo.first, song.source),
            highResArtUrl = song.highResArtUrl,
            lyrics = song.lyrics,
            language = song.language,
            bitrate = song.bitrate,
            playableSource = song.streamUrl,
            youtubeVideoId = song.youtubeVideoId
        )
    }

    /**
     * Resolves a local RecordingEntity into its canonical identity and version.
     */
    fun resolve(recording: RecordingEntity): CanonicalTrackIdentity {
        val cleanTitle = cleanTitle(recording.title)
        val primaryArtist = extractPrimaryArtist(recording.artist ?: "Unknown Artist")
        val versionInfo = classifyVersion(recording.title, recording.album, recording.artist ?: "", recording.durationMs)
        val canonicalId = generateCanonicalId(cleanTitle, primaryArtist)

        return CanonicalTrackIdentity(
            canonicalTrackId = canonicalId,
            providerTrackId = "local_${recording.id}",
            providerName = if (recording.sourcePackage?.contains("online") == true) "online" else "local",
            normalizedTitle = cleanTitle,
            normalizedPrimaryArtist = primaryArtist,
            albumId = recording.album,
            albumTitle = recording.album,
            releaseYear = null,
            durationMs = recording.durationMs,
            recordingVersion = versionInfo.first,
            versionConfidence = versionInfo.second,
            officialStatus = isOfficialVersion(versionInfo.first, "local"),
            highResArtUrl = recording.albumArtPath,
            lyrics = recording.lyrics,
            language = VibeFlowMoodAnalyzer.inferLanguage("${recording.title} ${recording.artist ?: ""}"),
            bitrate = "${recording.bitrateKbps}kbps",
            playableSource = recording.filePath,
            localRecordingId = recording.id
        )
    }

    /**
     * Cleans titles by removing promotional, resolution, and platform noise tags.
     */
    fun cleanTitle(rawTitle: String): String {
        var cleaned = rawTitle
        for (pattern in JUNK_SUFFIX_PATTERNS) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim().replace("\\s+".toRegex(), " ")
    }

    /**
     * Extracts the primary artist name before featured artists, guests, or producers.
     */
    fun extractPrimaryArtist(artistField: String): String {
        if (artistField.isBlank()) return "Unknown Artist"
        val delimiters = listOf(",", "&", "feat.", "ft.", "/", "vs.", "featuring", ";")
        var first = artistField
        for (d in delimiters) {
            val idx = first.indexOf(d, ignoreCase = true)
            if (idx > 0) {
                first = first.substring(0, idx)
            }
        }
        return first.trim()
    }

    /**
     * Generates a stable deterministic canonical ID combining normalized title stem and primary artist stem.
     */
    fun generateCanonicalId(normalizedTitle: String, primaryArtist: String): String {
        val titleStem = normalizedTitle.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]".toRegex(), "")
            .take(28)
        val artistStem = primaryArtist.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]".toRegex(), "")
            .take(18)
        return "${titleStem}_${artistStem}"
    }

    /**
     * Classifies a track into a version type with confidence score.
     */
    fun classifyVersion(
        title: String,
        album: String?,
        artist: String,
        durationMs: Long
    ): Pair<RecordingVersionType, Float> {
        val lowerTitle = title.lowercase(Locale.ROOT)
        val lowerAlbum = (album ?: "").lowercase(Locale.ROOT)

        // 1. Check for snippets / ringtones (< 55s unless titled interlude or prelude)
        if (durationMs in 1..54999L && !lowerTitle.contains("interlude") && !lowerTitle.contains("intro") && !lowerTitle.contains("prelude")) {
            return Pair(RecordingVersionType.SNIPPET_CLIP, 0.95f)
        }

        // 2. Slowed + Reverb
        for (pattern in SLOWED_REVERB_PATTERNS) {
            if (pattern.containsMatchIn(lowerTitle)) {
                return Pair(RecordingVersionType.SLOWED_REVERB, 0.95f)
            }
        }

        // 3. Sped Up / Nightcore
        for (pattern in SPED_UP_PATTERNS) {
            if (pattern.containsMatchIn(lowerTitle)) {
                return Pair(RecordingVersionType.SPED_UP_NIGHTCORE, 0.95f)
            }
        }

        // 4. Karaoke / Instrumental
        for (pattern in INSTRUMENTAL_PATTERNS) {
            if (pattern.containsMatchIn(lowerTitle)) {
                return Pair(RecordingVersionType.KARAOKE_INSTRUMENTAL, 0.95f)
            }
        }

        // 5. Covers
        for (pattern in COVER_PATTERNS) {
            if (pattern.containsMatchIn(lowerTitle)) {
                return Pair(RecordingVersionType.COVER, 0.90f)
            }
        }

        // 6. Remix / DJ / Club Edit
        for (pattern in REMIX_PATTERNS) {
            if (pattern.containsMatchIn(lowerTitle)) {
                return Pair(RecordingVersionType.REMIX, 0.92f)
            }
        }

        // 7. Acoustic / Unplugged
        for (pattern in ACOUSTIC_PATTERNS) {
            if (pattern.containsMatchIn(lowerTitle)) {
                return Pair(RecordingVersionType.ACOUSTIC, 0.90f)
            }
        }

        // 8. Live Concert
        for (pattern in LIVE_PATTERNS) {
            if (pattern.containsMatchIn(lowerTitle) || pattern.containsMatchIn(lowerAlbum)) {
                return Pair(RecordingVersionType.LIVE, 0.90f)
            }
        }

        // 9. Remastered
        for (pattern in REMASTERED_PATTERNS) {
            if (pattern.containsMatchIn(lowerTitle) || pattern.containsMatchIn(lowerAlbum)) {
                return Pair(RecordingVersionType.REMASTERED, 0.88f)
            }
        }

        // 10. Official Soundtrack
        if (lowerAlbum.contains("soundtrack") || lowerAlbum.contains("original motion picture") || lowerAlbum.contains("ost") || lowerTitle.contains("from \"")) {
            return Pair(RecordingVersionType.OFFICIAL_SOUNDTRACK, 0.90f)
        }

        // 11. Official Single / Album track
        if (lowerAlbum.contains("single") || lowerAlbum.contains("- single")) {
            return Pair(RecordingVersionType.OFFICIAL_SINGLE, 0.85f)
        }

        // Default: Original studio recording
        return Pair(RecordingVersionType.ORIGINAL_STUDIO, 0.85f)
    }

    private fun isOfficialVersion(version: RecordingVersionType, source: String): Boolean {
        return when (version) {
            RecordingVersionType.ORIGINAL_STUDIO,
            RecordingVersionType.OFFICIAL_SOUNDTRACK,
            RecordingVersionType.OFFICIAL_SINGLE,
            RecordingVersionType.REMASTERED -> true
            else -> false
        }
    }
}
