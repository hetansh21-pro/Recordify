package com.audiovault.app.core.intelligence

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class SponsorSegment(
    val category: String,
    val startMs: Long,
    val endMs: Long
)

/**
 * Community-driven SponsorBlock & Non-Music Segment Helper.
 * Queries SponsorBlock API to retrieve non-music, sponsor, and intro/outro timestamps.
 */
object SponsorBlockHelper {
    private const val TAG = "AudioVault/SponsorBlock"
    private const val API_BASE = "https://sponsor.ajay.app/api/skipSegments"

    suspend fun fetchSkipSegments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()

        val urlStr = "$API_BASE?videoID=$videoId&categories=[\"sponsor\",\"music_offtopic\",\"intro\",\"outro\",\"selfpromo\"]"
        try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Recordify-AudioVault/2.0")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonArr = JSONArray(response)
                val segments = mutableListOf<SponsorSegment>()

                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val category = obj.optString("category", "sponsor")
                    val segmentArr = obj.optJSONArray("segment")
                    if (segmentArr != null && segmentArr.length() >= 2) {
                        val startSec = segmentArr.getDouble(0)
                        val endSec = segmentArr.getDouble(1)
                        segments.add(
                            SponsorSegment(
                                category = category,
                                startMs = (startSec * 1000).toLong(),
                                endMs = (endSec * 1000).toLong()
                            )
                        )
                    }
                }
                Log.d(TAG, "Fetched ${segments.size} SponsorBlock segments for video: $videoId")
                return@withContext segments
            }
        } catch (e: Exception) {
            Log.w(TAG, "SponsorBlock query skipped or failed: ${e.message}")
        }
        return@withContext emptyList()
    }
}
