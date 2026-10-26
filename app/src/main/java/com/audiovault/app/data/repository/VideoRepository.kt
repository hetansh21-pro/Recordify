package com.audiovault.app.data.repository

import com.audiovault.app.data.local.dao.VideoDao
import com.audiovault.app.data.local.entity.DownloadedVideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(
    private val videoDao: VideoDao
) {
    val allVideos: Flow<List<DownloadedVideoEntity>> = videoDao.getAllVideos()
    val videoCount: Flow<Int> = videoDao.getVideoCount()

    suspend fun getVideoById(id: String): DownloadedVideoEntity? = withContext(Dispatchers.IO) {
        videoDao.getVideoById(id)
    }

    suspend fun getVideoByVideoId(videoId: String): DownloadedVideoEntity? = withContext(Dispatchers.IO) {
        videoDao.getVideoByVideoId(videoId)
    }

    suspend fun saveVideo(video: DownloadedVideoEntity) = withContext(Dispatchers.IO) {
        videoDao.insertVideo(video)
    }

    suspend fun deleteVideo(video: DownloadedVideoEntity) = withContext(Dispatchers.IO) {
        // Remove file from disk
        try {
            val file = File(video.filePath)
            if (file.exists()) {
                file.delete()
            }
            video.thumbnailPath?.let { thumbPath ->
                val thumbFile = File(thumbPath)
                if (thumbFile.exists()) {
                    thumbFile.delete()
                }
            }
        } catch (_: Exception) {}

        videoDao.deleteVideo(video)
    }

    suspend fun deleteVideoById(id: String) = withContext(Dispatchers.IO) {
        val video = videoDao.getVideoById(id)
        if (video != null) {
            deleteVideo(video)
        }
    }
}
