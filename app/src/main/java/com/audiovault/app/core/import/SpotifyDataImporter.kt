package com.audiovault.app.core.import

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.audiovault.app.core.recommendation.ListeningEventType
import com.audiovault.app.data.local.dao.ListeningHistoryDao
import com.audiovault.app.data.local.entity.ListeningHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream

data class SpotifyImportStats(
    val totalStreamsParsed: Int = 0,
    val totalHoursListened: Float = 0f,
    val topArtists: List<Pair<String, Int>> = emptyList(),
    val topGenres: List<String> = emptyList(),
    val totalLikedSongs: Int = 0,
    val earliestDate: String? = null,
    val latestDate: String? = null,
    val importedCount: Int = 0
)

data class SpotifyImportResult(
    val success: Boolean,
    val stats: SpotifyImportStats = SpotifyImportStats(),
    val errorMessage: String? = null
)

class SpotifyDataImporter(
    private val listeningHistoryDao: ListeningHistoryDao
) {
    companion object {
        private const val TAG = "Recordify/SpotifyImport"
        const val IMPORT_SESSION_ID = "spotify_import"
        private const val BATCH_SIZE = 500

        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val isoDateFormatMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val fallbackDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Imports from a Spotify export ZIP file URI or directly provided InputStream.
     */
    suspend fun importFromZip(
        inputStream: InputStream,
        onProgress: suspend (progress: Float, count: Int) -> Unit = { _, _ -> }
    ): SpotifyImportResult = withContext(Dispatchers.IO) {
        try {
            listeningHistoryDao.deleteImportedSpotifyHistory()

            val batch = mutableListOf<ListeningHistoryEntity>()
            val artistCounts = mutableMapOf<String, Int>()
            var totalStreams = 0
            var totalPlayMs = 0L
            var totalLiked = 0
            var minTimestamp = Long.MAX_VALUE
            var maxTimestamp = Long.MIN_VALUE

            val zipStream = ZipInputStream(inputStream)
            var entry = zipStream.nextEntry

            while (entry != null) {
                val entryName = entry.name.lowercase(Locale.ROOT)
                val fileName = File(entryName).name

                if (!entry.isDirectory) {
                    if (fileName.startsWith("streaming_history_audio") || 
                        fileName.startsWith("streaming_history_video") || 
                        fileName.startsWith("endsong")
                    ) {
                        parseExtendedHistoryStream(
                            inputStream = zipStream,
                            batch = batch,
                            artistCounts = artistCounts,
                            onItemParsed = { durationMs, ts ->
                                totalStreams++
                                totalPlayMs += durationMs
                                if (ts > 0) {
                                    if (ts < minTimestamp) minTimestamp = ts
                                    if (ts > maxTimestamp) maxTimestamp = ts
                                }
                            },
                            onFlushBatch = {
                                listeningHistoryDao.insertAll(it)
                                onProgress(0.5f, totalStreams)
                            }
                        )
                    } else if (fileName.startsWith("streaminghistory")) {
                        parseStandardHistoryStream(
                            inputStream = zipStream,
                            batch = batch,
                            artistCounts = artistCounts,
                            onItemParsed = { durationMs, ts ->
                                totalStreams++
                                totalPlayMs += durationMs
                                if (ts > 0) {
                                    if (ts < minTimestamp) minTimestamp = ts
                                    if (ts > maxTimestamp) maxTimestamp = ts
                                }
                            },
                            onFlushBatch = {
                                listeningHistoryDao.insertAll(it)
                                onProgress(0.5f, totalStreams)
                            }
                        )
                    } else if (fileName == "yourlibrary.json") {
                        val content = zipStream.bufferedReader().use { it.readText() }
                        val (likedCount, likedEntities) = parseYourLibraryContent(content)
                        totalLiked += likedCount
                        if (likedEntities.isNotEmpty()) {
                            listeningHistoryDao.insertAll(likedEntities)
                        }
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }

            // Flush any remaining batch
            if (batch.isNotEmpty()) {
                listeningHistoryDao.insertAll(batch)
                batch.clear()
            }

            onProgress(1.0f, totalStreams)

            val sortedArtists = artistCounts.toList().sortedByDescending { it.second }.take(10)
            val topGenres = inferTopGenresFromArtists(sortedArtists.map { it.first })
            val totalHours = totalPlayMs / (1000f * 60f * 60f)

            val earliestStr = if (minTimestamp != Long.MAX_VALUE) SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(minTimestamp) else null
            val latestStr = if (maxTimestamp != Long.MIN_VALUE) SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(maxTimestamp) else null

            SpotifyImportResult(
                success = totalStreams > 0 || totalLiked > 0,
                stats = SpotifyImportStats(
                    totalStreamsParsed = totalStreams,
                    totalHoursListened = totalHours,
                    topArtists = sortedArtists,
                    topGenres = topGenres,
                    totalLikedSongs = totalLiked,
                    earliestDate = earliestStr,
                    latestDate = latestStr,
                    importedCount = totalStreams + totalLiked
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Spotify ZIP", e)
            SpotifyImportResult(success = false, errorMessage = e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Imports directly from a folder path (e.g. extracted directory).
     */
    suspend fun importFromDirectory(
        directory: File,
        onProgress: suspend (progress: Float, count: Int) -> Unit = { _, _ -> }
    ): SpotifyImportResult = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext SpotifyImportResult(success = false, errorMessage = "Directory does not exist")
        }

        try {
            listeningHistoryDao.deleteImportedSpotifyHistory()

            val batch = mutableListOf<ListeningHistoryEntity>()
            val artistCounts = mutableMapOf<String, Int>()
            var totalStreams = 0
            var totalPlayMs = 0L
            var totalLiked = 0
            var minTimestamp = Long.MAX_VALUE
            var maxTimestamp = Long.MIN_VALUE

            val files = directory.listFiles()?.toList() ?: emptyList()
            val totalFiles = files.size.coerceAtLeast(1)

            for ((fileIndex, file) in files.withIndex()) {
                if (file.isFile) {
                    val name = file.name.lowercase(Locale.ROOT)
                    if (name.startsWith("streaming_history_audio") || 
                        name.startsWith("streaming_history_video") || 
                        name.startsWith("endsong")
                    ) {
                        file.inputStream().use { stream ->
                            parseExtendedHistoryStream(
                                inputStream = stream,
                                batch = batch,
                                artistCounts = artistCounts,
                                onItemParsed = { durationMs, ts ->
                                    totalStreams++
                                    totalPlayMs += durationMs
                                    if (ts > 0) {
                                        if (ts < minTimestamp) minTimestamp = ts
                                        if (ts > maxTimestamp) maxTimestamp = ts
                                    }
                                },
                                onFlushBatch = {
                                    listeningHistoryDao.insertAll(it)
                                    val progress = (fileIndex.toFloat() / totalFiles).coerceIn(0f, 0.95f)
                                    onProgress(progress, totalStreams)
                                }
                            )
                        }
                    } else if (name.startsWith("streaminghistory")) {
                        file.inputStream().use { stream ->
                            parseStandardHistoryStream(
                                inputStream = stream,
                                batch = batch,
                                artistCounts = artistCounts,
                                onItemParsed = { durationMs, ts ->
                                    totalStreams++
                                    totalPlayMs += durationMs
                                    if (ts > 0) {
                                        if (ts < minTimestamp) minTimestamp = ts
                                        if (ts > maxTimestamp) maxTimestamp = ts
                                    }
                                },
                                onFlushBatch = {
                                    listeningHistoryDao.insertAll(it)
                                    val progress = (fileIndex.toFloat() / totalFiles).coerceIn(0f, 0.95f)
                                    onProgress(progress, totalStreams)
                                }
                            )
                        }
                    } else if (name == "yourlibrary.json") {
                        val content = file.readText()
                        val (likedCount, likedEntities) = parseYourLibraryContent(content)
                        totalLiked += likedCount
                        if (likedEntities.isNotEmpty()) {
                            listeningHistoryDao.insertAll(likedEntities)
                        }
                    }
                }
            }

            // Flush remaining
            if (batch.isNotEmpty()) {
                listeningHistoryDao.insertAll(batch)
                batch.clear()
            }

            onProgress(1.0f, totalStreams)

            val sortedArtists = artistCounts.toList().sortedByDescending { it.second }.take(10)
            val topGenres = inferTopGenresFromArtists(sortedArtists.map { it.first })
            val totalHours = totalPlayMs / (1000f * 60f * 60f)

            val earliestStr = if (minTimestamp != Long.MAX_VALUE) SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(minTimestamp) else null
            val latestStr = if (maxTimestamp != Long.MIN_VALUE) SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(maxTimestamp) else null

            SpotifyImportResult(
                success = totalStreams > 0 || totalLiked > 0,
                stats = SpotifyImportStats(
                    totalStreamsParsed = totalStreams,
                    totalHoursListened = totalHours,
                    topArtists = sortedArtists,
                    topGenres = topGenres,
                    totalLikedSongs = totalLiked,
                    earliestDate = earliestStr,
                    latestDate = latestStr,
                    importedCount = totalStreams + totalLiked
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Spotify folder", e)
            SpotifyImportResult(success = false, errorMessage = e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Imports from a SAF Document Tree Uri (Folder selected by user in Android Document UI).
     */
    suspend fun importFromDocumentTree(
        context: Context,
        treeUri: Uri,
        onProgress: suspend (progress: Float, count: Int) -> Unit = { _, _ -> }
    ): SpotifyImportResult = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext SpotifyImportResult(success = false, errorMessage = "Cannot access selected folder")

            val files = rootDoc.listFiles()
            if (files.isEmpty()) {
                return@withContext SpotifyImportResult(success = false, errorMessage = "Selected folder is empty")
            }

            listeningHistoryDao.deleteImportedSpotifyHistory()

            val batch = mutableListOf<ListeningHistoryEntity>()
            val artistCounts = mutableMapOf<String, Int>()
            var totalStreams = 0
            var totalPlayMs = 0L
            var totalLiked = 0
            var minTimestamp = Long.MAX_VALUE
            var maxTimestamp = Long.MIN_VALUE

            val totalFiles = files.size

            for ((fileIndex, doc) in files.withIndex()) {
                val name = doc.name?.lowercase(Locale.ROOT) ?: ""
                if (name.startsWith("streaming_history_audio") || 
                    name.startsWith("streaming_history_video") || 
                    name.startsWith("endsong")
                ) {
                    context.contentResolver.openInputStream(doc.uri)?.use { stream ->
                        parseExtendedHistoryStream(
                            inputStream = stream,
                            batch = batch,
                            artistCounts = artistCounts,
                            onItemParsed = { durationMs, ts ->
                                totalStreams++
                                totalPlayMs += durationMs
                                if (ts > 0) {
                                    if (ts < minTimestamp) minTimestamp = ts
                                    if (ts > maxTimestamp) maxTimestamp = ts
                                }
                            },
                            onFlushBatch = {
                                listeningHistoryDao.insertAll(it)
                                val progress = (fileIndex.toFloat() / totalFiles).coerceIn(0f, 0.95f)
                                onProgress(progress, totalStreams)
                            }
                        )
                    }
                } else if (name.startsWith("streaminghistory")) {
                    context.contentResolver.openInputStream(doc.uri)?.use { stream ->
                        parseStandardHistoryStream(
                            inputStream = stream,
                            batch = batch,
                            artistCounts = artistCounts,
                            onItemParsed = { durationMs, ts ->
                                totalStreams++
                                totalPlayMs += durationMs
                                if (ts > 0) {
                                    if (ts < minTimestamp) minTimestamp = ts
                                    if (ts > maxTimestamp) maxTimestamp = ts
                                }
                            },
                            onFlushBatch = {
                                listeningHistoryDao.insertAll(it)
                                val progress = (fileIndex.toFloat() / totalFiles).coerceIn(0f, 0.95f)
                                onProgress(progress, totalStreams)
                            }
                        )
                    }
                } else if (name == "yourlibrary.json") {
                    context.contentResolver.openInputStream(doc.uri)?.use { stream ->
                        val content = stream.bufferedReader().use { it.readText() }
                        val (likedCount, likedEntities) = parseYourLibraryContent(content)
                        totalLiked += likedCount
                        if (likedEntities.isNotEmpty()) {
                            listeningHistoryDao.insertAll(likedEntities)
                        }
                    }
                }
            }

            // Flush remaining batch
            if (batch.isNotEmpty()) {
                listeningHistoryDao.insertAll(batch)
                batch.clear()
            }

            onProgress(1.0f, totalStreams)

            val sortedArtists = artistCounts.toList().sortedByDescending { it.second }.take(10)
            val topGenres = inferTopGenresFromArtists(sortedArtists.map { it.first })
            val totalHours = totalPlayMs / (1000f * 60f * 60f)

            val earliestStr = if (minTimestamp != Long.MAX_VALUE) SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(minTimestamp) else null
            val latestStr = if (maxTimestamp != Long.MIN_VALUE) SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(maxTimestamp) else null

            SpotifyImportResult(
                success = totalStreams > 0 || totalLiked > 0,
                stats = SpotifyImportStats(
                    totalStreamsParsed = totalStreams,
                    totalHoursListened = totalHours,
                    topArtists = sortedArtists,
                    topGenres = topGenres,
                    totalLikedSongs = totalLiked,
                    earliestDate = earliestStr,
                    latestDate = latestStr,
                    importedCount = totalStreams + totalLiked
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Spotify document tree", e)
            SpotifyImportResult(success = false, errorMessage = e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Streaming parser for Extended Spotify Streaming History JSON (endsong_*.json or Streaming_History_Audio_*.json).
     * Parses directly from InputStream using JsonReader to prevent OutOfMemoryError.
     */
    private suspend fun parseExtendedHistoryStream(
        inputStream: InputStream,
        batch: MutableList<ListeningHistoryEntity>,
        artistCounts: MutableMap<String, Int>,
        onItemParsed: (durationMs: Long, timestamp: Long) -> Unit,
        onFlushBatch: suspend (List<ListeningHistoryEntity>) -> Unit
    ) {
        val reader = JsonReader(BufferedReader(InputStreamReader(inputStream, "UTF-8")))
        reader.beginArray()

        while (reader.hasNext()) {
            var tsStr: String? = null
            var trackName: String? = null
            var artistName: String? = null
            var albumName: String? = null
            var trackUri: String? = null
            var msPlayed: Long = 0L
            var reasonStart: String? = null
            var reasonEnd: String? = null
            var skipped: Boolean = false

            reader.beginObject()
            while (reader.hasNext()) {
                val fieldName = reader.nextName()
                if (reader.peek() == android.util.JsonToken.NULL) {
                    reader.nextNull()
                    continue
                }

                when (fieldName) {
                    "ts" -> tsStr = reader.nextString()
                    "master_metadata_track_name" -> trackName = reader.nextString()
                    "master_metadata_album_artist_name" -> artistName = reader.nextString()
                    "master_metadata_album_album_name" -> albumName = reader.nextString()
                    "spotify_track_uri" -> trackUri = reader.nextString()
                    "ms_played" -> msPlayed = reader.nextLong()
                    "reason_start" -> reasonStart = reader.nextString()
                    "reason_end" -> reasonEnd = reader.nextString()
                    "skipped" -> skipped = reader.nextBoolean()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            if (!trackName.isNullOrBlank() && !artistName.isNullOrBlank()) {
                val timestamp = parseTimestamp(tsStr)
                val cleanArtist = artistName.trim()
                artistCounts[cleanArtist] = (artistCounts[cleanArtist] ?: 0) + 1

                val (eventType, eventWeight) = determineExtendedEventType(
                    msPlayed = msPlayed,
                    skipped = skipped,
                    reasonEnd = reasonEnd
                )

                val entity = ListeningHistoryEntity(
                    songId = trackUri ?: "spotify_${cleanArtist}_${trackName}".replace(" ", "_"),
                    title = trackName.trim(),
                    artist = cleanArtist,
                    album = albumName?.trim(),
                    albumArtPath = null,
                    streamUrl = null,
                    eventType = eventType.name,
                    eventWeight = eventWeight,
                    durationPlayedMs = msPlayed,
                    songDurationMs = msPlayed,
                    completionPercentage = if (eventType == ListeningEventType.PLAY_COMPLETED) 1.0f else 0.5f,
                    timestamp = timestamp,
                    sessionId = IMPORT_SESSION_ID,
                    isOnline = true,
                    genre = null,
                    language = null
                )

                batch.add(entity)
                onItemParsed(msPlayed, timestamp)

                if (batch.size >= BATCH_SIZE) {
                    val toInsert = batch.toList()
                    batch.clear()
                    onFlushBatch(toInsert)
                }
            }
        }
        reader.endArray()
    }

    /**
     * Streaming parser for Standard 1-Year History JSON (StreamingHistory*.json).
     */
    private suspend fun parseStandardHistoryStream(
        inputStream: InputStream,
        batch: MutableList<ListeningHistoryEntity>,
        artistCounts: MutableMap<String, Int>,
        onItemParsed: (durationMs: Long, timestamp: Long) -> Unit,
        onFlushBatch: suspend (List<ListeningHistoryEntity>) -> Unit
    ) {
        val reader = JsonReader(BufferedReader(InputStreamReader(inputStream, "UTF-8")))
        reader.beginArray()

        while (reader.hasNext()) {
            var endTimeStr: String? = null
            var artistName: String? = null
            var trackName: String? = null
            var msPlayed: Long = 0L

            reader.beginObject()
            while (reader.hasNext()) {
                val fieldName = reader.nextName()
                if (reader.peek() == android.util.JsonToken.NULL) {
                    reader.nextNull()
                    continue
                }

                when (fieldName) {
                    "endTime" -> endTimeStr = reader.nextString()
                    "artistName" -> artistName = reader.nextString()
                    "trackName" -> trackName = reader.nextString()
                    "msPlayed" -> msPlayed = reader.nextLong()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            if (!trackName.isNullOrBlank() && !artistName.isNullOrBlank()) {
                val timestamp = parseTimestamp(endTimeStr)
                val cleanArtist = artistName.trim()
                artistCounts[cleanArtist] = (artistCounts[cleanArtist] ?: 0) + 1

                val (eventType, eventWeight) = determineStandardEventType(msPlayed)

                val entity = ListeningHistoryEntity(
                    songId = "spotify_${cleanArtist}_${trackName}".replace(" ", "_"),
                    title = trackName.trim(),
                    artist = cleanArtist,
                    album = null,
                    albumArtPath = null,
                    streamUrl = null,
                    eventType = eventType.name,
                    eventWeight = eventWeight,
                    durationPlayedMs = msPlayed,
                    songDurationMs = msPlayed,
                    completionPercentage = if (eventType == ListeningEventType.PLAY_COMPLETED) 1.0f else 0.5f,
                    timestamp = timestamp,
                    sessionId = IMPORT_SESSION_ID,
                    isOnline = true,
                    genre = null,
                    language = null
                )

                batch.add(entity)
                onItemParsed(msPlayed, timestamp)

                if (batch.size >= BATCH_SIZE) {
                    val toInsert = batch.toList()
                    batch.clear()
                    onFlushBatch(toInsert)
                }
            }
        }
        reader.endArray()
    }

    /**
     * Parses YourLibrary.json for liked tracks, albums, and followed artists.
     */
    private fun parseYourLibraryContent(jsonString: String): Pair<Int, List<ListeningHistoryEntity>> {
        return try {
            val root = JSONObject(jsonString)
            val tracksArray = root.optJSONArray("tracks") ?: JSONArray()
            val list = mutableListOf<ListeningHistoryEntity>()
            val now = System.currentTimeMillis()

            for (i in 0 until tracksArray.length()) {
                val item = tracksArray.getJSONObject(i)
                val track = item.optString("track", "").trim()
                val artist = item.optString("artist", "").trim()
                val album = item.optString("album", "").trim()
                val uri = item.optString("uri", "")

                if (track.isNotBlank() && artist.isNotBlank()) {
                    list.add(
                        ListeningHistoryEntity(
                            songId = if (uri.isNotBlank()) uri else "spotify_liked_${artist}_${track}".replace(" ", "_"),
                            title = track,
                            artist = artist,
                            album = album.takeIf { it.isNotBlank() },
                            eventType = ListeningEventType.SONG_LIKED.name,
                            eventWeight = 10.0f,
                            durationPlayedMs = 0L,
                            songDurationMs = 0L,
                            completionPercentage = 1.0f,
                            timestamp = now - (i * 1000L),
                            sessionId = IMPORT_SESSION_ID,
                            isOnline = true
                        )
                    )
                }
            }
            Pair(list.size, list)
        } catch (_: Exception) {
            Pair(0, emptyList())
        }
    }

    private fun determineExtendedEventType(
        msPlayed: Long,
        skipped: Boolean,
        reasonEnd: String?
    ): Pair<ListeningEventType, Float> {
        return when {
            skipped && msPlayed < 10_000 -> Pair(ListeningEventType.SONG_SHORT_SKIP, -8.0f)
            skipped || reasonEnd == "fwdbtn" -> Pair(ListeningEventType.SONG_SKIPPED, -6.0f)
            reasonEnd == "trackdone" || msPlayed >= 60_000 -> Pair(ListeningEventType.PLAY_COMPLETED, 5.0f)
            msPlayed >= 30_000 -> Pair(ListeningEventType.PLAY_50_PERCENT, 3.0f)
            msPlayed >= 15_000 -> Pair(ListeningEventType.PLAY_25_PERCENT, 2.0f)
            else -> Pair(ListeningEventType.PLAY_STARTED, 1.0f)
        }
    }

    private fun determineStandardEventType(msPlayed: Long): Pair<ListeningEventType, Float> {
        return when {
            msPlayed < 10_000 -> Pair(ListeningEventType.SONG_SHORT_SKIP, -8.0f)
            msPlayed < 30_000 -> Pair(ListeningEventType.SONG_SKIPPED, -6.0f)
            msPlayed >= 60_000 -> Pair(ListeningEventType.PLAY_COMPLETED, 5.0f)
            else -> Pair(ListeningEventType.PLAY_50_PERCENT, 3.0f)
        }
    }

    private fun parseTimestamp(ts: String?): Long {
        if (ts.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            isoDateFormat.parse(ts)?.time
                ?: isoDateFormatMillis.parse(ts)?.time
                ?: fallbackDateFormat.parse(ts)?.time
                ?: System.currentTimeMillis()
        } catch (_: Exception) {
            try {
                fallbackDateFormat.parse(ts)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun inferTopGenresFromArtists(artists: List<String>): List<String> {
        val genres = mutableSetOf<String>()
        for (artist in artists) {
            val a = artist.lowercase()
            when {
                a.contains("shubh") || a.contains("sidhu") || a.contains("karan aujla") || 
                a.contains("ap dhillon") || a.contains("diljit") || a.contains("arjan") -> {
                    genres.add("Punjabi Pop & Hip-Hop")
                }
                a.contains("arijit") || a.contains("shreya") || a.contains("atif") || 
                a.contains("sonu") || a.contains("jubin") || a.contains("pritam") -> {
                    genres.add("Bollywood & Romantic")
                }
                a.contains("anirudh") || a.contains("rahman") || a.contains("sid sriram") -> {
                    genres.add("South Indian & Melodies")
                }
                a.contains("drake") || a.contains("eminem") || a.contains("travis") || 
                a.contains("weeknd") || a.contains("post malone") -> {
                    genres.add("Hip-Hop & R&B")
                }
                a.contains("ed sheeran") || a.contains("taylor") || a.contains("dua lipa") || 
                a.contains("coldplay") -> {
                    genres.add("Global Pop & Indie")
                }
            }
        }
        if (genres.isEmpty()) {
            genres.add("Top Hits & Pop")
            genres.add("Bollywood")
        }
        return genres.toList()
    }
}
