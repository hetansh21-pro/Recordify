package com.audiovault.app.ui.recordings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.data.repository.SortOption
import com.audiovault.app.data.storage.FileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class RecordingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AudioVaultApplication).container
    private val recordingRepository: RecordingRepository = container.recordingRepository
    private val fileManager: FileManager = container.fileManager
    private val playlistRepository: com.audiovault.app.data.repository.PlaylistRepository = container.playlistRepository
    val playerEngine: AudioPlayerEngine = container.playerEngine

    val playerState: StateFlow<PlayerState> = playerEngine.playerState

    val likedSongIds: StateFlow<Set<Long>> = playlistRepository.likedSongIds
    val playlists: StateFlow<List<com.audiovault.app.data.repository.UserPlaylist>> = playlistRepository.playlists

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST_FIRST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    fun toggleLikedSong(songId: Long): Boolean = playlistRepository.toggleLikedSong(songId)
    fun isSongLiked(songId: Long): Boolean = playlistRepository.isSongLiked(songId)
    fun createPlaylist(name: String, songId: Long? = null) = playlistRepository.createPlaylist(name, songId)
    fun toggleSongInPlaylist(playlistId: String, songId: Long): Boolean = playlistRepository.toggleSongInPlaylist(playlistId, songId)
    fun deletePlaylist(playlistId: String) = playlistRepository.deletePlaylist(playlistId)
    fun renamePlaylist(playlistId: String, newName: String) = playlistRepository.renamePlaylist(playlistId, newName)

    @OptIn(ExperimentalCoroutinesApi::class)
    val recordings: StateFlow<List<RecordingEntity>> =
        combine(_searchQuery, _sortOption) { query, sort ->
            Pair(query, sort)
        }.flatMapLatest { (query, sort) ->
            recordingRepository.getFilteredAndSortedRecordings(query, sort)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Automatically cleanup any existing short clips (<50s) or advertisements from previous sessions
        viewModelScope.launch {
            recordingRepository.cleanupInvalidAndAdRecordings()
        }

        // Automatic Cover Art Resolver: Fetches and caches high-res cover art in the background
        viewModelScope.launch {
            recordings.collect { list ->
                list.filter { it.albumArtPath.isNullOrBlank() && (!it.artist.isNullOrBlank() || it.isIdentifiedSong) }
                    .forEach { song ->
                        val artPath = com.audiovault.app.core.intelligence.CoverArtFetcher.fetchAndCacheCoverArt(
                            context = application,
                            title = song.title,
                            artist = song.artist
                        )
                        if (artPath != null) {
                            recordingRepository.updateAlbumArtPath(song.id, artPath)
                        }
                    }
            }
        }

        // Automatic Artist Photo Resolver: Fetches and caches artist pictures in the background once forever
        viewModelScope.launch {
            recordings.collect { list ->
                list.forEach { song ->
                    val artistStr = song.artist
                    if (!artistStr.isNullOrBlank()) {
                        val artists = com.audiovault.app.ui.library.splitArtists(artistStr)
                        artists.filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
                            .distinct()
                            .forEach { artistName ->
                                com.audiovault.app.core.intelligence.CoverArtFetcher.fetchAndCacheArtistArt(
                                    context = application,
                                    artistName = artistName,
                                    songHint = song.title
                                )
                            }
                    }
                }
            }
        }

        // Auto-play next song (random if shuffle enabled) when playback completes
        viewModelScope.launch {
            playerState.collect { state ->
                if (state.isCompleted) {
                    playNextTrack()
                }
            }
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChange(sort: SortOption) {
        _sortOption.value = sort
    }

    fun playPauseRecording(recording: RecordingEntity) {
        if (playerState.value.currentRecording?.id == recording.id) {
            playerEngine.togglePlayPause()
        } else {
            playerEngine.play(recording)
        }
    }

    fun playNextTrack() {
        val currentList = recordings.value
        if (currentList.isEmpty()) return

        val currentId = playerState.value.currentRecording?.id
        if (_isShuffleEnabled.value) {
            // Pick a random different song
            val candidateList = if (currentList.size > 1 && currentId != null) {
                currentList.filter { it.id != currentId }
            } else {
                currentList
            }
            val randomSong = candidateList[Random.nextInt(candidateList.size)]
            playerEngine.play(randomSong)
        } else {
            val currentIndex = currentList.indexOfFirst { it.id == currentId }
            val nextIndex = if (currentIndex != -1) (currentIndex + 1) % currentList.size else 0
            playerEngine.play(currentList[nextIndex])
        }
    }

    fun playPreviousTrack() {
        val currentList = recordings.value
        if (currentList.isEmpty()) return

        val currentId = playerState.value.currentRecording?.id
        if (_isShuffleEnabled.value) {
            val candidateList = if (currentList.size > 1 && currentId != null) {
                currentList.filter { it.id != currentId }
            } else {
                currentList
            }
            val randomSong = candidateList[Random.nextInt(candidateList.size)]
            playerEngine.play(randomSong)
        } else {
            val currentIndex = currentList.indexOfFirst { it.id == currentId }
            val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentList.size - 1
            playerEngine.play(currentList[prevIndex])
        }
    }

    fun renameRecording(recording: RecordingEntity, newTitle: String) {
        viewModelScope.launch {
            recordingRepository.renameRecording(recording, newTitle)
        }
    }

    fun updateLyrics(recording: RecordingEntity, lyrics: String) {
        viewModelScope.launch {
            recordingRepository.updateLyrics(recording, lyrics)
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            if (playerState.value.currentRecording?.id == recording.id) {
                playerEngine.stopAndClose()
            }
            recordingRepository.deleteRecording(recording)
        }
    }

    fun createShareIntent(recording: RecordingEntity): Intent? {
        val uri = fileManager.getShareableUri(recording.filePath) ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recording.displayArtistTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
