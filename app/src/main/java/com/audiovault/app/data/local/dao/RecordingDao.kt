package com.audiovault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.audiovault.app.data.local.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AudioVault recordings.
 */
@Dao
interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    suspend fun getAllRecordings(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchRecordings(query: String): Flow<List<RecordingEntity>>

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("UPDATE recordings SET albumArtPath = :artPath WHERE id = :id")
    suspend fun updateAlbumArtPath(id: Long, artPath: String)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("DELETE FROM recordings")
    suspend fun deleteAllRecordings()

    @Query("SELECT COUNT(*) FROM recordings")
    fun observeRecordingCount(): Flow<Int>

    @Query("SELECT SUM(fileSizeBytes) FROM recordings")
    fun observeTotalStorageBytes(): Flow<Long?>
    @Query("SELECT COUNT(*) FROM recordings WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title)) AND (:artist IS NULL OR artist IS NULL OR LOWER(TRIM(artist)) = LOWER(TRIM(:artist)))")
    suspend fun countDuplicateRecordings(title: String, artist: String?): Int

    @Query("SELECT EXISTS(SELECT 1 FROM recordings WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title)) AND (:artist IS NULL OR artist IS NULL OR LOWER(TRIM(artist)) = LOWER(TRIM(:artist))))")
    suspend fun isSongAlreadyRecorded(title: String, artist: String?): Boolean
}
