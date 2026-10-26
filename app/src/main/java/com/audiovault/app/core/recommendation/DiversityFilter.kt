package com.audiovault.app.core.recommendation

import com.audiovault.app.core.online.OnlineSongModel

object DiversityFilter {

    /**
     * Filters candidate recommendations to strictly enforce:
     * 1. 1-Hour Anti-Repeat Cooldown (prevents repeating recently played songs).
     * 2. Active Queue / Existing Track Deduplication.
     * 3. Consecutive Same Artist Limit (max 2 consecutive).
     * 4. Per-batch Artist Saturation Limit.
     */
    fun filterAndDiversify(
        candidates: List<OnlineSongModel>,
        excludeIds: Set<String> = emptySet(),
        currentPlayingSongId: String? = null,
        recentPlayedSongIds: Set<String> = emptySet(),
        recentPlayedNormalizedKeys: Set<String> = emptySet(),
        recentPlayedArtists: Set<String> = emptySet(),
        config: RecommendationConfig = RecommendationConfig()
    ): List<OnlineSongModel> {
        val result = mutableListOf<OnlineSongModel>()
        val seenNormalized = mutableSetOf<String>()
        val artistCountMap = mutableMapOf<String, Int>()

        var lastArtist = ""
        var consecutiveArtistCount = 0
        val normalizedRecentArtists = recentPlayedArtists.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()

        for (song in candidates) {
            // 1. Exclude currently playing track
            if (song.id == currentPlayingSongId) continue

            // 2. Exclude tracks currently queued or explicitly excluded
            if (excludeIds.contains(song.id)) continue

            val normalizedKey = normalize(song.title, song.artist)

            // 3. 1-Hour Anti-Repeat Cooldown check
            if (recentPlayedSongIds.contains(song.id) || recentPlayedNormalizedKeys.contains(normalizedKey)) {
                continue // Skip: was played in the last 1 hour
            }

            // 4. Batch deduplication (title + artist collision)
            if (seenNormalized.contains(normalizedKey)) continue

            val primaryArtist = song.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim()?.lowercase() ?: ""

            // 5. Rolling recent artist cooldown (never pick from recently played artists)
            if (primaryArtist.isNotBlank() && normalizedRecentArtists.contains(primaryArtist)) {
                continue
            }

            // 6. Consecutive artist rule (strict 1 consecutive limit to prevent loops)
            if (primaryArtist.isNotBlank() && primaryArtist == lastArtist) {
                if (consecutiveArtistCount >= config.maxConsecutiveSameArtist) {
                    continue // Skip to preserve flow diversity
                }
                consecutiveArtistCount++
            } else {
                lastArtist = primaryArtist
                consecutiveArtistCount = 1
            }

            // 7. Max artist representation per queue generation batch
            val currentArtistTotal = artistCountMap.getOrDefault(primaryArtist, 0)
            if (currentArtistTotal >= config.maxArtistCountPerSession) {
                continue
            }

            seenNormalized.add(normalizedKey)
            artistCountMap[primaryArtist] = currentArtistTotal + 1
            result.add(song)
        }

        return result
    }

    fun normalize(title: String, artist: String): String {
        val cleanTitle = title.lowercase().replace("[^a-zA-Z0-9]".toRegex(), "").take(20)
        val cleanArtist = artist.lowercase().replace("[^a-zA-Z0-9]".toRegex(), "").take(15)
        return "$cleanTitle|$cleanArtist"
    }
}
