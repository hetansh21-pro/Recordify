package com.audiovault.app.core.recommendation

import com.audiovault.app.data.local.entity.ListeningHistoryEntity
import kotlin.math.exp

data class UserMusicProfile(
    val topArtists: Map<String, Float> = emptyMap(),      // Artist Name -> Weighted Affinity (0.0 to 1.0)
    val topGenres: Map<String, Float> = emptyMap(),       // Genre/Mood -> Affinity
    val languagePreferences: Map<String, Float> = emptyMap(), // Language -> Affinity
    val skippedArtists: Map<String, Float> = emptyMap(),  // Artist -> Skip Penalty
    val likedSongIds: Set<String> = emptySet(),
    val totalPlays: Int = 0,
    val totalCompletions: Int = 0,
    val totalSkips: Int = 0,
    val discoveryPreference: Float = 0.70f,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
) {
    val completionRate: Float
        get() = if (totalPlays > 0) (totalCompletions.toFloat() / totalPlays.toFloat()).coerceIn(0f, 1f) else 0.5f

    val skipRate: Float
        get() = if (totalPlays > 0) (totalSkips.toFloat() / totalPlays.toFloat()).coerceIn(0f, 1f) else 0.1f

    companion object {
        fun buildFromHistory(
            history: List<ListeningHistoryEntity>,
            likedSongIds: Set<String> = emptySet(),
            config: RecommendationConfig = RecommendationConfig()
        ): UserMusicProfile {
            if (history.isEmpty()) {
                return UserMusicProfile(likedSongIds = likedSongIds)
            }

            val now = System.currentTimeMillis()
            val millisPerDay = 86_400_000.0

            val artistScores = mutableMapOf<String, Float>()
            val genreScores = mutableMapOf<String, Float>()
            val langScores = mutableMapOf<String, Float>()
            val skipPenalties = mutableMapOf<String, Float>()

            var plays = 0
            var completions = 0
            var skips = 0

            for (event in history) {
                val daysAgo = (now - event.timestamp).coerceAtLeast(0L) / millisPerDay
                val decayFactor = exp(-daysAgo / config.decayDays).toFloat()
                val weightedScore = event.eventWeight * decayFactor

                val cleanArtist = event.artist.trim()
                if (cleanArtist.isNotBlank()) {
                    val currentArtistScore = artistScores.getOrDefault(cleanArtist, 0f)
                    artistScores[cleanArtist] = currentArtistScore + weightedScore

                    if (event.eventType == ListeningEventType.SONG_SKIPPED.name || event.eventType == ListeningEventType.SONG_SHORT_SKIP.name) {
                        val currentSkip = skipPenalties.getOrDefault(cleanArtist, 0f)
                        skipPenalties[cleanArtist] = currentSkip + (1.0f * decayFactor)
                    }
                }

                // Inferred Genre / Mood keywords from title and artist
                val inferredGenres = inferGenres(event.title, event.artist, event.genre)
                for (g in inferredGenres) {
                    val currentGScore = genreScores.getOrDefault(g, 0f)
                    genreScores[g] = currentGScore + weightedScore
                }

                // Language
                val lang = event.language ?: inferLanguage(event.title, event.artist)
                if (!lang.isNullOrBlank()) {
                    val currentLScore = langScores.getOrDefault(lang, 0f)
                    langScores[lang] = currentLScore + weightedScore
                }

                when (event.eventType) {
                    ListeningEventType.PLAY_STARTED.name -> plays++
                    ListeningEventType.PLAY_COMPLETED.name -> completions++
                    ListeningEventType.SONG_SKIPPED.name, ListeningEventType.SONG_SHORT_SKIP.name -> skips++
                }
            }

            // Normalize scores to [0.0, 1.0] range
            val maxArtistScore = artistScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val normalizedArtists = artistScores.mapValues { (_, v) -> (v / maxArtistScore).coerceIn(0f, 1f) }
                .toList().sortedByDescending { it.second }.take(25).toMap()

            val maxGenreScore = genreScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val normalizedGenres = genreScores.mapValues { (_, v) -> (v / maxGenreScore).coerceIn(0f, 1f) }
                .toList().sortedByDescending { it.second }.take(15).toMap()

            val maxLangScore = langScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val normalizedLangs = langScores.mapValues { (_, v) -> (v / maxLangScore).coerceIn(0f, 1f) }
                .toList().sortedByDescending { it.second }.take(5).toMap()

            return UserMusicProfile(
                topArtists = normalizedArtists,
                topGenres = normalizedGenres,
                languagePreferences = normalizedLangs,
                skippedArtists = skipPenalties,
                likedSongIds = likedSongIds,
                totalPlays = plays,
                totalCompletions = completions,
                totalSkips = skips,
                discoveryPreference = 0.70f,
                lastActiveTimestamp = now
            )
        }

        private fun inferGenres(title: String, artist: String, explicitGenre: String?): List<String> {
            val genres = mutableListOf<String>()
            if (!explicitGenre.isNullOrBlank()) genres.add(explicitGenre)

            val text = "$title $artist".lowercase()
            if (text.contains("shubh") || text.contains("sidhu") || text.contains("karan aujla") || text.contains("ap dhillon") || text.contains("punjabi") || text.contains("diljit")) {
                genres.add("Punjabi & Desi Hits")
            }
            if (text.contains("hip hop") || text.contains("rap") || text.contains("drake") || text.contains("eminem") || text.contains("travis") || text.contains("divine") || text.contains("mc stan")) {
                genres.add("Hip-Hop & Rap")
            }
            if (text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") || text.contains("slowed")) {
                genres.add("Lo-fi / Chill")
            }
            if (text.contains("romantic") || text.contains("love") || text.contains("ishq") || text.contains("dil") || text.contains("heart") || text.contains("arijit") || text.contains("atif")) {
                genres.add("Romantic & Melodies")
            }
            if (text.contains("dance") || text.contains("party") || text.contains("remix") || text.contains("dj") || text.contains("dhamaka")) {
                genres.add("Party / Dance")
            }
            if (text.contains("sad") || text.contains("dard") || text.contains("judaai") || text.contains("alon") || text.contains("broken")) {
                genres.add("Sad / Melancholic")
            }
            if (text.contains("workout") || text.contains("gym") || text.contains("energetic") || text.contains("bass") || text.contains("power")) {
                genres.add("Energetic / Workout")
            }
            if (text.contains("acoustic") || text.contains("unplugged") || text.contains("reprise") || text.contains("piano")) {
                genres.add("Acoustic / Relax")
            }
            if (genres.isEmpty()) {
                genres.add("Bollywood / Pop")
            }
            return genres
        }

        private fun inferLanguage(title: String, artist: String): String? {
            val text = "$title $artist".lowercase()
            return when {
                text.contains("arijit") || text.contains("shreya") || text.contains("atif") || text.contains("sonu") ||
                        text.contains("jubin") || text.contains("kumar") || text.contains("neha") || text.contains("hindi") -> "Hindi"
                text.contains("diljit") || text.contains("sidhu") || text.contains("karan aujla") || text.contains("ap dhillon") ||
                        text.contains("shubh") || text.contains("punjabi") -> "Punjabi"
                text.contains("anirudh") || text.contains("ar rehman") || text.contains("sid sriram") || text.contains("tamil") -> "Tamil"
                text.contains("ed sheeran") || text.contains("taylor") || text.contains("drake") || text.contains("weeknd") ||
                        text.contains("eminem") || text.contains("english") -> "English"
                else -> null
            }
        }
    }
}
