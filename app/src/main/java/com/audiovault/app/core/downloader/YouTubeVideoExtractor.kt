package com.audiovault.app.core.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class VideoFormatInfo(
    val itag: Int,
    val qualityLabel: String,
    val mimeType: String,
    val url: String,
    val contentLength: Long,
    val isAudioAndVideo: Boolean,
    val width: Int = 0,
    val height: Int = 0,
    val resourceContent: String? = null
)

data class YouTubeVideoInfo(
    val videoId: String,
    val title: String,
    val author: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val formats: List<VideoFormatInfo>,
    val bestStream: VideoFormatInfo?
)

object YouTubeVideoExtractor {

    private val VIDEO_ID_PATTERNS = listOf(
        Pattern.compile("(?:v=|/v/|youtu\\.be/|/embed/|/shorts/|/live/|^)([a-zA-Z0-9_-]{11})"),
        Pattern.compile("^([a-zA-Z0-9_-]{11})$")
    )

    private val BASE_INVIDIOUS_INSTANCES = listOf(
        "https://invidious.f5.si",
        "https://inv.tux.pizza",
        "https://yewtu.be",
        "https://invidious.nerdvpn.de",
        "https://invidious.flokinet.to",
        "https://inv.nadeko.net",
        "https://invidious.drgns.space"
    )

