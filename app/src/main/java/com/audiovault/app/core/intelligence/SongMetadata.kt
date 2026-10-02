package com.audiovault.app.core.intelligence

/**
 * Metadata representing the currently playing song/track from active media sessions,
 * with intelligent advertisement and promo identification.
 */
data class SongMetadata(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val sourcePackage: String? = null,
    val lyrics: String? = null,
    val albumArtPath: String? = null,
    val isExplicitAd: Boolean = false
) {
    val displayTitle: String
        get() = if (!artist.isNullOrBlank()) "$artist - $title" else title

    /**
     * Identifies if this track is an audio advertisement, sponsor message, or commercial break.
     */
    val isAdvertisement: Boolean
        get() {
            if (isExplicitAd) return true
            val t = title.trim().lowercase()
            val a = (artist ?: "").trim().lowercase()
            val alb = (album ?: "").trim().lowercase()

            val adKeywords = listOf(
                "advertisement", "spotify ad", "audio ad", "sponsored", "commercial",
                "promo", "promotional", "upgrade to premium", "1 of 1", "1 of 2", "2 of 2",
                "1 of 3", "2 of 3", "3 of 3", "ad •", "ad 1 of", "ad 2 of", "ad 3 of"
            )
            if (adKeywords.any { t.contains(it) || a.contains(it) || alb.contains(it) }) return true

            // Exact and common ad titles across Spotify, YouTube Music, Radio, JioSaavn, Wynk, Gaana, etc.
            if (t == "advertisement" || t == "ad" || t == "commercial" || t == "audio ad" || t == "sponsored") return true
            if (a == "advertisement" || a == "ad" || a == "commercial" || a == "sponsored") return true

            // Prefix and keyword patterns
            if (t.startsWith("ad:") || t.startsWith("ad -") || t.startsWith("ad ") || t.startsWith("[ad]") || t.startsWith("(ad)")) return true
            if (a.startsWith("ad:") || a.startsWith("ad -") || a.startsWith("ad ") || a.startsWith("[ad]") || a.startsWith("(ad)")) return true
            if (t.contains("advertisement") || t.contains("sponsored") || t.contains("promotional") || t.contains("upgrade to premium")) return true
            if (t.contains("spotify") && (a.isBlank() || a.contains("spotify"))) return true
            if (a.contains("spotify free") || a.contains("spotify commercial") || a.contains("spotify ad")) return true
            if (alb.contains("advertisement") || alb.contains("commercial")) return true

            return false
        }

    val isIdentified: Boolean
        get() = title.isNotBlank() && !title.equals("Unknown", ignoreCase = true) && !isAdvertisement
}

