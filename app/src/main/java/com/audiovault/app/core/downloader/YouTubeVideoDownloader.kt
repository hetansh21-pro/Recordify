package com.audiovault.app.core.downloader

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.audiovault.app.data.local.entity.DownloadedVideoEntity
import com.audiovault.app.data.repository.VideoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

sealed class DownloadState {
    data object Idle : DownloadState()
    data class FetchingInfo(val videoUrl: String) : DownloadState()
    data class ReadyToDownload(val videoInfo: YouTubeVideoInfo) : DownloadState()
    data class Downloading(
        val videoInfo: YouTubeVideoInfo,
        val progress: Float, // 0.0 to 1.0
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedMbPerSec: Float
    ) : DownloadState()
    data class Success(val video: DownloadedVideoEntity) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class YouTubeVideoDownloader(
    private val context: Context,
    private val videoRepository: VideoRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var currentDownloadJob: Job? = null

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun resetState() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        _downloadState.value = DownloadState.Idle
    }

    fun fetchVideoDetails(urlOrId: String) {
        currentDownloadJob?.cancel()
        currentDownloadJob = scope.launch {
            _downloadState.value = DownloadState.FetchingInfo(urlOrId)
            try {
                val info = YouTubeVideoExtractor.fetchVideoInfo(urlOrId)
                _downloadState.value = DownloadState.ReadyToDownload(info)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _downloadState.value = DownloadState.Error(e.message ?: "Failed to fetch video details")
                }
            }
        }
    }

    fun startDownload(videoInfo: YouTubeVideoInfo, selectedFormat: VideoFormatInfo? = null) {
        currentDownloadJob?.cancel()
        currentDownloadJob = scope.launch {
            val formatToDownload = selectedFormat ?: videoInfo.bestStream ?: videoInfo.formats.firstOrNull()
            if (formatToDownload == null) {
                _downloadState.value = DownloadState.Error("No playable stream format found for this video")
                return@launch
            }

            // Immediately show downloading animation and progress in UI
            _downloadState.value = DownloadState.Downloading(
                videoInfo = videoInfo,
                progress = 0f,
                bytesDownloaded = 0L,
                totalBytes = formatToDownload.contentLength,
                speedMbPerSec = 0f
            )

            try {
                // Ensure storage directory
                val targetDir = getDownloadDirectory()
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                // Sanitize file name
                val safeTitle = sanitizeFilename(videoInfo.title)
                val videoFileName = "Recordify_${safeTitle}_${videoInfo.videoId}.mp4"
                val destinationFile = File(targetDir, videoFileName)

                // Save thumbnail locally
                var localThumbnailPath: String? = null
                try {
                    val thumbFile = File(targetDir, "thumb_${videoInfo.videoId}.jpg")
                    downloadThumbnailFile(videoInfo.thumbnailUrl, thumbFile)
                    if (thumbFile.exists()) {
                        localThumbnailPath = thumbFile.absolutePath
                    }
                } catch (_: Exception) {}

                // Resolve final stream URL if needed
                var directStreamUrl = formatToDownload.url
                if (directStreamUrl.isBlank() && formatToDownload.resourceContent != null) {
                    val resolved = VidssaveExtractor.resolveDownloadUrl(
                        VidssaveResource(
                            type = "video",
                            quality = formatToDownload.qualityLabel,
                            format = "MP4",
                            sizeBytes = formatToDownload.contentLength,
                            directDownloadUrl = null,
                            resourceContent = formatToDownload.resourceContent
                        )
                    )
                    if (!resolved.isNullOrBlank()) {
                        directStreamUrl = resolved
                    }
                }

                if (directStreamUrl.isBlank()) {
                    _downloadState.value = DownloadState.Error("Unable to resolve download stream for selected quality")
                    return@launch
                }

                // Stream download with progress calculation
                val url = URL(directStreamUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 25000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }

                val contentLength = if (formatToDownload.contentLength > 0) {
                    formatToDownload.contentLength
                } else {
                    connection.contentLengthLong.coerceAtLeast(1L)
                }

                val inputStream: InputStream = connection.inputStream
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastProgressUpdate = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L

                _downloadState.value = DownloadState.Downloading(
                    videoInfo = videoInfo,
                    progress = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = contentLength,
                    speedMbPerSec = 0f
                )

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    bytesSinceLastUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastProgressUpdate
                    if (timeDelta >= 400 || totalBytesRead == contentLength) {
                        val speedMb = if (timeDelta > 0) {
                            (bytesSinceLastUpdate.toFloat() / (1024f * 1024f)) / (timeDelta.toFloat() / 1000f)
                        } else 0f

                        val progress = if (contentLength > 0) {
                            (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        _downloadState.value = DownloadState.Downloading(
                            videoInfo = videoInfo,
                            progress = progress,
                            bytesDownloaded = totalBytesRead,
                            totalBytes = contentLength,
                            speedMbPerSec = speedMb
                        )

                        lastProgressUpdate = now
                        bytesSinceLastUpdate = 0L
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                connection.disconnect()

                // Register file with MediaScanner so it appears in device Gallery immediately
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destinationFile.absolutePath),
                        arrayOf("video/mp4"),
                        null
                    )
                } catch (_: Exception) {}

                // Save to Room database
                val entity = DownloadedVideoEntity(
                    id = UUID.randomUUID().toString(),
                    videoId = videoInfo.videoId,
                    title = videoInfo.title,
                    channelTitle = videoInfo.author,
                    filePath = destinationFile.absolutePath,
                    thumbnailPath = localThumbnailPath ?: videoInfo.thumbnailUrl,
                    durationSeconds = videoInfo.durationSeconds,
                    resolution = formatToDownload.qualityLabel,
                    fileSizeBytes = destinationFile.length(),
                    downloadedAt = System.currentTimeMillis()
                )

                videoRepository.saveVideo(entity)
                _downloadState.value = DownloadState.Success(entity)

                // Trigger background completion notification on mobile
                com.audiovault.app.core.service.AppNotificationManager.postVideoDownloadCompleteNotification(
                    context = context,
                    videoTitle = videoInfo.title
                )

            } catch (e: Exception) {
                if (e is CancellationException) {
                    _downloadState.value = DownloadState.Idle
                } else {
                    _downloadState.value = DownloadState.Error(e.message ?: "Failed to download video")
                }
            }
        }
    }

    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        _downloadState.value = DownloadState.Idle
    }

    private fun getDownloadDirectory(): File {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val recordifyDir = File(moviesDir, "Recordify")
        if (recordifyDir.exists() || recordifyDir.mkdirs()) {
            return recordifyDir
        }
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val recordifyDownloads = File(downloadsDir, "Recordify")
        if (recordifyDownloads.exists() || recordifyDownloads.mkdirs()) {
            return recordifyDownloads
        }
        return File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Recordify").apply { mkdirs() }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace("[\\\\/:*?\"<>|\\s]+".toRegex(), "_").take(60)
    }

    private fun downloadThumbnailFile(thumbUrl: String, destination: File) {
        try {
            val conn = URL(thumbUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode == 200) {
                conn.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
