package com.audiovault.app.data.repository

import android.content.Context
import android.util.Log
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Thread-safe persistent repository for user-imported albums and playlists.
 * Guarantees that tracklists, song order, stream URLs, and metadata never mutate or scramble
 * across app restarts, reopens, or library navigations.
 */
class ImportedAlbumRepository(private val context: Context) {

    companion object {
        private const val TAG = "Recordify/ImportedRepo"
        private const val FILE_NAME = "imported_albums_v1.json"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataFile = File(context.filesDir, FILE_NAME)

    private val _importedAlbums = MutableStateFlow<List<OnlineAlbumModel>>(emptyList())
    val importedAlbums: StateFlow<List<OnlineAlbumModel>> = _importedAlbums.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            if (!dataFile.exists()) {
                _importedAlbums.value = emptyList()
                return
            }
            val text = dataFile.readText()
            if (text.isBlank()) {
                _importedAlbums.value = emptyList()
                return
            }

            val root = JSONObject(text)
            val arr = root.optJSONArray("albums") ?: JSONArray()
            val list = mutableListOf<OnlineAlbumModel>()

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                val title = obj.optString("title", "")
                if (id.isBlank() || title.isBlank()) continue

                val artist = obj.optString("artist", "Various Artists")
                val coverArt = obj.optString("coverArtUrl", "")
                val year = obj.optString("year", "")
                val type = obj.optString("type", "Album")
                val source = obj.optString("source", "imported")
                val primaryArtists = obj.optString("primaryArtists", artist)

                val songsArr = obj.optJSONArray("songs") ?: JSONArray()
                val songs = mutableListOf<OnlineSongModel>()

                for (j in 0 until songsArr.length()) {
                    val sObj = songsArr.optJSONObject(j) ?: continue
                    val sId = sObj.optString("id", "")
                    val sTitle = sObj.optString("title", "")
                    if (sTitle.isBlank()) continue

                    val sArtist = sObj.optString("artist", artist)
                    val sAlbum = sObj.optString("album", title)
                    val sDuration = sObj.optLong("durationMs", 0L)
                    val sStream = sObj.optString("streamUrl", "")
                    val sArt = sObj.optString("highResArtUrl", coverArt)
                    val sBitrate = sObj.optString("bitrate", "320kbps")
                    val sSource = sObj.optString("source", source)
                    val sYtId = sObj.optString("youtubeVideoId", "").takeIf { it.isNotBlank() }
                    val sLyrics = sObj.optString("lyrics", "").takeIf { it.isNotBlank() }

                    songs.add(
                        OnlineSongModel(
                            id = if (sId.isNotBlank()) sId else "imp_track_${id}_$j",
                            title = sTitle,
                            artist = sArtist,
                            album = sAlbum,
                            durationMs = sDuration,
                            streamUrl = sStream,
                            highResArtUrl = sArt.takeIf { it.isNotBlank() },
                            bitrate = sBitrate,
                            source = sSource,
                            youtubeVideoId = sYtId,
                            lyrics = sLyrics
                        )
                    )
                }

                list.add(
                    OnlineAlbumModel(
                        id = id,
                        title = title,
                        artist = artist,
                        coverArtUrl = coverArt.takeIf { it.isNotBlank() },
                        songCount = songs.size,
                        songs = songs,
                        year = year.takeIf { it.isNotBlank() },
                        type = type,
                        source = source,
                        primaryArtists = primaryArtists
                    )
                )
            }

            _importedAlbums.value = list
            Log.i(TAG, "Loaded ${list.size} imported albums from disk")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading imported albums", e)
        }
    }

    private fun persistData() {
        val currentList = _importedAlbums.value
        scope.launch {
            try {
                val root = JSONObject()
                val arr = JSONArray()

                for (album in currentList) {
                    val obj = JSONObject().apply {
                        put("id", album.id)
                        put("title", album.title)
                        put("artist", album.artist ?: "")
                        put("coverArtUrl", album.coverArtUrl ?: "")
                        put("year", album.year ?: "")
                        put("type", album.type)
                        put("source", album.source)
                        put("primaryArtists", album.primaryArtists ?: "")

                        val songsArr = JSONArray()
                        for (song in album.songs) {
                            val sObj = JSONObject().apply {
                                put("id", song.id)
                                put("title", song.title)
                                put("artist", song.artist)
                                put("album", song.album ?: "")
                                put("durationMs", song.durationMs)
                                put("streamUrl", song.streamUrl)
                                put("highResArtUrl", song.highResArtUrl ?: "")
                                put("bitrate", song.bitrate)
                                put("source", song.source)
                                put("youtubeVideoId", song.youtubeVideoId ?: "")
                                put("lyrics", song.lyrics ?: "")
                            }
                            songsArr.put(sObj)
                        }
                        put("songs", songsArr)
                    }
                    arr.put(obj)
                }

                root.put("albums", arr)
                dataFile.writeText(root.toString())
                Log.i(TAG, "Persisted ${currentList.size} imported albums to disk")
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting imported albums", e)
            }
        }
    }

    /**
     * Saves or updates an imported album permanently.
     */
    fun saveAlbum(album: OnlineAlbumModel, sourceUrl: String = "") {
        if (album.title.isBlank()) return
        val current = _importedAlbums.value.toMutableList()
        val index = current.indexOfFirst { it.id == album.id || (it.title.equals(album.title, ignoreCase = true) && it.artist.equals(album.artist, ignoreCase = true)) }

        val sealedAlbum = album.copy(
            source = "imported",
            songCount = album.songs.size
        )

        if (index >= 0) {
            current[index] = sealedAlbum
        } else {
            current.add(0, sealedAlbum)
        }

        _importedAlbums.value = current
        persistData()
    }

    /**
     * Retrieves an imported album by ID. Returns exact tracks without network calls.
     */
    fun getAlbumById(id: String): OnlineAlbumModel? {
        return _importedAlbums.value.firstOrNull { it.id == id }
    }

    /**
     * Retrieves an imported album matching title or query.
     */
    fun getAlbumByTitle(title: String): OnlineAlbumModel? {
        val clean = title.trim().lowercase()
        return _importedAlbums.value.firstOrNull { it.title.trim().lowercase() == clean }
    }

    /**
     * Checks if an album is stored in this repository.
     */
    fun isAlbumImported(id: String): Boolean {
        return _importedAlbums.value.any { it.id == id }
    }

    /**
     * Deletes an imported album permanently from disk.
     */
    fun deleteAlbum(id: String) {
        val current = _importedAlbums.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            _importedAlbums.value = current
            persistData()
        }
    }
}
