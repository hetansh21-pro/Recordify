package com.audiovault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.audiovault.app.data.local.entity.ListeningHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ListeningHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ListeningHistoryEntity>)

    @Query("SELECT * FROM listening_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<ListeningHistoryEntity>>

    @Query("SELECT * FROM listening_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistorySync(limit: Int = 100): List<ListeningHistoryEntity>

    @Query("SELECT * FROM listening_history WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getHistorySince(sinceTimestamp: Long): List<ListeningHistoryEntity>

    @Query("SELECT * FROM listening_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSessionHistory(sessionId: String): List<ListeningHistoryEntity>

    @Query("SELECT COUNT(*) FROM listening_history WHERE eventType = 'SONG_SKIPPED'")
    suspend fun getTotalSkipCount(): Int

    @Query("SELECT COUNT(*) FROM listening_history WHERE eventType = 'PLAY_COMPLETED'")
    suspend fun getTotalCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM listening_history WHERE sessionId = 'spotify_import'")
    suspend fun getImportedSpotifyCount(): Int

    @Query("""
        SELECT title, artist, album, COUNT(*) as playCount, SUM(durationPlayedMs) as totalMs
        FROM listening_history
        WHERE title IS NOT NULL AND title != '' AND artist IS NOT NULL AND artist != ''
        GROUP BY title, artist
        ORDER BY totalMs DESC, playCount DESC
        LIMIT :limit
    """)
    suspend fun getTopMostPlayedSongs(limit: Int = 30): List<TopSongTuple>

    @Query("""
        SELECT artist, COUNT(*) as playCount
        FROM listening_history
        WHERE artist IS NOT NULL AND artist != ''
        GROUP BY artist
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getTopArtists(limit: Int = 15): List<TopArtistTuple>

    @Query("""
        SELECT album, artist, COUNT(*) as playCount
        FROM listening_history
        WHERE album IS NOT NULL AND album != ''
        GROUP BY album
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getTopAlbums(limit: Int = 10): List<TopAlbumTuple>

    @Query("DELETE FROM listening_history WHERE sessionId = 'spotify_import'")
    suspend fun deleteImportedSpotifyHistory()

    @Query("DELETE FROM listening_history WHERE timestamp < :beforeTimestamp")
    suspend fun pruneOldHistory(beforeTimestamp: Long)

    @Query("DELETE FROM listening_history")
    suspend fun clearAllHistory()
}

data class TopSongTuple(
    val title: String,
    val artist: String,
    val album: String?,
    val playCount: Int,
    val totalMs: Long
)

data class TopArtistTuple(
    val artist: String,
    val playCount: Int
)

data class TopAlbumTuple(
    val album: String,
    val artist: String?,
    val playCount: Int
)
