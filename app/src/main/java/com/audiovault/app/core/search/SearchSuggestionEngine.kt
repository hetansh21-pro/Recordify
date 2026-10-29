package com.audiovault.app.core.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Autocomplete and contextual query suggestion engine.
 * Generates Spotify-style search suggestions with regional Indian awareness.
 */
object SearchSuggestionEngine {

    /**
     * Pre-mapped high-intent queries matching Spotify India search suggestions.
     */
    private val PRESET_SUGGESTIONS = mapOf(
        "old songs" to listOf(
            "old songs hindi 90s",
            "old songs new version",
            "old songs",
            "old songs mashup",
            "old songs mashup hindi"
        ),
        "old song" to listOf(
            "old songs hindi 90s",
            "old songs new version",
            "old songs",
            "old songs mashup",
            "old songs mashup hindi"
        ),
        "olds" to listOf(
            "old songs hindi 90s",
            "old songs new version",
            "old songs",
            "old songs mashup",
            "old songs mashup hindi"
        ),
        "sad songs" to listOf(
            "sad songs hindi",
            "sad songs playlist",
            "sad songs english",
            "sad songs",
            "sad songs slowed and reverb"
        ),
        "sad song" to listOf(
            "sad songs hindi",
            "sad songs playlist",
            "sad songs english",
            "sad songs",
            "sad songs slowed and reverb"
        ),
        "90s songs" to listOf(
            "90s songs bollywood",
            "90s songs romantic",
            "90s songs hindi",
            "90s songs dance hits",
            "90s songs evergreen"
        ),
        "romantic songs" to listOf(
            "romantic songs hindi",
            "romantic songs bollywood",
            "romantic songs arijit singh",
            "romantic songs love hits",
            "romantic songs 90s"
        ),
        "love songs" to listOf(
            "love songs hindi",
            "love songs bollywood",
            "love songs arijit singh",
            "love songs romantic hits",
            "love songs 90s"
        ),
        "party songs" to listOf(
            "party songs hindi",
            "party songs bollywood dance",
            "party songs punjabi",
            "party songs club hits",
            "party songs dj mix"
        ),
        "punjabi songs" to listOf(
            "punjabi songs hits",
            "punjabi songs new",
            "punjabi songs dance",
            "punjabi songs romantic",
            "punjabi songs sad"
        ),
        "gujarati songs" to listOf(
            "gujarati songs garba",
            "gujarati songs romantic",
            "gujarati songs geet",
            "gujarati songs new",
            "gujarati songs folk"
        )
    )

    /**
     * Generates 3 to 5 contextual search suggestions for a given input query.
     */
    fun getLocalSuggestions(rawQuery: String): List<String> {
        val q = rawQuery.trim().lowercase()
        if (q.isBlank()) return emptyList()

        // 1. Direct preset match
        PRESET_SUGGESTIONS[q]?.let { return it }

        // 2. Partial preset match
        for ((key, list) in PRESET_SUGGESTIONS) {
            if (q.startsWith(key) || key.startsWith(q)) {
                return list
            }
        }

        // 3. Matched artist query expansion (e.g. "atif" -> "Atif Aslam all songs", "Atif Aslam", etc.)
        val matchedArtist = com.audiovault.app.core.catalog.ArtistImageCatalog.allArtistNames.firstOrNull {
            it.startsWith(q) || it.contains(" $q") || q.startsWith(it)
        }
        if (matchedArtist != null) {
            val capitalized = matchedArtist.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            return listOf(
                "$capitalized all songs",
                capitalized,
                "$capitalized top hits",
                "$capitalized song",
                "$capitalized mashup"
            )
        }

        // 4. Dynamic smart expansion for music search context
        return listOf(
            "$rawQuery all songs",
            rawQuery,
            "$rawQuery top hits",
            "$rawQuery song",
            "$rawQuery playlist"
        )
    }

    /**
     * Fetches live suggestions from JioSaavn autocomplete API with fallback to local suggestions.
     */
    suspend fun fetchLiveSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext emptyList()

        val local = getLocalSuggestions(clean)
        try {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val endpoint = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&query=$encoded&_format=json&ctx=web6dot0"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val onlineTitles = mutableListOf<String>()

                // Songs
                val songs = root.optJSONObject("songs")?.optJSONArray("data")
                if (songs != null) {
                    for (i in 0 until minOf(songs.length(), 2)) {
                        val s = songs.getJSONObject(i).optString("title", "")
                        if (s.isNotBlank() && !onlineTitles.contains(s)) onlineTitles.add(s)
                    }
                }

                // Albums
                val albums = root.optJSONObject("albums")?.optJSONArray("data")
                if (albums != null) {
                    for (i in 0 until minOf(albums.length(), 2)) {
                        val a = albums.getJSONObject(i).optString("title", "")
                        if (a.isNotBlank() && !onlineTitles.contains(a)) onlineTitles.add(a)
                    }
                }

                if (onlineTitles.isNotEmpty()) {
                    return@withContext (local.take(3) + onlineTitles).distinct().take(5)
                }
            }
        } catch (_: Exception) {}

        local
    }
}
