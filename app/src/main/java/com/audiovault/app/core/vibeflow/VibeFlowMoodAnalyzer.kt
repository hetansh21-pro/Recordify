package com.audiovault.app.core.vibeflow

import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.data.local.entity.RecordingEntity
import java.util.Calendar

/**
 * Pluggable contract for extracting or predicting acoustic features.
 * Default implementation uses lawful metadata analysis; can be swapped
 * with an on-device TFLite model or remote audio-analysis provider later.
 */
interface AudioAnalysisProvider {
    fun analyze(trackId: String, title: String, artist: String, album: String?): SongMoodProfile?
}

object VibeFlowMoodAnalyzer {

    private var externalProvider: AudioAnalysisProvider? = null

    fun setExternalProvider(provider: AudioAnalysisProvider?) {
        externalProvider = provider
    }

    /**
     * Extracts a structured SongMoodProfile from an OnlineSongModel.
     */
    fun analyze(song: OnlineSongModel): SongMoodProfile {
        // 1. Try external provider if available
        externalProvider?.analyze(song.id, song.title, song.artist, song.album)?.let {
            return it
        }

        return analyzeFromMetadata(
            trackId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            lyrics = song.lyrics,
            year = song.year,
            language = song.language,
            durationMs = song.durationMs
        )
    }

    /**
     * Extracts a structured SongMoodProfile from a local RecordingEntity.
     */
    fun analyze(recording: RecordingEntity): SongMoodProfile {
        externalProvider?.analyze(recording.id.toString(), recording.title, recording.artist ?: "", recording.album)?.let {
            return it
        }

        return analyzeFromMetadata(
            trackId = recording.id.toString(),
            title = recording.title,
            artist = recording.artist ?: "Unknown Artist",
            album = recording.album,
            lyrics = recording.lyrics,
            year = null,
            language = null,
            durationMs = recording.durationMs
        )
    }

