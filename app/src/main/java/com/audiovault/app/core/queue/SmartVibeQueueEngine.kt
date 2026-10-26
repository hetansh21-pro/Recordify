package com.audiovault.app.core.queue

import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.tv.TvCatalogFallbacks

enum class SongEra {
    OLD_RETRO,     // 60s, 70s, 80s, 90s, early 2000s classics
    NEW_MODERN,    // 2018 - 2026 current/recent chartbusters & viral tracks
    NEUTRAL
}

enum class SongVibeType {
    ROMANTIC,
    PARTY_PUNJABI,
    CHILL_LOFI,
    CLASSIC_RETRO,
    DEVOTIONAL,
    GENERAL
}

/**
 * Intelligent Era & Vibe Queue Engine for Recordify TV.
 * Ensures strict contextual continuity:
 * - When playing an OLD / RETRO / 90s song -> Next songs in queue are strictly OLD / RETRO / 90s classics.
 * - When playing a NEW / TRENDING / MODERN song -> Next songs in queue are strictly NEW / MODERN chartbusters.
 * - When playing a ROMANTIC song -> Next songs are strictly ROMANTIC melodies.
 * - When playing a PUNJABI / PARTY song -> Next songs are strictly PUNJABI & high-energy party hits.
 * - When playing a LO-FI / CHILL song -> Next songs are strictly LO-FI / acoustic study beats.
 */
object SmartVibeQueueEngine {

    private val RETRO_ARTISTS = listOf(
        "kishore kumar", "lata mangeshkar", "mohammed rafi", "mohd rafi", "rafi",
        "asha bhosle", "r.d. burman", "rd burman", "mukesh", "manna dey",
        "hemant kumar", "mahendra kapoor", "talat mahmood", "geeta dutt",
        "kumar sanu", "alka yagnik", "udit narayan", "anuradha paudwal",
        "sadhana sargam", "kavita krishnamurthy", "hariharan", "abhijeet",
        "pankaj udhas", "jagjit singh", "babul supriyo", "vinod rathod",
        "roop kumar rathod", "jatin-lalit", "nadeem-shravan", "anu malik",
        "laxmikant-pyarelal", "kalyanji-anandji", "bhupinder singh", "anup jalota"
    )

    private val MODERN_ARTISTS = listOf(
        "arijit singh", "badshah", "diljit dosanjh", "karan aujla", "ap dhillon",
        "sidhu moose wala", "sidhu", "king", "b praak", "jasleen royal",
        "mc stan", "divine", "sachin-jigar", "sachin jigar", "anirudh",
        "tanishk bagchi", "neha kakkar", "jubin nautiyal", "shilpa rao",
        "shreya ghoshal", "sushin shyam", "bhupinder babbal", "madhubanti bagchi",
        "stebin ben", "darshan raval", "vishal mishra", "tulsi kumar",
        "armaan malik", "sachet tandon", "parampara", "oaff", "savera",
        "raghav chaitanya", "raftaar", "emiway", "guru randhawa", "harrdy sandhu",
        "shubh", "ammy virk", "yo yo honey singh", "kanika kapoor", "tony kakkar"
    )

    private val RETRO_KEYWORDS = listOf(
        "90s", "80s", "70s", "60s", "classic", "retro", "evergreen", "vintage",
        "purane", "purana", "sadabahar", "nostalgic", "nostalgia", "ghazal",
        "jhankar", "ddlj", "saajan", "mohra", "dilwale dulhania",
        "kabhie kabhie", "sholay", "blackmail", "kanyadaan", "woh kaun thi",
        "jo jeeta wohi sikandar", "old is gold", "golden era"
    )

    private val MODERN_KEYWORDS = listOf(
        "2024", "2025", "2026", "2023", "2022", "2021", "2020", "2019", "2018", "aashiqui 2",
        "stree 2", "pushpa", "animal", "jawan", "pathaan", "dunki", "bad newz", "chhaava", "devara",
        "kalki", "fighter", "bhool bhulaiyaa", "shershaah", "brahmastra",
        "kabir singh", "tauba tauba", "aaj ki raat", "angaaron", "illuminati",
        "arjan vailly", "satranga", "chaleya", "o maahi", "viral", "trending", "latest"
    )

