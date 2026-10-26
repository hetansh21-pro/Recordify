package com.audiovault.app.core.search

import com.audiovault.app.core.identity.CanonicalTrackIdentity
import com.audiovault.app.core.identity.RecordingVersionType
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer

/**
 * Intelligent relevance scorer and ranking engine for multi-source search results.
 */
object SearchRelevanceRanker {

    private val INDIAN_LEGENDS = listOf(
        "mohammed rafi", "rafi", "kishore kumar", "lata mangeshkar", "asha bhosle", "mukesh",
        "talat mahmood", "manna dey", "hemant kumar", "r. d. burman", "r.d. burman", "s. d. burman",
        "salil chowdhury", "kalyanji-anandji", "laxmikant-pyarelal", "shaan", "kailash kher",
        "arijit singh", "shreya ghoshal", "atif aslam", "jubin nautiyal", "sonu nigam", "kk",
        "mohit chauhan", "anuv jain", "darshan raval", "farhan saeed", "mustafa zahid", "payal dev",
        "amaal mallik", "alka yagnik", "kumar sanu", "udit narayan", "himesh reshammiya",
        "vishal mishra", "sachet-parampara", "jasleen royal", "b praak", "sidhu moose wala", "diljit dosanjh"
    )

    fun isGenericIntentQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        val intentTerms = listOf(
            "old song", "old songs", "olds", "old is gold", "purane gane", "purane gaane", "purane geet",
            "sad song", "sad songs", "dard bhare", "romantic song", "romantic songs", "love song", "love songs",
            "90s song", "90s songs", "90s bollywood", "80s song", "80s songs", "70s song", "70s songs",
            "party songs", "party song", "dance songs", "dance song", "punjabi songs", "gujarati songs"
        )
        return intentTerms.any { q == it || q.startsWith("$it ") || q.endsWith(" $it") }
    }

    /**
     * Ranks canonical tracks according to the parsed search intent, country, and user preference.
     */
    fun rankTracks(
        tracks: List<CanonicalTrackIdentity>,
        intent: SearchIntent,
        userCountryCode: String = "IN",
        primaryLanguage: String = "Hindi",
        preferredLanguages: Set<String> = emptySet()
    ): List<CanonicalTrackIdentity> {
        val queryLower = intent.cleanQuery.lowercase().trim()
        val isGenericIntent = isGenericIntentQuery(queryLower) || isGenericIntentQuery(intent.rawQuery)

        return tracks.sortedWith(
            compareByDescending<CanonicalTrackIdentity> { track ->
                var score = 0f
                val titleLower = track.normalizedTitle.lowercase()
                val artistLower = track.normalizedPrimaryArtist.lowercase()

                // 1. Exact title or artist match (Only if not a generic category/mood intent)
                if (!isGenericIntent) {
                    if (titleLower == queryLower) {
                        score += 50f
                    } else if (titleLower.startsWith(queryLower)) {
                        score += 30f
                    } else if (titleLower.contains(queryLower)) {
                        score += 20f
                    }

                    if (artistLower.contains(queryLower)) {
                        score += 15f
                    }

                    // Single-letter and short prefix match boost
                    if (queryLower.isNotEmpty() && queryLower.length <= 3) {
                        if (titleLower.startsWith(queryLower)) {
                            score += 45f
                        }
                    }
                } else {
                    // For generic intent queries: Boost recognized Indian legendary artists
                    if (INDIAN_LEGENDS.any { artistLower.contains(it) }) {
                        score += 65f
                    }

                    // Boost native Indian provider (Saavn) & Indian languages
                    if (track.providerName == "saavn") {
                        score += 35f
                    }

                    // Decade/Era relevance for old/retro queries
                    if (queryLower.contains("old") || queryLower.contains("puran") || queryLower.contains("retro") || queryLower.contains("classic")) {
                        if (track.releaseYear != null && track.releaseYear <= 1999) {
                            score += 40f
                        }
                    }

                    // Sad songs specific catalog boost
                    if (queryLower.contains("sad") || queryLower.contains("dard")) {
                        val sadHits = listOf("thodi der", "jo tum mere ho", "tum hi aana", "zaroorat", "bewajah", "roke na ruke", "channa mereya")
                        if (sadHits.any { titleLower.contains(it) }) {
                            score += 55f
                        }
                    }
                }

                // 3. User Country & Language Relevance Boost
                val trackLang = track.language ?: VibeFlowMoodAnalyzer.inferLanguage("${track.normalizedTitle} ${track.normalizedPrimaryArtist}")
                if (trackLang != null) {
                    if (trackLang.equals(primaryLanguage, ignoreCase = true)) {
                        score += 40f
                        if (queryLower.isNotEmpty() && queryLower.length <= 3 && titleLower.startsWith(queryLower)) {
                            score += 35f // Prioritize native language matches for 1-letter/short searches
                        }
                    } else if (preferredLanguages.any { it.equals(trackLang, ignoreCase = true) }) {
                        score += 25f
                    } else if (preferredLanguages.isNotEmpty() && !preferredLanguages.any { it.equals(trackLang, ignoreCase = true) }) {
                        score -= 20f // Suppress unselected languages
                    }
                }

                // 4. Trending & Viral Match (Spotify Top, Instagram Reels, viral chart hits)
                val combinedMeta = "${track.normalizedTitle} ${track.normalizedPrimaryArtist} ${track.albumTitle ?: ""}".lowercase()
                if (combinedMeta.contains("trending") || combinedMeta.contains("viral") || combinedMeta.contains("reel") ||
                    combinedMeta.contains("top") || combinedMeta.contains("hit") || combinedMeta.contains("chart")) {
                    score += 30f
                }

                // 5. Original / Official status bonus
                if (track.officialStatus) {
                    score += 25f
                }
                if (track.recordingVersion == RecordingVersionType.ORIGINAL_STUDIO ||
                    track.recordingVersion == RecordingVersionType.OFFICIAL_SOUNDTRACK) {
                    score += 15f
                }

                // 6. Requested explicit version bonus
                if (intent.requestedVersion != null && track.recordingVersion == intent.requestedVersion) {
                    score += 40f
                }

                // 7. Decade accuracy bonus
                if (intent.decadeRange != null && track.releaseYear != null) {
                    if (track.releaseYear in intent.decadeRange) {
                        score += 30f
                    }
                }

                // 8. Playability and high quality
                if (track.isPlayable) {
                    score += 10f
                }
                if (track.bitrate.contains("320")) {
                    score += 5f
                }
                if (track.providerName == "spotify" || track.providerName == "saavn") {
                    score += 10f
                }

                score
            }.thenByDescending { it.versionConfidence }
        )
    }

    /**
     * Ranks albums according to the search intent, country, and language.
     */
    fun rankAlbums(
        albums: List<OnlineAlbumModel>,
        intent: SearchIntent,
        userCountryCode: String = "IN",
        primaryLanguage: String = "Hindi"
    ): List<OnlineAlbumModel> {
        val queryLower = intent.cleanQuery.lowercase().trim()

        return albums.sortedWith(
            compareByDescending { album ->
                var score = 0f
                val titleLower = album.title.lowercase()
                val artistLower = (album.artist ?: "").lowercase()

                if (titleLower == queryLower) {
                    score += 50f
                } else if (titleLower.startsWith(queryLower)) {
                    score += 30f
                } else if (titleLower.contains(queryLower)) {
                    score += 20f
                }

                if (artistLower.contains(queryLower)) {
                    score += 15f
                }

                // Short prefix match
                if (queryLower.isNotEmpty() && queryLower.length <= 3 && titleLower.startsWith(queryLower)) {
                    score += 35f
                }

                // Primary language album boost
                if (album.language?.equals(primaryLanguage, ignoreCase = true) == true) {
                    score += 30f
                }

                // Decade match
                val albumYear = album.year?.filter { it.isDigit() }?.toIntOrNull()
                if (intent.decadeRange != null && albumYear != null) {
                    if (albumYear in intent.decadeRange) {
                        score += 35f
                    }
                }

                // Has tracks
                if (album.songs.isNotEmpty()) {
                    score += 15f
                }

                score
            }
        )
    }
}
