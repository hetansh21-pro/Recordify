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
            val cleanedSong = songHint?.let { cleanSongTitle(it) }

            // Build combined queries prioritized by specificity
            val queries = mutableListOf<String>()
            if (!cleanedSong.isNullOrBlank()) {
                queries.add("$primaryArtist $cleanedSong")
                if (!primaryArtist.equals(rawArtist, ignoreCase = true)) {
                    queries.add("$rawArtist $cleanedSong")
                }
            }
            queries.add(primaryArtist)
            if (!primaryArtist.equals(rawArtist, ignoreCase = true)) {
                queries.add(rawArtist)
            }

            // 1. Primary Engine: iTunes & Apple Music Official HD Artist Profiles (Artist + Song Combined)
            for (query in queries) {
                try {
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    val isSongSearch = !cleanedSong.isNullOrBlank() && query.contains(cleanedSong)
                    val itunesUrl = if (isSongSearch) {
                        "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=5"
                    } else {
                        "https://itunes.apple.com/search?term=$encodedQuery&entity=musicArtist&limit=3"
                    }

                    val connection = (URL(itunesUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }

                    if (connection.responseCode == 200) {
                        val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(jsonStr)
                        val results = root.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            var bestArtistViewUrl: String? = null
                            val artistSlug = primaryArtist.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                val resArtist = item.optString("artistName")
                                val artUrl = item.optString("artistViewUrl").ifBlank { item.optString("artistLinkUrl") }
                                if (artUrl.isNotBlank()) {
                                    if (resArtist.contains(primaryArtist, ignoreCase = true) ||
                                        primaryArtist.contains(resArtist, ignoreCase = true) ||
                                        (artistSlug.isNotBlank() && artUrl.lowercase().contains(artistSlug))
                                    ) {
                                        bestArtistViewUrl = artUrl
                                        break
                                    }
                                }
                            }

                            if (bestArtistViewUrl == null) {
                                val first = results.getJSONObject(0)
                                bestArtistViewUrl = first.optString("artistViewUrl").ifBlank { first.optString("artistLinkUrl") }
                            }

                            if (!bestArtistViewUrl.isNullOrBlank()) {
                                val artistWebConn = (URL(bestArtistViewUrl).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 5000
                                    readTimeout = 5000
                                    requestMethod = "GET"
                                    setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                                }
                                if (artistWebConn.responseCode == 200) {
                                    val html = artistWebConn.inputStream.bufferedReader().use { it.readText() }
                                    val ogImageRegex = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                                    val twitterImageRegex = Regex("""<meta\s+name=["']twitter:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

                                    val imageUrl = ogImageRegex.find(html)?.groupValues?.get(1)
                                        ?: twitterImageRegex.find(html)?.groupValues?.get(1)

                                    if (!imageUrl.isNullOrBlank() && isValidArtistImageUrl(imageUrl)) {
                                        val imgConn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
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
                                            if (cachedFile.exists() && cachedFile.length() > 1000) {
                                                if (primaryArtist.isNotBlank()) {
                                                    val pFile = File(artDir, "${primaryArtist.lowercase().hashCode()}_artist.jpg")
                                                    if (!pFile.exists()) {
                                                        try { cachedFile.copyTo(pFile, overwrite = true) } catch (_: Exception) {}
                                                    }
                                                }
                                                Log.i(TAG, "Cached Apple Music artist portrait for '$rawArtist' via query '$query' -> ${cachedFile.absolutePath}")
                                                return@withContext cachedFile.absolutePath
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Apple Music query '$query' failed: ${e.message}")
                }
            }

            // 2. Secondary Engine: Wikipedia / Wikimedia Commons with Artist + Song context
            val wikiQueries = mutableListOf<String>()
            if (!cleanedSong.isNullOrBlank()) {
                wikiQueries.add("$primaryArtist $cleanedSong")
            }
            wikiQueries.addAll(listOf(primaryArtist, "$primaryArtist singer", "$primaryArtist musician", "$primaryArtist artist"))

            for (query in wikiQueries) {
                try {
                    val encodedQ = URLEncoder.encode(query, "UTF-8")
                    val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$encodedQ&gsrlimit=3&prop=pageimages|description&pithumbsize=1000&format=json"
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
                                val title = page.optString("title")
                                val desc = page.optString("description").lowercase()
                                val thumbObj = page.optJSONObject("thumbnail")
                                val thumbUrl = thumbObj?.optString("source")
                                if (!thumbUrl.isNullOrBlank()) {
                                    val isArtistRelevant = title.contains(primaryArtist, ignoreCase = true) ||
                                            desc.contains("singer") || desc.contains("musician") ||
                                            desc.contains("band") || desc.contains("artist") || desc.contains("rapper")

                                    if (isArtistRelevant) {
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
                                            if (cachedFile.exists() && cachedFile.length() > 1000) {
                                                if (primaryArtist.isNotBlank()) {
                                                    val pFile = File(artDir, "${primaryArtist.lowercase().hashCode()}_artist.jpg")
                                                    if (!pFile.exists()) {
                                                        try { cachedFile.copyTo(pFile, overwrite = true) } catch (_: Exception) {}
                                                    }
                                                }
                                                Log.i(TAG, "Cached Wikipedia artist portrait for '$rawArtist' -> ${cachedFile.absolutePath}")
                                                return@withContext cachedFile.absolutePath
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Fallback: Deezer Database Search
            for (candidate in queries) {
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
        } catch (e: Exception) {
            Log.d(TAG, "Artist portrait resolution skipped for '$artistName': ${e.message}")
        }
        null
    }

    private fun cleanSongTitle(title: String): String {
        val withoutPipes = title.split('|').firstOrNull()?.trim() ?: title
        var cleaned = withoutPipes
            .replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), "")
            .replace(Regex("(?i)\\b(official|music video|video|lyric video|lyrics|audio|hd|4k|full song|visualizer|remix|ft\\.?|feat\\.?)\\b.*"), "")
            .trim(' ', '-', '_', '/', ':', '"', '\'')
        return if (cleaned.length >= 2) cleaned else title.trim()
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
        if (url.contains("apple-music-share.png", ignoreCase = true)) return false
        if (url.contains("default_artist.png", ignoreCase = true)) return false
        return true
    }
}
