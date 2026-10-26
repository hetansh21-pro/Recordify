package com.audiovault.app.core.online

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object SpotifyWebMetadataFetcher {

    private const val TAG = "Recordify/SpotifyFetcher"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    private val NEXT_DATA_PATTERN = Pattern.compile("""<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""", Pattern.DOTALL)

    /**
     * Fetches a complete Spotify album with all tracks and high-res cover art.
     */
    suspend fun fetchAlbum(albumId: String): OnlineAlbumModel? = withContext(Dispatchers.IO) {
        if (albumId.isBlank()) return@withContext null
        try {
            val embedUrl = "https://open.spotify.com/embed/album/$albumId"
            val html = fetchHtml(embedUrl) ?: return@withContext fetchAlbumViaOEmbed(albumId)
            val jsonText = extractNextDataJson(html) ?: return@withContext fetchAlbumViaOEmbed(albumId)

            val root = JSONObject(jsonText)
            val entity = root.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity") ?: return@withContext fetchAlbumViaOEmbed(albumId)

            val title = entity.optString("name", entity.optString("title", "Spotify Album"))
            val artist = entity.optString("subtitle", "Various Artists")
            val coverArtUrl = extractBestImage(entity)

            val trackListArray = entity.optJSONArray("trackList")
            val songs = mutableListOf<OnlineSongModel>()

            if (trackListArray != null) {
                for (i in 0 until trackListArray.length()) {
                    val trackObj = trackListArray.optJSONObject(i) ?: continue
                    val trackTitle = trackObj.optString("title", "")
                    if (trackTitle.isBlank()) continue

                    val trackArtist = trackObj.optString("subtitle", artist)
                    val durationMs = trackObj.optLong("duration", 0L)
                    val uri = trackObj.optString("uri", "")
                    val trackId = if (uri.contains(":")) uri.substringAfterLast(":") else uri
                    val audioPreview = trackObj.optJSONObject("audioPreview")?.optString("url", "") ?: ""

                    songs.add(
                        OnlineSongModel(
                            id = "sp_$trackId",
                            title = trackTitle,
                            artist = trackArtist,
                            album = title,
                            durationMs = durationMs,
                            highResArtUrl = coverArtUrl,
                            source = "spotify",
                            bitrate = "320kbps",
                            streamUrl = "" // Playable CDN stream is dynamically resolved on demand!
                        )
                    )
                }
            }

            OnlineAlbumModel(
                id = "sp_$albumId",
                title = title,
                artist = artist,
                coverArtUrl = coverArtUrl,
                songs = songs,
                songCount = songs.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Spotify album $albumId", e)
            fetchAlbumViaOEmbed(albumId)
        }
    }

    /**
     * Fetches a Spotify playlist and converts it into an album-like collection with tracks.
     */
    suspend fun fetchPlaylist(playlistId: String): OnlineAlbumModel? = withContext(Dispatchers.IO) {
        if (playlistId.isBlank()) return@withContext null
        try {
            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val html = fetchHtml(embedUrl) ?: return@withContext null
            val jsonText = extractNextDataJson(html) ?: return@withContext null

            val root = JSONObject(jsonText)
            val entity = root.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity") ?: return@withContext null

            val title = entity.optString("name", entity.optString("title", "Spotify Playlist"))
            val artist = entity.optString("subtitle", "Spotify")
            val coverArtUrl = extractBestImage(entity)

            val trackListArray = entity.optJSONArray("trackList")
            val songs = mutableListOf<OnlineSongModel>()

            if (trackListArray != null) {
                for (i in 0 until trackListArray.length()) {
                    val trackObj = trackListArray.optJSONObject(i) ?: continue
                    val trackTitle = trackObj.optString("title", "")
                    if (trackTitle.isBlank()) continue

                    val trackArtist = trackObj.optString("subtitle", artist)
                    val durationMs = trackObj.optLong("duration", 0L)
                    val uri = trackObj.optString("uri", "")
                    val trackId = if (uri.contains(":")) uri.substringAfterLast(":") else uri
                    val audioPreview = trackObj.optJSONObject("audioPreview")?.optString("url", "") ?: ""

                    songs.add(
                        OnlineSongModel(
                            id = "sp_$trackId",
                            title = trackTitle,
                            artist = trackArtist,
                            album = title,
                            durationMs = durationMs,
                            highResArtUrl = coverArtUrl,
                            source = "spotify",
                            bitrate = "320kbps",
                            streamUrl = ""
                        )
                    )
                }
            }

            OnlineAlbumModel(
                id = "sp_pl_$playlistId",
                title = title,
                artist = artist,
                coverArtUrl = coverArtUrl,
                songs = songs,
                songCount = songs.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Spotify playlist $playlistId", e)
            null
        }
    }

    /**
     * Fetches metadata for a single Spotify track.
     */
    suspend fun fetchTrack(trackId: String): OnlineSongModel? = withContext(Dispatchers.IO) {
        if (trackId.isBlank()) return@withContext null
        try {
            val embedUrl = "https://open.spotify.com/embed/track/$trackId"
            val html = fetchHtml(embedUrl) ?: return@withContext null
            val jsonText = extractNextDataJson(html) ?: return@withContext null

            val root = JSONObject(jsonText)
            val entity = root.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity") ?: return@withContext null

            val title = entity.optString("name", entity.optString("title", ""))
            val artist = entity.optString("subtitle", "")
            val coverArtUrl = extractBestImage(entity)
            val durationMs = entity.optLong("duration", 0L)
            val audioPreview = entity.optJSONObject("audioPreview")?.optString("url", "") ?: ""

            OnlineSongModel(
                id = "sp_$trackId",
                title = title,
                artist = artist,
                album = "Spotify Single",
                durationMs = durationMs,
                highResArtUrl = coverArtUrl,
                source = "spotify",
                bitrate = "320kbps",
                streamUrl = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Spotify track $trackId", e)
            null
        }
    }

    private fun extractBestImage(entity: JSONObject): String {
        val visualIdentity = entity.optJSONObject("visualIdentity")
        val imageArray = visualIdentity?.optJSONArray("image")
        if (imageArray != null && imageArray.length() > 0) {
            var bestUrl = ""
            var bestWidth = 0
            for (i in 0 until imageArray.length()) {
                val img = imageArray.optJSONObject(i) ?: continue
                val width = img.optInt("maxWidth", 0)
                val url = img.optString("url", "")
                if (width >= bestWidth && url.isNotBlank()) {
                    bestWidth = width
                    bestUrl = url
                }
            }
            if (bestUrl.isNotBlank()) return bestUrl
        }
        return ""
    }

    private fun extractNextDataJson(html: String): String? {
        val matcher = NEXT_DATA_PATTERN.matcher(html)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun fetchHtml(urlString: String): String? {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP GET failed for $urlString", e)
            null
        }
    }

    private fun fetchAlbumViaOEmbed(albumId: String): OnlineAlbumModel? {
        return try {
            val oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/album/$albumId"
            val conn = (URL(oembedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(text)
            val title = obj.optString("title", "Spotify Album")
            val thumbnail = obj.optString("thumbnail_url", "")

            OnlineAlbumModel(
                id = "sp_$albumId",
                title = title,
                artist = "Spotify Album",
                coverArtUrl = thumbnail.takeIf { it.isNotBlank() },
                songs = emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }
}