    fun extractVideoId(urlOrId: String): String? {
        val trimmed = urlOrId.trim()
        for (pattern in VIDEO_ID_PATTERNS) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                val id = matcher.group(1)
                if (!id.isNullOrBlank() && id.length == 11) {
                    return id
                }
            }
        }
        return null
    }

    suspend fun fetchAudioStreamUrl(videoIdOrUrl: String): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(videoIdOrUrl) ?: return@withContext null

        // Strategy 1: Vidssave MP3 / Audio Engine
        try {
            val vsResult = VidssaveExtractor.parseVideo(videoId)
            if (vsResult != null && vsResult.resources.isNotEmpty()) {
                // Look for dedicated audio resource first
                val audioResources = vsResult.resources.filter {
                    it.type.equals("audio", ignoreCase = true) ||
                    it.format.equals("MP3", ignoreCase = true) ||
                    it.format.equals("M4A", ignoreCase = true)
                }
                for (r in audioResources) {
                    val streamUrl = VidssaveExtractor.resolveDownloadUrl(r)
                    if (!streamUrl.isNullOrBlank()) {
                        return@withContext streamUrl
                    }
                }

                // If dedicated audio isn't resolved, use best video MP4 stream (plays audio perfectly in ExoPlayer)
                val videoResources = vsResult.resources.filter { it.type.equals("video", ignoreCase = true) }
                for (r in videoResources) {
                    val streamUrl = VidssaveExtractor.resolveDownloadUrl(r)
                    if (!streamUrl.isNullOrBlank()) {
                        return@withContext streamUrl
                    }
                }
            }
        } catch (_: Exception) {}

        // Strategy 2: Invidious Instances (Audio adaptive streams & direct formats)
        val instances = mutableListOf<String>().apply { addAll(BASE_INVIDIOUS_INSTANCES) }
        try {
            val dynamicInstances = fetchDynamicInstances()
            for (inst in dynamicInstances) {
                if (!instances.contains(inst)) instances.add(inst)
            }
        } catch (_: Exception) {}

        for (instanceUrl in instances) {
            try {
                val url = URL("$instanceUrl/api/v1/videos/$videoId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == 200) {
                    val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val root = JSONObject(text)
                    val adaptiveFormats = root.optJSONArray("adaptiveFormats")
                    if (adaptiveFormats != null) {
                        for (i in 0 until adaptiveFormats.length()) {
                            val item = adaptiveFormats.getJSONObject(i)
                            val directUrl = item.optString("url", "")
                            val type = item.optString("type", "")
                            if (directUrl.isNotBlank() && (type.contains("audio/mp4", ignoreCase = true) || type.contains("audio/webm", ignoreCase = true))) {
                                return@withContext directUrl
                            }
                        }
                    }

                    val formatStreams = root.optJSONArray("formatStreams")
                    if (formatStreams != null && formatStreams.length() > 0) {
                        val directUrl = formatStreams.getJSONObject(0).optString("url", "")
                        if (directUrl.isNotBlank()) return@withContext directUrl
                    }
                }
            } catch (_: Exception) {}
        }

        // Strategy 3: Watch Page Direct formats
        try {
            val watchInfo = fetchFromWatchHtml(videoId)
            if (watchInfo != null && watchInfo.formats.isNotEmpty()) {
                return@withContext watchInfo.bestStream?.url
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    suspend fun fetchVideoInfo(videoIdOrUrl: String): YouTubeVideoInfo = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(videoIdOrUrl)
            ?: throw IllegalArgumentException("Invalid YouTube URL or Video ID: $videoIdOrUrl")

        // Strategy 1: Vidssave Engine (Fast, High Quality HD, 100% Reliable)
        try {
            val vsResult = VidssaveExtractor.parseVideo(videoId)
            if (vsResult != null && vsResult.resources.isNotEmpty()) {
                val formats = mutableListOf<VideoFormatInfo>()
                val videoResources = vsResult.resources.filter { it.type == "video" }
                for (r in videoResources) {
                    val height = parseHeightFromQuality(r.quality)
                    formats.add(
                        VideoFormatInfo(
                            itag = when (height) {
                                2160 -> 313
                                1440 -> 271
                                1080 -> 137
                                720 -> 22
                                480 -> 135
                                360 -> 18
                                240 -> 133
                                144 -> 160
                                else -> if (height > 0) height else 22
                            },
                            qualityLabel = if (r.quality.endsWith("p", ignoreCase = true)) r.quality.uppercase() else "${r.quality}P",
                            mimeType = "video/${r.format.lowercase()}",
                            url = r.directDownloadUrl ?: "",
                            contentLength = r.sizeBytes,
                            isAudioAndVideo = true,
                            height = height,
                            resourceContent = r.resourceContent
                        )
                    )
                }

                if (formats.isNotEmpty()) {
                    val sorted = formats.distinctBy { it.qualityLabel }.sortedByDescending { it.height }
                    val best = sorted.firstOrNull { it.height <= 1080 } ?: sorted.first()
                    return@withContext YouTubeVideoInfo(
                        videoId = videoId,
                        title = vsResult.title,
                        author = vsResult.author,
                        durationSeconds = vsResult.durationSeconds,
                        thumbnailUrl = vsResult.thumbnailUrl,
                        formats = sorted,
                        bestStream = best
                    )
                }
            }
        } catch (_: Exception) {}

        // Strategy 2: Multi-Instance Invidious Registry
        val instances = mutableListOf<String>().apply { addAll(BASE_INVIDIOUS_INSTANCES) }
        try {
            val dynamicInstances = fetchDynamicInstances()
            for (inst in dynamicInstances) {
                if (!instances.contains(inst)) {
                    instances.add(inst)
                }
            }
        } catch (_: Exception) {}

        for (instanceUrl in instances) {
            try {
                val info = fetchFromInstance(instanceUrl, videoId)
                if (info != null && info.formats.isNotEmpty()) {
                    return@withContext info
                }
            } catch (_: Exception) {}
        }

        // Strategy 3: YouTube Watch Page HTML
        try {
            val info = fetchFromWatchHtml(videoId)
            if (info != null && info.formats.isNotEmpty()) {
                return@withContext info
            }
        } catch (_: Exception) {}

        throw RuntimeException("Unable to extract video streams for ID: $videoId. Please check link or internet connection.")
    }

    private fun fetchDynamicInstances(): List<String> {
        val list = mutableListOf<String>()
        try {
            val url = URL("https://api.invidious.io/instances.json?sort_by=health")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val root = JSONArray(text)
                for (i in 0 until root.length().coerceAtMost(15)) {
                    val item = root.getJSONArray(i)
                    val infoObj = item.getJSONObject(1)
                    val uri = infoObj.optString("uri", "")
                    val type = infoObj.optString("type", "")
                    if (uri.startsWith("https://") && type == "https") {
                        list.add(uri)
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun fetchFromInstance(instanceUrl: String, videoId: String): YouTubeVideoInfo? {
        val url = URL("$instanceUrl/api/v1/videos/$videoId")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 7000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            setRequestProperty("Accept", "application/json")
        }

        if (conn.responseCode != 200) return null

        val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val root = JSONObject(text)

        val title = root.optString("title", "YouTube Video")
        val author = root.optString("author", "YouTube Creator")
        val lengthSeconds = root.optLong("lengthSeconds", 0L)
        val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val formatStreams = root.optJSONArray("formatStreams")
        val adaptiveFormats = root.optJSONArray("adaptiveFormats")
        val formatsList = mutableListOf<VideoFormatInfo>()

        if (formatStreams != null) {
            for (i in 0 until formatStreams.length()) {
                val item = formatStreams.getJSONObject(i)
                val directUrl = item.optString("url", "")
                if (directUrl.isNotEmpty()) {
                    val quality = item.optString("qualityLabel", item.optString("resolution", "720p HD"))
                    val itag = item.optInt("itag", 22)
                    val height = parseHeightFromQuality(quality)
                    formatsList.add(
                        VideoFormatInfo(
                            itag = itag,
                            qualityLabel = if (quality.contains("p", ignoreCase = true)) quality.uppercase() else "${quality}P",
                            mimeType = item.optString("type", "video/mp4"),
                            url = directUrl,
                            contentLength = item.optString("size", "0").toLongOrNull() ?: 0L,
                            isAudioAndVideo = true,
                            height = height
                        )
                    )
                }
            }
        }

        if (adaptiveFormats != null) {
            for (i in 0 until adaptiveFormats.length()) {
                val item = adaptiveFormats.getJSONObject(i)
                val directUrl = item.optString("url", "")
                val mimeType = item.optString("type", "")
                if (directUrl.isNotEmpty() && mimeType.contains("video/mp4", ignoreCase = true)) {
                    val quality = item.optString("qualityLabel", item.optString("resolution", "MP4 Video"))
                    val height = parseHeightFromQuality(quality)
                    formatsList.add(
                        VideoFormatInfo(
                            itag = item.optInt("itag", 137),
                            qualityLabel = if (quality.contains("p", ignoreCase = true)) quality.uppercase() else "${quality}P",
                            mimeType = mimeType,
                            url = directUrl,
                            contentLength = item.optString("size", "0").toLongOrNull() ?: 0L,
                            isAudioAndVideo = formatStreams == null || formatStreams.length() == 0,
                            height = height
                        )
                    )
                }
            }
        }

        if (formatsList.isEmpty()) return null

        val sortedFormats = formatsList.distinctBy { it.qualityLabel }.sortedByDescending { it.height }
        val bestStream = sortedFormats.firstOrNull { it.height <= 1080 } ?: sortedFormats.first()

        return YouTubeVideoInfo(
            videoId = videoId,
            title = title,
            author = author,
            durationSeconds = lengthSeconds,
            thumbnailUrl = thumbnailUrl,
            formats = sortedFormats,
            bestStream = bestStream
        )
    }

    private fun fetchFromWatchHtml(videoId: String): YouTubeVideoInfo? {
        val url = URL("https://www.youtube.com/watch?v=$videoId")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }

        if (conn.responseCode != 200) return null
        val html = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }

        val matcher = Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\});(?:var\\s+|</script)").matcher(html)
        if (!matcher.find()) return null

        val jsonStr = matcher.group(1) ?: return null
        val root = JSONObject(jsonStr)

        val videoDetails = root.optJSONObject("videoDetails") ?: return null
        val title = videoDetails.optString("title", "YouTube Video")
        val author = videoDetails.optString("author", "YouTube Creator")
        val lengthSeconds = videoDetails.optString("lengthSeconds", "0").toLongOrNull() ?: 0L
        val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val streamingData = root.optJSONObject("streamingData") ?: return null
        val formats = streamingData.optJSONArray("formats")
        val formatsList = mutableListOf<VideoFormatInfo>()

        if (formats != null) {
            for (i in 0 until formats.length()) {
                val item = formats.getJSONObject(i)
                val directUrl = item.optString("url", "")
                if (directUrl.isNotEmpty()) {
                    val quality = item.optString("qualityLabel", "720p HD")
                    formatsList.add(
                        VideoFormatInfo(
                            itag = item.optInt("itag", 22),
                            qualityLabel = quality.uppercase(),
                            mimeType = item.optString("mimeType", "video/mp4"),
                            url = directUrl,
                            contentLength = item.optString("contentLength", "0").toLongOrNull() ?: 0L,
                            isAudioAndVideo = true,
                            height = parseHeightFromQuality(quality)
                        )
                    )
                }
            }
        }

        if (formatsList.isEmpty()) return null

        return YouTubeVideoInfo(
            videoId = videoId,
            title = title,
            author = author,
            durationSeconds = lengthSeconds,
            thumbnailUrl = thumbnailUrl,
            formats = formatsList,
            bestStream = formatsList.maxByOrNull { it.height } ?: formatsList.first()
        )
    }

    private fun parseHeightFromQuality(quality: String): Int {
        val clean = quality.lowercase()
        return when {
            clean.contains("1080") -> 1080
            clean.contains("720") -> 720
            clean.contains("480") -> 480
            clean.contains("360") -> 360
            clean.contains("240") -> 240
            clean.contains("144") -> 144
            else -> 720
        }
    }
}
