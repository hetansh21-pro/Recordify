package com.audiovault.app.core.vibeflow

/**
 * Structured mood and acoustic profile for a musical track.
 * Missing features are explicitly marked as null / unknown so scoring
 * renormalizes without pretending fabricated audio data is present.
 */
data class SongMoodProfile(
    val trackId: String,
    val title: String,
    val artist: String,
    val genre: String? = null,
    val moodLabels: Set<MoodLabel> = emptySet(),
    val valence: Float? = null,           // 0.0 (sad/gloomy) to 1.0 (happy/euphoric)
    val energy: Float? = null,            // 0.0 (calm/chill) to 1.0 (intense/driving)
    val arousal: Float? = null,           // 0.0 (sleepy/peaceful) to 1.0 (excited/adrenaline)
    val acousticIntensity: Float? = null, // 0.0 (electronic/synthesized) to 1.0 (pure acoustic/unplugged)
    val tempoBpm: Int? = null,            // Beats per minute if known or estimated
    val danceability: Float? = null,      // 0.0 to 1.0 if trustworthy data available
    val acousticness: Float? = null,      // 0.0 to 1.0 if trustworthy data available
    val language: String? = null,
    val releasePeriod: String? = null,    // e.g. "2020s", "2010s", "2000s", "90s", "Retro"
    val instrumentalVocal: VocalType = VocalType.UNKNOWN,
    val confidenceScore: Float = 0.5f,    // Confidence in the profile (0.0 to 1.0)
    val featureSources: Map<String, String> = emptyMap() // Map of feature name to origin source
) {
    val isAcousticEstimated: Boolean get() = acousticIntensity != null
    val isTempoEstimated: Boolean get() = tempoBpm != null
}

enum class MoodLabel(val displayName: String, val emoji: String) {
    HAPPY("Happy", "😊"),
    SAD("Moody & Melancholy", "🌧️"),
    ROMANTIC("Romantic", "❤️"),
    NOSTALGIC("Nostalgic", "📼"),
    CALM("Calm & Peaceful", "🌿"),
    ENERGETIC("Energetic", "⚡"),
    AGGRESSIVE("Aggressive & Powerful", "🔥"),
    HOPEFUL("Hopeful & Inspiring", "✨"),
    FOCUS("Deep Focus", "🎧"),
    LATE_NIGHT("Late Night", "🌙")
}

enum class VocalType {
    VOCAL,
    INSTRUMENTAL,
    UNKNOWN
}
