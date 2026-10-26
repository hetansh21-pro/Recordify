package com.audiovault.app.core.search

import com.audiovault.app.core.identity.RecordingVersionType
import java.util.Locale

/**
 * Natural language search intent parser.
 * Extracts entity target (Songs, Albums, Artists, Playlists), decade/year filters,
 * language, genre, mood, and explicit version requests.
 */
object SearchIntentParser {

    private val DECADE_PATTERNS = mapOf(
        "(?i)\\b(19)?70['’]?s\\b".toRegex() to 1970..1979,
        "(?i)\\b(19)?80['’]?s\\b".toRegex() to 1980..1989,
        "(?i)\\b(19)?90['’]?s\\b".toRegex() to 1990..1999,
        "(?i)\\b(20)?00['’]?s\\b".toRegex() to 2000..2009,
        "(?i)\\b(20)?10['’]?s\\b".toRegex() to 2010..2019,
        "(?i)\\b(20)?20['’]?s\\b".toRegex() to 2020..2029
    )

    private val RANGE_YEAR_PATTERN = "(?i)\\b(?:from\\s+)?(19\\d{2}|20\\d{2})\\s*(?:to|-|until)\\s*(19\\d{2}|20\\d{2})\\b".toRegex()
    private val SINGLE_YEAR_PATTERN = "(?i)\\b(19\\d{2}|20\\d{2})\\b".toRegex()

    private val LANGUAGES = listOf(
        "hindi", "punjabi", "gujarati", "english", "tamil", "telugu",
        "bengali", "marathi", "kannada", "malayalam", "spanish", "korean", "bhojpuri"
    )

    private val GENRES = listOf(
        "bollywood", "pop", "rock", "hip hop", "hip-hop", "rap", "lofi", "lo-fi",
        "classical", "folk", "sufi", "ghazal", "indie", "edm", "metal", "devotional", "qawwali"
    )

    private val MOODS = listOf(
        "romantic", "love", "sad", "party", "dance", "chill", "calm", "relaxing",
        "energetic", "workout", "nostalgic", "late night", "morning", "focus", "acoustic"
    )