    private val ROMANTIC_KEYWORDS = listOf(
        "love", "romantic", "ishq", "pyaar", "dil", "mohabbat", "humsafar",
        "tum hi ho", "kesariya", "jeene laga hoon", "sun saathiya", "kaun tujhe",
        "hasi ban gaye", "tera ban jaunga", "raataan lambiyan", "hawayein", "saathiya",
        "deewana", "pehla nasha", "dheere dheere", "chura ke dil", "mera dil",
        "bahon ke darmiyan", "heeriye", "apna bana le", "pehle bhi main"
    )

    private val PUNJABI_PARTY_KEYWORDS = listOf(
        "punjabi", "bhangra", "party", "dance", "dj", "club", "softly",
        "born to shine", "lover", "295", "so high", "white brown black",
        "tauba tauba", "proper patola", "gallan goodiyaan", "nach", "dhol",
        "bass", "dhamaka", "energetic", "workout"
    )

    private val CHILL_LOFI_KEYWORDS = listOf(
        "lofi", "lo-fi", "chill", "acoustic", "study", "ambient", "sleep",
        "unplugged", "iktara", "kabira", "kun faya kun", "safarnama",
        "kho gaye hum kahan", "phir le aya dil"
    )

    /**
     * Determines whether a track is an Old/Retro classic or Modern/Recent hit.
     */
    fun detectEra(title: String, artist: String, album: String? = null, yearStr: String? = null): SongEra {
        val lowerArtist = artist.lowercase()
        val lowerText = "${title.lowercase()} $lowerArtist ${(album ?: "").lowercase()}"

        val year = yearStr?.toIntOrNull()
        if (year != null) {
            if (year < 2008) return SongEra.OLD_RETRO
            if (year >= 2018) return SongEra.NEW_MODERN
        }

        // 1. Check known modern artists (Arijit Singh, Sachin-Jigar, Shreya Ghoshal, Badshah, etc.)
        if (MODERN_ARTISTS.any { lowerArtist.contains(it) }) return SongEra.NEW_MODERN

        // 2. Check known retro artists (Kishore Kumar, Lata Mangeshkar, Mohd Rafi, Kumar Sanu, etc.)
        if (RETRO_ARTISTS.any { lowerArtist.contains(it) }) return SongEra.OLD_RETRO

        // 3. Check title/album modern keywords
        if (MODERN_KEYWORDS.any { lowerText.contains(it) }) return SongEra.NEW_MODERN

        // 4. Check title/album retro keywords
        if (RETRO_KEYWORDS.any { lowerText.contains(it) }) return SongEra.OLD_RETRO

        return SongEra.NEUTRAL
    }

    /**
     * Determines the primary musical vibe of the track.
     */
    fun detectVibeType(title: String, artist: String, album: String? = null, yearStr: String? = null): SongVibeType {
        val lowerText = "${title.lowercase()} ${artist.lowercase()} ${(album ?: "").lowercase()}"

        val era = detectEra(title, artist, album, yearStr)
        if (era == SongEra.OLD_RETRO) {
            return SongVibeType.CLASSIC_RETRO
        }

        if (PUNJABI_PARTY_KEYWORDS.any { lowerText.contains(it) }) {
            return SongVibeType.PARTY_PUNJABI
        }

        if (CHILL_LOFI_KEYWORDS.any { lowerText.contains(it) }) {
            return SongVibeType.CHILL_LOFI
        }

        if (ROMANTIC_KEYWORDS.any { lowerText.contains(it) }) {
            return SongVibeType.ROMANTIC
        }

        return SongVibeType.GENERAL
    }

