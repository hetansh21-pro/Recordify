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
    fun getInstantSeedQueue(
        seed: QueueItem,
        preferredLanguages: Set<String> = emptySet(),
        userCountry: String = "IN"
    ): List<QueueItem> {
        val era = detectEra(seed.title, seed.artist, seed.album)
        val vibe = detectVibeType(seed.title, seed.artist, seed.album)
        val isEnglishAllowed = preferredLanguages.any { it.equals("English", ignoreCase = true) }

        val curatedPool = CuratedAlbumCatalog.curatedAlbums.flatMap { it.songs }

        val sourceSongs: List<OnlineSongModel> = when {
            // Old song played -> strictly old/retro/90s songs
            era == SongEra.OLD_RETRO || vibe == SongVibeType.CLASSIC_RETRO -> {
                (TvCatalogFallbacks.golden90sBollywood + curatedPool
                    .filter { detectEra(it.title, it.artist) == SongEra.OLD_RETRO }).shuffled()
            }
            // Punjabi/Party song played -> strictly Punjabi/Party hits
            vibe == SongVibeType.PARTY_PUNJABI -> {
                (TvCatalogFallbacks.punjabiParty + curatedPool
                    .filter { detectVibeType(it.title, it.artist) == SongVibeType.PARTY_PUNJABI }).shuffled()
            }
            // Chill/Lo-Fi song played -> strictly Chill/Lo-Fi songs
            vibe == SongVibeType.CHILL_LOFI -> {
                TvCatalogFallbacks.chillLofi.shuffled()
            }
            // Romantic song played -> strictly Romantic songs
            vibe == SongVibeType.ROMANTIC -> {
                (TvCatalogFallbacks.romanticMelodies + curatedPool
                    .filter { detectVibeType(it.title, it.artist) == SongVibeType.ROMANTIC }).shuffled()
            }
            // New / Modern song played -> strictly New / Trending songs
            era == SongEra.NEW_MODERN -> {
                (TvCatalogFallbacks.newReleases + TvCatalogFallbacks.topHitsIndia + curatedPool
                    .filter { detectEra(it.title, it.artist) == SongEra.NEW_MODERN }).shuffled()
            }
            // General fallback
            else -> {
                (TvCatalogFallbacks.topHitsIndia + TvCatalogFallbacks.newReleases).shuffled()
            }
        }

        // Filter out the seed track itself, enforce era compatibility, and STRICTLY exclude English if not preferred
        val seedTitleNorm = com.audiovault.app.core.intelligence.TrackMatcher.normalize(seed.title)
        val seedArtistNorm = seed.artist.lowercase().trim()

        val filteredSongs = sourceSongs
            .filter { song ->
                val sTitleNorm = com.audiovault.app.core.intelligence.TrackMatcher.normalize(song.title)
                val sArtist = song.artist.lowercase().trim()
                if (song.id == seed.id || sTitleNorm == seedTitleNorm ||
                    (seedTitleNorm.length >= 4 && sTitleNorm.length >= 4 && com.audiovault.app.core.intelligence.TrackMatcher.similarity(sTitleNorm, seedTitleNorm) >= 0.80)) {
                    return@filter false
                }
                // If user did not select English, strictly filter out any English/Western songs
                if (!isEnglishAllowed && preferredLanguages.isNotEmpty()) {
                    val sLang = song.language ?: com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer.inferLanguage("${song.title} ${song.artist}")
                    if (sLang?.equals("English", ignoreCase = true) == true ||
                        com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer.isEnglishArtistOrTitle(song.title, song.artist)) {
                        return@filter false
                    }
                }
                true
            }
            .distinctBy { com.audiovault.app.core.intelligence.TrackMatcher.normalize(it.title) }

        // Apply artist diversity: max 2 songs per artist, zero consecutive songs by the same artist
        val diverseInstantList = mutableListOf<OnlineSongModel>()
        val artistCount = mutableMapOf<String, Int>()
        var lastArtist = seedArtistNorm.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""

        for (song in filteredSongs) {
            val primaryArtist = song.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim()?.lowercase() ?: ""
            if (primaryArtist.isNotBlank() && primaryArtist == lastArtist) continue
            val count = artistCount.getOrDefault(primaryArtist, 0)
            if (count >= 2 && primaryArtist.isNotBlank()) continue

            diverseInstantList.add(song)
            if (primaryArtist.isNotBlank()) {
                artistCount[primaryArtist] = count + 1
                lastArtist = primaryArtist
            }
            if (diverseInstantList.size >= 12) break
        }

        // Fallback to fill up to 12 if needed without duplicate titles
        if (diverseInstantList.size < 12) {
            for (song in filteredSongs) {
                if (diverseInstantList.none { it.id == song.id }) {
                    diverseInstantList.add(song)
                    if (diverseInstantList.size >= 12) break
                }
            }
        }

        return diverseInstantList.map { QueueItem.fromOnlineSong(it, "Smart Auto-Queue • ${vibe.name.replace("_", " ")}") }
    }

    /**
     * Resolves related artists matching the seed artist's vocal texture, genre, and emotional vibe.
     * Modeled after Spotify's related-artist graph (e.g. Arijit Singh -> KK, Atif Aslam, Mustafa Zahid, Jasleen Royal).
     */
    fun getAffinityArtists(primaryArtist: String, vibe: SongVibeType): List<String> {
        val lower = primaryArtist.lowercase()
        return when {
            // Arijit Singh cluster
            lower.contains("arijit") -> listOf("KK", "Atif Aslam", "Mustafa Zahid", "Mohit Chauhan", "Jasleen Royal", "Mohammed Irfan", "Ankit Tiwari", "Mithoon", "Shreya Ghoshal")
            // KK cluster
            lower.contains("kk") || lower.contains("k.k.") -> listOf("Atif Aslam", "Mohit Chauhan", "Shaan", "Sonu Nigam", "Mustafa Zahid", "Lucky Ali", "Arijit Singh")
            // Atif Aslam cluster
            lower.contains("atif") -> listOf("KK", "Mustafa Zahid", "Rahat Fateh Ali Khan", "Arijit Singh", "Mohit Chauhan", "Bilal Saeed")
            // Mohit Chauhan cluster
            lower.contains("mohit") -> listOf("Lucky Ali", "Papon", "KK", "Atif Aslam", "Javed Ali", "Arijit Singh")
            // Sonu Nigam cluster
            lower.contains("sonu nigam") -> listOf("Shaan", "Udit Narayan", "Kumar Sanu", "Abhijeet", "Alka Yagnik", "KK")
            // Shreya Ghoshal cluster
            lower.contains("shreya") -> listOf("Sunidhi Chauhan", "Monali Thakur", "Palak Muchhal", "Alka Yagnik", "Shilpa Rao", "Jasleen Royal")
            // Jubin Nautiyal cluster
            lower.contains("jubin") -> listOf("Arijit Singh", "Rochak Kohli", "Stebin Ben", "Akhil Sachdeva", "B Praak")
            // B Praak / Jaani cluster
            lower.contains("praak") || lower.contains("jaani") -> listOf("Hardy Sandhu", "Ammy Virk", "Akhil Sachdeva", "Arijit Singh")
            // Indie / Acoustic / Lo-fi cluster
            lower.contains("prateek") || lower.contains("kuhad") -> listOf("Anuv Jain", "Jasleen Royal", "The Local Train", "When Chai Met Toast", "Zaeden")
            lower.contains("anuv") || lower.contains("jain") -> listOf("Prateek Kuhad", "Zaeden", "The Local Train", "Osho Jain", "Jasleen Royal")
            // Golden Retro clusters
            lower.contains("kishore") -> listOf("Lata Mangeshkar", "Mohd Rafi", "Mukesh", "Asha Bhosle", "Manna Dey", "R.D. Burman")
            lower.contains("lata") -> listOf("Mohd Rafi", "Kishore Kumar", "Asha Bhosle", "Mukesh", "Geeta Dutt", "Hemant Kumar")
            lower.contains("rafi") -> listOf("Kishore Kumar", "Mukesh", "Manna Dey", "Lata Mangeshkar", "Asha Bhosle")
            lower.contains("kumar sanu") -> listOf("Udit Narayan", "Alka Yagnik", "Anuradha Paudwal", "Abhijeet", "Sonu Nigam")
            lower.contains("udit") -> listOf("Kumar Sanu", "Alka Yagnik", "Kavita Krishnamurthy", "Abhijeet", "Sonu Nigam")
            // Punjabi / High Energy clusters
            lower.contains("dhillon") -> listOf("Shubh", "Karan Aujla", "Diljit Dosanjh", "Sidhu Moose Wala", "Gurinder Gill")
            lower.contains("diljit") -> listOf("Karan Aujla", "Sidhu Moose Wala", "AP Dhillon", "Ammy Virk", "Badshah", "Guru Randhawa")
            lower.contains("karan aujla") -> listOf("Diljit Dosanjh", "Shubh", "Sidhu Moose Wala", "AP Dhillon")
            lower.contains("badshah") -> listOf("Honey Singh", "Raftaar", "Ikka", "Guru Randhawa", "Divine")
            lower.contains("divine") -> listOf("Naezy", "Seedhe Maut", "KR\$NA", "Raftaar", "MC Stan")
            lower.contains("seedhe maut") -> listOf("KR\$NA", "Prabh Deep", "Rawal", "Divine")
            // Global pop / western clusters
            lower.contains("taylor") -> listOf("Olivia Rodrigo", "Gracie Abrams", "Sabrina Carpenter", "Billie Eilish")
            lower.contains("weeknd") -> listOf("Post Malone", "Drake", "Kendrick Lamar", "Bruno Mars")
            lower.contains("sheeran") -> listOf("Shawn Mendes", "Charlie Puth", "James Arthur", "Lewis Capaldi")
            // Fallback by vibe
            vibe == SongVibeType.ROMANTIC -> listOf("KK", "Atif Aslam", "Mohit Chauhan", "Jasleen Royal", "Mustafa Zahid", "Mohammed Irfan", "Ankit Tiwari")
            vibe == SongVibeType.CLASSIC_RETRO -> listOf("Kishore Kumar", "Mohd Rafi", "Lata Mangeshkar", "Mukesh", "Kumar Sanu", "Udit Narayan")
            vibe == SongVibeType.PARTY_PUNJABI -> listOf("Diljit Dosanjh", "Karan Aujla", "AP Dhillon", "Badshah", "Sidhu Moose Wala")
            vibe == SongVibeType.CHILL_LOFI -> listOf("Anuv Jain", "Prateek Kuhad", "Jasleen Royal", "The Local Train", "Zaeden")
            else -> listOf("Arijit Singh", "Sachin-Jigar", "Pritam", "Shreya Ghoshal", "Vishal Mishra")
        }
    }

    /**
     * Generates intelligent, era-aware and language-connected search queries for background replenishment.
     * Incorporates artist affinity and mood-matching clusters for maximum queue diversity.
     */
    fun generateIntelligentSearchQueries(seed: QueueItem, targetLanguage: String): List<String> {
        val primaryArtist = seed.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""
        val era = detectEra(seed.title, seed.artist, seed.album)
        val vibe = detectVibeType(seed.title, seed.artist, seed.album)
        val lang = targetLanguage.ifBlank { "Hindi" }

        val queries = mutableListOf<String>()

        if (!seed.album.isNullOrBlank() && !seed.album.equals("Recordify", ignoreCase = true) && !seed.album.equals("Soundtrack", ignoreCase = true)) {
            queries.add("${seed.album} songs")
        }
        if (primaryArtist.isNotBlank()) {
            queries.add("$primaryArtist best songs")
        }

        // Weave in 2-3 affinity artists matching the seed artist's vibe to guarantee recommendation diversity
        val affinityArtists = getAffinityArtists(primaryArtist, vibe).shuffled().take(3)
        for (affinity in affinityArtists) {
            queries.add("$affinity $lang top songs")
        }

        when {
            era == SongEra.OLD_RETRO || vibe == SongVibeType.CLASSIC_RETRO -> {
                queries.add("90s $lang romantic evergreen hits")
                queries.add("Golden 70s 80s Kishore Kumar Lata hits")
            }

            vibe == SongVibeType.PARTY_PUNJABI -> {
                queries.add("Punjabi top hits dance superhit")
                queries.add("High energy party bhangra songs")
            }

            vibe == SongVibeType.CHILL_LOFI -> {
                queries.add("Chill lofi acoustic $lang songs")
                queries.add("Acoustic study relax beats $lang")
            }

            vibe == SongVibeType.ROMANTIC -> {
                queries.add("Romantic $lang love melodies superhit")
                queries.add("Soulful romantic $lang songs")
            }

            era == SongEra.NEW_MODERN -> {
                queries.add("Latest $lang chartbusters 2024 2025")
                queries.add("Trending new $lang songs viral")
            }

            else -> {
                queries.add("$lang superhit songs")
            }
        }

        return queries.distinct()
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
