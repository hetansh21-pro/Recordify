package com.audiovault.app.core.online

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

private const val TAG = "Recordify/SpotifySearch"
private const val EMBED_BOOTSTRAP_URL = "https://open.spotify.com/embed/track/7bxaFZ1O3cHkgLKMsdC3xR"
private const val PATHFINDER_URL = "https://api-partner.spotify.com/pathfinder/v1/query"
private const val SEARCH_DESKTOP_HASH = "75bbf6bfcfdf85b8fc828417bfad92b7cd66bf7f556d85670f4da8292373ebec"
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

data class SpotifyTopResultItem(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val type: String, // "Song", "Artist", "Album", "Playlist"
    val uri: String,
    val song: OnlineSongModel? = null,
    val album: OnlineAlbumModel? = null
)

data class SpotifySearchResult(
    val tracks: List<OnlineSongModel> = emptyList(),
    val albums: List<OnlineAlbumModel> = emptyList(),
    val artists: List<ArtistItemModel> = emptyList(),
    val topResult: SpotifyTopResultItem? = null
)

object SpotifyWebSearchService {

    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var tokenExpiresAtMs: Long = 0L

    private val TOKEN_REGEX = Pattern.compile(""""accessToken":"([^"]+)"""")
    private val TOKEN_EXP_REGEX = Pattern.compile(""""accessTokenExpirationTimestampMs":([0-9]+)""")

    /**
     * Searches Spotify Web directly in the background using the Pathfinder GraphQL engine.
     * Returns authentic Spotify tracks, albums, artists, and top match.
     */
    suspend fun searchSpotify(query: String, limit: Int = 15): SpotifySearchResult? = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext null

        try {
            val token = getOrFetchAccessToken()
            if (token.isNullOrBlank()) {
                Log.w(TAG, "No valid Spotify token available for search")
                return@withContext null
            }

            val variablesJson = JSONObject().apply {
                put("searchTerm", cleanQuery)
                put("offset", 0)
                put("limit", limit)
                put("numberOfTopResults", 3)
            }

            val extensionsJson = JSONObject().apply {
                put("persistedQuery", JSONObject().apply {
                    put("version", 1)
                    put("sha256Hash", SEARCH_DESKTOP_HASH)
                })
            }

            val queryParams = "operationName=searchDesktop" +
                    "&variables=" + URLEncoder.encode(variablesJson.toString(), "UTF-8") +
                    "&extensions=" + URLEncoder.encode(extensionsJson.toString(), "UTF-8")

            val fullUrl = "$PATHFINDER_URL?$queryParams"

            val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("app-platform", "WebPlayer")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode == 401 || responseCode == 403) {
                // Invalidate cached token on auth rejection
                cachedAccessToken = null
                tokenExpiresAtMs = 0L
                conn.disconnect()
                return@withContext null
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Spotify Pathfinder returned HTTP $responseCode")
                conn.disconnect()
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            conn.disconnect()

            parseSearchResponse(cleanQuery, sb.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Spotify Web Search failed for query '$cleanQuery'", e)
            null
        }
    }

    private fun parseSearchResponse(rawQuery: String, jsonStr: String): SpotifySearchResult {
        val root = JSONObject(jsonStr)
        val dataObj = root.optJSONObject("data") ?: return SpotifySearchResult()
        val searchObj = dataObj.optJSONObject("search") ?: return SpotifySearchResult()

        // 1. Parse Tracks
        val tracksList = mutableListOf<OnlineSongModel>()
        val tracksObj = searchObj.optJSONObject("tracks")
        val trackItems = tracksObj?.optJSONArray("items")
        if (trackItems != null) {
            for (i in 0 until trackItems.length()) {
                val item = trackItems.optJSONObject(i) ?: continue
                val track = item.optJSONObject("track") ?: continue
                val id = track.optString("id", "")
                val name = track.optString("name", "")
                if (id.isBlank() || name.isBlank()) continue

                val albumObj = track.optJSONObject("album")
                val albumName = albumObj?.optString("name", "")
                val albumArtUrl = extractBestImage(albumObj?.optJSONObject("coverArt")?.optJSONArray("sources"))

                val artistsObj = track.optJSONObject("artists")
                val artistItems = artistsObj?.optJSONArray("items")
                val artistNames = mutableListOf<String>()
                if (artistItems != null) {
                    for (j in 0 until artistItems.length()) {
                        val a = artistItems.optJSONObject(j)?.optJSONObject("profile")?.optString("name", "")
                        if (!a.isNullOrBlank()) artistNames.add(a)
                    }
                }
                val artistDisplay = if (artistNames.isNotEmpty()) artistNames.joinToString(", ") else "Unknown Artist"

                val durationMs = track.optJSONObject("duration")?.optLong("totalMilliseconds", 0L) ?: 0L

                tracksList.add(
                    OnlineSongModel(
                        id = "sp_$id",
                        title = name,
                        artist = artistDisplay,
                        album = albumName,
                        durationMs = durationMs,
                        highResArtUrl = albumArtUrl,
                        source = "spotify",
                        bitrate = "320kbps",
                        streamUrl = "" // Stream resolved dynamically on-demand with 320kbps audio!
                    )
                )
            }
        }

        // 2. Parse Albums
        val albumsList = mutableListOf<OnlineAlbumModel>()
        val albumsObj = searchObj.optJSONObject("albums")
        val albumItems = albumsObj?.optJSONArray("items")
        if (albumItems != null) {
            for (i in 0 until albumItems.length()) {
                val item = albumItems.optJSONObject(i) ?: continue
                val name = item.optString("name", "")
                val uri = item.optString("uri", "")
                if (name.isBlank() || uri.isBlank()) continue
                val albumId = uri.substringAfterLast(":")

                val artUrl = extractBestImage(item.optJSONObject("coverArt")?.optJSONArray("sources"))
                val type = item.optString("type", "Album")

                val artistsObj = item.optJSONObject("artists")
                val artistItems = artistsObj?.optJSONArray("items")
                val artistNames = mutableListOf<String>()
                if (artistItems != null) {
                    for (j in 0 until artistItems.length()) {
                        val a = artistItems.optJSONObject(j)?.optJSONObject("profile")?.optString("name", "")
                        if (!a.isNullOrBlank()) artistNames.add(a)
                    }
                }
                val artistDisplay = if (artistNames.isNotEmpty()) artistNames.joinToString(", ") else "Spotify"
                val year = item.optJSONObject("date")?.optString("year", "")

                albumsList.add(
                    OnlineAlbumModel(
                        id = "sp_album_$albumId",
                        title = name,
                        artist = artistDisplay,
                        coverArtUrl = artUrl,
                        year = year,
                        type = if (type.equals("SINGLE", ignoreCase = true)) "Single" else "Album",
                        source = "spotify",
                        songs = emptyList() // Fetched on demand via SpotifyWebMetadataFetcher.fetchAlbum
                    )
                )
            }
        }

        // 3. Parse Artists
        val artistsList = mutableListOf<ArtistItemModel>()
        val artistsObj = searchObj.optJSONObject("artists")
        val artistItems = artistsObj?.optJSONArray("items")
        if (artistItems != null) {
            for (i in 0 until artistItems.length()) {
                val item = artistItems.optJSONObject(i) ?: continue
                val uri = item.optString("uri", "")
                val name = item.optJSONObject("profile")?.optString("name", "") ?: continue
                if (name.isBlank() || uri.isBlank()) continue
                val artistId = uri.substringAfterLast(":")

                val avatarSources = item.optJSONObject("visuals")
                    ?.optJSONObject("avatarImage")
                    ?.optJSONArray("sources")
                val avatarUrl = extractBestImage(avatarSources)

                artistsList.add(
                    ArtistItemModel(
                        id = "sp_artist_$artistId",
                        name = name,
                        imageUrl = avatarUrl,
                        role = "Artist"
                    )
                )
            }
        }

        // 4. Parse Spotify "Top result"
        var topResult: SpotifyTopResultItem? = null
        val topResultsObj = searchObj.optJSONObject("topResults")
        val topItems = topResultsObj?.optJSONArray("items")
        if (topItems != null && topItems.length() > 0) {
            val firstTop = topItems.optJSONObject(0)
            if (firstTop != null) {
                val uri = firstTop.optString("uri", "")
                val name = firstTop.optString("name", "")
                val description = firstTop.optString("description", "")

                if (uri.startsWith("spotify:artist:")) {
                    val matchingArtist = artistsList.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    topResult = SpotifyTopResultItem(
                        title = name.ifBlank { matchingArtist?.name ?: "" },
                        subtitle = "Artist",
                        imageUrl = matchingArtist?.imageUrl ?: "",
                        type = "Artist",
                        uri = uri
                    )
                } else if (uri.startsWith("spotify:track:")) {
                    val matchingTrack = tracksList.firstOrNull { it.title.equals(name, ignoreCase = true) }
                        ?: tracksList.firstOrNull()
                    if (matchingTrack != null) {
                        topResult = SpotifyTopResultItem(
                            title = matchingTrack.title,
                            subtitle = "Song • ${matchingTrack.artist}",
                            imageUrl = matchingTrack.highResArtUrl ?: "",
                            type = "Song",
                            uri = uri,
                            song = matchingTrack
                        )
                    }
                } else if (uri.startsWith("spotify:album:")) {
                    val matchingAlbum = albumsList.firstOrNull { it.title.equals(name, ignoreCase = true) }
                        ?: albumsList.firstOrNull()
                    if (matchingAlbum != null) {
                        topResult = SpotifyTopResultItem(
                            title = matchingAlbum.title,
                            subtitle = "Album • ${matchingAlbum.artist ?: "Various Artists"}",
                            imageUrl = matchingAlbum.coverArtUrl ?: "",
                            type = "Album",
                            uri = uri,
                            album = matchingAlbum
                        )
                    }
                }
            }
        }

        // If no explicit topResult parsed, pick best match from songs or artists
        if (topResult == null) {
            val exactArtist = artistsList.firstOrNull { it.name.equals(rawQuery, ignoreCase = true) }
            if (exactArtist != null) {
                topResult = SpotifyTopResultItem(
                    title = exactArtist.name,
                    subtitle = "Artist",
                    imageUrl = exactArtist.imageUrl,
                    type = "Artist",
                    uri = "spotify:artist:${exactArtist.id.removePrefix("sp_artist_")}"
                )
            } else if (tracksList.isNotEmpty()) {
                val firstSong = tracksList.first()
                topResult = SpotifyTopResultItem(
                    title = firstSong.title,
                    subtitle = "Song • ${firstSong.artist}",
                    imageUrl = firstSong.highResArtUrl ?: "",
                    type = "Song",
                    uri = "spotify:track:${firstSong.id.removePrefix("sp_")}",
                    song = firstSong
                )
            }
        }

        Log.d(TAG, "Spotify search for '$rawQuery' returned: ${tracksList.size} tracks, ${albumsList.size} albums, ${artistsList.size} artists, topResult=${topResult?.title}")

        return SpotifySearchResult(
            tracks = tracksList,
            albums = albumsList,
            artists = artistsList,
            topResult = topResult
        )
    }

    private fun extractBestImage(sources: JSONArray?): String {
        if (sources == null || sources.length() == 0) return ""
        var bestUrl = ""
        var bestDim = 0

        for (i in 0 until sources.length()) {
            val src = sources.optJSONObject(i) ?: continue
            val url = src.optString("url", "")
            val width = src.optInt("width", 0)
            val height = src.optInt("height", 0)
            val dim = maxOf(width, height)

            if (url.isNotBlank() && (dim >= bestDim || bestUrl.isBlank())) {
                bestDim = dim
                bestUrl = url
            }
        }

        return bestUrl
    }

    private suspend fun getOrFetchAccessToken(): String? {
        val now = System.currentTimeMillis()
        if (cachedAccessToken != null && now < (tokenExpiresAtMs - 120_000L)) {
            return cachedAccessToken
        }

        return try {
            val conn = (URL(EMBED_BOOTSTRAP_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            }

            val html = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val tokenMatcher = TOKEN_REGEX.matcher(html)
            val token = if (tokenMatcher.find()) tokenMatcher.group(1) else null

            val expMatcher = TOKEN_EXP_REGEX.matcher(html)
            val exp = if (expMatcher.find()) expMatcher.group(1)?.toLongOrNull() else null

            if (!token.isNullOrBlank()) {
                cachedAccessToken = token
                tokenExpiresAtMs = exp ?: (now + 3600_000L)
                Log.d(TAG, "Successfully captured fresh Spotify Web anonymous access token (expires at $tokenExpiresAtMs)")
                token
            } else {
                Log.w(TAG, "Could not extract Spotify accessToken from embed page")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed bootstrapping Spotify Web access token", e)
            null
        }
    }
}