    /**
     * Instantly seeds the upcoming queue with 10+ matching tracks when a user manually starts a song,
     * ensuring immediate zero-latency era and vibe consistency.
     */
    fun getInstantSeedQueue(seed: QueueItem): List<QueueItem> {
        val era = detectEra(seed.title, seed.artist, seed.album)
        val vibe = detectVibeType(seed.title, seed.artist, seed.album)

        val sourceSongs: List<OnlineSongModel> = when {
            // Old song played -> strictly old/retro/90s songs
            era == SongEra.OLD_RETRO || vibe == SongVibeType.CLASSIC_RETRO -> {
                TvCatalogFallbacks.golden90sBollywood
            }
            // Punjabi/Party song played -> strictly Punjabi/Party hits
            vibe == SongVibeType.PARTY_PUNJABI -> {
                TvCatalogFallbacks.punjabiParty
            }
            // Chill/Lo-Fi song played -> strictly Chill/Lo-Fi songs
            vibe == SongVibeType.CHILL_LOFI -> {
                TvCatalogFallbacks.chillLofi
            }
            // Romantic song played -> strictly Romantic songs
            vibe == SongVibeType.ROMANTIC -> {
                TvCatalogFallbacks.romanticMelodies
            }
            // New / Modern song played -> strictly New / Trending songs
            era == SongEra.NEW_MODERN -> {
                TvCatalogFallbacks.newReleases + TvCatalogFallbacks.topHitsIndia
            }
            // General fallback
            else -> {
                TvCatalogFallbacks.topHitsIndia
            }
        }

        // Filter out the seed track itself and map to QueueItem
        val seedTitleNorm = seed.title.lowercase().trim()
        val seedArtistNorm = seed.artist.lowercase().trim()

        return sourceSongs
            .filter { song ->
                val sTitle = song.title.lowercase().trim()
                val sArtist = song.artist.lowercase().trim()
                song.id != seed.id && !sTitle.contains(seedTitleNorm) && !seedTitleNorm.contains(sTitle)
            }
            .distinctBy { it.title.lowercase().trim() }
            .take(12)
            .map { QueueItem.fromOnlineSong(it, "Smart Auto-Queue • ${vibe.name.replace("_", " ")}") }
    }

    /**
     * Generates intelligent, era-aware search queries for network background replenishment.
     */
    fun generateIntelligentSearchQueries(seed: QueueItem, targetLanguage: String): List<String> {
        val primaryArtist = seed.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""
        val era = detectEra(seed.title, seed.artist, seed.album)
        val vibe = detectVibeType(seed.title, seed.artist, seed.album)

        val queries = mutableListOf<String>()

        when {
            era == SongEra.OLD_RETRO || vibe == SongVibeType.CLASSIC_RETRO -> {
                if (primaryArtist.isNotBlank()) {
                    queries.add("$primaryArtist 90s evergreen hits")
                    queries.add("$primaryArtist golden retro classics")
                }
                queries.add("90s Bollywood romantic evergreen hits")
                queries.add("Golden 70s 80s Kishore Kumar Lata hits")
                queries.add("Evergreen old Hindi songs superhit")
            }

            vibe == SongVibeType.PARTY_PUNJABI -> {
                if (primaryArtist.isNotBlank()) {
                    queries.add("$primaryArtist Punjabi party hits")
                }
                queries.add("Punjabi top hits dance superhit")
                queries.add("High energy party bhangra songs")
            }

            vibe == SongVibeType.CHILL_LOFI -> {
                if (primaryArtist.isNotBlank()) {
                    queries.add("$primaryArtist chill acoustic")
                }
                queries.add("Chill lofi acoustic Hindi songs")
                queries.add("Acoustic study relax beats")
            }

            vibe == SongVibeType.ROMANTIC -> {
                if (primaryArtist.isNotBlank()) {
                    queries.add("$primaryArtist romantic love songs")
                }
                queries.add("Romantic Hindi love melodies superhit")
                queries.add("Soulful romantic Bollywood songs")
            }

            era == SongEra.NEW_MODERN -> {
                if (primaryArtist.isNotBlank()) {
                    queries.add("$primaryArtist latest trending songs")
                }
                queries.add("Latest Bollywood chartbusters 2024 2025")
                queries.add("Trending new Hindi songs viral")
            }

            else -> {
                if (primaryArtist.isNotBlank()) {
                    queries.add("$primaryArtist $targetLanguage top songs")
                }
                queries.add("Bollywood $targetLanguage superhit songs")
            }
        }

        return queries
    }

    /**
     * Validates whether a candidate track is an era and vibe match for the current seed track.
     * Prevents modern songs from polluting an old-songs queue, and vintage tracks from polluting a modern queue.
     */
    fun isCandidateEraCompatible(seed: QueueItem, candidateTitle: String, candidateArtist: String, candidateAlbum: String?): Boolean {
        val seedEra = detectEra(seed.title, seed.artist, seed.album)
        val candEra = detectEra(candidateTitle, candidateArtist, candidateAlbum)

        // Rule 1: If seed is Old/Retro, strictly reject Modern tracks
        if (seedEra == SongEra.OLD_RETRO && candEra == SongEra.NEW_MODERN) {
            return false
        }

        // Rule 2: If seed is Modern/New, strictly reject Old/Retro tracks
        if (seedEra == SongEra.NEW_MODERN && candEra == SongEra.OLD_RETRO) {
            return false
        }

        return true
    }
}
