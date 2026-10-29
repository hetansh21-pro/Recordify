package com.audiovault.app.core.online

import android.util.Log
import com.audiovault.app.core.downloader.YouTubeVideoExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Pattern

enum class AlbumLinkType {
    SPOTIFY_ALBUM,
    SPOTIFY_PLAYLIST,
    JIOSAAVN_ALBUM,
    YOUTUBE_PLAYLIST,
    UNKNOWN
}

data class AlbumImportResult(
    val album: OnlineAlbumModel? = null,
    val success: Boolean = false,
    val message: String = ""
)

object AlbumLinkImporter {

    private const val TAG = "Recordify/AlbumImporter"

    private val SPOTIFY_ALBUM_REGEX = Pattern.compile("""open\.spotify\.com/(?:[a-zA-Z]{2,4}(?:-[a-zA-Z]{2,4})?/)?album/([a-zA-Z0-9]+)""", Pattern.CASE_INSENSITIVE)
    private val SPOTIFY_PLAYLIST_REGEX = Pattern.compile("""open\.spotify\.com/(?:[a-zA-Z]{2,4}(?:-[a-zA-Z]{2,4})?/)?playlist/([a-zA-Z0-9]+)""", Pattern.CASE_INSENSITIVE)
    private val JIOSAAVN_ALBUM_REGEX = Pattern.compile("""(?:jiosaavn|saavn)\.com/album/([^/]+)/([a-zA-Z0-9_-]+)""", Pattern.CASE_INSENSITIVE)
    private val YOUTUBE_PLAYLIST_REGEX = Pattern.compile("""(?:youtube\.com|youtu\.be)/(?:playlist\?list=|watch\?.*list=)([a-zA-Z0-9_-]+)""", Pattern.CASE_INSENSITIVE)

    fun detectLinkType(url: String): AlbumLinkType {
        val trimmed = url.trim()
        if (SPOTIFY_ALBUM_REGEX.matcher(trimmed).find() || trimmed.startsWith("spotify:album:")) {
            return AlbumLinkType.SPOTIFY_ALBUM
        }
        if (SPOTIFY_PLAYLIST_REGEX.matcher(trimmed).find() || trimmed.startsWith("spotify:playlist:")) {
            return AlbumLinkType.SPOTIFY_PLAYLIST
        }
        if (JIOSAAVN_ALBUM_REGEX.matcher(trimmed).find() || (trimmed.contains("jiosaavn.com") && trimmed.contains("/album/"))) {
            return AlbumLinkType.JIOSAAVN_ALBUM
        }
        if (YOUTUBE_PLAYLIST_REGEX.matcher(trimmed).find() || trimmed.contains("list=PL") || trimmed.contains("list=OLAK5uy_")) {
            return AlbumLinkType.YOUTUBE_PLAYLIST
        }
        return AlbumLinkType.UNKNOWN
    }

    fun isSupportedLink(url: String): Boolean = detectLinkType(url) != AlbumLinkType.UNKNOWN

    /**
     * Imports a complete album or playlist from any supported music link,
     * extracts all song names and metadata, and prepares it for instant playback in Recordify.
     */
    suspend fun importAlbumFromLink(rawUrl: String): AlbumImportResult = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        if (url.isBlank()) {
            return@withContext AlbumImportResult(message = "Please enter a valid link.")
        }

        val type = detectLinkType(url)
        Log.i(TAG, "Importing link of type $type: $url")

