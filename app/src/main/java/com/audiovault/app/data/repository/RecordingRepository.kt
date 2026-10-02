package com.audiovault.app.data.repository

import com.audiovault.app.core.intelligence.SongMetadata
import com.audiovault.app.data.local.dao.RecordingDao
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.data.storage.FileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

enum class SortOption {
    NEWEST_FIRST,
    OLDEST_FIRST,
    DURATION_LONGEST,
    DURATION_SHORTEST,
    TITLE_A_TO_Z
}

class RecordingRepository(
    private val recordingDao: RecordingDao,
    private val fileManager: FileManager
) {

    val recordingsFlow: Flow<List<RecordingEntity>> = recordingDao.observeRecordings()
    val recordingCountFlow: Flow<Int> = recordingDao.observeRecordingCount()
    val totalStorageBytesFlow: Flow<Long> = recordingDao.observeTotalStorageBytes().map { it ?: 0L }

    fun getFilteredAndSortedRecordings(
        query: String = "",
        sortOption: SortOption = SortOption.NEWEST_FIRST
    ): Flow<List<RecordingEntity>> {
        val baseFlow = if (query.isBlank()) {
            recordingDao.observeRecordings()
        } else {
            recordingDao.searchRecordings(query.trim())
        }

        return baseFlow.map { list ->
            when (sortOption) {
                SortOption.NEWEST_FIRST -> list.sortedByDescending { it.createdAt }
                SortOption.OLDEST_FIRST -> list.sortedBy { it.createdAt }
                SortOption.DURATION_LONGEST -> list.sortedByDescending { it.durationMs }
                SortOption.DURATION_SHORTEST -> list.sortedBy { it.durationMs }
                SortOption.TITLE_A_TO_Z -> list.sortedBy { it.title.lowercase() }
            }
        }
    }

    suspend fun isSongAlreadyRecorded(title: String, artist: String?): Boolean {
        return recordingDao.isSongAlreadyRecorded(title.trim(), artist?.trim())
    }

    fun isAdvertisement(title: String, artist: String?): Boolean {
        val t = title.trim().lowercase()
        val a = (artist ?: "").trim().lowercase()

        val adKeywords = listOf(
            "advertisement", "spotify ad", "audio ad", "sponsored", "commercial",
            "promo", "promotional", "upgrade to premium", "1 of 1", "1 of 2", "2 of 2",
            "1 of 3", "2 of 3", "3 of 3", "ad •", "ad 1 of", "ad 2 of", "ad 3 of"
        )
        if (adKeywords.any { t.contains(it) || a.contains(it) }) return true
        if (t == "ad" || a == "ad") return true
        if (t.startsWith("ad:") || t.startsWith("ad -") || t.startsWith("ad ") || t.startsWith("[ad]") || t.startsWith("(ad)")) return true
        if (a.startsWith("ad:") || a.startsWith("ad -") || a.startsWith("ad ") || a.startsWith("[ad]") || a.startsWith("(ad)")) return true
        if (a.contains("spotify free") || a.contains("spotify commercial") || a.contains("spotify ad")) return true
        return false
    }

    suspend fun saveRecording(file: File, durationMs: Long, metadata: SongMetadata? = null, bitrateKbps: Int = 256): Long {
        // 1. Minimum duration enforcement: Reject any audio under 50 seconds (50,000ms)
        if (durationMs < 50_000L) {
            android.util.Log.i("AudioVault/Repo", "Recording too short (${durationMs}ms < 50000ms). Discarding.")
            if (file.exists()) {
                file.delete()
            }
            return -1L
        }

        val title = fileManager.generateDefaultTitle(metadata)

        // 2. Advertisement filter: Reject any ads or commercials
        if (metadata?.isAdvertisement == true || isAdvertisement(title, metadata?.artist)) {
            android.util.Log.i("AudioVault/Repo", "Advertisement detected ('$title' by '${metadata?.artist}'). Discarding.")
            if (file.exists()) {
                file.delete()
            }
            return -1L
        }
        
        // 3. Duplicate check: Skip if already in library
        if (metadata != null && metadata.isIdentified) {
            val isDuplicate = recordingDao.isSongAlreadyRecorded(metadata.title.trim(), metadata.artist?.trim())
            if (isDuplicate) {
                android.util.Log.i("AudioVault/Repo", "Duplicate detected for '${metadata.displayTitle}'. Skipping save.")
                if (file.exists()) {
                    file.delete()
                }
                return -1L
            }
        }

        val entity = RecordingEntity(
            fileName = file.name,
            filePath = file.absolutePath,
            title = title,
            artist = metadata?.artist,
            album = metadata?.album,
            lyrics = metadata?.lyrics,
            albumArtPath = metadata?.albumArtPath,
            createdAt = System.currentTimeMillis(),
            durationMs = durationMs,
            fileSizeBytes = file.length(),
            bitrateKbps = bitrateKbps,
            mimeType = "audio/mp4",
            sourcePackage = metadata?.sourcePackage,
            isIdentifiedSong = metadata?.isIdentified == true
        )
        return recordingDao.insertRecording(entity)
    }

    suspend fun updateAlbumArtPath(id: Long, artPath: String) {
        recordingDao.updateAlbumArtPath(id, artPath)
    }

    suspend fun cleanupInvalidAndAdRecordings() {
        try {
            val allRecordings = recordingDao.getAllRecordings()
            allRecordings.forEach { rec ->
                if (rec.durationMs < 50_000L || isAdvertisement(rec.title, rec.artist)) {
                    android.util.Log.i("AudioVault/Repo", "Cleaning up short/ad recording: ${rec.title} (${rec.durationMs}ms)")
                    deleteRecording(rec)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioVault/Repo", "Error cleaning up invalid recordings", e)
        }
    }

    suspend fun getRecordingById(id: Long): RecordingEntity? {
        return recordingDao.getRecordingById(id)
    }

    suspend fun renameRecording(recording: RecordingEntity, newTitle: String) {
        if (newTitle.isNotBlank()) {
            val updated = recording.copy(title = newTitle.trim())
            recordingDao.updateRecording(updated)
        }
    }

    suspend fun updateLyrics(recording: RecordingEntity, lyrics: String) {
        val updated = recording.copy(lyrics = lyrics.trim())
        recordingDao.updateRecording(updated)
    }

    suspend fun deleteRecording(recording: RecordingEntity) {
        fileManager.deleteFile(recording.filePath)
        recordingDao.deleteRecording(recording)
    }

    suspend fun clearAllRecordings() {
        fileManager.clearAllRecordingFiles()
        recordingDao.deleteAllRecordings()
    }
}
