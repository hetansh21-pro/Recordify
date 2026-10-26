package com.audiovault.app.core.intelligence

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Convx-inspired track metadata & artwork matching engine.
 * Normalizes song and album titles, strips bracketed noise ("feat.", "From ...", "Remastered"),
 * compares title and artist tokens using bi-gram similarity, and verifies matches before accepting
 * artwork or resolving playlist songs.
 */
object TrackMatcher {

    private val bracketed = Regex(
        """\s*[(\[][^)\]]*(feat|ft\.|with |remaster|version|edit|deluxe|explicit|clean|original motion picture|soundtrack|from "[^"]*")[^)\]]*[)\]]""",
        RegexOption.IGNORE_CASE
    )
    private val trailingKind = Regex("""\s+-\s+(single|ep|audio|video|lyrics)$""", RegexOption.IGNORE_CASE)
    private val marks = Regex("""\p{Mn}+""")
    private val nonWord = Regex("""[^\p{L}\p{N}]+""")

    private val normalizeCache = ConcurrentHashMap<String, String>()
    private val bigramCache = ConcurrentHashMap<String, Set<String>>()

    const val MIN_ARTWORK_MATCH_SCORE = 0.55
    const val MIN_PLAYLIST_MATCH_SCORE = 0.60

    /**
     * Normalizes a song title or artist name:
     * - Lowercases and removes accents/diacritics
     * - Strips "(feat. ...)", "(From Film)", "- Single", "[Remastered]"
     * - Replaces punctuation with spaces and normalizes multiple spaces
     */
    fun normalize(s: String?): String {
        if (s.isNullOrBlank()) return ""
        normalizeCache[s]?.let { return it }

        val base = s.lowercase(Locale.ROOT)
            .replace(bracketed, "")
            .replace(trailingKind, "")
            .replace("&", " and ")

        val normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
            .replace(marks, "")
            .replace(nonWord, " ")
            .trim()
            .replace(Regex("\\s+"), " ")

        normalizeCache[s] = normalized
        return normalized
    }

    private fun bigrams(normalized: String): Set<String> {
        bigramCache[normalized]?.let { return it }
        val b = if (normalized.length < 2) emptySet() else normalized.windowed(2).toSet()
        bigramCache[normalized] = b
        return b
    }

    /**
     * Computes Sørensen–Dice bi-gram similarity between two strings (0.0 to 1.0).
     */
    fun similarity(a: String, b: String): Double {
        val normA = normalize(a)
        val normB = normalize(b)
        if (normA.isEmpty() || normB.isEmpty()) return 0.0
        if (normA == normB) return 1.0
        if (normA.contains(normB) || normB.contains(normA)) {
            val minLen = minOf(normA.length, normB.length).toDouble()
            val maxLen = maxOf(normA.length, normB.length).toDouble()
            return (minLen / maxLen).coerceAtLeast(0.8)
        }
        val bgA = bigrams(normA)
        val bgB = bigrams(normB)
        if (bgA.isEmpty() || bgB.isEmpty()) return 0.0
        val intersection = bgA.count { it in bgB }
        return (2.0 * intersection) / (bgA.size + bgB.size)
    }

    /**
     * Checks if candidate artist matches expected artist.
     * True if either primary artist token appears in the other (e.g. "Arijit Singh" matches "Arijit Singh, Pritam").
     */
    fun artistMatches(expected: String?, candidate: String?): Boolean {
        if (expected.isNullOrBlank() || candidate.isNullOrBlank()) return true
        val normExpected = normalize(expected)
        val normCandidate = normalize(candidate)
        if (normExpected == normCandidate) return true

        val eFirst = normalize(expected.split(",", " & ", " and ", " x ", " feat", " ft.").first())
        val cFirst = normalize(candidate.split(",", " & ", " and ", " x ", " feat", " ft.").first())

        if (eFirst.isNotEmpty() && normCandidate.contains(eFirst)) return true
        if (cFirst.isNotEmpty() && normExpected.contains(cFirst)) return true

        // Check token intersection
        val eTokens = normExpected.split(" ").filter { it.length >= 3 }
        val cTokens = normCandidate.split(" ").filter { it.length >= 3 }
        return eTokens.any { it in cTokens }
    }

    /**
     * Calculates a composite match score between requested track and candidate track.
     * Combines Title Similarity (60%) and Artist Similarity (40%).
     */
    fun matchScore(
        expectedTitle: String,
        expectedArtist: String?,
        candidateTitle: String,
        candidateArtist: String?
    ): Double {
        val titleScore = similarity(expectedTitle, candidateTitle)
        val hasExpectedArtist = !expectedArtist.isNullOrBlank()
        val hasCandidateArtist = !candidateArtist.isNullOrBlank()

        val artistScore = when {
            !hasExpectedArtist -> 1.0
            !hasCandidateArtist -> 0.5
            artistMatches(expectedArtist, candidateArtist) -> 1.0
            else -> similarity(expectedArtist, candidateArtist) * 0.5
        }
        return (titleScore * 0.60) + (artistScore * 0.40)
    }

    /**
     * Verifies if candidate metadata is a reliable match for artwork or audio resolution.
     */
    fun isConfidentMatch(
        expectedTitle: String,
        expectedArtist: String?,
        candidateTitle: String,
        candidateArtist: String?,
        threshold: Double = MIN_ARTWORK_MATCH_SCORE
    ): Boolean {
        // Strict Convx requirement: If expected artist is specified and candidate artist is present,
        // they MUST match. Mismatched artists (e.g. Arijit Singh vs Enrique Iglesias) are rejected.
        if (!expectedArtist.isNullOrBlank() && !candidateArtist.isNullOrBlank()) {
            if (!artistMatches(expectedArtist, candidateArtist)) {
                return false
            }
        }

        // Strict guard: if titles share no common words and similarity is low, reject
        val normExpected = normalize(expectedTitle)
        val normCandidate = normalize(candidateTitle)
        if (normExpected != normCandidate && !normExpected.contains(normCandidate) && !normCandidate.contains(normExpected)) {
            val titleSim = similarity(expectedTitle, candidateTitle)
            if (titleSim < 0.45) return false
        }
        return matchScore(expectedTitle, expectedArtist, candidateTitle, candidateArtist) >= threshold
    }
}
