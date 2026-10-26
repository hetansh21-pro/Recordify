package com.audiovault.app.core.vibeflow

import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.recommendation.UserMusicProfile
import com.audiovault.app.core.recommendation.VibeCategory
import kotlin.math.abs
import kotlin.math.max

data class VibeFlowScoreResult(
    val score: Float,               // Normalized 0.0 to 1.0
    val explanation: String,        // Transparent user-facing reason
    val moodMatchPercent: Int,      // e.g. 85%
    val seedArtistMatch: Boolean,
    val isFreshDiscovery: Boolean
)

data class VibeFlowWeights(
    val moodSimilarity: Float = 0.30f,
    val genreAndSonic: Float = 0.20f,
    val tempoEnergy: Float = 0.15f,
    val userPreference: Float = 0.20f,
    val freshnessDiversity: Float = 0.10f,
    val listeningContext: Float = 0.05f
)

object VibeFlowScorer {

    /**
     * Calculates a deterministic, explainable recommendation score between a candidate track
     * and the current listening context / seed track.
     * Missing features are automatically renormalized so incomplete metadata does not skew predictions.
     */
    fun scoreCandidate(
        candidate: OnlineSongModel,
        candidateProfile: SongMoodProfile,
        seedProfile: SongMoodProfile?,
        userProfile: UserMusicProfile,
        currentVibe: VibeCategory? = null,
        recentPlayedSongIds: Set<String> = emptySet(),
        recentPlayedNormalizedKeys: Set<String> = emptySet(),
        recentArtistsQueue: Set<String> = emptySet(),
        weights: VibeFlowWeights = VibeFlowWeights()
    ): VibeFlowScoreResult {
        var activeWeightSum = 0f
        var totalWeightedScore = 0f

        val explanations = mutableListOf<String>()

        // 1. MOOD SIMILARITY (Target: 30%)
        var moodScore: Float? = null
        if (seedProfile != null && (seedProfile.moodLabels.isNotEmpty() || candidateProfile.moodLabels.isNotEmpty())) {
            val commonMoods = seedProfile.moodLabels.intersect(candidateProfile.moodLabels)
            val unionMoods = seedProfile.moodLabels.union(candidateProfile.moodLabels)
            val jaccard = if (unionMoods.isNotEmpty()) commonMoods.size.toFloat() / unionMoods.size.toFloat() else 0.5f

            var valenceComp = 1.0f
            if (seedProfile.valence != null && candidateProfile.valence != null) {
                valenceComp = 1.0f - abs(seedProfile.valence - candidateProfile.valence)
            }

            moodScore = (jaccard * 0.70f + valenceComp * 0.30f).coerceIn(0.1f, 1.0f)
            totalWeightedScore += (moodScore * weights.moodSimilarity)
            activeWeightSum += weights.moodSimilarity

            if (commonMoods.isNotEmpty()) {
                val moodNames = commonMoods.joinToString(", ") { it.displayName }
                explanations.add("$moodNames mood match")
            }
        }

        // 2. GENRE & SONIC SIMILARITY (Target: 20%)
        var genreScore: Float? = null
        val seedGenre = seedProfile?.genre
        val candGenre = candidateProfile.genre
        if (seedGenre != null && candGenre != null) {
            genreScore = if (seedGenre.equals(candGenre, ignoreCase = true)) 1.0f else 0.40f
            totalWeightedScore += (genreScore * weights.genreAndSonic)
            activeWeightSum += weights.genreAndSonic
            if (genreScore > 0.8f) {
                explanations.add("Sonic match in $candGenre")
            }
        } else if (seedProfile != null) {
            // Sonic acousticness fallback
            if (seedProfile.acousticIntensity != null && candidateProfile.acousticIntensity != null) {
                val diff = abs(seedProfile.acousticIntensity - candidateProfile.acousticIntensity)
                genreScore = (1.0f - diff).coerceIn(0.2f, 1.0f)
                totalWeightedScore += (genreScore * weights.genreAndSonic)
                activeWeightSum += weights.genreAndSonic
            }
        }

        // 3. TEMPO & ENERGY COMPATIBILITY (Target: 15%)
        var tempoEnergyScore: Float? = null
        val seedEnergy = seedProfile?.energy
        val candEnergy = candidateProfile.energy
        if (seedEnergy != null && candEnergy != null) {
            val energyDiff = abs(seedEnergy - candEnergy)
            var teScore = 1.0f - energyDiff

            if (seedProfile.tempoBpm != null && candidateProfile.tempoBpm != null) {
                val bpmDiff = abs(seedProfile.tempoBpm - candidateProfile.tempoBpm)
                val bpmScore = (1.0f - (bpmDiff / 80f)).coerceIn(0.0f, 1.0f)
                teScore = (teScore * 0.6f + bpmScore * 0.4f)
            }

            tempoEnergyScore = teScore.coerceIn(0.1f, 1.0f)
            totalWeightedScore += (tempoEnergyScore * weights.tempoEnergy)
            activeWeightSum += weights.tempoEnergy
        }

        // 4. USER PREFERENCE (Target: 20%)
        var userPrefScore = 0.50f
        val cleanCandidateArtist = candidate.artist.trim()

        // Artist affinity from user profile
        val artistAffinity = userProfile.topArtists.entries.firstOrNull {
            it.key.contains(cleanCandidateArtist, ignoreCase = true) || cleanCandidateArtist.contains(it.key, ignoreCase = true)
        }?.value ?: 0.0f

        userPrefScore += (artistAffinity * 0.35f)

        // Liked track bonus
        val isLiked = userProfile.likedSongIds.contains(candidate.id)
        if (isLiked) {
            userPrefScore += 0.25f
            explanations.add("In your Liked Songs")
        } else if (artistAffinity > 0.2f) {
            explanations.add("Favorite artist ($cleanCandidateArtist)")
        }

        // Skip penalty (negative feedback)
        val skipPenalty = userProfile.skippedArtists.entries.firstOrNull {
            it.key.contains(cleanCandidateArtist, ignoreCase = true) || cleanCandidateArtist.contains(it.key, ignoreCase = true)
        }?.value ?: 0.0f
        if (skipPenalty > 0f) {
            userPrefScore -= (skipPenalty * 0.20f).coerceAtMost(0.40f)
        }

        userPrefScore = userPrefScore.coerceIn(0.05f, 1.0f)
        totalWeightedScore += (userPrefScore * weights.userPreference)
        activeWeightSum += weights.userPreference

        // 5. FRESHNESS & ARTIST DIVERSITY (Target: 10%)
        var diversityScore = 0.70f
        val primaryArtist = candidate.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""
        var isFresh = false

        if (recentArtistsQueue.any { it.equals(primaryArtist, ignoreCase = true) }) {
            diversityScore -= 0.35f // Penalty for repeating recent artist
        } else if (artistAffinity < 0.1f && !isLiked) {
            diversityScore += (userProfile.discoveryPreference * 0.25f)
            isFresh = true
            explanations.add("Fresh recommendation")
        }

        diversityScore = diversityScore.coerceIn(0.1f, 1.0f)
        totalWeightedScore += (diversityScore * weights.freshnessDiversity)
        activeWeightSum += weights.freshnessDiversity

        // 6. LISTENING CONTEXT (Target: 5%)
        var contextScore = 0.50f
        if (currentVibe != null) {
            val vibeCategory = currentVibe
            val moodMatch = when (vibeCategory) {
                VibeCategory.ROMANTIC -> candidateProfile.moodLabels.contains(MoodLabel.ROMANTIC)
                VibeCategory.CHILL -> candidateProfile.moodLabels.contains(MoodLabel.CALM)
                VibeCategory.LOFI -> candidateProfile.moodLabels.contains(MoodLabel.FOCUS) || candidateProfile.moodLabels.contains(MoodLabel.CALM)
                VibeCategory.PARTY, VibeCategory.ENERGETIC -> candidateProfile.moodLabels.contains(MoodLabel.ENERGETIC)
                VibeCategory.WORKOUT -> candidateProfile.moodLabels.contains(MoodLabel.AGGRESSIVE) || candidateProfile.moodLabels.contains(MoodLabel.ENERGETIC)
                VibeCategory.SAD -> candidateProfile.moodLabels.contains(MoodLabel.SAD)
                VibeCategory.NIGHT -> candidateProfile.moodLabels.contains(MoodLabel.LATE_NIGHT) || candidateProfile.moodLabels.contains(MoodLabel.CALM)
                VibeCategory.MORNING -> candidateProfile.moodLabels.contains(MoodLabel.HOPEFUL) || candidateProfile.moodLabels.contains(MoodLabel.CALM)
                else -> true
            }
            contextScore = if (moodMatch) 1.0f else 0.40f
            if (moodMatch) {
                explanations.add("Fits your ${vibeCategory.displayName}")
            }
        }
        totalWeightedScore += (contextScore * weights.listeningContext)
        activeWeightSum += weights.listeningContext

        // DYNAMIC RENORMALIZATION: Scale by active weights sum so missing features do not penalize tracks
        val finalNormalizedScore = if (activeWeightSum > 0f) {
            (totalWeightedScore / activeWeightSum).coerceIn(0.05f, 1.0f)
        } else {
            0.50f
        }

        val moodPercent = ((moodScore ?: finalNormalizedScore) * 100).toInt().coerceIn(50, 99)
        val explanationString = if (explanations.isNotEmpty()) {
            explanations.distinct().take(2).joinToString(" • ")
        } else {
            "${moodPercent}% Vibe Flow match"
        }

        val seedArtistMatch = seedProfile != null && primaryArtist.equals(
            seedProfile.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim(),
            ignoreCase = true
        )

        return VibeFlowScoreResult(
            score = finalNormalizedScore,
            explanation = explanationString,
            moodMatchPercent = moodPercent,
            seedArtistMatch = seedArtistMatch,
            isFreshDiscovery = isFresh
        )
    }
}
