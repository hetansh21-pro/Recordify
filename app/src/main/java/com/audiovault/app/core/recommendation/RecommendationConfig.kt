package com.audiovault.app.core.recommendation

data class RecommendationConfig(
    // Queue distribution percentages (Sum = 1.0)
    val similarRatio: Float = 0.40f,
    val favoritesRatio: Float = 0.25f,
    val discoveryRatio: Float = 0.20f,
    val trendingRatio: Float = 0.10f,
    val explorationRatio: Float = 0.05f,

    // Recency Decay Parameter (in days)
    val decayDays: Double = 14.0,

    // Diversity & Anti-Repeat Limits (Strict Zero Back-to-Back Same Artist)
    val maxConsecutiveSameArtist: Int = 1,
    val maxArtistCountPerSession: Int = 2,
    val songCooldownMinutes: Long = 60L, // 1 hour anti-repeat cooldown
    val recentRepeatWindowHours: Long = 1L,

    // Event Weights
    val weightPlayStarted: Float = 1.0f,
    val weightPlay25Percent: Float = 2.0f,
    val weightPlay50Percent: Float = 3.0f,
    val weightPlay75Percent: Float = 4.0f,
    val weightPlayCompleted: Float = 5.0f,
    val weightSongReplayed: Float = 6.0f,
    val weightSongLiked: Float = 10.0f,
    val weightAddedToPlaylist: Float = 12.0f,
    val weightSongSearched: Float = 3.0f,
    val weightArtistOpened: Float = 2.0f,
    val weightAlbumOpened: Float = 2.0f,

    // Penalties
    val penaltySongSkipped: Float = -6.0f,
    val penaltyShortPlaySkipped: Float = -8.0f, // Skipped in < 10 seconds
    val penaltySongUnliked: Float = -5.0f,
    val penaltyRemovedFromPlaylist: Float = -3.0f,

    // Queue Auto-refill Threshold
    val queueRefillThreshold: Int = 2,
    val queueBatchSize: Int = 10
)
