package com.audiovault.app.core.online

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

/**
 * Spotify Live Home Page Daily Sync Engine.
 * Fetches real Spotify daily flagship charts, playlists, and new releases every single day,
 * matching Recordify's Home Page with Spotify's live daily curated content.
 * Automatically invalidates and refreshes cache at the start of each new calendar day.
 */
object SpotifyLiveHomeSync {

    private const val TAG = "Recordify/SpotifySync"
    private const val CACHE_FILE = "spotify_home_live_cache.json"

    // Official Spotify Daily Chart & Shelf Playlists that update every day
    private val DAILY_SPOTIFY_PLAYLIST_IDS = listOf(
        "37i9dQZF1DXcBWIGoYBM5M", // Today's Top Hits (50 tracks - #1 global chart)
        "37i9dQZF1DX4JAvHpjipBk", // New Music Friday (100 tracks - fresh weekly & daily releases)
        "37i9dQZF1DXbYM3nMM0oPk", // Mega Hit Mix (75 tracks - high energy hits)
        "37i9dQZF1DX0XUsuxWHRQd", // RapCaviar / Top Charts (51 tracks)
        "37i9dQZF1DX1s9knjP51Oa", // Calm Vibes & Acoustic (100 tracks)
        "37i9dQZF1DX4o1oenSJRJd", // All-Out 2000s
        "37i9dQZF1DXbTxeAdrVG2l"  // All-Out 90s Classics
    )

    suspend fun getLiveDailyHomeContent(
        context: Context,
        forceRefresh: Boolean = false
    ): SpotifyHomeContent? = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentDay = cal.get(Calendar.DAY_OF_YEAR)

        val cacheFile = File(context.filesDir, CACHE_FILE)

