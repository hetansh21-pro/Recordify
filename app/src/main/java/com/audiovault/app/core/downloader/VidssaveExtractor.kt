package com.audiovault.app.core.downloader

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class VidssaveResource(
    val type: String, // "video" or "audio"
    val quality: String, // "1080P", "720P", "480P", "360P", etc.
    val format: String, // "MP4", "WEBM", "MP3"
    val sizeBytes: Long,
    val directDownloadUrl: String?,
    val resourceContent: String?
)

data class VidssaveVideoResult(
    val videoId: String,
    val title: String,
    val author: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val resources: List<VidssaveResource>
)

object VidssaveExtractor {

    private val AES_KEYS = listOf(
        "4c9b7d2e4c9b7d2e4c9b7d2e4c9b7d21",
        "rz18efAXUbdiaO7k"
    )

    private const val API_BASE = "https://api.vidssave.com/api/contentsite_api"

    suspend fun parseVideo(videoUrlOrId: String): VidssaveVideoResult? = withContext(Dispatchers.IO) {
        val videoId = YouTubeVideoExtractor.extractVideoId(videoUrlOrId) ?: return@withContext null
        val fullUrl = "https://www.youtube.com/watch?v=$videoId"

        try {
            val endpoint = "$API_BASE/media/parse"
            val postParams = mapOf(
                "hostname" to "vidssave.com",
                "auth" to "4c9b7d21",
                "origin" to "source",
                "link" to fullUrl
            )

            val responseText = executePost(endpoint, postParams) ?: return@withContext null
            val rootJson = JSONObject(responseText)
            val dataCipher = rootJson.optString("data", "")
            if (dataCipher.isEmpty()) return@withContext null

            val decryptedJsonStr = decryptPayload(dataCipher) ?: return@withContext null
            val dataObj = JSONObject(decryptedJsonStr)

            val title = dataObj.optString("title", "YouTube Video")
            val durationSeconds = dataObj.optLong("duration", 0L)
            var thumbnailUrl = dataObj.optString("thumbnail", "")
            if (thumbnailUrl.isEmpty()) {
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            }
            val userItem = dataObj.optJSONObject("user_item")
            val channelName = userItem?.optString("nickname", "YouTube Creator") ?: "YouTube Creator"

            val resourcesList = mutableListOf<VidssaveResource>()
            val resArray = dataObj.optJSONArray("resources")
            if (resArray != null) {
                for (i in 0 until resArray.length()) {
                    val r = resArray.getJSONObject(i)
                    val type = r.optString("type", "video")
                    val quality = r.optString("quality", "720P")
                    val format = r.optString("format", "MP4")
                    val size = r.optLong("size", 0L)
                    val downloadUrl = r.optString("download_url", "").takeIf { it.isNotBlank() }
                    val resourceContent = r.optString("resource_content", "").takeIf { it.isNotBlank() }

                    resourcesList.add(
                        VidssaveResource(
                            type = type,
                            quality = quality,
                            format = format,
                            sizeBytes = size,
                            directDownloadUrl = downloadUrl,
                            resourceContent = resourceContent
                        )
                    )
                }
            }

            if (resourcesList.isEmpty()) return@withContext null

            return@withContext VidssaveVideoResult(
                videoId = videoId,
                title = title,
                author = channelName,
                durationSeconds = durationSeconds,
                thumbnailUrl = thumbnailUrl,
                resources = resourcesList
            )
        } catch (_: Exception) {
            return@withContext null
        }
    }

