package com.audiovault.app.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class UserPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val songIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

class PlaylistRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataFile = File(context.filesDir, "playlists_v1.json")

    private val _likedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedSongIds: StateFlow<Set<Long>> = _likedSongIds.asStateFlow()

    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    private val _savedArtists = MutableStateFlow<Set<String>>(emptySet())
    val savedArtists: StateFlow<Set<String>> = _savedArtists.asStateFlow()

    private val _manualArtists = MutableStateFlow<Set<String>>(emptySet())
    val manualArtists: StateFlow<Set<String>> = _manualArtists.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            if (dataFile.exists()) {
                val jsonStr = dataFile.readText()
                if (jsonStr.isNotBlank()) {
                    val root = JSONObject(jsonStr)

                    // Load liked song IDs
                    val likedArr = root.optJSONArray("likedSongIds")
                    val likedSet = mutableSetOf<Long>()
                    if (likedArr != null) {
                        for (i in 0 until likedArr.length()) {
                            likedSet.add(likedArr.getLong(i))
                        }
                    }
                    _likedSongIds.value = likedSet

                    // Load saved artists
                    val savedArtistsArr = root.optJSONArray("savedArtists")
                    val artistSet = mutableSetOf<String>()
                    if (savedArtistsArr != null) {
                        for (i in 0 until savedArtistsArr.length()) {
                            artistSet.add(savedArtistsArr.getString(i))
                        }
                    }
                    _savedArtists.value = artistSet

                    // Load manual artists
                    val manualArtistsArr = root.optJSONArray("manualArtists")
                    val manualArtistSet = mutableSetOf<String>()
                    if (manualArtistsArr != null) {
                        for (i in 0 until manualArtistsArr.length()) {
                            manualArtistSet.add(manualArtistsArr.getString(i))
                        }
                    }
                    _manualArtists.value = manualArtistSet

                    // Load custom playlists
                    val playlistArr = root.optJSONArray("playlists")
                    val pList = mutableListOf<UserPlaylist>()
                    if (playlistArr != null) {
                        for (i in 0 until playlistArr.length()) {
                            val obj = playlistArr.getJSONObject(i)
                            val id = obj.optString("id", UUID.randomUUID().toString())
                            val name = obj.optString("name", "My Playlist")
                            val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            val songsArr = obj.optJSONArray("songIds")
                            val sIds = mutableListOf<Long>()
                            if (songsArr != null) {
                                for (j in 0 until songsArr.length()) {
                                    sIds.add(songsArr.getLong(j))
                                }
                            }
                            pList.add(UserPlaylist(id = id, name = name, songIds = sIds, createdAt = createdAt))
                        }
                    }
                    _playlists.value = pList
                }
            }
        } catch (e: Exception) {
            Log.e("AudioVault/Playlist", "Error loading playlists data", e)
        }
    }

    private fun persistData() {
        scope.launch {
            try {
                val root = JSONObject()

                val likedArr = JSONArray()
                _likedSongIds.value.forEach { likedArr.put(it) }
                root.put("likedSongIds", likedArr)

                val artistArr = JSONArray()
                _savedArtists.value.forEach { artistArr.put(it) }
                root.put("savedArtists", artistArr)

                val manualArr = JSONArray()
                _manualArtists.value.forEach { manualArr.put(it) }
                root.put("manualArtists", manualArr)

                val pArr = JSONArray()
                _playlists.value.forEach { p ->
                    val pObj = JSONObject()
                    pObj.put("id", p.id)
                    pObj.put("name", p.name)
                    pObj.put("createdAt", p.createdAt)
                    val sArr = JSONArray()
                    p.songIds.forEach { sArr.put(it) }
                    pObj.put("songIds", sArr)
                    pArr.put(pObj)
                }
                root.put("playlists", pArr)

                dataFile.writeText(root.toString())
            } catch (e: Exception) {
                Log.e("AudioVault/Playlist", "Error saving playlists data", e)
            }
        }
    }

    fun isArtistSaved(artistName: String): Boolean {
        val cleanName = artistName.trim()
        return _savedArtists.value.any { it.equals(cleanName, ignoreCase = true) }
    }

    fun addManualArtist(artistName: String) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return
        val current = _manualArtists.value.toMutableSet()
        val existing = current.firstOrNull { it.equals(cleanName, ignoreCase = true) }
        if (existing == null) {
            current.add(cleanName)
            _manualArtists.value = current
            persistData()
        }
    }

    fun removeArtist(artistName: String) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return
        val mCurrent = _manualArtists.value.toMutableSet()
        val mRemoved = mCurrent.removeAll { it.equals(cleanName, ignoreCase = true) }
        if (mRemoved) {
            _manualArtists.value = mCurrent
        }
        val sCurrent = _savedArtists.value.toMutableSet()
        val sRemoved = sCurrent.removeAll { it.equals(cleanName, ignoreCase = true) }
        if (sRemoved) {
            _savedArtists.value = sCurrent
        }
        if (mRemoved || sRemoved) {
            persistData()
        }
    }

    fun saveArtist(artistName: String) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return
        val current = _savedArtists.value.toMutableSet()
        val existing = current.firstOrNull { it.equals(cleanName, ignoreCase = true) }
        if (existing == null) {
            current.add(cleanName)
            _savedArtists.value = current
            persistData()
        }
    }

    fun removeSavedArtist(artistName: String) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return
        val current = _savedArtists.value.toMutableSet()
        val removed = current.removeAll { it.equals(cleanName, ignoreCase = true) }
        if (removed) {
            _savedArtists.value = current
            persistData()
        }
    }

    fun toggleSavedArtist(artistName: String): Boolean {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return false
        val current = _savedArtists.value.toMutableSet()
        val existing = current.firstOrNull { it.equals(cleanName, ignoreCase = true) }
        val isNowSaved = if (existing != null) {
            current.remove(existing)
            false
        } else {
            current.add(cleanName)
            true
        }
        _savedArtists.value = current
        persistData()
        return isNowSaved
    }

    fun isSongLiked(songId: Long): Boolean {
        return _likedSongIds.value.contains(songId)
    }

    fun toggleLikedSong(songId: Long): Boolean {
        val current = _likedSongIds.value.toMutableSet()
        val isNowLiked = if (current.contains(songId)) {
            current.remove(songId)
            false
        } else {
            current.add(songId)
            true
        }
        _likedSongIds.value = current
        persistData()
        return isNowLiked
    }

    fun createPlaylist(name: String, initialSongId: Long? = null): UserPlaylist {
        val cleanName = name.trim().ifBlank { "My Playlist #${_playlists.value.size + 1}" }
        val songList = if (initialSongId != null) listOf(initialSongId) else emptyList()
        val newPlaylist = UserPlaylist(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            songIds = songList,
            createdAt = System.currentTimeMillis()
        )
        _playlists.value = listOf(newPlaylist) + _playlists.value
        persistData()
        return newPlaylist
    }

    fun deletePlaylist(playlistId: String) {
        _playlists.value = _playlists.value.filter { it.id != playlistId }
        persistData()
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        if (newName.isBlank()) return
        _playlists.value = _playlists.value.map { p ->
            if (p.id == playlistId) p.copy(name = newName.trim()) else p
        }
        persistData()
    }

    fun toggleSongInPlaylist(playlistId: String, songId: Long): Boolean {
        var isNowInPlaylist = false
        _playlists.value = _playlists.value.map { p ->
            if (p.id == playlistId) {
                val songs = p.songIds.toMutableList()
                if (songs.contains(songId)) {
                    songs.remove(songId)
                    isNowInPlaylist = false
                } else {
                    songs.add(songId)
                    isNowInPlaylist = true
                }
                p.copy(songIds = songs)
            } else {
                p
            }
        }
        persistData()
        return isNowInPlaylist
    }

    fun isSongInPlaylist(playlistId: String, songId: Long): Boolean {
        return _playlists.value.find { it.id == playlistId }?.songIds?.contains(songId) == true
    }
}