    fun parse(rawQuery: String): SearchIntent {
        val trimmed = rawQuery.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        var decadeRange: IntRange? = null
        var exactYear: Int? = null

        // 1. Check range year like "1985 to 1995"
        val rangeMatch = RANGE_YEAR_PATTERN.find(lower)
        if (rangeMatch != null) {
            val y1 = rangeMatch.groupValues[1].toIntOrNull()
            val y2 = rangeMatch.groupValues[2].toIntOrNull()
            if (y1 != null && y2 != null) {
                decadeRange = if (y1 <= y2) y1..y2 else y2..y1
            }
        }

        // 2. Check decade like "90s", "1980s"
        if (decadeRange == null) {
            for ((pattern, range) in DECADE_PATTERNS) {
                if (pattern.containsMatchIn(lower)) {
                    decadeRange = range
                    break
                }
            }
        }

        // 3. Check single year
        if (decadeRange == null) {
            val yearMatch = SINGLE_YEAR_PATTERN.find(lower)
            if (yearMatch != null) {
                exactYear = yearMatch.groupValues[1].toIntOrNull()
            }
        }

        // 4. Check explicit version request
        var requestedVersion: RecordingVersionType? = null
        var isOriginalOnly = true

        if (lower.contains("remix") || lower.contains("remixes")) {
            requestedVersion = RecordingVersionType.REMIX
            isOriginalOnly = false
        } else if (lower.contains("slowed") || lower.contains("reverb")) {
            requestedVersion = RecordingVersionType.SLOWED_REVERB
            isOriginalOnly = false
        } else if (lower.contains("sped up") || lower.contains("speed up") || lower.contains("nightcore")) {
            requestedVersion = RecordingVersionType.SPED_UP_NIGHTCORE
            isOriginalOnly = false
        } else if (lower.contains("acoustic") || lower.contains("unplugged")) {
            requestedVersion = RecordingVersionType.ACOUSTIC
            isOriginalOnly = false
        } else if (lower.contains("live version") || lower.contains("live concert") || lower.contains("live in") || lower.contains("live at")) {
            requestedVersion = RecordingVersionType.LIVE
            isOriginalOnly = false
        } else if (lower.contains("karaoke") || lower.contains("instrumental")) {
            requestedVersion = RecordingVersionType.KARAOKE_INSTRUMENTAL
            isOriginalOnly = false
        }

        // 5. Detect language, genre, mood
        val detectedLanguage = LANGUAGES.firstOrNull { lower.contains(it) }
        val detectedGenre = GENRES.firstOrNull { lower.contains(it) }
        val detectedMood = MOODS.firstOrNull { lower.contains(it) }

        // 6. Detect query type and target entity
        var queryType = SearchQueryType.GENERAL_SONG
        var targetEntity = SearchTargetEntity.ALL

        val isSoundtrack = lower.contains("soundtrack") || lower.contains("ost") || lower.contains("score")
        val isAlbumIntent = lower.contains("album") || lower.contains("albums") || lower.contains("discography")
        val isArtistIntent = lower.contains("all songs by") || lower.contains("discography by") || lower.contains("all albums by") || lower.contains("artist")
        val isPlaylistIntent = lower.contains("playlist") || lower.contains("playlists")
        val isExplicitSongIntent = lower.contains("song") || lower.contains("songs") || lower.contains("track") || lower.contains("tracks")

        if (isSoundtrack) {
            queryType = SearchQueryType.SOUNDTRACK_SEARCH
            targetEntity = SearchTargetEntity.ALBUMS
        } else if (isAlbumIntent) {
            queryType = SearchQueryType.ALBUM_SEARCH
            targetEntity = SearchTargetEntity.ALBUMS
        } else if (isArtistIntent) {
            queryType = SearchQueryType.ARTIST_DISCOGRAPHY
            targetEntity = SearchTargetEntity.ARTISTS
        } else if (isPlaylistIntent) {
            targetEntity = SearchTargetEntity.PLAYLISTS
        } else if (requestedVersion != null) {
            queryType = SearchQueryType.EXPLICIT_VERSION
            targetEntity = SearchTargetEntity.SONGS
        } else if (decadeRange != null || exactYear != null) {
            queryType = SearchQueryType.DECADE_SEARCH
            targetEntity = if (isAlbumIntent) SearchTargetEntity.ALBUMS else SearchTargetEntity.ALL
        } else if (detectedMood != null && !isAlbumIntent) {
            queryType = SearchQueryType.MOOD_DISCOVERY
            targetEntity = SearchTargetEntity.SONGS
        } else if (detectedGenre != null && !isAlbumIntent) {
            queryType = SearchQueryType.GENRE_DISCOVERY
            targetEntity = SearchTargetEntity.ALL
        } else if (isExplicitSongIntent) {
            queryType = SearchQueryType.GENERAL_SONG
            targetEntity = SearchTargetEntity.SONGS
        }

        // 7. Clean query text for search backend
        var clean = trimmed
        val phrasesToStrip = listOf(
            "(?i)\\bofficial\\s+soundtrack\\s+of\\b",
            "(?i)\\bsoundtrack\\s+of\\b",
            "(?i)\\bofficial\\s+soundtrack\\b",
            "(?i)\\bsoundtrack\\b",
            "(?i)\\ball\\s+albums\\s+by\\b",
            "(?i)\\ball\\s+albums\\b",
            "(?i)\\balbums\\s+by\\b",
            "(?i)\\balbums\\b",
            "(?i)\\balbum\\b",
            "(?i)\\ball\\s+songs\\s+by\\b",
            "(?i)\\bdiscography\\s+by\\b",
            "(?i)\\bdiscography\\b",
            "(?i)\\bbest\\s+original\\s+songs\\s+from\\s+the\\b",
            "(?i)\\bbest\\s+songs\\s+from\\s+the\\b",
            "(?i)\\bsongs\\s+from\\s+the\\b",
            "(?i)\\bfind\\s+acoustic\\s+versions\\s+of\\b",
            "(?i)\\bacoustic\\s+versions?\\s+of\\b",
            "(?i)\\bacoustic\\s+version\\b",
            "(?i)\\bremixes?\\s+of\\b",
            "(?i)\\bfor\\s+late\\s+night\\b",
            "(?i)\\bfor\\s+night\\b",
            "(?i)\\boriginal\\s+studio\\s+version\\s+of\\s+a\\s+song\\b",
            "(?i)\\boriginal\\s+version\\s+of\\s+this\\s+song\\b",
            "(?i)\\boriginal\\s+version\\s+of\\b",
            "(?i)\\boriginal\\s+version\\b",
            "(?i)\\boriginal\\s+songs?\\b",
            "(?i)\\bplaylist\\b",
            "(?i)\\bplaylists\\b"
        )
        for (pattern in phrasesToStrip) {
            clean = clean.replace(pattern.toRegex(), "")
        }

        // Remove decade references from clean string
        for (pattern in DECADE_PATTERNS.keys) {
            clean = clean.replace(pattern, "")
        }
        clean = clean.replace(RANGE_YEAR_PATTERN, "")

        clean = clean.trim().replace("\\s+".toRegex(), " ")
        if (clean.isBlank()) {
            clean = when {
                detectedGenre != null -> detectedGenre.replaceFirstChar { it.uppercase() }
                detectedLanguage != null -> "${detectedLanguage.replaceFirstChar { it.uppercase() }} Hits"
                detectedMood != null -> "${detectedMood.replaceFirstChar { it.uppercase() }} Songs"
                else -> trimmed
            }
        }

        return SearchIntent(
            rawQuery = trimmed,
            cleanQuery = clean,
            queryType = queryType,
            targetEntity = targetEntity,
            decadeRange = decadeRange,
            exactYear = exactYear,
            detectedLanguage = detectedLanguage,
            detectedGenre = detectedGenre,
            detectedMood = detectedMood,
            requestedVersion = requestedVersion,
            isOriginalOnly = isOriginalOnly
        )
    }
}
