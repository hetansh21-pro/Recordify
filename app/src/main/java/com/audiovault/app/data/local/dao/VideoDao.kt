package com.audiovault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.audiovault.app.data.local.entity.DownloadedVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM downloaded_videos ORDER BY downloadedAt DESC")
    fun getAllVideos(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): DownloadedVideoEntity?

    @Query("SELECT * FROM downloaded_videos WHERE videoId = :videoId LIMIT 1")
    suspend fun getVideoByVideoId(videoId: String): DownloadedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: DownloadedVideoEntity)

    @Update
    suspend fun updateVideo(video: DownloadedVideoEntity)

    @Delete
    suspend fun deleteVideo(video: DownloadedVideoEntity)

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)

    @Query("SELECT COUNT(*) FROM downloaded_videos")
    fun getVideoCount(): Flow<Int>
}
