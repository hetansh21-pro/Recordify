package com.audiovault.app.core.recommendation

import com.audiovault.app.core.online.OnlineSongModel

object ScoringEngine {

    fun calculatePersonalizationScore(
        song: OnlineSongModel,
        profile: UserMusicProfile,
        currentSong: OnlineSongModel? = null,
        currentVibe: VibeCategory? = null,
        config: RecommendationConfig = RecommendationConfig()
    ): Float {
        var score = 0.50f // Baseline candidate score

        // 1. Artist Affinity
        val cleanArtist = song.artist.trim()
        val artistAffinity = profile.topArtists.entries.firstOrNull {
            it.key.contains(cleanArtist, ignoreCase = true) || cleanArtist.contains(it.key, ignoreCase = true)
        }?.value ?: 0f
        score += (artistAffinity * 0.35f)

        // 2. Similarity to currently playing track (if available)
        if (currentSong != null) {
            val similarity = SimilarityEngine.calculateSimilarity(currentSong, song)
            score += (similarity * 0.30f)
        }

        // 3. Vibe Alignment
        if (currentVibe != null) {
            val songText = "${song.title} ${song.artist} ${song.album ?: ""}".lowercase()
            val vibeMatch = when (currentVibe) {
                VibeCategory.LOFI -> songText.contains("lofi") || songText.contains("chill") || songText.contains("slowed")
                VibeCategory.ROMANTIC -> songText.contains("romantic") || songText.contains("love") || songText.contains("ishq")
                VibeCategory.PARTY, VibeCategory.ENERGETIC -> songText.contains("party") || songText.contains("dance") || songText.contains("remix")
                VibeCategory.WORKOUT -> songText.contains("workout") || songText.contains("gym") || songText.contains("bass")
                VibeCategory.SAD -> songText.contains("sad") || songText.contains("dard") || songText.contains("broken")
                VibeCategory.CHILL, VibeCategory.MORNING, VibeCategory.NIGHT -> songText.contains("acoustic") || songText.contains("unplugged") || songText.contains("relax")
                else -> true
            }
            if (vibeMatch) score += 0.15f
        }

        // 4. User Likes
        if (profile.likedSongIds.contains(song.id)) {
            score += 0.25f
        }

        // 5. Skip Penalty
        val skipPenalty = profile.skippedArtists.entries.firstOrNull {
            it.key.contains(cleanArtist, ignoreCase = true) || cleanArtist.contains(it.key, ignoreCase = true)
        }?.value ?: 0f
        if (skipPenalty > 0f) {
            score -= (skipPenalty * 0.15f).coerceAtMost(0.40f)
        }

        // 6. Discovery Bonus (for fresh high-quality tracks)
        if (artistAffinity < 0.1f && !profile.likedSongIds.contains(song.id)) {
            score += (profile.discoveryPreference * 0.10f)
        }

        return score.coerceIn(0.05f, 1.0f)
    }
}
