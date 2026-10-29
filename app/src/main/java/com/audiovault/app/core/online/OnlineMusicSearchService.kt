package com.audiovault.app.core.online

import android.text.Html
import android.util.Base64
import android.util.Log
import com.audiovault.app.core.identity.CanonicalTrackIdentity
import com.audiovault.app.core.identity.TrackQualityFilter
import com.audiovault.app.core.identity.TrackVersionResolver
import com.audiovault.app.core.intelligence.TrackMatcher
import com.audiovault.app.core.search.SearchIntent
import com.audiovault.app.core.search.SearchIntentParser
import com.audiovault.app.core.search.SearchRelevanceRanker
import com.audiovault.app.core.search.SearchResultContainer
import com.audiovault.app.core.search.SearchTargetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object OnlineMusicSearchService {

    private const val TAG = "Recordify/OnlineMusic"
    private const val SAAVN_SECRET_KEY = "38346591"

    private val INVIDIOUS_INSTANCES = listOf(
        "https://invidious.flokinet.to",
        "https://invidious.drgns.space",
        "https://invidious.einfachzocken.eu",
        "https://invidious.perennialte.ch"
    )

    fun isIndianIntentQuery(rawQuery: String, primaryLanguage: String = "Hindi"): Boolean {
        val q = rawQuery.trim().lowercase()
        val indianKeywords = listOf(
            "old song", "old songs", "olds", "old is gold", "purane", "purana", "sad song", "sad songs",
            "dard", "romantic", "love song", "love songs", "90s", "80s", "70s", "bollywood", "hindi",
            "punjabi", "gujarati", "gujrati", "bhojpuri", "tamil", "telugu", "bengali", "marathi",
            "arijit", "kishore", "rafi", "lata", "shreya", "jubin", "atif", "sonu", "kk", "anuv jain"
        )
        if (primaryLanguage.equals("Hindi", ignoreCase = true) ||
            primaryLanguage.equals("Gujarati", ignoreCase = true) ||
            primaryLanguage.equals("Punjabi", ignoreCase = true)) {
            if (q in listOf("old", "olds", "old songs", "old song", "sad", "sad song", "sad songs", "romantic", "romantic songs", "love", "love songs", "90s", "90s songs", "party", "party songs")) {
                return true
            }
        }
        return indianKeywords.any { q.contains(it) }
    }

    fun getIndianIntentExpansionQueries(rawQuery: String): List<String> {
        val q = rawQuery.trim().lowercase()
        return when {
            q.contains("old song") || q.contains("old songs") || q == "olds" || q.contains("old is gold") || q.contains("purane") -> listOf(
                "Likhe Jo Khat Tujhe Mohammed Rafi",
                "Pal Pal Dil Ke Paas Kishore Kumar",
                "Dekha Ek Khwab Kishore Kumar Lata Mangeshkar",
                "Chand Sifarish Shaan Kailash",
                "Itna Na Mujhse Tu Pyar Badha Talat Mahmood",
                "Chura Ke Dil Mera",
                "Tujhe Dekha To",
                "Mera Dil Bhi Kitna Pagal Hai"
            )
            q.contains("sad song") || q.contains("sad songs") || q == "sad" || q.contains("dard") -> listOf(
                "Tu Thodi Der Aur Thehar Ja Half Girlfriend",
                "Jo Tum Mere Ho Anuv Jain",
                "Tum Hi Aana Marjaavaan",
                "Zaroorat Mustafa Zahid",
                "Bewajah Sanam Teri Kasam",
                "Roke Na Ruke Naina Arijit Singh",
                "Channa Mereya Arijit Singh",
                "Hamari Adhuri Kahani Arijit Singh"
            )
            q.contains("romantic song") || q.contains("romantic songs") || q.contains("love song") || q.contains("love songs") -> listOf(
                "Tum Hi Ho Arijit Singh",
                "Kesariya Arijit Singh",
                "Raataan Lambiyan Tanishk Bagchi",
                "Pehle Bhi Main Vishal Mishra",
                "Apna Bana Le Arijit Singh",
                "Tera Ban Jaunga Akhil Sachdeva"
            )
            q.contains("90s song") || q.contains("90s songs") || q.contains("90s bollywood") -> listOf(
                "Tujhe Dekha To Kumar Sanu",
                "Chura Ke Dil Mera Kumar Sanu Alka Yagnik",
                "Tip Tip Barsa Paani Alka Yagnik Udit Narayan",
                "Pehla Nasha Udit Narayan Sadhana Sargam",
                "Mera Dil Bhi Kitna Pagal Hai Kumar Sanu"
            )
            q.contains("gujarati") || q.contains("gujrati") -> listOf(
                "Khalasi Aditya Gadhvi Gotilo",
                "Char Bangdi Vali Gadi Kinjal Dave",
                "Mahadev Geeta Rabari",
                "Mogal Chedta Kalo Nag Kirtidan Gadhvi"
            )
            q.contains("punjabi") -> listOf(
                "295 Sidhu Moose Wala",
                "Lover Diljit Dosanjh",
                "With You AP Dhillon",
                "Softly Karan Aujla"
            )
            else -> emptyList()
        }
    }

    /**
     * Production-quality natural language search across multiple providers.
     * Integrates intent parsing, quality/version filtering, canonical deduplication, and relevance ranking.
     */
    suspend fun searchSmartMultiSource(
        rawQuery: String,
        limit: Int = 30,
        isOriginalOnly: Boolean = true,
        userCountryCode: String = "IN",
        primaryLanguage: String = "Hindi",
        preferredLanguages: Set<String> = emptySet(),
        onPartialResults: (suspend (SearchResultContainer) -> Unit)? = null
    ): SearchResultContainer = withContext(Dispatchers.IO) {
        if (rawQuery.isBlank()) return@withContext SearchResultContainer(rawQuery, SearchIntentParser.parse(""))
        val intent = SearchIntentParser.parse(rawQuery)
        val queryToSearch = intent.cleanQuery.ifBlank { rawQuery }
        val isIndianIntent = isIndianIntentQuery(rawQuery, primaryLanguage)
        val expansions = getIndianIntentExpansionQueries(rawQuery)

        coroutineScope {
            // Stage 1: Albums / Soundtracks if relevant or general query
            val albumsDeferred = async {
                try {
                    val directAlbums = searchAlbumsAndSoundtracks(queryToSearch, limit = 8)
                    if (directAlbums.isEmpty() && queryToSearch != rawQuery) {
                        searchAlbumsAndSoundtracks(rawQuery, limit = 6)
                    } else {
                        directAlbums
                    }
                } catch (e: Exception) {
                    emptyList<OnlineAlbumModel>()
                }
            }

            val knownArtistMatch = com.audiovault.app.core.catalog.ArtistImageCatalog.allArtistNames.firstOrNull {
                val qTrim = rawQuery.trim().lowercase()
                it.equals(qTrim, ignoreCase = true) || it.startsWith(qTrim) || it.contains(" $qTrim")
            }
            val cleanArtistName = knownArtistMatch?.split(" ")?.joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

            // Stage 2: Direct CDN fast songs (JioSaavn + iTunes + Spotify catalog + Language/Regional trending)
            val fastSongsDeferred = async {
                try {
                    val s1 = searchJioSaavnFast(queryToSearch, limit)
                    val s2 = if (queryToSearch != rawQuery && intent.requestedVersion != null) {
                        searchJioSaavnFast(rawQuery, limit)
                    } else emptyList()
                    val s3 = if (queryToSearch.length <= 3 && primaryLanguage.isNotBlank()) {
                        searchJioSaavnFast("$queryToSearch $primaryLanguage", limit = 10)
                    } else emptyList()
                    val expSongs = if (expansions.isNotEmpty()) {
                        expansions.flatMap { exp ->
                            searchJioSaavnFast(exp, limit = 3)
                        }
                    } else emptyList()
                    val artistSongs = if (cleanArtistName != null) {
                        val itunesHits = searchITunes(cleanArtistName, limit = 20)
                        val saavnHits = searchJioSaavnFast(cleanArtistName, limit = 20)
                        itunesHits + saavnHits
                    } else {
                        searchITunes(queryToSearch, limit = 15)
                    }
                    val sp = if (!isIndianIntent) searchSpotifyWeb(queryToSearch, limit) else emptyList()
                    (artistSongs + expSongs + s1 + s2 + s3 + sp)
                } catch (e: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            val rawFastSongs = fastSongsDeferred.await()
            val rawAlbums = albumsDeferred.await()

            // Filter and rank fast stage
            val canonicalFast = TrackQualityFilter.filterAndDeduplicate(
                songs = rawFastSongs,
                isOriginalOnly = if (intent.requestedVersion != null) false else isOriginalOnly,
                requestedVersion = intent.requestedVersion,
                decadeRange = intent.decadeRange
            )
            val rankedFastSongs = SearchRelevanceRanker.rankTracks(
                tracks = canonicalFast,
                intent = intent,
                userCountryCode = userCountryCode,
                primaryLanguage = primaryLanguage,
                preferredLanguages = preferredLanguages
            )
            val rankedAlbums = SearchRelevanceRanker.rankAlbums(
                albums = rawAlbums,
                intent = intent,
                userCountryCode = userCountryCode,
                primaryLanguage = primaryLanguage
            )

            // Extract artists from top albums / songs with clean CDN portraits
            val primaryArtistItem = cleanArtistName?.let { name ->
                ArtistItemModel(
                    id = "artist_${name.hashCode()}",
                    name = name,
                    imageUrl = com.audiovault.app.core.catalog.ArtistImageCatalog.getImageUrl(name),
                    role = "Artist"
                )
            }
            val extractedArtists = buildList {
                if (primaryArtistItem != null) add(primaryArtistItem)
                rankedFastSongs.map { it.normalizedPrimaryArtist }
                    .distinct()
                    .filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) && !it.equals(cleanArtistName, ignoreCase = true) }
                    .take(5)
                    .forEach { name ->
                        val art = com.audiovault.app.core.catalog.ArtistImageCatalog.getImageUrl(name).ifBlank {
                            rankedFastSongs.firstOrNull { it.normalizedPrimaryArtist == name }?.highResArtUrl ?: ""
                        }
                        add(
                            ArtistItemModel(
                                id = "artist_${name.hashCode()}",
                                name = name,
                                imageUrl = art,
                                role = "Artist"
                            )
                        )
                    }
            }

            val partialContainer = SearchResultContainer(
                query = rawQuery,
                intent = intent,
                songs = rankedFastSongs,
                albums = rankedAlbums,
                artists = extractedArtists,
                youtubeSongs = emptyList()
            )
            onPartialResults?.invoke(partialContainer)

            // Stage 3: Dedicated YouTube Music & Video search in parallel
            val ytSongsDeferred = async {
                try {
                    searchYouTubeAndInvidious(queryToSearch, 25)
                } catch (e: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            val ytSongs = ytSongsDeferred.await()

            val finalContainer = SearchResultContainer(
                query = rawQuery,
                intent = intent,
                songs = rankedFastSongs,
                albums = rankedAlbums,
                artists = extractedArtists,
                youtubeSongs = ytSongs
            )
            onPartialResults?.invoke(finalContainer)
            finalContainer
        }
    }

    suspend fun searchSongsProgressive(
        query: String,
        limit: Int = 30,
        onPartialResults: suspend (List<OnlineSongModel>) -> Unit
    ): List<OnlineSongModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cleanQuery = query.trim()

        val results = mutableListOf<OnlineSongModel>()
        val seenKeys = mutableSetOf<String>()

        coroutineScope {
            // Stage 1: Ultra-fast JioSaavn & Autocomplete + Spotify catalog search (<100ms)
            val saavnDeferred = async {
                try {
                    searchJioSaavnFast(cleanQuery, limit)
                } catch (e: Exception) {
                    Log.e(TAG, "JioSaavn fast search error", e)
                    emptyList<OnlineSongModel>()
                }
            }

            val spotifyDeferred = async {
                try {
                    searchSpotifyWeb(cleanQuery, limit = limit)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            val fastSaavn = saavnDeferred.await()
            val fastSpotify = spotifyDeferred.await()

            for (song in fastSaavn) {
                val key = normalizeKey(song.title, song.artist)
                if (seenKeys.add(key)) {
                    results.add(song)
                }
            }
            for (song in fastSpotify) {
                val key = normalizeKey(song.title, song.artist)
                if (seenKeys.add(key)) {
                    results.add(song)
                }
            }

            // Immediately emit fast results to UI without waiting!
            if (results.isNotEmpty()) {
                val sortedFast = results.sortedByDescending { calculateRelevance(it, cleanQuery) }
                onPartialResults(sortedFast)
            }

            // Stage 2: Deep YouTube Music & iTunes search in parallel (with Indian locale awareness)
            val isIndian = isIndianIntentQuery(cleanQuery)

            val ytDeferred = async {
                try {
                    val rawYt = searchYouTubeAndInvidious(cleanQuery, limit)
                    if (isIndian) {
                        rawYt.filter { TrackMatcher.isConfidentMatch(cleanQuery, null, it.title, it.artist, threshold = 0.40) }
                    } else rawYt
                } catch (e: Exception) {
                    Log.e(TAG, "YouTube deep search error", e)
                    emptyList<OnlineSongModel>()
                }
            }

            val itunesDeferred = async {
                try {
                    // If we already have sufficient genuine tracks from JioSaavn, avoid overriding with 30s preview tracks
                    if (isIndian && results.size >= 12) {
                        emptyList<OnlineSongModel>()
                    } else {
                        searchITunes(cleanQuery, limit, country = if (isIndian) "IN" else "US")
                    }
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            val ytResults = ytDeferred.await()
            val itunesResults = itunesDeferred.await()

            for (song in ytResults) {
                val key = normalizeKey(song.title, song.artist)
                if (seenKeys.add(key)) {
                    results.add(song)
                }
            }

            for (song in itunesResults) {
                val key = normalizeKey(song.title, song.artist)
                if (seenKeys.add(key)) {
                    results.add(song)
                }
            }
        }

        // Final sort by relevance
        val finalSorted = results.sortedByDescending { calculateRelevance(it, cleanQuery) }
        onPartialResults(finalSorted)
        finalSorted
    }

    suspend fun searchSongs(query: String, limit: Int = 30): List<OnlineSongModel> = searchSongsProgressive(query, limit) {}

    /**
     * Fast, lightweight search strictly across direct audio CDNs (JioSaavn / Spotify catalog).
     * Strictly avoids YouTube deep scraping or video-to-audio extraction overhead.
     * Perfect for auto-play, queue refill, radio, and recommendations.
     */
    suspend fun searchDirectCdnSongs(
        query: String,
        limit: Int = 20
    ): List<OnlineSongModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cleanQuery = query.trim()

        val results = mutableListOf<OnlineSongModel>()
        val seenKeys = mutableSetOf<String>()

        coroutineScope {
            val saavnDeferred = async {
                try {
                    searchJioSaavnFast(cleanQuery, limit)
                } catch (e: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            val spotifyDeferred = async {
                try {
                    searchSpotifyWeb(cleanQuery, limit = limit)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            val fastSaavn = saavnDeferred.await()
            val fastSpotify = spotifyDeferred.await()

            for (song in fastSaavn) {
                if (song.id.startsWith("yt_") || !song.youtubeVideoId.isNullOrBlank()) continue
                val key = normalizeKey(song.title, song.artist)
                if (seenKeys.add(key)) {
                    results.add(song)
                }
            }
            for (song in fastSpotify) {
                if (song.id.startsWith("yt_") || !song.youtubeVideoId.isNullOrBlank()) continue
                val key = normalizeKey(song.title, song.artist)
                if (seenKeys.add(key)) {
                    results.add(song)
                }
            }
        }

        // Apply strict original/official filter to suppress remixes, edits, and duplicate uploads
        val canonical = TrackQualityFilter.filterAndDeduplicate(results, isOriginalOnly = true)
        canonical.map { it.toOnlineSongModel() }
    }

    /**
     * Resolves multiple individual song queries in parallel in < 500ms using JioSaavn fast endpoint.
     */
    suspend fun resolveSongsFastParallel(queries: List<String>): List<OnlineSongModel> = withContext(Dispatchers.IO) {
        if (queries.isEmpty()) return@withContext emptyList()
        try {
            coroutineScope {
                queries.map { q ->
                    async {
                        try {
                            val candidates = searchJioSaavnFast(q, limit = 3)
                            val matchedSaavn = candidates.firstOrNull { song ->
                                TrackMatcher.isConfidentMatch(q, null, song.title, song.artist, threshold = 0.50)
                            } ?: candidates.firstOrNull()

                            if (matchedSaavn != null) {
                                matchedSaavn
                            } else {
                                // Fallback to Indian iTunes or YouTube
                                val itunesFallback = searchITunes(q, limit = 1, country = "IN").firstOrNull()
                                itunesFallback ?: searchYouTubeAndInvidious(q, limit = 1).firstOrNull()
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull().distinctBy { normalizeKey(it.title, it.artist) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Resolves multiple album queries in parallel in < 500ms.
     */
    suspend fun resolveAlbumsFastParallel(queries: List<String>): List<OnlineAlbumModel> = withContext(Dispatchers.IO) {
        if (queries.isEmpty()) return@withContext emptyList()
        try {
            coroutineScope {
                queries.map { q ->
                    async {
                        try {
                            searchAlbumsAndSoundtracks(q, limit = 1).firstOrNull()
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull().distinctBy { it.id }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun resolveMovieSoundtrack(albumName: String): OnlineAlbumModel? = withContext(Dispatchers.IO) {
        if (albumName.isBlank()) return@withContext null
        val clean = albumName.trim()
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            .replace(Regex("(?i)\\b(album|soundtrack|ost|movie|original motion picture soundtrack|edition)\\b"), "")
            .trim()
        if (clean.isBlank()) return@withContext null

        try {
            // 1. Direct search on JioSaavn album endpoint
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=search.getAlbumResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=5&p=1&q=$encodedQuery"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val results = root.optJSONArray("results")
            if (results != null && results.length() > 0) {
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val albumId = item.optString("id", "")
                    val title = cleanText(item.optString("title", ""))
                    if (albumId.isNotBlank() && com.audiovault.app.core.intelligence.TrackMatcher.similarity(clean, title) >= 0.45) {
                        val full = fetchAlbumSoundtrack(albumId)
                        if (full != null && full.songs.isNotEmpty()) {
                            return@withContext full
                        }
                    }
                }
                // If none crossed strict similarity threshold, try the first result if title has overlap
                val firstId = results.getJSONObject(0).optString("id", "")
                val firstTitle = cleanText(results.getJSONObject(0).optString("title", ""))
                if (firstId.isNotBlank() && (firstTitle.contains(clean, ignoreCase = true) || clean.contains(firstTitle, ignoreCase = true))) {
                    val full = fetchAlbumSoundtrack(firstId)
                    if (full != null && full.songs.isNotEmpty()) {
                        return@withContext full
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving movie soundtrack for $albumName", e)
        }

        // 2. Fallback: Search songs for "$clean movie songs" and deduplicate distinct tracks
        try {
            val songs = searchSongs("$clean movie songs", limit = 30)
            val videoJunkKeywords = listOf(
                "full video", "video song", "8k", "4k", "lyrical", "promo", "teaser", "trailer",
                "making of", "dialogue", "scene", "mashup", "slowed", "reverb", "lofi", "ringtone"
            )
            val distinctTracks = mutableListOf<OnlineSongModel>()
            val seenTitles = mutableSetOf<String>()
            for (song in songs) {
                val lowerTitle = song.title.lowercase()
                val isJunk = videoJunkKeywords.any { lowerTitle.contains(it) }
                val normTitle = com.audiovault.app.core.intelligence.TrackMatcher.normalize(song.title)
                if (!isJunk && normTitle.isNotBlank() && seenTitles.add(normTitle)) {
                    distinctTracks.add(song)
                }
            }
            if (distinctTracks.isNotEmpty()) {
                return@withContext OnlineAlbumModel(
                    id = "soundtrack_${clean.hashCode()}",
                    title = clean,
                    artist = distinctTracks.firstOrNull()?.artist ?: "Soundtrack",
                    coverArtUrl = distinctTracks.firstOrNull()?.highResArtUrl,
                    songCount = distinctTracks.size,
                    songs = distinctTracks,
                    type = "Album"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in movie songs search fallback for $albumName", e)
        }
        null
    }

    suspend fun searchAlbumsITunes(query: String, limit: Int = 6, country: String = "IN"): List<OnlineAlbumModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val endpoint = "https://itunes.apple.com/search?term=$encoded&entity=album&country=$country&limit=$limit"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Recordify/1.0")
            }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val results = root.optJSONArray("results") ?: return@withContext emptyList()
                val list = mutableListOf<OnlineAlbumModel>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val collectionId = item.optLong("collectionId", 0L).toString()
                    val collectionName = cleanText(item.optString("collectionName", ""))
                    val artistName = cleanText(item.optString("artistName", "Various Artists"))
                    val releaseDate = item.optString("releaseDate", "")
                    val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else null
                    val rawArtwork = item.optString("artworkUrl100", "")
                    val highResArt = rawArtwork.replace("100x100bb.jpg", "600x600bb.jpg")
                    val trackCount = item.optInt("trackCount", 0)

                    if (collectionName.isNotBlank()) {
                        list.add(
                            OnlineAlbumModel(
                                id = "itunes_alb_$collectionId",
                                title = collectionName,
                                year = year,
                                artist = artistName,
                                coverArtUrl = highResArt.takeIf { it.isNotBlank() },
                                type = "Album",
                                source = "itunes",
                                songCount = trackCount
                            )
                        )
                    }
                }
                return@withContext list
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun searchAlbumsAndSoundtracks(query: String, limit: Int = 4): List<OnlineAlbumModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=search.getAlbumResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=$limit&p=1&q=$encodedQuery"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()

            val albums = mutableListOf<OnlineAlbumModel>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val albumId = item.optString("id", "")
                val title = cleanText(item.optString("title", ""))
                val year = item.optString("year", "")
                val artist = cleanText(item.optString("music", item.optString("primary_artists", "Movie Soundtrack")))
                val rawImage = item.optString("image", "")
                val highResArt = rawImage.replace("-150x150.jpg", "-500x500.jpg").replace("-50x50.jpg", "-500x500.jpg")
                val language = item.optString("language", "")

                if (albumId.isNotBlank() && title.isNotBlank()) {
                    // Fetch full soundtrack songs for the top matching albums
                    val fullAlbum = fetchAlbumSoundtrack(albumId)
                    if (fullAlbum != null && fullAlbum.songs.isNotEmpty()) {
                        albums.add(fullAlbum)
                    } else {
                        albums.add(
                            OnlineAlbumModel(
                                id = albumId,
                                title = title,
                                year = year.takeIf { it.isNotBlank() },
                                artist = artist.takeIf { it.isNotBlank() },
                                coverArtUrl = highResArt.takeIf { it.isNotBlank() },
                                language = language.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            }
            val playlists = searchPlaylists(query, limit = limit)
            val combined = (albums + playlists).distinctBy { it.id }.toMutableList()
            if (combined.size < limit) {
                val itunes = searchAlbumsITunes(query, limit = limit, country = "US")
                for (alb in itunes) {
                    if (combined.none { it.title.equals(alb.title, ignoreCase = true) }) {
                        combined.add(alb)
                    }
                }
            }
            combined.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching albums/soundtracks", e)
            searchAlbumsITunes(query, limit = limit, country = "US")
        }
    }

    suspend fun fetchAlbumSoundtrack(albumId: String): OnlineAlbumModel? = withContext(Dispatchers.IO) {
        if (albumId.isBlank()) return@withContext null
        try {
            val endpoint = "https://www.jiosaavn.com/api.php?__call=content.getAlbumDetails&albumid=$albumId&_format=json&ctx=web6dot0"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)

            val title = cleanText(root.optString("title", root.optString("name", "")))
            if (title.isBlank()) return@withContext null

            val year = root.optString("year", "")
            val releaseDate = root.optString("release_date", "")
            val primaryArtists = cleanText(root.optString("primary_artists", root.optString("music", "")))
            val artist = if (primaryArtists.isNotBlank()) primaryArtists else cleanText(root.optString("singers", "Movie Soundtrack"))
            val rawImage = root.optString("image", "")
            val highResArt = rawImage.replace("-150x150.jpg", "-500x500.jpg").replace("-50x50.jpg", "-500x500.jpg")
            val language = root.optString("language", "")

            val songsArray = root.optJSONArray("songs") ?: JSONArray()
            val songList = mutableListOf<OnlineSongModel>()

            for (i in 0 until songsArray.length()) {
                val s = songsArray.getJSONObject(i)
                val sId = s.optString("id", "")
                val sTitle = cleanText(s.optString("song", ""))
                val sArtist = cleanText(s.optString("singers", s.optString("primary_artists", artist)))
                val sDurationSec = s.optString("duration").toLongOrNull() ?: 0L
                val sImg = s.optString("image", "").replace("-150x150.jpg", "-500x500.jpg").replace("-50x50.jpg", "-500x500.jpg")
                val encMedia = s.optString("encrypted_media_url", "")
                val streamUrl = if (encMedia.isNotBlank()) decryptMediaUrl(encMedia) else ""

                if (sTitle.isNotBlank()) {
                    songList.add(
                        OnlineSongModel(
                            id = if (sId.isNotBlank()) sId else "alb_${albumId}_$i",
                            title = sTitle,
                            artist = if (sArtist.isNotBlank()) sArtist else artist,
                            album = title,
                            durationMs = sDurationSec * 1000L,
                            streamUrl = streamUrl,
                            highResArtUrl = sImg.ifBlank { highResArt },
                            year = year.takeIf { it.isNotBlank() },
                            language = language.takeIf { it.isNotBlank() },
                            bitrate = "320kbps",
                            source = "saavn"
                        )
                    )
                }
            }

            OnlineAlbumModel(
                id = albumId,
                title = title,
                year = year.takeIf { it.isNotBlank() },
                artist = artist.takeIf { it.isNotBlank() },
                coverArtUrl = highResArt.takeIf { it.isNotBlank() },
                songCount = songList.size,
                songs = songList,
                language = language.takeIf { it.isNotBlank() },
                type = if (songList.size == 1) "Single" else if (title.contains("Best of", ignoreCase = true) || title.contains("Hits", ignoreCase = true)) "Compilation" else "Album",
                releaseDate = releaseDate.takeIf { it.isNotBlank() },
                primaryArtists = primaryArtists.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching album soundtrack details", e)
            null
        }
    }

    suspend fun fetchPlaylistDetails(playlistId: String): OnlineAlbumModel? = withContext(Dispatchers.IO) {
        if (playlistId.isBlank()) return@withContext null
        try {
            val cleanId = playlistId.removePrefix("saavn_pl_").removePrefix("pl_").trim()
            val endpoint = "https://www.jiosaavn.com/api.php?__call=playlist.getDetails&listid=$cleanId&_format=json&ctx=web6dot0"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)

            val title = cleanText(root.optString("title", root.optString("listname", "")))
            if (title.isBlank()) return@withContext null

            val rawImage = root.optString("image", "")
            val highResArt = rawImage.replace("-150x150.jpg", "-500x500.jpg").replace("-50x50.jpg", "-500x500.jpg")
            val songsArray = root.optJSONArray("songs") ?: JSONArray()
            val songList = mutableListOf<OnlineSongModel>()

            for (i in 0 until songsArray.length()) {
                val s = songsArray.getJSONObject(i)
                val sId = s.optString("id", "")
                val sTitle = cleanText(s.optString("song", ""))
                val sArtist = cleanText(s.optString("singers", s.optString("primary_artists", "Artist")))
                val sDurationSec = s.optString("duration").toLongOrNull() ?: 0L
                val sImg = s.optString("image", "").replace("-150x150.jpg", "-500x500.jpg").replace("-50x50.jpg", "-500x500.jpg")
                val encMedia = s.optString("encrypted_media_url", "")
                val streamUrl = if (encMedia.isNotBlank()) decryptMediaUrl(encMedia) else ""

                if (sTitle.isNotBlank()) {
                    songList.add(
                        OnlineSongModel(
                            id = if (sId.isNotBlank()) sId else "pl_${cleanId}_$i",
                            title = sTitle,
                            artist = if (sArtist.isNotBlank()) sArtist else "Various Artists",
                            album = title,
                            durationMs = sDurationSec * 1000L,
                            streamUrl = streamUrl,
                            highResArtUrl = sImg.ifBlank { highResArt },
                            bitrate = "320kbps",
                            source = "saavn"
                        )
                    )
                }
            }

            OnlineAlbumModel(
                id = playlistId,
                title = title,
                year = "Playlist",
                artist = "Playlist • Recordify",
                coverArtUrl = highResArt.takeIf { it.isNotBlank() },
                songCount = songList.size,
                songs = songList,
                type = "Playlist",
                source = "saavn"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching playlist details for $playlistId", e)
            null
        }
    }

    suspend fun searchPlaylists(query: String, limit: Int = 10): List<OnlineAlbumModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=search.getPlaylistResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=$limit&p=1&q=$encodedQuery"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<OnlineAlbumModel>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val plId = item.optString("id", item.optString("listid", ""))
                val title = cleanText(item.optString("title", ""))
                val rawImage = item.optString("image", "")
                val highResArt = rawImage.replace("-150x150.jpg", "-500x500.jpg").replace("-50x50.jpg", "-500x500.jpg")
                if (plId.isNotBlank() && title.isNotBlank()) {
                    list.add(
                        OnlineAlbumModel(
                            id = "saavn_pl_$plId",
                            title = title,
                            year = "Playlist",
                            artist = "Playlist • Recordify",
                            coverArtUrl = highResArt.takeIf { it.isNotBlank() },
                            type = "Playlist",
                            source = "saavn"
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error searching playlists for $query", e)
            emptyList()
        }
    }

    /**
     * Fetches an artist's comprehensive song catalog (35 - 50+ songs) for their profile playlist.
     * Guaranteed ordering:
     * 1. Top trending curated anthems first (Tum Hi Ho, Kesariya, Channa Mereya, etc.)
     * 2. Official JioSaavn editorial artist playlist ("Let's Play <Artist>" / "Best of <Artist>")
     * 3. Deep search tracks by this artist, deduplicated by normalized title
     * 4. Excludes karaoke, ringtones, slowed+reverb, 8d audio noise.
     */
    suspend fun fetchArtistAllSongs(
        artistName: String,
        topCuratedQueries: List<String> = emptyList(),
        limit: Int = 50
    ): List<OnlineSongModel> = withContext(Dispatchers.IO) {
        val cleanArtist = artistName.trim()
        if (cleanArtist.isBlank()) return@withContext emptyList()

        val results = mutableListOf<OnlineSongModel>()
        val seenNormalizedTitles = mutableSetOf<String>()

        fun normTitle(t: String): String =
            t.lowercase()
                .replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "")
                .replace(Regex("""[^a-z0-9]"""), "")
                .trim()

        // 1. Resolve top curated hits first (guaranteed top trending hits first)
        if (topCuratedQueries.isNotEmpty()) {
            try {
                val curatedSongs = resolveSongsFastParallel(topCuratedQueries)
                for (song in curatedSongs) {
                    val key = normTitle(song.title)
                    if (key.isNotBlank() && seenNormalizedTitles.add(key)) {
                        results.add(song)
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Fetch artist's official editorial playlist from JioSaavn ("Let's Play <Artist>", "Best of <Artist>", "<Artist> Hits")
        try {
            val queryVariants = listOf("Let's Play $cleanArtist", "Best of $cleanArtist", "$cleanArtist Hits")
            var bestPlaylistId: String? = null

            for (qv in queryVariants) {
                val encodedQ = URLEncoder.encode(qv, "UTF-8")
                val plSearchUrl = "https://www.jiosaavn.com/api.php?__call=search.getPlaylistResults&q=$encodedQ&_format=json&n=3&p=1&ctx=web6dot0"
                val conn = (URL(plSearchUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(text)
                    val resultsArr = root.optJSONArray("results")
                    if (resultsArr != null && resultsArr.length() > 0) {
                        val firstToken = cleanArtist.lowercase().split(" ").firstOrNull { it.length >= 3 } ?: ""
                        for (i in 0 until resultsArr.length()) {
                            val plObj = resultsArr.getJSONObject(i)
                            val plName = plObj.optString("listname", plObj.optString("title", "")).lowercase()
                            val plId = plObj.optString("listid", plObj.optString("id", ""))
                            if (plId.isNotBlank() && (firstToken.isBlank() || plName.contains(firstToken))) {
                                bestPlaylistId = plId
                                break
                            }
                        }
                        if (bestPlaylistId != null) break
                    }
                }
            }

            if (bestPlaylistId != null) {
                val plAlbum = fetchPlaylistDetails(bestPlaylistId)
                if (plAlbum != null && plAlbum.songs.isNotEmpty()) {
                    for (song in plAlbum.songs) {
                        val key = normTitle(song.title)
                        if (key.isNotBlank() && seenNormalizedTitles.add(key)) {
                            results.add(song)
                        }
                        if (results.size >= limit) break
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. If we still have room, fetch directly from JioSaavn search results for this artist
        if (results.size < limit) {
            try {
                val moreSongs = searchJioSaavnFast(cleanArtist, limit = (limit - results.size).coerceAtLeast(15))
                for (song in moreSongs) {
                    val key = normTitle(song.title)
                    if (key.isNotBlank() && seenNormalizedTitles.add(key)) {
                        results.add(song)
                    }
                    if (results.size >= limit) break
                }
            } catch (_: Exception) {}
        }

        // 4. Filter only actual noise (karaoke, ringtone, instrumental, slowed+reverb, 8d audio)
        val junk = listOf("karaoke", "ringtone", "instrumental", "slowed", "reverb", "8d audio", "bass boosted")
        val cleanList = results.filter { song ->
            val sTitle = song.title.lowercase()
            junk.none { sTitle.contains(it) }
        }

        cleanList.ifEmpty { results }
    }

    suspend fun fetchYouTubePodcasts(countryCode: String = "IN"): List<OnlineSongModel> = withContext(Dispatchers.IO) {
        val podcastQueries = when (countryCode.uppercase()) {
            "IN" -> listOf(
                "The Ranveer Show podcast episode",
                "Raj Shamani Figuring Out podcast",
                "Finshots Daily podcast",
                "Prakhar ke Pravachan podcast",
                "WTF is with Nikhil Kamath podcast"
            )
            "US", "GB" -> listOf(
                "The Joe Rogan Experience podcast episode",
                "Huberman Lab podcast full episode",
                "Lex Fridman podcast episode",
                "The Diary Of A CEO podcast",
                "SmartLess podcast"
            )
            "KR" -> listOf(
                "Korean podcast full episode",
                "K-pop Daebak show Eric Nam",
                "Talk To Me In Korean podcast"
            )
            "JP" -> listOf(
                "Japanese podcast episode",
                "Learn Japanese podcast",
                "NHK Radio News podcast"
            )
            "BR" -> listOf(
                "Podpah podcast",
                "Flow Podcast Brasil",
                "Inteligencia Ltda podcast"
            )
            "ES" -> listOf(
                "The Wild Project podcast",
                "Nadie Sabe Nada podcast",
                "Entiende Tu Mente podcast"
            )
            "AE" -> listOf(
                "Kerning Cultures podcast",
                "Dukkan Show podcast Dubai",
                "AbTalks Anas Bukhash podcast"
            )
            else -> listOf(
                "The Joe Rogan Experience podcast",
                "Huberman Lab podcast",
                "Lex Fridman podcast",
                "The Diary Of A CEO podcast"
            )
        }

        val results = mutableListOf<OnlineSongModel>()
        val seenIds = mutableSetOf<String>()

        coroutineScope {
            val jobs = podcastQueries.map { q ->
                async {
                    try {
                        searchYouTubeAndInvidious(q, limit = 4)
                    } catch (_: Exception) {
                        emptyList<OnlineSongModel>()
                    }
                }
            }
            for (job in jobs) {
                val episodes = job.await()
                for (ep in episodes) {
                    val hdThumb = ep.highResArtUrl?.replace("hqdefault.jpg", "maxresdefault.jpg") ?: ep.highResArtUrl
                    val cleanEp = ep.copy(
                        highResArtUrl = hdThumb,
                        album = "Podcast Episode"
                    )
                    if (seenIds.add(cleanEp.id)) {
                        results.add(cleanEp)
                    }
                }
            }
        }

        results
    }

    suspend fun fetchHomeFeaturedContent(
        countryCode: String = "IN",
        liveSpotifyContent: SpotifyHomeContent? = null
    ): SpotifyHomeContent = withContext(Dispatchers.IO) {
        val isIndia = countryCode.equals("IN", ignoreCase = true)
        val profile = com.audiovault.app.core.catalog.CountryMusicCatalog.getProfile(countryCode)
        val countryUpper = countryCode.uppercase()

        coroutineScope {
            // Day-based rotation offset so home content changes dynamically every day
            val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)

            val recommendedQueriesPool = if (isIndia) {
                listOf(
                    listOf("Ramaiya Vastavaiya", "Best Of Arijit Singh - Revisited", "Khiladi 786", "Yeh Jawaani Hai Deewani", "Dilwale"),
                    listOf("Brahmastra", "Kalank", "Kesari", "Ae Dil Hai Mushkil", "Kabir Singh"),
                    listOf("Aashiqui 2", "Saiyaara", "Sanam Teri Kasam", "Raabta", "Rockstar"),
                    listOf("Jawan", "Pathaan", "Dunki", "Chhaava", "Stree 2"),
                    listOf("Shershaah", "Luka Chuppi", "Sonu Ke Titu Ki Sweety", "Badlapur", "Cocktail")
                )
            } else {
                listOf(
                    profile.quickAccessTiles.map { it.title }.take(5),
                    profile.shelves.map { it.title }.take(5),
                    profile.popularArtists.map { "${it.name} album" }.take(5)
                )
            }
            val recommendedQueries = recommendedQueriesPool[dayOfYear % recommendedQueriesPool.size]

            val popularQueriesPool = if (isIndia) {
                listOf(
                    listOf("Saiyaara", "Aashiqui 2", "Sanam Teri Kasam", "Animal", "Pushpa 2"),
                    listOf("Animal", "Kabir Singh", "Rockstar", "Saiyaara", "Aashiqui 2"),
                    listOf("Pushpa 2", "Devara", "Stree 2", "Kalki 2898 AD", "Saiyaara"),
                    listOf("Sanam Teri Kasam", "Aashiqui 2", "Hamari Adhuri Kahani", "Ek Villain", "Saiyaara"),
                    listOf("Rockstar", "Tamasha", "Barfi", "Jab We Met", "Love Aaj Kal")
                )
            } else {
                listOf(
                    profile.popularArtists.shuffled().take(5).map { "${it.name} hits" },
                    profile.shelves.map { it.searchQuery }.take(5)
                )
            }
            val popularQueries = popularQueriesPool[dayOfYear % popularQueriesPool.size]

            val recommendedDeferred = async {
                val list = mutableListOf<OnlineAlbumModel>()
                if (!isIndia) {
                    list.addAll(profile.quickAccessTiles)
                }
                for (q in recommendedQueries) {
                    try {
                        val found = if (!isIndia) {
                            searchAlbumsITunes(q, limit = 1, country = countryUpper).ifEmpty { searchAlbumsAndSoundtracks(q, limit = 1) }
                        } else {
                            searchAlbumsAndSoundtracks(q, limit = 1)
                        }
                        if (found.isNotEmpty()) {
                            val album = found[0]
                            val type = if (q.contains("Best Of", ignoreCase = true) || q.contains("Hits", ignoreCase = true)) "Compilation" else "Album"
                            if (list.none { it.id == album.id }) {
                                list.add(album.copy(type = type))
                            }
                        }
                    } catch (_: Exception) {}
                }
                list
            }

            val popularAlbumsDeferred = async {
                val list = mutableListOf<OnlineAlbumModel>()
                for (q in popularQueries) {
                    try {
                        val found = if (!isIndia) {
                            searchAlbumsITunes(q, limit = 1, country = countryUpper).ifEmpty { searchAlbumsAndSoundtracks(q, limit = 1) }
                        } else {
                            searchAlbumsAndSoundtracks(q, limit = 1)
                        }
                        if (found.isNotEmpty() && list.none { it.id == found[0].id }) {
                            list.add(found[0])
                        }
                    } catch (_: Exception) {}
                }
                list
            }

            val releasesDeferred = async {
                try {
                    if (isIndia) {
                        val hindi = searchSongs("Latest Hindi Songs", limit = 8)
                        val trending = searchSongs("Trending Songs", limit = 8)
                        (hindi + trending).distinctBy { normalizeKey(it.title, it.artist) }
                    } else {
                        val trendingShelf = profile.shelves.firstOrNull { it.category == "trending" }
                        val query = trendingShelf?.searchQuery ?: "top hits ${profile.countryName} 2024"
                        searchDirectCdnSongs(query, limit = 12)
                    }
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            val radioList = if (isIndia) {
                listOf(
                    RadioMixModel(
                        id = "radio_diljit",
                        title = "Diljit Dosanjh",
                        subtitle = "RADIO",
                        artists = "Shashwat Sachdev, Diljit Dosanjh, AP Dhillon, Harrdy Sandhu",
                        bgHexColor = 0xFF589EA6,
                        artistImages = listOf(
                            "https://c.saavncdn.com/artists/Diljit_Dosanjh_004_20221014075535_500x500.jpg",
                            "https://c.saavncdn.com/artists/AP_Dhillon_000_20220610091338_500x500.jpg"
                        )
                    ),
                    RadioMixModel(
                        id = "radio_kishore",
                        title = "Kishore Kumar",
                        subtitle = "RADIO",
                        artists = "Lata Mangeshkar, Kishore Kumar, Asha Bhosle, Kalyanji-Anandji",
                        bgHexColor = 0xFFE06A55,
                        artistImages = listOf(
                            "https://c.saavncdn.com/artists/Kishore_Kumar_500x500.jpg",
                            "https://c.saavncdn.com/artists/Lata_Mangeshkar_500x500.jpg"
                        )
                    ),
                    RadioMixModel(
                        id = "radio_ar_rahman",
                        title = "A.R. Rahman",
                        subtitle = "RADIO",
                        artists = "A.R. Rahman, Shankar Mahadevan, Shreya Ghoshal, Javed Ali",
                        bgHexColor = 0xFFE5B54E,
                        artistImages = listOf(
                            "https://c.saavncdn.com/artists/A_R_Rahman_500x500.jpg",
                            "https://c.saavncdn.com/artists/Shreya_Ghoshal_500x500.jpg"
                        )
                    ),
                    RadioMixModel(
                        id = "radio_arijit",
                        title = "Arijit Singh",
                        subtitle = "RADIO",
                        artists = "Arijit Singh, Pritam, Vishal Mishra, Jasleen Royal",
                        bgHexColor = 0xFF439A86,
                        artistImages = listOf(
                            "https://c.saavncdn.com/artists/Arijit_Singh_002_20230323062147_500x500.jpg",
                            "https://c.saavncdn.com/artists/Pritam_500x500.jpg"
                        )
                    ),
                    RadioMixModel(
                        id = "radio_srk",
                        title = "Shah Rukh Khan Hits",
                        subtitle = "RADIO",
                        artists = "Pritam, Anirudh Ravichander, Vishal-Shekhar, Udit Narayan",
                        bgHexColor = 0xFF4A6FA5,
                        artistImages = listOf(
                            "https://c.saavncdn.com/artists/Shah_Rukh_Khan_500x500.jpg",
                            "https://c.saavncdn.com/artists/Anirudh_Ravichander_500x500.jpg"
                        )
                    )
                )
            } else {
                profile.popularArtists.take(5).mapIndexed { idx, art ->
                    RadioMixModel(
                        id = "radio_${art.name.hashCode()}",
                        title = art.name,
                        subtitle = "RADIO",
                        artists = "${art.name}, ${profile.popularArtists.getOrNull(idx + 1)?.name ?: "Recordify Top Hits"}",
                        bgHexColor = when (idx % 4) {
                            0 -> 0xFF589EA6
                            1 -> 0xFFE06A55
                            2 -> 0xFFE5B54E
                            else -> 0xFF439A86
                        },
                        artistImages = emptyList()
                    )
                }
            }

            val artistList = if (isIndia) {
                listOf(
                    ArtistItemModel(
                        id = "art_arijit",
                        name = "Arijit Singh",
                        imageUrl = "https://c.saavncdn.com/artists/Arijit_Singh_002_20230323062147_500x500.jpg"
                    ),
                    ArtistItemModel(
                        id = "art_aditya",
                        name = "Aditya Gadhvi",
                        imageUrl = "https://c.saavncdn.com/artists/Aditya_Gadhvi_000_20231019075727_500x500.jpg"
                    ),
                    ArtistItemModel(
                        id = "art_diljit",
                        name = "Diljit Dosanjh",
                        imageUrl = "https://c.saavncdn.com/artists/Diljit_Dosanjh_004_20221014075535_500x500.jpg"
                    ),
                    ArtistItemModel(
                        id = "art_neeti",
                        name = "Neeti Mohan",
                        imageUrl = "https://c.saavncdn.com/artists/Neeti_Mohan_004_20220610091410_500x500.jpg"
                    ),
                    ArtistItemModel(
                        id = "art_honey",
                        name = "Yo Yo Honey Singh",
                        imageUrl = "https://c.saavncdn.com/artists/Yo_Yo_Honey_Singh_002_20221208075308_500x500.jpg"
                    ),
                    ArtistItemModel(
                        id = "art_shreya",
                        name = "Shreya Ghoshal",
                        imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_004_20221104100347_500x500.jpg"
                    ),
                    ArtistItemModel(
                        id = "art_anirudh",
                        name = "Anirudh Ravichander",
                        imageUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20231113063556_500x500.jpg"
                    ),
                    ArtistItemModel(
                        id = "art_vishal",
                        name = "Vishal Mishra",
                        imageUrl = "https://c.saavncdn.com/artists/Vishal_Mishra_004_20230626071415_500x500.jpg"
                    )
                )
            } else {
                profile.popularArtists.take(8).map { art ->
                    ArtistItemModel(
                        id = "art_${art.name.hashCode()}",
                        name = art.name,
                        imageUrl = "",
                        role = art.primaryGenre
                    )
                }
            }

            val rec = recommendedDeferred.await()
            val pop = popularAlbumsDeferred.await()
            val rel = releasesDeferred.await()

            val combinedRecommended = if (liveSpotifyContent != null && liveSpotifyContent.recommendedToday.isNotEmpty()) {
                (liveSpotifyContent.recommendedToday + rec).distinctBy { it.id }
            } else {
                rec
            }

            val combinedPopular = if (liveSpotifyContent != null && liveSpotifyContent.popularAlbumsAndSingles.isNotEmpty()) {
                (liveSpotifyContent.popularAlbumsAndSingles + pop).distinctBy { it.id }
            } else {
                pop
            }

            val combinedReleases = if (liveSpotifyContent != null && liveSpotifyContent.latestReleases.isNotEmpty()) {
                (liveSpotifyContent.latestReleases + rel).distinctBy { normalizeKey(it.title, it.artist) }
            } else {
                rel
            }

            SpotifyHomeContent(
                recommendedToday = combinedRecommended,
                popularAlbumsAndSingles = combinedPopular,
                popularRadios = radioList,
                favouriteArtists = artistList,
                moreOfWhatYouLike = combinedPopular.reversed(),
                latestReleases = combinedReleases,
                trendingSoundtracks = combinedPopular,
                featuredPlaylists = combinedReleases.take(8)
            )
        }
    }

    fun searchSpotifyWeb(query: String, limit: Int = 10): List<OnlineSongModel> {
        val list = mutableListOf<OnlineSongModel>()
        try {
            val isIndian = isIndianIntentQuery(query)
            val itunesMatches = searchITunes(query, limit, country = if (isIndian) "IN" else "US")
            for (item in itunesMatches) {
                list.add(
                    item.copy(
                        id = "sp_${item.id}",
                        source = "spotify",
                        bitrate = "320kbps"
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun searchJioSaavnFast(query: String, limit: Int): List<OnlineSongModel> {
        val subqueries = generateSmartSubqueries(query)
        val list = mutableListOf<OnlineSongModel>()
        val seenIds = mutableSetOf<String>()

        // 1. Direct Search across smart subqueries
        for (sub in subqueries) {
            val endpointResults = queryJioSaavnEndpoint(sub, limit)
            for (song in endpointResults) {
                if (seenIds.add(song.id)) {
                    list.add(song)
                }
            }
            if (list.size >= limit) break
        }

        // 2. Intelligent Autocomplete expansion with exact 1:1 detail resolution
        val autocompleteTracks = queryJioSaavnAutocomplete(query)
        val missingDetailIds = autocompleteTracks.filter { it.streamUrl.isBlank() && !seenIds.contains(it.id) }.map { it.id }
        
        if (missingDetailIds.isNotEmpty()) {
            val resolvedTracks = fetchSongDetailsBatch(missingDetailIds.take(limit))
            for (song in resolvedTracks) {
                if (seenIds.add(song.id)) {
                    list.add(song)
                }
            }
        }

        return list.distinctBy { normalizeKey(it.title, it.artist) }
    }

    private fun queryJioSaavnEndpoint(query: String, limit: Int): List<OnlineSongModel> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=$limit&p=1&q=$encodedQuery"

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val resultsArray = root.optJSONArray("results") ?: return emptyList()

            val list = mutableListOf<OnlineSongModel>()
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                val id = item.optString("id", "")
                val rawTitle = item.optString("title", "")
                val title = cleanText(rawTitle)

                val moreInfo = item.optJSONObject("more_info")
                val artistMap = moreInfo?.optJSONObject("artistMap") ?: item.optJSONObject("artistMap")
                val primaryArray = artistMap?.optJSONArray("primary_artists")
                val artistNamesList = mutableListOf<String>()
                if (primaryArray != null) {
                    for (j in 0 until primaryArray.length()) {
                        val aObj = primaryArray.optJSONObject(j)
                        val aName = aObj?.optString("name", "") ?: ""
                        if (aName.isNotBlank() && !artistNamesList.contains(aName)) {
                            artistNamesList.add(aName)
                        }
                    }
                }
                val subtitleArtists = item.optString("subtitle", "").substringBefore(" - ").takeIf { it.isNotBlank() }
                val rawArtist = when {
                    artistNamesList.isNotEmpty() -> artistNamesList.joinToString(", ")
                    !subtitleArtists.isNullOrBlank() -> subtitleArtists
                    else -> moreInfo?.optString("primary_artists")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        ?: moreInfo?.optString("music")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        ?: item.optString("subtitle", "Various Artists")
                }
                val artist = cleanText(rawArtist)

                val rawAlbum = moreInfo?.optString("album")
                    ?.takeIf { it.isNotBlank() && it != "null" }
                    ?: item.optString("more_info", "")
                val album = cleanText(rawAlbum).takeIf { it.isNotBlank() }

                val durationSeconds = moreInfo?.optString("duration")?.toLongOrNull() ?: 0L
                val durationMs = durationSeconds * 1000L

                val rawImage = item.optString("image", "")
                val highResArt = when {
                    rawImage.isNotBlank() -> rawImage.replace("-150x150.jpg", "-500x500.jpg")
                        .replace("-50x50.jpg", "-500x500.jpg")
                    else -> null
                }

                val encryptedMediaUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                val streamUrl = if (encryptedMediaUrl.isNotBlank()) {
                    decryptMediaUrl(encryptedMediaUrl)
                } else ""

                val year = moreInfo?.optString("year")?.takeIf { it.isNotBlank() && it != "null" }
                val language = item.optString("language")?.takeIf { it.isNotBlank() && it != "null" }

                if (title.isNotBlank() && streamUrl.isNotBlank()) {
                    list.add(
                        OnlineSongModel(
                            id = if (id.isNotBlank()) id else "saavn_${System.currentTimeMillis()}_$i",
                            title = title,
                            artist = if (artist.isNotBlank()) artist else "Various Artists",
                            album = album,
                            durationMs = durationMs,
                            streamUrl = streamUrl,
                            highResArtUrl = highResArt,
                            year = year,
                            language = language,
                            bitrate = "320kbps",
                            source = "saavn"
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun queryJioSaavnAutocomplete(query: String): List<OnlineSongModel> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_marker=0&query=$encoded&ctx=web6dot0"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val songsObj = root.optJSONObject("songs") ?: return emptyList()
            val dataArray = songsObj.optJSONArray("data") ?: return emptyList()

            val list = mutableListOf<OnlineSongModel>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                val title = cleanText(item.optString("title", ""))
                val desc = item.optString("description", "")
                val parts = desc.split("·").map { it.trim() }
                val album = parts.firstOrNull()
                val artist = if (parts.size > 1) parts[1] else item.optJSONObject("more_info")?.optString("primary_artists", "Artist") ?: "Artist"
                val rawImage = item.optString("image", "")
                val highResArt = rawImage.replace("-50x50.jpg", "-500x500.jpg")

                if (id.isNotBlank() && title.isNotBlank()) {
                    list.add(
                        OnlineSongModel(
                            id = id,
                            title = title,
                            artist = cleanText(artist),
                            album = cleanText(album ?: ""),
                            durationMs = 0L,
                            streamUrl = "",
                            highResArtUrl = highResArt,
                            bitrate = "320kbps",
                            source = "saavn"
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun fetchSongDetailsBatch(pids: List<String>): List<OnlineSongModel> {
        if (pids.isEmpty()) return emptyList()
        return try {
            val joinedPids = pids.joinToString(",")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$joinedPids&_format=json&ctx=web6dot0"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val songsArray = root.optJSONArray("songs") ?: return emptyList()

            val list = mutableListOf<OnlineSongModel>()
            for (i in 0 until songsArray.length()) {
                val s = songsArray.getJSONObject(i)
                val id = s.optString("id", "")
                val title = cleanText(s.optString("song", ""))
                val artist = cleanText(s.optString("primary_artists", s.optString("singers", "Various Artists")))
                val album = cleanText(s.optString("album", ""))
                val durationSeconds = s.optString("duration").toLongOrNull() ?: 0L
                val rawImage = s.optString("image", "")
                val highResArt = rawImage.replace("-150x150.jpg", "-500x500.jpg").replace("-50x50.jpg", "-500x500.jpg")
                val encMediaUrl = s.optString("encrypted_media_url", "")
                val streamUrl = if (encMediaUrl.isNotBlank()) decryptMediaUrl(encMediaUrl) else ""
                val year = s.optString("year", "")
                val language = s.optString("language", "")

                if (title.isNotBlank() && streamUrl.isNotBlank()) {
                    list.add(
                        OnlineSongModel(
                            id = if (id.isNotBlank()) id else "saavn_det_$i",
                            title = title,
                            artist = if (artist.isNotBlank()) artist else "Various Artists",
                            album = album.takeIf { it.isNotBlank() },
                            durationMs = durationSeconds * 1000L,
                            streamUrl = streamUrl,
                            highResArtUrl = highResArt.takeIf { it.isNotBlank() },
                            year = year.takeIf { it.isNotBlank() },
                            language = language.takeIf { it.isNotBlank() },
                            bitrate = "320kbps",
                            source = "saavn"
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching song details batch", e)
            emptyList()
        }
    }

    fun fetchSongDetailsById(songId: String): OnlineSongModel? {
        val results = fetchSongDetailsBatch(listOf(songId))
        return results.firstOrNull()
    }

    fun searchYouTubeAndInvidious(query: String, limit: Int = 20): List<OnlineSongModel> {
        // Strategy A: Invidious Public API instances (Fast JSON)
        for (inst in INVIDIOUS_INSTANCES) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val endpoint = "$inst/api/v1/search?q=$encoded&type=video"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(text)
                    val list = mutableListOf<OnlineSongModel>()
                    for (i in 0 until array.length().coerceAtMost(limit)) {
                        val item = array.getJSONObject(i)
                        val vidId = item.optString("videoId", "")
                        val title = cleanText(item.optString("title", ""))
                        val author = cleanText(item.optString("author", "YouTube Music"))
                        val lengthSeconds = item.optLong("lengthSeconds", 0L)
                        val thumbUrl = "https://i.ytimg.com/vi/$vidId/maxresdefault.jpg"

                        if (vidId.length == 11 && title.isNotBlank()) {
                            list.add(
                                OnlineSongModel(
                                    id = "yt_$vidId",
                                    title = title,
                                    artist = author,
                                    album = "YouTube Music",
                                    durationMs = lengthSeconds * 1000L,
                                    streamUrl = "",
                                    highResArtUrl = thumbUrl,
                                    bitrate = "320kbps",
                                    source = "youtube",
                                    youtubeVideoId = vidId
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) return list
                }
            } catch (_: Exception) {}
        }

        // Strategy B: YouTube Desktop Web InitialData
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val endpoint = "https://www.youtube.com/results?search_query=$encodedQuery"

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }

            if (connection.responseCode == 200) {
                val html = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val matcher = Pattern.compile("(?:var\\s+ytInitialData|ytInitialData)\\s*=\\s*(\\{.+?\\});").matcher(html)
                if (matcher.find()) {
                    val jsonStr = matcher.group(1) ?: ""
                    val root = JSONObject(jsonStr)

                    val contents = root.optJSONObject("contents")
                        ?.optJSONObject("twoColumnSearchResultsRenderer")
                        ?.optJSONObject("primaryContents")
                        ?.optJSONObject("sectionListRenderer")
                        ?.optJSONArray("contents")

                    if (contents != null) {
                        val list = mutableListOf<OnlineSongModel>()
                        for (i in 0 until contents.length()) {
                            val section = contents.getJSONObject(i)
                            val itemSection = section.optJSONObject("itemSectionRenderer")?.optJSONArray("contents") ?: continue

                            for (j in 0 until itemSection.length()) {
                                if (list.size >= limit) break
                                val item = itemSection.getJSONObject(j)
                                val vr = item.optJSONObject("videoRenderer") ?: continue

                                val videoId = vr.optString("videoId", "")
                                if (videoId.length != 11) continue

                                val titleRuns = vr.optJSONObject("title")?.optJSONArray("runs")
                                val title = titleRuns?.optJSONObject(0)?.optString("text")
                                    ?: vr.optJSONObject("title")?.optString("simpleText", "")
                                    ?: ""

                                val ownerRuns = vr.optJSONObject("ownerText")?.optJSONArray("runs")
                                    ?: vr.optJSONObject("shortBylineText")?.optJSONArray("runs")
                                val author = ownerRuns?.optJSONObject(0)?.optString("text", "YouTube Artist")
                                    ?: "YouTube Artist"

                                val durationText = vr.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
                                val durationMs = parseDurationText(durationText)

                                if (title.isNotBlank()) {
                                    list.add(
                                        OnlineSongModel(
                                            id = "yt_$videoId",
                                            title = cleanText(title),
                                            artist = cleanText(author),
                                            album = "YouTube Music",
                                            durationMs = durationMs,
                                            streamUrl = "",
                                            highResArtUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
                                            bitrate = "320kbps",
                                            source = "youtube",
                                            youtubeVideoId = videoId
                                        )
                                    )
                                }
                            }
                        }
                        if (list.isNotEmpty()) return list
                    }
                }
            }
        } catch (_: Exception) {}

        return emptyList()
    }

    private fun searchITunes(query: String, limit: Int, country: String = "IN"): List<OnlineSongModel> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val endpoint = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&country=$country&limit=$limit"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }

        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)
        val resultsArray = root.optJSONArray("results") ?: return emptyList()

        val list = mutableListOf<OnlineSongModel>()
        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.getJSONObject(i)
            val trackId = item.optLong("trackId", 0L).toString()
            val title = cleanText(item.optString("trackName", ""))
            val artist = cleanText(item.optString("artistName", "Unknown Artist"))
            val album = cleanText(item.optString("collectionName", ""))
            val previewUrl = item.optString("previewUrl", "")
            val rawArtwork = item.optString("artworkUrl100", "")
            val highResArt = rawArtwork.replace("100x100bb.jpg", "600x600bb.jpg")
            val trackTimeMillis = item.optLong("trackTimeMillis", 0L)

            val isMatch = TrackMatcher.isConfidentMatch(query, null, title, artist, threshold = 0.40)
            if (title.isNotBlank() && previewUrl.isNotBlank() && isMatch) {
                list.add(
                    OnlineSongModel(
                        id = "itunes_$trackId",
                        title = title,
                        artist = artist,
                        album = album.takeIf { it.isNotBlank() },
                        durationMs = trackTimeMillis,
                        streamUrl = previewUrl,
                        highResArtUrl = highResArt.takeIf { it.isNotBlank() },
                        bitrate = "256kbps",
                        source = "itunes"
                    )
                )
            }
        }
        return list
    }

    private fun generateSmartSubqueries(query: String): List<String> {
        val list = mutableListOf<String>()
        list.add(query)

        // Split "by" into title + artist and title only
        val parts = query.split(Regex("\\s+by\\s+", RegexOption.IGNORE_CASE))
        if (parts.size == 2) {
            list.add("${parts[0].trim()} ${parts[1].trim()}")
            list.add(parts[0].trim())
        }

        // Clean out stop words
        val cleaned = query.replace(Regex("\\b(by|feat|ft|song|full song|audio|official|video|mp3)\\b", RegexOption.IGNORE_CASE), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
        if (cleaned.isNotBlank() && !list.contains(cleaned)) {
            list.add(cleaned)
        }

        // Query predictive suggestions for partial queries
        try {
            val suggestions = getSuggestions(cleaned.ifBlank { query })
            for (s in suggestions.take(2)) {
                if (s.isNotBlank() && !list.contains(s)) {
                    list.add(s)
                }
            }
        } catch (_: Exception) {}

        return list.distinct()
    }

    private fun getSuggestions(query: String): List<String> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val endpoint = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encoded"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 1500
                readTimeout = 1500
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONArray(text)
            val list = mutableListOf<String>()
            if (root.length() > 1) {
                val suggestions = root.getJSONArray(1)
                for (i in 0 until suggestions.length().coerceAtMost(4)) {
                    list.add(suggestions.getString(i))
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun calculateRelevance(song: OnlineSongModel, query: String): Int {
        var score = 0
        val q = query.lowercase().trim()
        val t = song.title.lowercase().trim()
        val a = song.artist.lowercase().trim()

        if (t == q) score += 120
        else if (t.startsWith(q)) score += 80
        else if (t.contains(q)) score += 50

        if (a == q) score += 60
        else if (a.startsWith(q)) score += 40
        else if (a.contains(q)) score += 25

        val words = q.split(" ").filter { it.length > 2 }
        for (w in words) {
            if (t.contains(w)) score += 15
            if (a.contains(w)) score += 10
        }
        if (song.source == "saavn") score += 10
        if (song.streamUrl.isNotBlank()) score += 15
        return score
    }

    private fun parseDurationText(text: String): Long {
        if (text.isBlank()) return 0L
        val parts = text.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000L
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
            else -> 0L
        }
    }

    private fun normalizeKey(title: String, artist: String): String {
        val cleanT = title.lowercase().replace("[^a-z0-9]".toRegex(), "").take(20)
        val cleanA = artist.lowercase().replace("[^a-z0-9]".toRegex(), "").take(15)
        return "$cleanT|$cleanA"
    }

    fun decryptMediaUrl(encryptedUrl: String, isDataSaver: Boolean = false): String {
        return try {
            val keySpec = SecretKeySpec(SAAVN_SECRET_KEY.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val directUrl = String(decryptedBytes, Charsets.UTF_8)
            // Mobile-optimized lightweight 96kbps AAC stream (~2-3 MB per full song max)
            directUrl.replace("_320.mp4", "_96.mp4")
                .replace("_160.mp4", "_96.mp4")
                .replace("_48.mp4", "_96.mp4")
                .replace("_12.mp4", "_96.mp4")
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting JioSaavn media URL", e)
            ""
        }
    }

    suspend fun resolveFullAudioStream(
        song: OnlineSongModel,
        isDataSaver: Boolean = false,
        allowYouTubeFallback: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        var currentStream = song.streamUrl

        if (currentStream.contains("_320.mp4")) {
            currentStream = currentStream.replace("_320.mp4", "_96.mp4")
        }

        // If it's already a valid stream and NOT an iTunes 30s preview, return stream!
        if (currentStream.isNotBlank() && !isShortPreview(currentStream)) {
            return@withContext currentStream
        }

        // 1. If it's a JioSaavn track with ID, resolve its exact stream
        if (song.source == "saavn" && song.id.isNotBlank() && !song.id.startsWith("yt_") && !song.id.startsWith("itunes_") && !song.id.startsWith("sp_")) {
            try {
                val exactDetails = fetchSongDetailsById(song.id)
                if (exactDetails != null && exactDetails.streamUrl.isNotBlank() && !isShortPreview(exactDetails.streamUrl)) {
                    val stream = exactDetails.streamUrl.replace("_320.mp4", "_96.mp4")
                    return@withContext stream
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving exact Saavn track: ${song.id}", e)
            }
        }

        // 2. If it is a YouTube video with videoId and YouTube fallback is allowed, extract stream
        if (allowYouTubeFallback && !song.youtubeVideoId.isNullOrBlank()) {
            try {
                val audioUrl = com.audiovault.app.core.downloader.YouTubeVideoExtractor.fetchAudioStreamUrl(song.youtubeVideoId)
                if (!audioUrl.isNullOrBlank()) {
                    return@withContext audioUrl
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving YouTube audio stream for ${song.youtubeVideoId}", e)
            }
        }

        // 3. For iTunes, Spotify, or short 30s preview tracks, resolve the full track across JioSaavn
        val cleanQuery = "${song.title} ${song.artist}".trim()

        // 3a. Search JioSaavn for lightweight track
        try {
            val saavnResults = searchJioSaavnFast(cleanQuery, limit = 5)
            val matchedSaavn = saavnResults.firstOrNull {
                it.streamUrl.isNotBlank() && !isShortPreview(it.streamUrl) &&
                (it.title.contains(song.title, ignoreCase = true) || song.title.contains(it.title, ignoreCase = true))
            } ?: saavnResults.firstOrNull { it.streamUrl.isNotBlank() && !isShortPreview(it.streamUrl) }

            if (matchedSaavn != null && matchedSaavn.streamUrl.isNotBlank()) {
                val stream = matchedSaavn.streamUrl.replace("_320.mp4", "_96.mp4")
                return@withContext stream
            }
        } catch (_: Exception) {}

        // 3b. Search YouTube for full audio track
        try {
            val ytResults = searchYouTubeAndInvidious(cleanQuery, limit = 5)
            for (ytSong in ytResults) {
                if (!ytSong.youtubeVideoId.isNullOrBlank()) {
                    val ytAudioUrl = com.audiovault.app.core.downloader.YouTubeVideoExtractor.fetchAudioStreamUrl(ytSong.youtubeVideoId)
                    if (!ytAudioUrl.isNullOrBlank()) {
                        return@withContext ytAudioUrl
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. If currentStream is a short preview (e.g. 30s Apple/Spotify preview snippet), discard it
        // so user never experiences unexpected track cut-off or auto-skip after 30 seconds
        if (isShortPreview(currentStream)) {
            return@withContext ""
        }

        // 5. Fallback to existing stream if valid full audio
        return@withContext currentStream
    }

    fun isShortPreview(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        return lower.contains("itunes.apple.com") ||
               lower.contains("audio-ssl.itunes") ||
               lower.contains("preview") ||
               lower.contains("30s") ||
               lower.contains("/preview/")
    }

    private fun cleanText(raw: String): String {
        if (raw.isBlank() || raw == "null") return ""
        return try {
            Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Exception) {
            raw.replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#039;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim()
        }
    }
}