    suspend fun resolveDownloadUrl(resource: VidssaveResource): String? = withContext(Dispatchers.IO) {
        // If direct URL is already present and valid, return it
        if (!resource.directDownloadUrl.isNullOrBlank()) {
            return@withContext resource.directDownloadUrl
        }

        val resContent = resource.resourceContent ?: return@withContext null

        try {
            // 1. Call media/download
            val downloadEndpoint = "$API_BASE/media/download"
            val dlParams = mapOf(
                "hostname" to "vidssave.com",
                "auth" to "4c9b7d21",
                "request" to resContent,
                "no_encrypt" to "1"
            )

            val dlRespText = executePost(downloadEndpoint, dlParams) ?: return@withContext null
            val dlJson = JSONObject(dlRespText)
            val dlDataCipher = dlJson.optString("data", "")
            if (dlDataCipher.isEmpty()) return@withContext null

            val decryptedDl = decryptPayload(dlDataCipher) ?: return@withContext null
            val dlObj = JSONObject(decryptedDl)

            val directLink = dlObj.optString("download_link", dlObj.optString("download_url", ""))
            if (directLink.isNotBlank()) {
                return@withContext directLink
            }

            val taskId = dlObj.optString("task_id", "")
            if (taskId.isEmpty()) return@withContext null

            // 2. Poll download_query endpoint (up to 5 attempts)
            for (attempt in 1..6) {
                delay(1200)
                val queryUrl = "$API_BASE/media/download_query?auth=20250901majwlqo&task_id=" +
                        URLEncoder.encode(taskId, "UTF-8") +
                        "&download_domain=vidssave.com&origin=content_site"

                val qRespText = executeGet(queryUrl) ?: continue

                // Check if SSE event or JSON
                var jsonDataStr = qRespText
                if (qRespText.contains("data:")) {
                    val lines = qRespText.split("\n")
                    for (line in lines) {
                        if (line.startsWith("data:")) {
                            jsonDataStr = line.substring(5).trim()
                            break
                        }
                    }
                }

                try {
                    val qJson = JSONObject(jsonDataStr)
                    val status = qJson.optString("status", "")
                    val finalLink = qJson.optString("download_link", qJson.optString("download_url", ""))
                    if (status == "success" && finalLink.isNotBlank()) {
                        return@withContext finalLink
                    }
                    if (qJson.has("data")) {
                        val encryptedData = qJson.optString("data", "")
                        if (encryptedData.isNotEmpty()) {
                            val decStr = decryptPayload(encryptedData)
                            if (decStr != null) {
                                val decObj = JSONObject(decStr)
                                val link = decObj.optString("download_link", decObj.optString("download_url", ""))
                                if (link.isNotBlank()) {
                                    return@withContext link
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    private fun decryptPayload(base64Ciphertext: String): String? {
        val trimmed = base64Ciphertext.trim()
        val rawBytes = try {
            Base64.decode(trimmed, Base64.DEFAULT)
        } catch (_: Exception) {
            return null
        }

        for (keyStr in AES_KEYS) {
            try {
                val keyBytes = keyStr.toByteArray(StandardCharsets.UTF_8)
                val ivBytes = keyStr.substring(0, 16).toByteArray(StandardCharsets.UTF_8)

                val secretKey = SecretKeySpec(keyBytes, "AES")
                val ivSpec = IvParameterSpec(ivBytes)

                val cipher = Cipher.getInstance("AES/CBC/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

                val decryptedBytes = cipher.doFinal(rawBytes)
                val rawString = String(decryptedBytes, StandardCharsets.UTF_8)
                // Remove trailing null bytes (ZeroPadding)
                val cleanString = rawString.trimEnd { it == '\u0000' }
                if (cleanString.isNotEmpty() && (cleanString.startsWith("{") || cleanString.startsWith("["))) {
                    return cleanString
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun executePost(urlStr: String, params: Map<String, String>): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 8000
                readTimeout = 12000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setRequestProperty("Origin", "https://vidssave.com")
                setRequestProperty("Referer", "https://vidssave.com/")
            }

            val body = params.entries.joinToString("&") {
                URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
            }

            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }

            if (conn.responseCode == 200) {
                BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun executeGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 12000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setRequestProperty("Origin", "https://vidssave.com")
                setRequestProperty("Referer", "https://vidssave.com/")
            }

            if (conn.responseCode == 200) {
                BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
