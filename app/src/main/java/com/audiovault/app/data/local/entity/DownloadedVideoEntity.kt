package com.audiovault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_videos")
data class DownloadedVideoEntity(
    @PrimaryKey
    val id: String,
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val durationSeconds: Long = 0,
    val resolution: String = "720p HD",
    val fileSizeBytes: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)