        try {
            when (type) {
                AlbumLinkType.SPOTIFY_ALBUM -> {
                    val albumId = extractId(url, SPOTIFY_ALBUM_REGEX) ?: url.substringAfterLast(":").substringBefore("?")
                    val album = SpotifyWebMetadataFetcher.fetchAlbum(albumId)
                    if (album != null && album.songs.isNotEmpty()) {
                        val enrichedAlbum = preResolveInitialTracks(album)
                        return@withContext AlbumImportResult(
                            album = enrichedAlbum,
                            success = true,
                            message = "Successfully imported '${album.title}' with ${album.songs.size} tracks!"
                        )
                    } else {
                        return@withContext AlbumImportResult(message = "Could not fetch tracks from this Spotify album.")
                    }
                }

                AlbumLinkType.SPOTIFY_PLAYLIST -> {
                    val playlistId = extractId(url, SPOTIFY_PLAYLIST_REGEX) ?: url.substringAfterLast(":").substringBefore("?")
                    val album = SpotifyWebMetadataFetcher.fetchPlaylist(playlistId)
                    if (album != null && album.songs.isNotEmpty()) {
                        val enrichedAlbum = preResolveInitialTracks(album)
                        return@withContext AlbumImportResult(
                            album = enrichedAlbum,
                            success = true,
                            message = "Successfully imported '${album.title}' with ${album.songs.size} tracks!"
                        )
                    } else {
                        return@withContext AlbumImportResult(message = "Could not fetch tracks from this Spotify playlist.")
                    }
                }

                AlbumLinkType.JIOSAAVN_ALBUM -> {
                    val matcher = JIOSAAVN_ALBUM_REGEX.matcher(url)
                    val albumName = if (matcher.find()) {
                        URLDecoder.decode(matcher.group(1) ?: "", "UTF-8").replace("-", " ")
                    } else ""
                    val token = if (matcher.find(0)) matcher.group(2) ?: "" else ""

                    // 1. Try token / album ID directly
                    if (token.isNotBlank()) {
                        val directAlbum = OnlineMusicSearchService.fetchAlbumSoundtrack(token)
                        if (directAlbum != null && directAlbum.songs.isNotEmpty()) {
                            return@withContext AlbumImportResult(
                                album = directAlbum,
                                success = true,
                                message = "Successfully imported '${directAlbum.title}' with ${directAlbum.songs.size} tracks!"
                            )
                        }
                    }

                    // 2. Try webapi.get with token
                    if (token.isNotBlank()) {
                        val tokenAlbum = fetchSaavnAlbumViaToken(token)
                        if (tokenAlbum != null && tokenAlbum.songs.isNotEmpty()) {
                            return@withContext AlbumImportResult(
                                album = tokenAlbum,
                                success = true,
                                message = "Successfully imported '${tokenAlbum.title}' with ${tokenAlbum.songs.size} tracks!"
                            )
                        }
                    }

                    // 3. Fallback: Search album by title
                    if (albumName.isNotBlank()) {
                        val matchedAlbum = OnlineMusicSearchService.resolveMovieSoundtrack(albumName)
                        if (matchedAlbum != null && matchedAlbum.songs.isNotEmpty()) {
                            return@withContext AlbumImportResult(
                                album = matchedAlbum,
                                success = true,
                                message = "Successfully imported '${matchedAlbum.title}' with ${matchedAlbum.songs.size} tracks!"
                            )
                        }
                    }

                    return@withContext AlbumImportResult(message = "Could not parse JioSaavn album from link.")
                }

                AlbumLinkType.YOUTUBE_PLAYLIST -> {
                    val playlistId = extractId(url, YOUTUBE_PLAYLIST_REGEX) ?: url.substringAfter("list=").substringBefore("&")
                    val album = fetchYouTubePlaylist(playlistId)
                    if (album != null && album.songs.isNotEmpty()) {
                        return@withContext AlbumImportResult(
                            album = album,
                            success = true,
                            message = "Successfully imported '${album.title}' with ${album.songs.size} YouTube tracks!"
                        )
                    } else {
                        return@withContext AlbumImportResult(message = "Could not fetch tracks from this YouTube playlist.")
                    }
                }

                AlbumLinkType.UNKNOWN -> {
                    // Smart fallback: If it's a general text or unknown URL, try searching as an album title!
                    val query = url.replace("https://", "").replace("http://", "").substringBefore("?").trim()
                    val fallbackAlbum = OnlineMusicSearchService.resolveMovieSoundtrack(query)
                    if (fallbackAlbum != null && fallbackAlbum.songs.isNotEmpty()) {
                        return@withContext AlbumImportResult(
                            album = fallbackAlbum,
                            success = true,
                            message = "Found matching album '${fallbackAlbum.title}' with ${fallbackAlbum.songs.size} tracks!"
                        )
                    }
                    return@withContext AlbumImportResult(message = "Unsupported link format. Please paste a Spotify, JioSaavn, or YouTube link.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing album from link: $url", e)
            return@withContext AlbumImportResult(message = "Failed to import album: ${e.localizedMessage ?: "Network error"}")
        }
    }

    private fun extractId(url: String, pattern: Pattern): String? {
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Pre-resolves full audio streams for the first 3 songs of an imported album in parallel,
     * so that the user experiences instant 0-second latency playback.
     */
    private suspend fun preResolveInitialTracks(album: OnlineAlbumModel): OnlineAlbumModel = coroutineScope {
        val topSongs = album.songs.take(3)
        val remainingSongs = album.songs.drop(3)

        val resolvedTop = topSongs.map { song ->
            async {
                try {
                    if (song.streamUrl.isBlank()) {
                        val stream = OnlineMusicSearchService.resolveFullAudioStream(song, isDataSaver = false, allowYouTubeFallback = true)
                        if (stream.isNotBlank()) song.copy(streamUrl = stream) else song
                    } else song
                } catch (_: Exception) {
                    song
                }
            }
        }.awaitAll()

        album.copy(songs = resolvedTop + remainingSongs)
    }

    private fun fetchSaavnAlbumViaToken(token: String): OnlineAlbumModel? {
        return try {
            val endpoint = "https://www.jiosaavn.com/api.php?__call=webapi.get&token=$token&type=album&_format=json&ctx=web6dot0"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) return null
            val root = JSONObject(text)
            val title = root.optString("title", root.optString("name", ""))
            if (title.isBlank()) return null

            val albumId = root.optString("id", token)
            val artist = root.optString("primary_artists", root.optString("singers", "Various Artists"))
            val coverArt = root.optString("image", "").replace("-150x150.jpg", "-500x500.jpg").replace("-50x50.jpg", "-500x500.jpg")
            val songsArray = root.optJSONArray("songs") ?: JSONArray()
            val songs = mutableListOf<OnlineSongModel>()

            for (i in 0 until songsArray.length()) {
                val s = songsArray.optJSONObject(i) ?: continue
                val sId = s.optString("id", "")
                val sTitle = s.optString("song", "")
                val sArtist = s.optString("singers", s.optString("primary_artists", artist))
                val sDuration = s.optString("duration").toLongOrNull() ?: 0L
                val sArt = s.optString("image", "").replace("-150x150.jpg", "-500x500.jpg")
                val encMedia = s.optString("encrypted_media_url", "")
                val streamUrl = if (encMedia.isNotBlank()) OnlineMusicSearchService.decryptMediaUrl(encMedia) else ""

                if (sTitle.isNotBlank()) {
                    songs.add(
                        OnlineSongModel(
                            id = if (sId.isNotBlank()) sId else "saavn_imp_${albumId}_$i",
                            title = sTitle,
                            artist = sArtist,
                            album = title,
                            durationMs = sDuration * 1000L,
                            streamUrl = streamUrl,
                            highResArtUrl = sArt.ifBlank { coverArt },
                            source = "saavn"
                        )
                    )
                }
            }

            OnlineAlbumModel(
                id = "saavn_$albumId",
                title = title,
                artist = artist,
                coverArtUrl = coverArt,
                songs = songs,
                songCount = songs.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Saavn album via token $token", e)
            null
        }
    }

    private suspend fun fetchYouTubePlaylist(playlistId: String): OnlineAlbumModel? = withContext(Dispatchers.IO) {
        val instances = listOf("https://invidious.nerdvpn.de", "https://yewtu.be", "https://inv.tux.pizza")
        for (base in instances) {
            try {
                val endpoint = "$base/api/v1/playlists/$playlistId"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(text)
                    val title = root.optString("title", "YouTube Playlist")
                    val author = root.optString("author", "YouTube")
                    val videosArray = root.optJSONArray("videos") ?: JSONArray()
                    val songs = mutableListOf<OnlineSongModel>()
                    var bestCoverArt = ""

                    for (i in 0 until videosArray.length()) {
                        val v = videosArray.optJSONObject(i) ?: continue
                        val vId = v.optString("videoId", "")
                        val vTitle = v.optString("title", "")
                        if (vId.isBlank() || vTitle.isBlank()) continue
                        val vAuthor = v.optString("author", author)
                        val vDurSec = v.optLong("lengthSeconds", 0L)
                        val thumbArray = v.optJSONArray("videoThumbnails")
                        val thumb = if (thumbArray != null && thumbArray.length() > 0) {
                            thumbArray.optJSONObject(thumbArray.length() - 1)?.optString("url", "") ?: ""
                        } else "https://i.ytimg.com/vi/$vId/hqdefault.jpg"

                        if (bestCoverArt.isBlank() && thumb.isNotBlank()) {
                            bestCoverArt = thumb
                        }

                        songs.add(
                            OnlineSongModel(
                                id = "yt_$vId",
                                title = vTitle,
                                artist = vAuthor,
                                album = title,
                                durationMs = vDurSec * 1000L,
                                highResArtUrl = thumb,
                                source = "youtube",
                                youtubeVideoId = vId
                            )
                        )
                    }

                    if (songs.isNotEmpty()) {
                        return@withContext OnlineAlbumModel(
                            id = "yt_pl_$playlistId",
                            title = title,
                            artist = author,
                            coverArtUrl = bestCoverArt,
                            songs = songs,
                            songCount = songs.size,
                            source = "youtube",
                            type = "Playlist"
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }
}
