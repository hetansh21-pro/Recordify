package com.audiovault.app.core.recommendation

import com.audiovault.app.core.online.OnlineSongModel
import kotlin.math.max

object SimilarityEngine {

    fun calculateSimilarity(songA: OnlineSongModel, songB: OnlineSongModel): Float {
        if (songA.id == songB.id) return 1.0f

        var score = 0.0f

        // 1. Artist exact or overlap (Weight: 0.35)
        val artistsA = songA.artist.lowercase().split(",", "&", "feat.", "ft.", "/").map { it.trim() }.filter { it.isNotBlank() }
        val artistsB = songB.artist.lowercase().split(",", "&", "feat.", "ft.", "/").map { it.trim() }.filter { it.isNotBlank() }

        val commonArtists = artistsA.intersect(artistsB.toSet())
        if (commonArtists.isNotEmpty()) {
            score += 0.35f
        } else {
            val aContainsB = artistsA.any { a -> artistsB.any { b -> a.contains(b) || b.contains(a) } }
            if (aContainsB) score += 0.20f
        }

        // 2. Language matching (Weight: 0.20)
        if (!songA.language.isNullOrBlank() && !songB.language.isNullOrBlank()) {
            if (songA.language.equals(songB.language, ignoreCase = true)) {
                score += 0.20f
            }
        } else {
            // Infer language match
            val langA = inferLang(songA.title, songA.artist)
            val langB = inferLang(songB.title, songB.artist)
            if (langA != null && langA == langB) {
                score += 0.15f
            }
        }

        // 3. Mood / Vibe keyword overlap (Weight: 0.25)
        val keywordsA = extractKeywords(songA.title + " " + (songA.album ?: ""))
        val keywordsB = extractKeywords(songB.title + " " + (songB.album ?: ""))
        val commonKeywords = keywordsA.intersect(keywordsB)
        if (commonKeywords.isNotEmpty()) {
            score += (commonKeywords.size.toFloat() / max(keywordsA.size, keywordsB.size).coerceAtLeast(1).toFloat() * 0.25f).coerceIn(0.05f, 0.25f)
        }

        // 4. Album similarity (Weight: 0.10)
        if (!songA.album.isNullOrBlank() && songA.album.equals(songB.album, ignoreCase = true)) {
            score += 0.10f
        }

        // 5. Title word token overlap (Weight: 0.10)
        val tokensA = songA.title.lowercase().split("[^a-zA-Z0-9]".toRegex()).filter { it.length > 2 }.toSet()
        val tokensB = songB.title.lowercase().split("[^a-zA-Z0-9]".toRegex()).filter { it.length > 2 }.toSet()
        val commonTokens = tokensA.intersect(tokensB)
        if (commonTokens.isNotEmpty()) {
            score += 0.10f
        }

        return score.coerceIn(0.0f, 1.0f)
    }

    private fun extractKeywords(text: String): Set<String> {
        val lower = text.lowercase()
        val found = mutableSetOf<String>()
        val vibeWords = listOf("lofi", "chill", "love", "romantic", "dance", "remix", "sad", "acoustic", "party", "slowed", "reverb", "mashup", "dhamaka")
        for (w in vibeWords) {
            if (lower.contains(w)) found.add(w)
        }
        return found
    }

    private fun inferLang(title: String, artist: String): String? {
        val text = "$title $artist".lowercase()
        return when {
            text.contains("arijit") || text.contains("shreya") || text.contains("atif") || text.contains("sonu") || text.contains("hindi") -> "Hindi"
            text.contains("diljit") || text.contains("sidhu") || text.contains("karan") || text.contains("punjabi") -> "Punjabi"
            text.contains("anirudh") || text.contains("rehman") || text.contains("tamil") -> "Tamil"
            text.contains("ed sheeran") || text.contains("taylor") || text.contains("english") -> "English"
            else -> null
        }
    }
}