    fun analyzeFromMetadata(
        trackId: String,
        title: String,
        artist: String,
        album: String? = null,
        lyrics: String? = null,
        year: String? = null,
        language: String? = null,
        durationMs: Long = 0L
    ): SongMoodProfile {
        val combinedText = "${title.lowercase()} ${artist.lowercase()} ${(album ?: "").lowercase()} ${(lyrics ?: "").lowercase()}"
        val moods = mutableSetOf<MoodLabel>()
        val sources = mutableMapOf<String, String>()

        var valence: Float? = null
        var energy: Float? = null
        var arousal: Float? = null
        var acousticIntensity: Float? = null
        var estimatedBpm: Int? = null
        var confidence = 0.55f

        // Detection rules based on rich musical keywords across Hindi, English, Punjabi, etc.
        val hasLofi = combinedText.contains("lofi") || combinedText.contains("lo-fi") || combinedText.contains("slowed") || combinedText.contains("chillhop")
        val hasRomantic = combinedText.contains("love") || combinedText.contains("romantic") || combinedText.contains("ishq") || combinedText.contains("pyaar") || combinedText.contains("dil") || combinedText.contains("humsafar") || combinedText.contains("sweetheart")
        val hasSad = combinedText.contains("sad") || combinedText.contains("dard") || combinedText.contains("broken") || combinedText.contains("judaai") || combinedText.contains("cry") || combinedText.contains("heartbreak") || combinedText.contains("alone")
        val hasParty = combinedText.contains("party") || combinedText.contains("dance") || combinedText.contains("remix") || combinedText.contains("dj") || combinedText.contains("club") || combinedText.contains("dhamaka") || combinedText.contains("bhangra")
        val hasWorkout = combinedText.contains("gym") || combinedText.contains("workout") || combinedText.contains("bass") || combinedText.contains("power") || combinedText.contains("motivation") || combinedText.contains("beast")
        val hasAcoustic = combinedText.contains("acoustic") || combinedText.contains("unplugged") || combinedText.contains("reprise") || combinedText.contains("piano") || combinedText.contains("guitar")
        val hasFocus = combinedText.contains("study") || combinedText.contains("focus") || combinedText.contains("ambient") || combinedText.contains("peace") || combinedText.contains("calm")
        val hasNostalgic = combinedText.contains("retro") || combinedText.contains("classic") || combinedText.contains("90s") || combinedText.contains("golden") || combinedText.contains("purane") || combinedText.contains("memories")

        if (hasRomantic) {
            moods.add(MoodLabel.ROMANTIC)
            valence = 0.75f
            energy = 0.50f
            arousal = 0.60f
            sources["mood_romantic"] = "title_keyword"
        }

        if (hasSad) {
            moods.add(MoodLabel.SAD)
            valence = (valence ?: 0.5f) * 0.4f // Pull down valence
            energy = (energy ?: 0.5f) * 0.5f
            arousal = 0.35f
            sources["mood_sad"] = "title_keyword"
        }

        if (hasParty || hasWorkout) {
            if (hasParty) moods.add(MoodLabel.ENERGETIC)
            if (hasWorkout) moods.add(MoodLabel.AGGRESSIVE)
            valence = (valence ?: 0.5f).coerceAtLeast(0.70f)
            energy = 0.88f
            arousal = 0.90f
            estimatedBpm = 128
            sources["mood_energetic"] = "dance_keyword"
        }

        if (hasLofi || hasFocus) {
            moods.add(MoodLabel.FOCUS)
            moods.add(MoodLabel.CALM)
            energy = 0.35f
            arousal = 0.25f
            valence = valence ?: 0.60f
            estimatedBpm = 85
            sources["mood_focus"] = "lofi_keyword"
        }

        if (hasAcoustic) {
            moods.add(MoodLabel.CALM)
            acousticIntensity = 0.85f
            energy = (energy ?: 0.5f).coerceAtMost(0.45f)
            sources["acousticIntensity"] = "acoustic_keyword"
        }

        if (hasNostalgic) {
            moods.add(MoodLabel.NOSTALGIC)
            sources["mood_nostalgic"] = "retro_keyword"
        }

        // Release period classification
        val releasePeriod = year?.toIntOrNull()?.let { yr ->
            when {
                yr >= 2020 -> "2020s"
                yr in 2010..2019 -> "2010s"
                yr in 2000..2009 -> "2000s"
                yr in 1990..1999 -> "90s"
                else -> "Retro"
            }
        }

        // Vocal/Instrumental hint
        val instrumental = when {
            combinedText.contains("instrumental") || combinedText.contains("karaoke") || combinedText.contains("lofi beats") -> VocalType.INSTRUMENTAL
            lyrics != null && lyrics.length > 50 -> VocalType.VOCAL
            else -> VocalType.UNKNOWN
        }

        // Default fallbacks if no strong keywords detected
        if (moods.isEmpty()) {
            moods.add(MoodLabel.HOPEFUL)
            valence = 0.60f
            energy = 0.55f
            arousal = 0.50f
            confidence = 0.35f // Explicitly lower confidence when relying on default fallback
            sources["fallback"] = "default_neutral"
        } else {
            confidence = (0.50f + (moods.size * 0.12f)).coerceAtMost(0.92f)
        }

        return SongMoodProfile(
            trackId = trackId,
            title = title,
            artist = artist,
            genre = inferGenre(combinedText),
            moodLabels = moods,
            valence = valence,
            energy = energy,
            arousal = arousal,
            acousticIntensity = acousticIntensity,
            tempoBpm = estimatedBpm,
            danceability = if (hasParty) 0.85f else if (hasSad) 0.30f else null,
            acousticness = acousticIntensity,
            language = language ?: inferLanguage(combinedText),
            releasePeriod = releasePeriod,
            instrumentalVocal = instrumental,
            confidenceScore = confidence,
            featureSources = sources
        )
    }

    private fun inferGenre(text: String): String? {
        return when {
            text.contains("bollywood") || text.contains("filmi") || text.contains("hindi") -> "Bollywood"
            text.contains("punjabi") || text.contains("bhangra") -> "Punjabi"
            text.contains("pop") -> "Pop"
            text.contains("lofi") || text.contains("lo-fi") -> "Lo-Fi"
            text.contains("acoustic") || text.contains("indie") -> "Indie Acoustic"
            text.contains("rock") -> "Rock"
            text.contains("edm") || text.contains("remix") -> "Electronic/Dance"
            text.contains("hip hop") || text.contains("rap") -> "Hip-Hop"
            text.contains("ghazal") || text.contains("sufi") -> "Sufi/Ghazal"
            else -> null
        }
    }

