package com.audiovault.app.core.intelligence

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches high-resolution (600x600) album artwork from public metadata services (iTunes HD, Deezer)
 * and authentic artist portraits from Deezer & Wikipedia Commons, caching all images locally on disk.
 */
object CoverArtFetcher {

    private const val TAG = "AudioVault/CoverArt"

    fun getCachedCoverArtPath(context: Context, title: String, artist: String?): String? {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank() || cleanTitle.startsWith("Recording ", ignoreCase = true)) return null
        val artDir = File(context.filesDir, "album_art")
        val cleanKey = if (!artist.isNullOrBlank()) "${artist.trim()} - $cleanTitle" else cleanTitle

        val file1 = File(artDir, "${cleanKey.hashCode()}_art.jpg")
        if (file1.exists() && file1.length() > 500) return file1.absolutePath

        val file2 = File(artDir, "${cleanKey.lowercase().hashCode()}_art.jpg")
        if (file2.exists() && file2.length() > 500) return file2.absolutePath

        return null
    }

    suspend fun fetchAndCacheCoverArt(
        context: Context,
        title: String,
        artist: String?
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank() || cleanTitle.startsWith("Recording ", ignoreCase = true)) {
            return@withContext null
        }

        try {
            val artDir = File(context.filesDir, "album_art").apply { if (!exists()) mkdirs() }
            val cleanKey = if (!artist.isNullOrBlank()) "${artist.trim()} - $cleanTitle" else cleanTitle
            val cachedFile = File(artDir, "${cleanKey.hashCode()}_art.jpg")

            if (cachedFile.exists() && cachedFile.length() > 1024) {
                return@withContext cachedFile.absolutePath
            }

            val cachedFileLower = File(artDir, "${cleanKey.lowercase().hashCode()}_art.jpg")
            if (cachedFileLower.exists() && cachedFileLower.length() > 1024) {
                return@withContext cachedFileLower.absolutePath
            }

            val searchQuery = if (!artist.isNullOrBlank()) "${artist.trim()} $cleanTitle" else cleanTitle
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")

            // 1. Primary: iTunes Search API (fast, official 600x600 HD artwork)
            try {
                val itunesUrl = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=1"
                val connection = (URL(itunesUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Recordify/1.0")
                }

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(jsonStr)
                    val results = root.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val firstItem = results.getJSONObject(0)
                        val rawUrl = firstItem.optString("artworkUrl100")
                        if (rawUrl.isNotBlank()) {
                            val hdUrl = rawUrl.replace("100x100bb.jpg", "600x600bb.jpg")
                            val imgConn = (URL(hdUrl).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 6000
                                readTimeout = 6000
                                requestMethod = "GET"
                                setRequestProperty("User-Agent", "Recordify/1.0")
                            }
                            if (imgConn.responseCode == 200) {
                                imgConn.inputStream.use { input ->
                                    FileOutputStream(cachedFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (cachedFile.exists() && cachedFile.length() > 500) {
                                    Log.i(TAG, "Successfully downloaded iTunes cover art for '$searchQuery' -> ${cachedFile.absolutePath}")
                                    return@withContext cachedFile.absolutePath
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "iTunes cover art search failed for '$searchQuery': ${e.message}")
            }

            // 2. Fallback: Deezer Track Search API for HD album cover
            try {
                val deezerUrl = "https://api.deezer.com/search?q=$encodedQuery&limit=1"
                val deezerConn = (URL(deezerUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (deezerConn.responseCode == 200) {
                    val jsonStr = deezerConn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(jsonStr)
                    val data = root.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val first = data.getJSONObject(0)
                        val album = first.optJSONObject("album")
                        val coverUrl = album?.optString("cover_xl")?.ifBlank {
                            album.optString("cover_big")
                        }
                        if (!coverUrl.isNullOrBlank()) {
                            val imgConn = (URL(coverUrl).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 6000
                                readTimeout = 6000
                                requestMethod = "GET"
                                setRequestProperty("User-Agent", "Mozilla/5.0")
                            }
                            if (imgConn.responseCode == 200) {
                                imgConn.inputStream.use { input ->
                                    FileOutputStream(cachedFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (cachedFile.exists() && cachedFile.length() > 500) {
                                    Log.i(TAG, "Successfully downloaded Deezer cover art for '$searchQuery' -> ${cachedFile.absolutePath}")
                                    return@withContext cachedFile.absolutePath
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Deezer cover art search failed for '$searchQuery': ${e.message}")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Cover art resolution failed for '$title': ${e.message}")
        }
        null
    }

    fun getCachedArtistArtPath(context: Context, artistName: String): String? {
        val cleanArtist = artistName.trim().lowercase()
        if (cleanArtist.isBlank() || cleanArtist == "unknown artist") return null
        val artDir = File(context.filesDir, "artist_art")

        val mainFile = File(artDir, "${cleanArtist.hashCode()}_artist.jpg")
        if (mainFile.exists() && mainFile.length() > 500) {
            return mainFile.absolutePath
        }

        val primary = extractPrimaryArtist(artistName).lowercase()
        if (primary.isNotBlank() && primary != cleanArtist) {
            val primaryFile = File(artDir, "${primary.hashCode()}_artist.jpg")
            if (primaryFile.exists() && primaryFile.length() > 500) {
                return primaryFile.absolutePath
            }
        }
        return null
    }

    suspend fun fetchAndCacheArtistArt(
        context: Context,
        artistName: String,
        songHint: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val rawArtist = artistName.trim()
        if (rawArtist.isBlank() || rawArtist.equals("Unknown Artist", ignoreCase = true)) {
            return@withContext null
        }

        try {
            val artDir = File(context.filesDir, "artist_art").apply { if (!exists()) mkdirs() }
            val cachedFile = File(artDir, "${rawArtist.lowercase().hashCode()}_artist.jpg")

            if (cachedFile.exists() && cachedFile.length() > 1024) {
                return@withContext cachedFile.absolutePath
            }

            val primaryArtist = extractPrimaryArtist(rawArtist)
            val searchCandidates = mutableListOf<String>()
            if (primaryArtist.isNotBlank() && !primaryArtist.equals(rawArtist, ignoreCase = true)) {
                searchCandidates.add(primaryArtist)
            }
            searchCandidates.add(rawArtist)

            // 1. Search Deezer Artist Database (Instant & 1000x1000 Crystal Clear HD)
            for (candidate in searchCandidates) {
                try {
                    val encodedArtist = URLEncoder.encode(candidate, "UTF-8")
                    val deezerUrl = "https://api.deezer.com/search/artist?q=$encodedArtist&limit=1"
                    val deezerConn = (URL(deezerUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }
                    if (deezerConn.responseCode == 200) {
                        val jsonStr = deezerConn.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(jsonStr)
                        val data = root.optJSONArray("data")
                        if (data != null && data.length() > 0) {
                            val first = data.getJSONObject(0)
                            val picUrl = first.optString("picture_xl").ifBlank {
                                first.optString("picture_big").ifBlank {
                                    first.optString("picture_medium")
                                }
                            }

                            if (isValidArtistImageUrl(picUrl)) {
                                val imgConn = (URL(picUrl).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 6000
                                    readTimeout = 6000
                                    requestMethod = "GET"
                                    setRequestProperty("User-Agent", "Mozilla/5.0")
                                }
                                if (imgConn.responseCode == 200) {
                                    imgConn.inputStream.use { input ->
                                        FileOutputStream(cachedFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    if (cachedFile.exists() && cachedFile.length() > 500) {
                                        if (primaryArtist.isNotBlank()) {
                                            val pFile = File(artDir, "${primaryArtist.lowercase().hashCode()}_artist.jpg")
                                            if (!pFile.exists()) {
                                                try { cachedFile.copyTo(pFile, overwrite = true) } catch (_: Exception) {}
                                            }
                                        }
                                        Log.i(TAG, "Cached Deezer artist portrait for '$rawArtist' -> ${cachedFile.absolutePath}")
                                        return@withContext cachedFile.absolutePath
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Search Deezer by Track + Artist Hint to extract official artist picture
            if (!songHint.isNullOrBlank()) {
                try {
                    val trackQuery = URLEncoder.encode("$primaryArtist $songHint", "UTF-8")
                    val trackUrl = "https://api.deezer.com/search?q=$trackQuery&limit=1"
                    val trackConn = (URL(trackUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }
                    if (trackConn.responseCode == 200) {
                        val jsonStr = trackConn.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(jsonStr)
                        val data = root.optJSONArray("data")
                        if (data != null && data.length() > 0) {
                            val first = data.getJSONObject(0)
                            val artistObj = first.optJSONObject("artist")
                            val picUrl = artistObj?.optString("picture_xl")?.ifBlank {
                                artistObj.optString("picture_big")
                            }
                            if (!picUrl.isNullOrBlank() && isValidArtistImageUrl(picUrl)) {
                                val imgConn = (URL(picUrl).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 6000
                                    readTimeout = 6000
                                    requestMethod = "GET"
                                    setRequestProperty("User-Agent", "Mozilla/5.0")
                                }
                                if (imgConn.responseCode == 200) {
                                    imgConn.inputStream.use { input ->
                                        FileOutputStream(cachedFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    if (cachedFile.exists() && cachedFile.length() > 500) {
                                        Log.i(TAG, "Cached Deezer Track-Artist portrait for '$rawArtist' -> ${cachedFile.absolutePath}")
                                        return@withContext cachedFile.absolutePath
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Search Wikipedia / Wikimedia Commons for real artist portrait photo
            val wikiQueries = mutableListOf(primaryArtist, "$primaryArtist singer", "$primaryArtist musician")
            for (query in wikiQueries) {
                try {
                    val encodedQ = URLEncoder.encode(query, "UTF-8")
                    val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$encodedQ&gsrlimit=1&prop=pageimages&pithumbsize=600&format=json"
                    val wikiConn = (URL(wikiUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "RecordifyMusicApp/1.0 (contact@audiovault.app)")
                    }
                    if (wikiConn.responseCode == 200) {
                        val jsonStr = wikiConn.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(jsonStr)
                        val pages = root.optJSONObject("query")?.optJSONObject("pages")
                        if (pages != null) {
                            val keys = pages.keys()
                            while (keys.hasNext()) {
                                val page = pages.getJSONObject(keys.next())
                                val thumbObj = page.optJSONObject("thumbnail")
                                val thumbUrl = thumbObj?.optString("source")
                                if (!thumbUrl.isNullOrBlank()) {
                                    val imgConn = (URL(thumbUrl).openConnection() as HttpURLConnection).apply {
                                        connectTimeout = 6000
                                        readTimeout = 6000
                                        requestMethod = "GET"
                                        setRequestProperty("User-Agent", "RecordifyMusicApp/1.0 (contact@audiovault.app)")
                                    }
                                    if (imgConn.responseCode == 200) {
                                        imgConn.inputStream.use { input ->
                                            FileOutputStream(cachedFile).use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        if (cachedFile.exists() && cachedFile.length() > 500) {
                                            Log.i(TAG, "Cached Wikipedia artist portrait for '$rawArtist' -> ${cachedFile.absolutePath}")
                                            return@withContext cachedFile.absolutePath
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.d(TAG, "Artist portrait resolution skipped for '$artistName': ${e.message}")
        }
        null
    }

    private fun extractPrimaryArtist(raw: String): String {
        val cleaned = raw.split(Regex("(?i)[,&/|]|\\b(?:feat\\.?|ft\\.?|featuring|vs\\.?|with|x)\\b"))
            .firstOrNull()?.trim() ?: raw.trim()
        return cleaned.trim('(', ')', '[', ']', '"', '\'')
    }

    private fun isValidArtistImageUrl(url: String): Boolean {
        if (url.isBlank()) return false
        if (url.contains("images/artist//")) return false
        if (url.contains("250x250-000000")) return false
        if (url.contains("d41d8cd98f00b204e9800998ecf8427e")) return false
        return true
    }
}
