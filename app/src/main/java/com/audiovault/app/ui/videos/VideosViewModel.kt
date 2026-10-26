package com.audiovault.app.ui.videos

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.core.downloader.DownloadState
import com.audiovault.app.core.downloader.VideoFormatInfo
import com.audiovault.app.core.downloader.YouTubeVideoDownloader
import com.audiovault.app.core.downloader.YouTubeVideoInfo
import com.audiovault.app.data.local.entity.DownloadedVideoEntity
import com.audiovault.app.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class VideosViewModel(application: Application) : AndroidViewModel(application) {

    private val appContainer = (application as AudioVaultApplication).container
    private val videoRepository: VideoRepository = appContainer.videoRepository
    val downloader: YouTubeVideoDownloader = YouTubeVideoDownloader(application, videoRepository)

    val downloadState: StateFlow<DownloadState> = downloader.downloadState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activePlayingVideo = MutableStateFlow<DownloadedVideoEntity?>(null)
    val activePlayingVideo: StateFlow<DownloadedVideoEntity?> = _activePlayingVideo.asStateFlow()

    val downloadedVideos: StateFlow<List<DownloadedVideoEntity>> = combine(
        videoRepository.allVideos,
        _searchQuery
    ) { videos, query ->
        if (query.isBlank()) {
            videos
        } else {
            videos.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.channelTitle.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun fetchVideoInfo(url: String) {
        downloader.fetchVideoDetails(url)
    }

    fun startDownload(videoInfo: YouTubeVideoInfo, format: VideoFormatInfo? = null) {
        downloader.startDownload(videoInfo, format)
    }

    fun cancelDownload() {
        downloader.cancelDownload()
    }

    fun resetDownloadState() {
        downloader.resetState()
    }

    fun deleteVideo(video: DownloadedVideoEntity) {
        viewModelScope.launch {
            if (_activePlayingVideo.value?.id == video.id) {
                _activePlayingVideo.value = null
            }
            videoRepository.deleteVideo(video)
        }
    }

    fun playVideo(video: DownloadedVideoEntity) {
        _activePlayingVideo.value = video
    }

    fun closePlayer() {
        _activePlayingVideo.value = null
    }

    fun createShareIntent(video: DownloadedVideoEntity): Intent? {
        val file = File(video.filePath)
        if (!file.exists()) return null

        val context = getApplication<Application>()
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            return null
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, video.title)
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            putExtra(Intent.EXTRA_TEXT, "${video.title} - ${video.channelTitle}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