    fun inferLanguage(rawText: String): String? {
        val text = rawText.lowercase().trim()

        // 1. Unicode Script Range Detection
        for (ch in rawText) {
            when (ch) {
                in '\u0A80'..'\u0AFF' -> return "Gujarati"
                in '\u0900'..'\u097F' -> return "Hindi"
                in '\u0A00'..'\u0A7F' -> return "Punjabi"
                in '\u0B80'..'\u0BFF' -> return "Tamil"
                in '\u0C00'..'\u0C7F' -> return "Telugu"
                in '\u0980'..'\u09FF' -> return "Bengali"
                in '\u0D00'..'\u0D7F' -> return "Malayalam"
                in '\u0C80'..'\u0CFF' -> return "Kannada"
                in '\u0600'..'\u06FF' -> return "Urdu"
                in '\uAC00'..'\uD7AF' -> return "Korean"
                in '\u3040'..'\u30FF' -> return "Japanese"
            }
        }

        // 2. Keyword & Artist Catalogue Detection
        return when {
            // Gujarati
            text.contains("gujarati") || text.contains("kinjal dave") || text.contains("geeta rabari") ||
            text.contains("kirtidan") || text.contains("jignesh kaviraj") || text.contains("aditya gadhvi") ||
            text.contains("sachin-jigar") || text.contains("sachin jigar") || text.contains("osman mir") ||
            text.contains("vijay suvada") || text.contains("umesh barot") || text.contains("aishwarya majmudar") ||
            text.contains("jigardan") || text.contains("sanjay oza") || text.contains("parthiv gohil") ||
            text.contains("garba") || text.contains("dayro") || text.contains("tahuko") ||
            text.contains("dholida") || text.contains("chogada") || text.contains("khalasi") ||
            text.contains("dakla") || text.contains("sanedo") || text.contains("dhol") -> "Gujarati"

            // Hindi / Bollywood
            text.contains("hindi") || text.contains("bollywood") || text.contains("arijit") ||
            text.contains("shreya ghoshal") || text.contains("pritam") || text.contains("atif aslam") ||
            text.contains("atif") || text.contains("neha kakkar") || text.contains("sonu nigam") ||
            text.contains("mohit chauhan") || text.contains("jubin nautiyal") || text.contains("vishal mishra") ||
            text.contains("darshan raval") || text.contains("alka yagnik") || text.contains("kumar sanu") ||
            text.contains("udit narayan") || text.contains("kishore kumar") || text.contains("lata mangeshkar") ||
            text.contains("mohd rafi") || text.contains("mukesh") || text.contains("armaan malik") ||
            text.contains("sunidhi chauhan") || text.contains("anuradha paudwal") || text.contains("badshah") ||
            text.contains("yo yo honey singh") || text.contains("shreya") -> "Hindi"

            // Punjabi
            text.contains("punjabi") || text.contains("diljit") || text.contains("sidhu moose") ||
            text.contains("sidhu") || text.contains("karan aujla") || text.contains("ap dhillon") ||
            text.contains("shubh") || text.contains("guru randhawa") || text.contains("ammy virk") ||
            text.contains("b praak") || text.contains("jasleen royal") || text.contains("harrdy sandhu") ||
            text.contains("jassi gill") || text.contains("bhangra") -> "Punjabi"

            // Tamil
            text.contains("tamil") || text.contains("kollywood") || text.contains("anirudh") ||
            text.contains("ar rahman") || text.contains("rahman") || text.contains("sid sriram") ||
            text.contains("yuvan") || text.contains("harris jayaraj") || text.contains("ilayaraja") ||
            text.contains("dhanush") || text.contains("spb") -> "Tamil"

            // Telugu
            text.contains("telugu") || text.contains("tollywood") || text.contains("dsp") ||
            text.contains("devi sri prasad") || text.contains("thaman") || text.contains("ram miriyala") -> "Telugu"

            // Spanish
            text.contains("spanish") || text.contains("bad bunny") || text.contains("rosalia") ||
            text.contains("j balvin") || text.contains("maluma") || text.contains("enrique iglesias") ||
            text.contains("luis fonsi") || text.contains("shakira") -> "Spanish"

            // Korean
            text.contains("k-pop") || text.contains("korean") || text.contains("bts") ||
            text.contains("blackpink") || text.contains("jungkook") || text.contains("newjeans") ||
            text.contains("stray kids") || text.contains("twice") -> "Korean"

            // Japanese
            text.contains("j-pop") || text.contains("japanese") || text.contains("anime") ||
            text.contains("yoasobi") || text.contains("kenshi yonezu") || text.contains("lisa") -> "Japanese"

            // English
            text.contains("english") || text.contains("ed sheeran") || text.contains("taylor swift") ||
            text.contains("taylor") || text.contains("billie eilish") || text.contains("drake") ||
            text.contains("the weeknd") || text.contains("dua lipa") || text.contains("justin bieber") ||
            text.contains("eminem") || text.contains("post malone") || text.contains("ariana grande") ||
            text.contains("bruno mars") || text.contains("charlie puth") || text.contains("coldplay") ||
            text.contains("imagine dragons") -> "English"

            else -> null
        }
    }
}