        // 1. Check if cache is fresh from today
        if (!forceRefresh && cacheFile.exists()) {
            try {
                val cached = readCache(cacheFile, currentYear, currentDay)
                if (cached != null) {
                    Log.i(TAG, "Loaded live Spotify home content from today's cache (day=$currentDay)")
                    return@withContext cached
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cache read failed, fetching fresh: ${e.message}")
            }
        }

        // 2. Fetch fresh live daily playlists from Spotify in parallel
        Log.i(TAG, "Fetching fresh live Spotify daily playlists for day $currentDay...")
        try {
            val freshContent = fetchLiveDailySpotify(DAILY_SPOTIFY_PLAYLIST_IDS)
            if (freshContent.recommendedToday.isNotEmpty()) {
                writeCache(cacheFile, currentYear, currentDay, freshContent)
                return@withContext freshContent
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live Spotify playlists", e)
        }

        // Fallback to cache if available
        if (cacheFile.exists()) {
            try {
                return@withContext readCache(cacheFile, -1, -1)
            } catch (_: Exception) {}
        }

        null
    }

    private suspend fun fetchLiveDailySpotify(playlistIds: List<String>): SpotifyHomeContent = coroutineScope {
        val deferredPlaylists = playlistIds.map { pid ->
            async {
                try {
                    SpotifyWebMetadataFetcher.fetchPlaylist(pid)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed fetching playlist $pid: ${e.message}")
                    null
                }
            }
        }

        val results = deferredPlaylists.awaitAll().filterNotNull().filter { it.songs.isNotEmpty() }

        val recommended = mutableListOf<OnlineAlbumModel>()
        val popular = mutableListOf<OnlineAlbumModel>()
        val latestReleases = mutableListOf<OnlineSongModel>()

        for ((index, playlist) in results.withIndex()) {
            val albumModel = playlist.copy(
                type = "Playlist",
                source = "spotify",
                songCount = playlist.songs.size
            )

            if (index < 4) {
                recommended.add(albumModel)
            } else {
                popular.add(albumModel)
            }

            // Extract fresh top songs for New Releases
            if (playlist.title.contains("New Music", ignoreCase = true) ||
                playlist.title.contains("Top Hits", ignoreCase = true)
            ) {
                for (song in playlist.songs.take(12)) {
                    if (latestReleases.none { it.id == song.id || it.title.equals(song.title, ignoreCase = true) }) {
                        latestReleases.add(song)
                    }
                }
            }
        }

        SpotifyHomeContent(
            recommendedToday = recommended,
            popularAlbumsAndSingles = popular,
            latestReleases = latestReleases
        )
    }

    private fun readCache(file: File, checkYear: Int, checkDay: Int): SpotifyHomeContent? {
        val text = file.readText()
        if (text.isBlank()) return null
        val root = JSONObject(text)

        val cachedYear = root.optInt("year", -1)
        val cachedDay = root.optInt("dayOfYear", -1)

        if (checkYear > 0 && checkDay > 0 && (cachedYear != checkYear || cachedDay != checkDay)) {
            return null // Stale cache from a previous day
        }

        val recArr = root.optJSONArray("recommendedToday") ?: JSONArray()
        val popArr = root.optJSONArray("popularAlbums") ?: JSONArray()
        val relArr = root.optJSONArray("latestReleases") ?: JSONArray()

        val recommended = parseAlbumsJson(recArr)
        val popular = parseAlbumsJson(popArr)
        val releases = parseSongsJson(relArr)

        if (recommended.isEmpty() && popular.isEmpty()) return null

        return SpotifyHomeContent(
            recommendedToday = recommended,
            popularAlbumsAndSingles = popular,
            latestReleases = releases
        )
    }

    private fun writeCache(file: File, year: Int, day: Int, content: SpotifyHomeContent) {
        try {
            val root = JSONObject().apply {
                put("year", year)
                put("dayOfYear", day)
                put("timestamp", System.currentTimeMillis())

                put("recommendedToday", serializeAlbumsJson(content.recommendedToday))
                put("popularAlbums", serializeAlbumsJson(content.popularAlbumsAndSingles))
                put("latestReleases", serializeSongsJson(content.latestReleases))
            }
            file.writeText(root.toString())
            Log.i(TAG, "Wrote live Spotify home content to cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing cache", e)
        }
    }

    private fun parseAlbumsJson(arr: JSONArray): List<OnlineAlbumModel> {
        val list = mutableListOf<OnlineAlbumModel>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id", "")
            val title = obj.optString("title", "")
            if (id.isBlank() || title.isBlank()) continue

            val artist = obj.optString("artist", "Spotify")
            val cover = obj.optString("coverArtUrl", "")
            val songs = parseSongsJson(obj.optJSONArray("songs") ?: JSONArray())

            list.add(
                OnlineAlbumModel(
                    id = id,
                    title = title,
                    artist = artist,
                    coverArtUrl = cover.takeIf { it.isNotBlank() },
                    songs = songs,
                    songCount = songs.size,
                    type = "Playlist",
                    source = "spotify"
                )
            )
        }
        return list
    }

    private fun parseSongsJson(arr: JSONArray): List<OnlineSongModel> {
        val list = mutableListOf<OnlineSongModel>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id", "")
            val title = obj.optString("title", "")
            if (title.isBlank()) continue

            list.add(
                OnlineSongModel(
                    id = id,
                    title = title,
                    artist = obj.optString("artist", "Various Artists"),
                    album = obj.optString("album", ""),
                    durationMs = obj.optLong("durationMs", 0L),
                    highResArtUrl = obj.optString("highResArtUrl", "").takeIf { it.isNotBlank() },
                    source = "spotify",
                    bitrate = "320kbps",
                    streamUrl = ""
                )
            )
        }
        return list
    }

    private fun serializeAlbumsJson(albums: List<OnlineAlbumModel>): JSONArray {
        val arr = JSONArray()
        for (a in albums) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("title", a.title)
                put("artist", a.artist ?: "")
                put("coverArtUrl", a.coverArtUrl ?: "")
                put("songs", serializeSongsJson(a.songs))
            }
            arr.put(obj)
        }
        return arr
    }

    private fun serializeSongsJson(songs: List<OnlineSongModel>): JSONArray {
        val arr = JSONArray()
        for (s in songs) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album ?: "")
                put("durationMs", s.durationMs)
                put("highResArtUrl", s.highResArtUrl ?: "")
            }
            arr.put(obj)
        }
        return arr
    }
}
