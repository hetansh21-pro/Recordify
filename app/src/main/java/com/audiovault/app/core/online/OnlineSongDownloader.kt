package com.audiovault.app.core.online

import android.content.Context
import android.util.Log
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.data.storage.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineSongDownloader @Inject constructor(
    private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val fileManager: FileManager
) {
    companion object {
        private const val TAG = "Recordify/Downloader"
    }

    suspend fun downloadAndSave(
        song: OnlineSongModel,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val resolvedStreamUrl = com.audiovault.app.core.online.OnlineMusicSearchService.resolveFullAudioStream(
            song = song,
            isDataSaver = false, // High fidelity for offline download!
            allowYouTubeFallback = true
        )

        if (resolvedStreamUrl.isBlank()) {
            Log.e(TAG, "Cannot download song with empty stream URL: ${song.title}")
            return@withContext false
        }

        var tempAudioFile: File? = null
        var finalAudioFile: File? = null
        try {
            val recordingsDir = fileManager.recordingsDirectory
            val timestamp = System.currentTimeMillis()
            val cleanTitle = song.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_").take(30)
            val tempFile = File(recordingsDir, "online_${cleanTitle}_$timestamp.tmp")
            val finalFile = File(recordingsDir, "online_${cleanTitle}_$timestamp.m4a")
            tempAudioFile = tempFile
            finalAudioFile = finalFile

            // 1. Download Audio Stream with Multi-Hop Redirect Follower
            var currentUrl = resolvedStreamUrl
            var audioConn: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 6

            while (redirectCount < maxRedirects) {
                audioConn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("Accept-Encoding", "identity")
                }

                val code = audioConn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                    val location = audioConn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(URL(currentUrl), location).toString()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            // If primary stream failed or gave 403/404, attempt fallback bitrates / alternative streams
            if (audioConn == null || audioConn.responseCode !in 200..299) {
                Log.w(TAG, "Primary stream download failed (code ${audioConn?.responseCode}), trying fallbacks for: ${song.title}")
                val fallbackUrls = mutableListOf<String>()
                if (currentUrl.contains("_320.mp4")) {
                    fallbackUrls.add(currentUrl.replace("_320.mp4", "_160.mp4"))
                    fallbackUrls.add(currentUrl.replace("_320.mp4", "_96.mp4"))
                } else if (currentUrl.contains("_160.mp4")) {
                    fallbackUrls.add(currentUrl.replace("_160.mp4", "_320.mp4"))
                    fallbackUrls.add(currentUrl.replace("_160.mp4", "_96.mp4"))
                } else if (currentUrl.contains("_96.mp4")) {
                    fallbackUrls.add(currentUrl.replace("_96.mp4", "_320.mp4"))
                    fallbackUrls.add(currentUrl.replace("_96.mp4", "_160.mp4"))
                }
                if (!song.youtubeVideoId.isNullOrBlank()) {
                    val ytUrl = com.audiovault.app.core.downloader.YouTubeVideoExtractor.fetchAudioStreamUrl(song.youtubeVideoId)
                    if (!ytUrl.isNullOrBlank()) fallbackUrls.add(ytUrl)
                }

                for (altUrl in fallbackUrls) {
                    try {
                        val altConn = (URL(altUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 12000
                            readTimeout = 25000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            setRequestProperty("Accept", "*/*")
                        }
                        if (altConn.responseCode in 200..299) {
                            audioConn = altConn
                            currentUrl = altUrl
                            Log.i(TAG, "Fallback stream connected successfully (${altConn.responseCode}) for: ${song.title}")
                            break
                        }
                    } catch (_: Exception) {}
                }
            }

            if (audioConn == null || audioConn.responseCode !in 200..299) {
                Log.e(TAG, "Download HTTP error ${audioConn?.responseCode} for: ${song.title}")
                return@withContext false
            }

            val totalLength = audioConn.contentLengthLong
            var downloadedBytes = 0L

            audioConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    while (coroutineContext.isActive) {
                        bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalLength > 0) {
                            val progress = (downloadedBytes.toFloat() / totalLength).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            if (!coroutineContext.isActive) {
                tempFile.delete()
                return@withContext false
            }

            if (!tempFile.exists() || tempFile.length() < 30_000L) { // Must be at least 30KB
                Log.e(TAG, "Audio file download resulted in incomplete/empty file (${tempFile.length()} bytes) for: ${song.title}")
                tempFile.delete()
                return@withContext false
            }

            // Atomically rename .tmp -> .m4a
            if (finalFile.exists()) finalFile.delete()
            val renameSuccess = tempFile.renameTo(finalFile)
            val file = if (renameSuccess) finalFile else tempFile

            // 2. Download Album Art if available
            var localArtPath: String? = null
            if (!song.highResArtUrl.isNullOrBlank() && coroutineContext.isActive) {
                try {
                    val artFile = File(recordingsDir, "art_${cleanTitle}_$timestamp.jpg")
                    val artConn = (URL(song.highResArtUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }
                    if (artConn.responseCode in 200..299) {
                        artConn.inputStream.use { input ->
                            FileOutputStream(artFile).use { output ->
                                input.copyTo(output)
                                output.flush()
                            }
                        }
                        if (artFile.exists() && artFile.length() > 0L) {
                            localArtPath = artFile.absolutePath
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to download album art for: ${song.title}", e)
                }
            }

            if (!coroutineContext.isActive) {
                file.delete()
                return@withContext false
            }

            // 3. Save to Room database
            val id = recordingRepository.saveOnlineRecording(
                file = file,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                albumArtPath = localArtPath,
                lyrics = song.lyrics,
                bitrateKbps = 320
            )

            Log.i(TAG, "Successfully downloaded and saved online song: ${song.title} (ID: $id)")
            return@withContext id > 0L
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                tempAudioFile?.delete()
                finalAudioFile?.delete()
                Log.i(TAG, "Download cancelled by user: ${song.title}")
            } else {
                Log.e(TAG, "Failed to download online song: ${song.title}", e)
            }
            return@withContext false
        }
    }
}
