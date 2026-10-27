package com.audiovault.app.ui.recordings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.data.repository.SortOption
import com.audiovault.app.data.storage.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.audiovault.app.core.identity.CanonicalTrackIdentity
import com.audiovault.app.core.identity.RecordingVersionType
import com.audiovault.app.core.queue.QueueItem
import com.audiovault.app.core.queue.QueueMode
import com.audiovault.app.core.queue.PlaybackQueueState
import com.audiovault.app.core.queue.PlaybackRepeatMode
import com.audiovault.app.core.search.SearchResultContainer
import kotlin.random.Random

class RecordingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AudioVaultApplication).container
    private val recordingRepository: RecordingRepository = container.recordingRepository
    private val fileManager: FileManager = container.fileManager
    private val playlistRepository: com.audiovault.app.data.repository.PlaylistRepository = container.playlistRepository
    val playerEngine: AudioPlayerEngine = container.playerEngine
    private val onlineSongDownloader = container.onlineSongDownloader
    private val userPreferencesRepository = container.userPreferencesRepository
    val recommendationService: com.audiovault.app.core.recommendation.RecommendationService = container.recommendationService
    val playbackQueueManager: com.audiovault.app.core.queue.PlaybackQueueManager = container.playbackQueueManager
    val queueState: StateFlow<com.audiovault.app.core.queue.PlaybackQueueState> = playbackQueueManager.queueState

    val playerState: StateFlow<PlayerState> = playerEngine.playerState
    val streamingMetrics: StateFlow<com.audiovault.app.core.player.StreamingMetrics> = playerEngine.streamingMetrics

    val userSettings: StateFlow<com.audiovault.app.data.repository.UserSettings> =
        userPreferencesRepository.userSettingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = com.audiovault.app.data.repository.UserSettings()
        )

    private val notificationManager = com.audiovault.app.core.service.PlaybackNotificationManager(
        context = application,
        playerEngine = playerEngine,
        onPlayNext = { playNextTrack() },
        onPlayPrevious = { playPreviousTrack() }
    )

    val homeFeed: StateFlow<com.audiovault.app.core.recommendation.HomeRecommendationFeed> = recommendationService.homeFeed
    val currentVibe: StateFlow<com.audiovault.app.core.recommendation.UserVibe> = recommendationService.currentVibe
    val userProfile: StateFlow<com.audiovault.app.core.recommendation.UserMusicProfile> = recommendationService.userProfile
    val smartQueue: StateFlow<List<OnlineSongModel>> = recommendationService.smartQueueEngine.queue

    val likedSongIds: StateFlow<Set<Long>> = playlistRepository.likedSongIds
    val playlists: StateFlow<List<com.audiovault.app.data.repository.UserPlaylist>> = playlistRepository.playlists
    val savedArtists: StateFlow<Set<String>> = playlistRepository.savedArtists
    val manualArtists: StateFlow<Set<String>> = playlistRepository.manualArtists
    val recentSearches: StateFlow<List<com.audiovault.app.data.repository.RecentlyPlayedSearchItem>> =
        userPreferencesRepository.recentSearchesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userName: StateFlow<String> = userPreferencesRepository.userNameFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _onlineSearchResults = MutableStateFlow<List<OnlineSongModel>>(emptyList())
    val onlineSearchResults: StateFlow<List<OnlineSongModel>> = _onlineSearchResults.asStateFlow()

    private val _searchResultContainer = MutableStateFlow<SearchResultContainer?>(null)
    val searchResultContainer: StateFlow<SearchResultContainer?> = _searchResultContainer.asStateFlow()

    private val _isSearchOriginalOnly = MutableStateFlow(true)
    val isSearchOriginalOnly: StateFlow<Boolean> = _isSearchOriginalOnly.asStateFlow()

    private val _selectedDecade = MutableStateFlow<IntRange?>(null)
    val selectedDecade: StateFlow<IntRange?> = _selectedDecade.asStateFlow()

    private val _selectedLanguageFilter = MutableStateFlow<String?>(null)
    val selectedLanguageFilter: StateFlow<String?> = _selectedLanguageFilter.asStateFlow()

    private val _selectedVersionFilter = MutableStateFlow<RecordingVersionType?>(null)
    val selectedVersionFilter: StateFlow<RecordingVersionType?> = _selectedVersionFilter.asStateFlow()

    private val _isOnlineSearching = MutableStateFlow(false)
    val isOnlineSearching: StateFlow<Boolean> = _isOnlineSearching.asStateFlow()

    private val _downloadingSongIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingSongIds: StateFlow<Set<String>> = _downloadingSongIds.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _albumDownloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val albumDownloadProgress: StateFlow<Map<String, Float>> = _albumDownloadProgress.asStateFlow()

    private val downloadJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val albumDownloadJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    private var onlineSearchJob: Job? = null
    private var activeStreamResolutionJob: Job? = null
    private var pendingSongPreviewJob: Job? = null

    private val _pendingSongCountdown = MutableStateFlow(0)
    val pendingSongCountdown: StateFlow<Int> = _pendingSongCountdown.asStateFlow()

    private val _isPendingSongPreview = MutableStateFlow(false)
    val isPendingSongPreview: StateFlow<Boolean> = _isPendingSongPreview.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST_FIRST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _matchingAlbums = MutableStateFlow<List<com.audiovault.app.core.online.OnlineAlbumModel>>(emptyList())
    val matchingAlbums: StateFlow<List<com.audiovault.app.core.online.OnlineAlbumModel>> = _matchingAlbums.asStateFlow()

    private val _homeContent = MutableStateFlow(com.audiovault.app.core.online.SpotifyHomeContent())
    val homeContent: StateFlow<com.audiovault.app.core.online.SpotifyHomeContent> = _homeContent.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<com.audiovault.app.core.online.OnlineAlbumModel?>(null)
    val selectedAlbum: StateFlow<com.audiovault.app.core.online.OnlineAlbumModel?> = _selectedAlbum.asStateFlow()

    private val _isAlbumLoading = MutableStateFlow(false)
    val isAlbumLoading: StateFlow<Boolean> = _isAlbumLoading.asStateFlow()

    private val _homeLatestReleases = MutableStateFlow<List<OnlineSongModel>>(emptyList())
    val homeLatestReleases: StateFlow<List<OnlineSongModel>> = _homeLatestReleases.asStateFlow()

    private val _homeTrendingSoundtracks = MutableStateFlow<List<com.audiovault.app.core.online.OnlineAlbumModel>>(emptyList())
    val homeTrendingSoundtracks: StateFlow<List<com.audiovault.app.core.online.OnlineAlbumModel>> = _homeTrendingSoundtracks.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    val isShuffleEnabled: StateFlow<Boolean> = playbackQueueManager.queueState
        .map { it.isShuffleEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val repeatMode: StateFlow<PlaybackRepeatMode> = playbackQueueManager.queueState
        .map { it.repeatMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PlaybackRepeatMode.OFF
        )

    private val _activeAlbumContext = MutableStateFlow<com.audiovault.app.core.online.OnlineAlbumModel?>(null)
    val activeAlbumContext: StateFlow<com.audiovault.app.core.online.OnlineAlbumModel?> = _activeAlbumContext.asStateFlow()

    private val recentArtistsQueue = ArrayDeque<String>()

    fun toggleLikedSong(songId: Long, recordingFallback: RecordingEntity? = null): Boolean {
        var finalSongId = songId
        val targetRecording = if (songId > 0) {
            allRecordings.value.firstOrNull { it.id == songId } ?: recordingFallback
        } else {
            recordingFallback ?: playerState.value.currentRecording
        }

        if (targetRecording != null && (targetRecording.id <= 0 || allRecordings.value.none { it.id == targetRecording.id })) {
            viewModelScope.launch {
                val saved = recordingRepository.getOrCreateOnlineRecording(targetRecording)
                finalSongId = saved.id
                if (playerState.value.currentRecording?.filePath == targetRecording.filePath || playerState.value.currentRecording?.id == targetRecording.id) {
                    playerEngine.updateCurrentRecording(saved)
                }
                val isLiked = playlistRepository.toggleLikedSong(finalSongId)
                val eventType = if (isLiked) {
                    com.audiovault.app.core.recommendation.ListeningEventType.SONG_LIKED
                } else {
                    com.audiovault.app.core.recommendation.ListeningEventType.SONG_UNLIKED
                }
                recommendationService.recordEvent(
                    com.audiovault.app.core.recommendation.ListeningEvent(
                        songId = finalSongId.toString(),
                        title = saved.title,
                        artist = saved.artist ?: "Unknown Artist",
                        album = saved.album,
                        albumArtPath = saved.albumArtPath,
                        streamUrl = saved.filePath,
                        type = eventType,
                        isOnline = saved.sourcePackage == "com.audiovault.online"
                    )
                )
            }
            return true
        }

        val isLiked = playlistRepository.toggleLikedSong(songId)
        viewModelScope.launch {
            val rec = allRecordings.value.firstOrNull { it.id == songId }
            val songTitle = rec?.title ?: "Song $songId"
            val songArtist = rec?.artist ?: "Unknown Artist"
            val eventType = if (isLiked) {
                com.audiovault.app.core.recommendation.ListeningEventType.SONG_LIKED
            } else {
                com.audiovault.app.core.recommendation.ListeningEventType.SONG_UNLIKED
            }
            recommendationService.recordEvent(
                com.audiovault.app.core.recommendation.ListeningEvent(
                    songId = songId.toString(),
                    title = songTitle,
                    artist = songArtist,
                    album = rec?.album,
                    albumArtPath = rec?.albumArtPath,
                    streamUrl = rec?.filePath,
                    type = eventType,
                    isOnline = rec?.sourcePackage == "com.audiovault.online"
                )
            )
        }
        return isLiked
    }
    fun isSongLiked(songId: Long): Boolean = playlistRepository.isSongLiked(songId)
    fun toggleSavedArtist(artistName: String): Boolean = playlistRepository.toggleSavedArtist(artistName)
    fun isArtistSaved(artistName: String): Boolean = playlistRepository.isArtistSaved(artistName)
    fun createPlaylist(name: String, songId: Long? = null, recordingFallback: RecordingEntity? = null) {
        if (songId != null && songId <= 0 && recordingFallback != null) {
            viewModelScope.launch {
                val saved = recordingRepository.getOrCreateOnlineRecording(recordingFallback)
                if (playerState.value.currentRecording?.filePath == recordingFallback.filePath || playerState.value.currentRecording?.id == recordingFallback.id) {
                    playerEngine.updateCurrentRecording(saved)
                }
                playlistRepository.createPlaylist(name, saved.id)
            }
        } else {
            playlistRepository.createPlaylist(name, songId)
        }
    }
    fun toggleSongInPlaylist(playlistId: String, songId: Long, recordingFallback: RecordingEntity? = null): Boolean {
        val targetRecording = if (songId > 0) {
            allRecordings.value.firstOrNull { it.id == songId } ?: recordingFallback
        } else {
            recordingFallback ?: playerState.value.currentRecording
        }

        if (targetRecording != null && (targetRecording.id <= 0 || allRecordings.value.none { it.id == targetRecording.id })) {
            viewModelScope.launch {
                val saved = recordingRepository.getOrCreateOnlineRecording(targetRecording)
                if (playerState.value.currentRecording?.filePath == targetRecording.filePath || playerState.value.currentRecording?.id == targetRecording.id) {
                    playerEngine.updateCurrentRecording(saved)
                }
                playlistRepository.toggleSongInPlaylist(playlistId, saved.id)
            }
            return true
        }

        return playlistRepository.toggleSongInPlaylist(playlistId, songId)
    }
    fun deletePlaylist(playlistId: String) = playlistRepository.deletePlaylist(playlistId)
    fun renamePlaylist(playlistId: String, newName: String) = playlistRepository.renamePlaylist(playlistId, newName)

    val allRecordings: StateFlow<List<RecordingEntity>> = recordingRepository.recordingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
                                    context = getApplication<Application>(),
                                    artistName = artistName,
                                    songHint = song.title
                                )
                            }
                    }
                }
            }
        }

        // Observe Data Saver setting and dynamically adapt audio player load control & buffer
        viewModelScope.launch {
            userPreferencesRepository.userSettingsFlow.collect { settings ->
                playerEngine.updateDataSaverMode(settings.isDataSaverEnabled)
            }
        }

        // Attach Player Engine Telemetry Callbacks for Listening Event Milestones
        playerEngine.onProgressMilestone = { rec, frac ->
            viewModelScope.launch {
                val eventType = when {
                    frac >= 0.75f -> com.audiovault.app.core.recommendation.ListeningEventType.PLAY_75_PERCENT
                    frac >= 0.50f -> com.audiovault.app.core.recommendation.ListeningEventType.PLAY_50_PERCENT
                    else -> com.audiovault.app.core.recommendation.ListeningEventType.PLAY_25_PERCENT
                }
                recommendationService.recordEvent(
                    com.audiovault.app.core.recommendation.ListeningEvent(
                        songId = rec.id.toString(),
                        title = rec.title,
                        artist = rec.artist ?: "Unknown Artist",
                        album = rec.album,
                        albumArtPath = rec.albumArtPath,
                        streamUrl = rec.filePath,
                        type = eventType,
                        durationPlayedMs = (rec.durationMs * frac).toLong(),
                        songDurationMs = rec.durationMs,
                        completionPercentage = frac,
                        isOnline = rec.sourcePackage == "com.audiovault.online"
                    )
                )
            }
        }

        // Observe Player Engine state to continuously update Spotify-style system notification
        viewModelScope.launch {
            playerEngine.playerState.collect { state ->
                notificationManager.updateNotification(state)
            }
        }
    }

    fun togglePlayback() {
        playerEngine.togglePlayPause()
    }

    fun pausePlayback() {
        playerEngine.pause()
    }

    fun resumePlayback() {
        playerEngine.resume()
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun seekForward(deltaMs: Long = 5000L) {
        val current = playerState.value.currentPositionMs
        val duration = playerState.value.durationMs
        seekTo((current + deltaMs).coerceAtMost(duration))
    }

    fun seekBackward(deltaMs: Long = 5000L) {
        val current = playerState.value.currentPositionMs
        seekTo((current - deltaMs).coerceAtLeast(0L))
    }

    fun cycleRepeatMode() {
        playbackQueueManager.toggleRepeatMode()
    }

    fun toggleShuffle() {
        playbackQueueManager.toggleShuffle()
    }

    fun toggleRepeatMode() {
        playbackQueueManager.toggleRepeatMode()
    }

    fun removeFromQueue(index: Int) {
        playbackQueueManager.removeAt(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playbackQueueManager.moveQueueItem(fromIndex, toIndex)
    }

    fun moveQueueItemUp(index: Int) {
        if (index > 0) {
            playbackQueueManager.moveQueueItem(index, index - 1)
        }
    }

    fun moveQueueItemDown(index: Int) {
        val queueSize = queueState.value.upcomingQueue.size
        if (index < queueSize - 1) {
            playbackQueueManager.moveQueueItem(index, index + 1)
        }
    }

    fun playFromQueue(index: Int) {
        playbackQueueManager.playImmediatelyFromQueue(index, isDataSaver = userSettings.value.isDataSaverEnabled)
    }

    fun refreshVibeFlowQueue() {
        playbackQueueManager.refreshVibeFlowQueue()
    }

    fun toggleAutoQueue() {
        playbackQueueManager.toggleAutoQueue()
    }

    fun togglePersonalization() {
        playbackQueueManager.togglePersonalization()
    }

    fun clearQueue() {
        playbackQueueManager.clearUpcoming()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchOnlineMusic(query)
    }

    fun onSortOptionChange(sort: SortOption) {
        _sortOption.value = sort
    }

    fun toggleSearchOriginalOnly() {
        val next = !_isSearchOriginalOnly.value
        _isSearchOriginalOnly.value = next
        viewModelScope.launch {
            userPreferencesRepository.setSearchOriginalOnly(next)
        }
        searchOnlineMusic(_searchQuery.value)
    }

    fun setSelectedDecade(range: IntRange?) {
        _selectedDecade.value = range
    }

    fun setSelectedLanguageFilter(lang: String?) {
        _selectedLanguageFilter.value = lang
    }

    fun setSelectedVersionFilter(version: RecordingVersionType?) {
        _selectedVersionFilter.value = version
    }

    fun searchOnlineMusic(query: String) {
        onlineSearchJob?.cancel()
        val trimmed = query.trim()

        // 1. Detect pasted Spotify links (Album, Playlist, Track)
        val spotifyMatch = com.audiovault.app.core.online.SpotifyLinkParser.parse(trimmed)
        if (spotifyMatch != null) {
            onlineSearchJob = viewModelScope.launch {
                _isOnlineSearching.value = true
                try {
                    when (spotifyMatch.type) {
                        com.audiovault.app.core.online.SpotifyEntityType.ALBUM -> {
                            val album = com.audiovault.app.core.online.SpotifyWebMetadataFetcher.fetchAlbum(spotifyMatch.id)
                            if (album != null) {
                                _matchingAlbums.value = listOf(album)
                                openAlbumDetails(album)
                            }
                        }
                        com.audiovault.app.core.online.SpotifyEntityType.PLAYLIST -> {
                            val playlistAlbum = com.audiovault.app.core.online.SpotifyWebMetadataFetcher.fetchPlaylist(spotifyMatch.id)
                            if (playlistAlbum != null) {
                                _matchingAlbums.value = listOf(playlistAlbum)
                                openAlbumDetails(playlistAlbum)
                            }
                        }
                        com.audiovault.app.core.online.SpotifyEntityType.TRACK -> {
                            val track = com.audiovault.app.core.online.SpotifyWebMetadataFetcher.fetchTrack(spotifyMatch.id)
                            if (track != null) {
                                _onlineSearchResults.value = listOf(track)
                                playOnlineSong(track)
                            }
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Recordify/Search", "Error resolving Spotify link", e)
                } finally {
                    _isOnlineSearching.value = false
                }
            }
            return
        }

        // 2. Allow 1-letter and short prefix searches
        if (trimmed.isEmpty()) {
            _onlineSearchResults.value = emptyList()
            _matchingAlbums.value = emptyList()
            _searchResultContainer.value = null
            _isOnlineSearching.value = false
            return
        }

        val settings = userSettings.value
        val debounceMs = if (trimmed.length <= 2) 90L else 120L

        // Check for matching pre-built curated albums (90s, Old Songs, Retro Classics)
        val curatedMatches = com.audiovault.app.core.catalog.CuratedAlbumCatalog.findMatchingAlbums(trimmed)
        if (curatedMatches.isNotEmpty()) {
            _matchingAlbums.value = curatedMatches
        }

        onlineSearchJob = viewModelScope.launch {
            delay(debounceMs)
            _isOnlineSearching.value = true
            try {
                com.audiovault.app.core.online.OnlineMusicSearchService.searchSmartMultiSource(
                    rawQuery = trimmed,
                    limit = 30,
                    isOriginalOnly = _isSearchOriginalOnly.value,
                    userCountryCode = settings.userCountryCode,
                    primaryLanguage = settings.primaryLanguage,
                    preferredLanguages = settings.preferredLanguages
                ) { partial ->
                    val combinedAlbums = if (curatedMatches.isNotEmpty()) {
                        (curatedMatches + partial.albums).distinctBy { it.id }
                    } else {
                        partial.albums
                    }
                    _searchResultContainer.value = partial.copy(albums = combinedAlbums)
                    _matchingAlbums.value = combinedAlbums
                    _onlineSearchResults.value = partial.songs.map { it.toOnlineSongModel() }
                }
            } catch (e: Exception) {
                if (_onlineSearchResults.value.isEmpty()) {
                    _onlineSearchResults.value = emptyList()
                }
            } finally {
                _isOnlineSearching.value = false
            }
        }
    }

    fun loadHomeContent(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (recommendationService.homeFeed.value.madeForYou.isEmpty() && _homeContent.value.popularAlbumsAndSingles.isEmpty()) {
                _isHomeLoading.value = true
            }

            try {
                // 1. Dynamic Spotify & Taste recommendation feed in parallel
                val feedJob = launch {
                    try {
                        val currentSettings = userSettings.value
                        recommendationService.loadHomeFeed(
                            userName = userName.value,
                            forceRefresh = forceRefresh,
                            countryCode = currentSettings.userCountryCode,
                            countryName = currentSettings.userCountryName,
                            primaryLang = currentSettings.primaryLanguage,
                            preferredLangs = currentSettings.preferredLanguages,
                            favoriteGenres = currentSettings.favoriteGenres,
                            favoriteArtists = currentSettings.favoriteArtists
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("Recordify/Home", "Error loading recommendation feed", e)
                    }
                }

                // 2. Fetch curated albums and singles in parallel
                val featuredJob = launch {
                    if (_homeContent.value.popularAlbumsAndSingles.isEmpty() || forceRefresh) {
                        try {
                            val content = com.audiovault.app.core.online.OnlineMusicSearchService.fetchHomeFeaturedContent()
                            _homeContent.value = content
                            _homeLatestReleases.value = content.latestReleases
                            _homeTrendingSoundtracks.value = content.popularAlbumsAndSingles
                        } catch (e: Exception) {
                            android.util.Log.e("Recordify/Home", "Error loading featured content", e)
                        }
                    }
                }

                // Turn off loading indicator as soon as any content arrives
                feedJob.join()
                featuredJob.join()
            } catch (e: Exception) {
                android.util.Log.e("Recordify/Home", "Error in home content orchestrator", e)
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun refreshVibe() {
        viewModelScope.launch {
            _isHomeLoading.value = true
            try {
                recommendationService.changeVibe()
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun changeVibe(vibeCategory: com.audiovault.app.core.recommendation.VibeCategory) {
        viewModelScope.launch {
            _isHomeLoading.value = true
            try {
                recommendationService.changeVibe(vibeCategory)
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun openAlbumDetails(
        album: com.audiovault.app.core.online.OnlineAlbumModel,
        autoPlayIndex: Int? = null
    ) {
        _selectedAlbum.value = album

        // Save album to Recent Searches
        viewModelScope.launch {
            userPreferencesRepository.addRecentSearchItem(
                com.audiovault.app.data.repository.RecentlyPlayedSearchItem(
                    id = "alb_${album.id}",
                    title = album.title,
                    artist = album.artist ?: "Soundtrack",
                    albumArtPath = album.coverArtUrl,
                    isOnline = true,
                    isAlbum = true,
                    albumId = album.id
                )
            )
        }

        if (album.songs.isEmpty()) {
            viewModelScope.launch {
                _isAlbumLoading.value = true
                try {
                    val isArtist = album.type.equals("Artist", ignoreCase = true) ||
                            album.year.equals("Artist", ignoreCase = true) ||
                            album.id.startsWith("artist_")

                    val fullAlbum: com.audiovault.app.core.online.OnlineAlbumModel? = when {
                        isArtist -> {
                            val targetArtistName = (album.primaryArtists ?: album.title)
                                .replace(Regex("(?i)\\b(artist|album|playlist|official)\\b"), "")
                                .trim()
                            val artistSongs = com.audiovault.app.core.online.OnlineMusicSearchService.fetchArtistAllSongs(
                                artistName = targetArtistName,
                                topCuratedQueries = album.trackQueries,
                                limit = 50
                            )
                            if (artistSongs.isNotEmpty()) {
                                album.copy(songs = artistSongs, songCount = artistSongs.size)
                            } else null
                        }
                        album.trackQueries.isNotEmpty() -> {
                            val resolvedSongs = com.audiovault.app.core.online.OnlineMusicSearchService.resolveSongsFastParallel(album.trackQueries)
                            if (resolvedSongs.isNotEmpty()) {
                                album.copy(songs = resolvedSongs, songCount = resolvedSongs.size)
                            } else null
                        }
                        album.id.startsWith("saavn_pl_") || album.type.equals("Playlist", ignoreCase = true) -> {
                            com.audiovault.app.core.online.OnlineMusicSearchService.fetchPlaylistDetails(album.id)
                        }
                        album.id.startsWith("sp_") -> {
                            val cleanId = album.id.removePrefix("sp_")
                            if (album.type.equals("Playlist", ignoreCase = true) || cleanId.length > 20) {
                                com.audiovault.app.core.online.SpotifyWebMetadataFetcher.fetchPlaylist(cleanId)
                            } else {
                                com.audiovault.app.core.online.SpotifyWebMetadataFetcher.fetchAlbum(cleanId)
                            }
                        }
                        !album.id.startsWith("curated_") && album.id.all { it.isDigit() } -> {
                            com.audiovault.app.core.online.OnlineMusicSearchService.fetchAlbumSoundtrack(album.id)
                        }
                        else -> null
                    }

                    val resolvedAlbum = if (fullAlbum != null && fullAlbum.songs.isNotEmpty()) {
                        fullAlbum
                    } else {
                        // 1. Resolve authentic movie / album soundtrack directly from official catalog
                        val movieSoundtrack = com.audiovault.app.core.online.OnlineMusicSearchService.resolveMovieSoundtrack(album.title)
                        if (movieSoundtrack != null && movieSoundtrack.songs.isNotEmpty()) {
                            movieSoundtrack
                        } else {
                            // 2. Clean semantic fallback search
                            val cleanTitle = album.title
                                .replace(Regex("[\\p{So}\\p{Cn}]"), "")
                                .replace(Regex("(?i)\\b(playlist|spotify|album|official|edition|curated)\\b"), "")
                                .trim()
                            val cleanArtist = (album.primaryArtists ?: album.artist ?: "")
                                .replace(Regex("[\\p{So}\\p{Cn}]"), "")
                                .replace(Regex("(?i)\\b(playlist|spotify|various artists)\\b"), "")
                                .trim()
                            val queryToSearch = album.searchQuery ?: "$cleanTitle $cleanArtist".trim()
                            val foundSongs = com.audiovault.app.core.online.OnlineMusicSearchService.searchSongs(queryToSearch.trim(), limit = 35)
                            if (foundSongs.isNotEmpty()) {
                                album.copy(songs = foundSongs, songCount = foundSongs.size)
                            } else null
                        }
                    }

                    // Enforce Strict Filtering: only authentic matching songs, top trending first, no remix/edit/leakage
                    val finalAlbum = if (resolvedAlbum != null) {
                        if (isArtist) {
                            resolvedAlbum
                        } else {
                            val isIndianPlaylist = album.type.equals("Playlist", ignoreCase = true) ||
                                    album.language.equals("hindi", ignoreCase = true) ||
                                    com.audiovault.app.core.online.OnlineMusicSearchService.isIndianIntentQuery(album.title + " " + (album.primaryArtists ?: ""))

                            if (isIndianPlaylist) {
                                val junkKeywords = listOf("remix", "re-mix", "lofi", "lo-fi", "slowed", "reverb", "mashup", "karaoke", "ringtone")
                                val filteredIndian = resolvedAlbum.songs.filter { song ->
                                    val sLang = song.language?.lowercase()
                                    val sArtist = song.artist.lowercase()
                                    val sTitle = song.title.lowercase()
                                    val isExplicitEnglish = sLang == "english" || sLang == "western"
                                    val isWesternArtist = sArtist.contains("taylor swift") || sArtist.contains("ed sheeran") ||
                                            sArtist.contains("drake") || sArtist.contains("billie eilish") || sArtist.contains("the weeknd")
                                    val isNotJunk = junkKeywords.none { sTitle.contains(it) }
                                    !isExplicitEnglish && !isWesternArtist && isNotJunk
                                }
                                resolvedAlbum.copy(
                                    songs = filteredIndian.ifEmpty { resolvedAlbum.songs },
                                    songCount = filteredIndian.ifEmpty { resolvedAlbum.songs }.size
                                )
                            } else {
                                resolvedAlbum
                            }
                        }
                    } else null

                    if (finalAlbum != null) {
                        _selectedAlbum.value = finalAlbum
                        if (autoPlayIndex != null && finalAlbum.songs.isNotEmpty()) {
                            _activeAlbumContext.value = finalAlbum
                            playbackQueueManager.playAlbum(
                                finalAlbum,
                                startIndex = autoPlayIndex.coerceIn(0, finalAlbum.songs.size - 1),
                                isDataSaver = userSettings.value.isDataSaverEnabled
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Recordify/Album", "Error loading album details", e)
                } finally {
                    _isAlbumLoading.value = false
                }
            }
        } else if (autoPlayIndex != null) {
            _activeAlbumContext.value = album
            playbackQueueManager.playAlbum(
                album,
                startIndex = autoPlayIndex.coerceIn(0, album.songs.size - 1),
                isDataSaver = userSettings.value.isDataSaverEnabled
            )
        }
    }

    fun closeAlbumDetails() {
        _selectedAlbum.value = null
    }

    fun openAlbumByName(albumName: String?, artistName: String? = null, coverArtUrl: String? = null) {
        val cleanAlbum = albumName?.trim() ?: return
        if (cleanAlbum.isBlank()) return

        val primaryArtist = artistName?.split(",", "&", "feat.", "ft.", "/")?.firstOrNull()?.trim()

        val albumModel = com.audiovault.app.core.online.OnlineAlbumModel(
            id = "album_${cleanAlbum.hashCode()}",
            title = cleanAlbum,
            artist = primaryArtist ?: "Soundtrack",
            coverArtUrl = coverArtUrl,
            source = "online",
            type = "Album"
        )
        openAlbumDetails(albumModel)
    }

    fun playAlbumSoundtrack(album: com.audiovault.app.core.online.OnlineAlbumModel, startIndex: Int = 0) {
        if (album.songs.isEmpty()) {
            openAlbumDetails(album, autoPlayIndex = startIndex)
            return
        }
        _activeAlbumContext.value = album
        playbackQueueManager.playAlbum(album, startIndex = startIndex, isDataSaver = userSettings.value.isDataSaverEnabled)
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            userPreferencesRepository.clearRecentSearches()
        }
    }

    fun clearListeningHistoryAndReset() {
        viewModelScope.launch {
            userPreferencesRepository.clearRecentSearches()
            recommendationService.clearHistoryAndResetRecommendations()
            loadHomeContent(forceRefresh = true)
        }
    }

    fun setUserCountry(code: String, name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserCountry(code, name)
            loadHomeContent(forceRefresh = true)
        }
    }

    fun setMusicLanguages(primary: String, preferred: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setMusicLanguages(primary, preferred)
            loadHomeContent(forceRefresh = true)
        }
    }

    fun setFavoriteGenres(genres: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setFavoriteGenres(genres)
            loadHomeContent(forceRefresh = true)
        }
    }

    fun setFavoriteArtists(artists: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setFavoriteArtists(artists)
            loadHomeContent(forceRefresh = true)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(completed)
            loadHomeContent(forceRefresh = true)
        }
    }

    fun removeRecentSearch(id: String) {
        viewModelScope.launch {
            userPreferencesRepository.removeRecentSearchItem(id)
        }
    }

    fun playOnlineSong(
        song: OnlineSongModel,
        upcomingSongs: List<OnlineSongModel>? = null,
        isManualAction: Boolean = true,
        fromAlbumContext: Boolean = false
    ) {
        val current = playerState.value.currentRecording
        if (current != null && (current.filePath == song.streamUrl || current.title.equals(song.title, ignoreCase = true))) {
            playerEngine.togglePlayPause()
            return
        }

        if (!fromAlbumContext) {
            _activeAlbumContext.value = null
        }

        pendingSongPreviewJob?.cancel()
        pendingSongPreviewJob = null
        activeStreamResolutionJob?.cancel()
        val isDataSaver = userSettings.value.isDataSaverEnabled
        val isSkipSaverEnabled = userSettings.value.isSongPreviewSkipSaverEnabled

        val queueItem = QueueItem.fromOnlineSong(song)
        val upcomingQueueItems = upcomingSongs?.filter { it.id != song.id }?.map { QueueItem.fromOnlineSong(it) }
        val virtualRecording = queueItem.toRecordingEntity()

        // Persist every manually played song into Recent Searches / Recently Played
        viewModelScope.launch {
            userPreferencesRepository.addRecentSearchItem(
                com.audiovault.app.data.repository.RecentlyPlayedSearchItem(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    albumArtPath = song.highResArtUrl,
                    isOnline = true,
                    streamUrl = song.streamUrl,
                    youtubeVideoId = song.youtubeVideoId
                )
            )
        }

        // 0. OFFLINE-FIRST: Check if already downloaded locally on device (0 MB data consumed, instant play!)
        val localMatch = findMatchingLocalRecording(song)
        if (localMatch != null) {
            val localFile = java.io.File(localMatch.filePath)
            if (localFile.exists() && localFile.length() > 0L) {
                _isPendingSongPreview.value = false
                _pendingSongCountdown.value = 0
                playPauseRecording(localMatch, isManualAction = isManualAction)
                return
            }
        }

        // 1. SMART 10s SKIP PROTECTION: If enabled and triggered by autoplay/queue or user browsing
        if (isSkipSaverEnabled) {
            // Show song title & album art immediately on Player without loading stream over network!
            playerEngine.setPlaceholderPlaying(virtualRecording)
            _isPendingSongPreview.value = true
            _pendingSongCountdown.value = 10

            pendingSongPreviewJob = viewModelScope.launch {
                for (sec in 10 downTo 1) {
                    _pendingSongCountdown.value = sec
                    kotlinx.coroutines.delay(1000L)
                }
                _isPendingSongPreview.value = false
                _pendingSongCountdown.value = 0
                // Timer finished! Stream song audio now via queue manager
                playbackQueueManager.playTrackExplicitly(
                    item = queueItem,
                    newUpcoming = upcomingQueueItems,
                    isDataSaver = isDataSaver
                )
            }
            return
        }

        _isPendingSongPreview.value = false
        _pendingSongCountdown.value = 0
        playbackQueueManager.playTrackExplicitly(
            item = queueItem,
            newUpcoming = upcomingQueueItems,
            isDataSaver = isDataSaver
        )
    }

    fun playImmediatelyWithoutWaiting() {
        val currentRec = playerState.value.currentRecording ?: return
        if (_isPendingSongPreview.value) {
            pendingSongPreviewJob?.cancel()
            pendingSongPreviewJob = null
            _isPendingSongPreview.value = false
            _pendingSongCountdown.value = 0

            val item = QueueItem.fromRecording(currentRec)
            playbackQueueManager.playTrackExplicitly(item, isDataSaver = userSettings.value.isDataSaverEnabled)
        } else {
            playerEngine.togglePlayPause()
        }
    }

    private fun startActualOnlineStream(
        song: OnlineSongModel,
        virtualRecording: RecordingEntity,
        isManualAction: Boolean,
        isDataSaver: Boolean
    ) {
        val streamUrl = virtualRecording.filePath
        // Telemetry: Record PLAY_STARTED event now that playback genuinely begins
        viewModelScope.launch {
            recommendationService.recordEvent(
                com.audiovault.app.core.recommendation.ListeningEvent(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    albumArtPath = song.highResArtUrl,
                    streamUrl = song.streamUrl,
                    type = com.audiovault.app.core.recommendation.ListeningEventType.PLAY_STARTED,
                    durationPlayedMs = 0L,
                    songDurationMs = song.durationMs,
                    completionPercentage = 0f,
                    isOnline = true
                )
            )

            // When not in Album mode, asynchronously fill queue recommendations without blocking UI
            if (_activeAlbumContext.value == null) {
                val (cooldownIds, cooldownNormalized) = recommendationService.getRecentCooldownSongs()
                val userArtistPool = recommendationService.getUserArtistUniverse()
                val primaryArtist = song.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""
                if (primaryArtist.isNotBlank()) {
                    recentArtistsQueue.addLast(primaryArtist)
                    while (recentArtistsQueue.size > 5) {
                        recentArtistsQueue.removeFirst()
                    }
                }
                recommendationService.smartQueueEngine.generateInitialQueue(
                    seedSong = song,
                    profile = userProfile.value,
                    vibe = currentVibe.value.category,
                    recentPlayedSongIds = cooldownIds,
                    recentPlayedNormalizedKeys = cooldownNormalized,
                    recentPlayedArtists = recentArtistsQueue.toSet(),
                    userArtistPool = userArtistPool
                )
            }
        }

        // If direct stream URL is already present and valid, play immediately (<5ms)!
        if (streamUrl.isNotBlank() && !com.audiovault.app.core.online.OnlineMusicSearchService.isShortPreview(streamUrl)) {
            playerEngine.play(virtualRecording)
            if (isManualAction) {
                viewModelScope.launch {
                    userPreferencesRepository.addRecentSearchItem(
                        com.audiovault.app.data.repository.RecentlyPlayedSearchItem(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            albumArtPath = song.highResArtUrl,
                            isOnline = true,
                            streamUrl = streamUrl,
                            youtubeVideoId = song.youtubeVideoId
                        )
                    )
                }
            }
            return
        }

        // Show immediate Mini Player UI placeholder so user sees immediate feedback
        playerEngine.setPlaceholderPlaying(virtualRecording)

        // Resolve stream in background with rapid cancellation (allow YouTube only for explicit manual user searches)
        activeStreamResolutionJob = viewModelScope.launch {
            val resolvedUrl = com.audiovault.app.core.online.OnlineMusicSearchService.resolveFullAudioStream(
                song = song,
                isDataSaver = isDataSaver,
                allowYouTubeFallback = isManualAction
            )
            if (isActive && resolvedUrl.isNotBlank()) {
                val optimizedUrl = if (resolvedUrl.contains("_320.mp4")) resolvedUrl.replace("_320.mp4", "_96.mp4") else resolvedUrl
                playerEngine.play(virtualRecording.copy(filePath = optimizedUrl))
                if (isManualAction) {
                    userPreferencesRepository.addRecentSearchItem(
                        com.audiovault.app.data.repository.RecentlyPlayedSearchItem(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            albumArtPath = song.highResArtUrl,
                            isOnline = true,
                            streamUrl = optimizedUrl,
                            youtubeVideoId = song.youtubeVideoId
                        )
                    )
                }
            }
        }
    }

    fun playRecentSearchItem(item: com.audiovault.app.data.repository.RecentlyPlayedSearchItem) {
        if (item.isAlbum && !item.albumId.isNullOrBlank()) {
            openAlbumDetails(
                com.audiovault.app.core.online.OnlineAlbumModel(
                    id = item.albumId,
                    title = item.title,
                    artist = item.artist,
                    coverArtUrl = item.albumArtPath,
                    songCount = 0,
                    songs = emptyList()
                )
            )
            return
        }

        if (item.isOnline) {
            playOnlineSong(
                OnlineSongModel(
                    id = item.id,
                    title = item.title,
                    artist = item.artist,
                    streamUrl = item.streamUrl ?: "",
                    highResArtUrl = item.albumArtPath,
                    youtubeVideoId = item.youtubeVideoId
                )
            )
        } else {
            val local = allRecordings.value.firstOrNull { it.id == item.localRecordingId || it.title.equals(item.title, ignoreCase = true) }
            if (local != null) {
                playPauseRecording(local)
            }
        }
    }

    fun downloadOnlineSong(song: OnlineSongModel, onComplete: ((Boolean) -> Unit)? = null) {
        if (_downloadingSongIds.value.contains(song.id)) return

        _downloadingSongIds.update { it + song.id }
        val job = viewModelScope.launch {
            try {
                val success = onlineSongDownloader.downloadAndSave(song) { progress ->
                    _downloadProgress.update { it + (song.id to progress) }
                }
                if (success) {
                    val current = playerState.value.currentRecording
                    if (current != null && (current.title.equals(song.title, ignoreCase = true) || current.filePath == song.streamUrl)) {
                        val localMatch = allRecordings.value.firstOrNull { it.title.equals(song.title, ignoreCase = true) }
                        if (localMatch != null) {
                            playerEngine.updateCurrentRecording(localMatch)
                        }
                    }
                }
                onComplete?.invoke(success)
            } finally {
                _downloadingSongIds.update { it - song.id }
                _downloadProgress.update { it - song.id }
                downloadJobs.remove(song.id)
            }
        }
        downloadJobs[song.id] = job
    }

    fun downloadCurrentPlayingSong(onComplete: ((Boolean) -> Unit)? = null) {
        val currentRec = playerState.value.currentRecording ?: return
        if (!currentRec.filePath.startsWith("http")) return // Already a local file
        val songId = if (currentRec.id > 0) currentRec.id.toString() else currentRec.filePath.hashCode().toString()
        val song = OnlineSongModel(
            id = songId,
            title = currentRec.title,
            artist = currentRec.artist ?: "Unknown Artist",
            album = currentRec.album,
            durationMs = currentRec.durationMs,
            streamUrl = currentRec.filePath,
            highResArtUrl = currentRec.albumArtPath
        )
        downloadOnlineSong(song, onComplete)
    }

    fun cancelDownload(songId: String) {
        downloadJobs[songId]?.cancel()
        downloadJobs.remove(songId)
        _downloadingSongIds.update { it - songId }
        _downloadProgress.update { it - songId }
    }

    fun cancelCurrentPlayingDownload() {
        val currentRec = playerState.value.currentRecording ?: return
        val idStr = if (currentRec.id > 0) currentRec.id.toString() else currentRec.filePath.hashCode().toString()
        cancelDownload(idStr)
        cancelDownload(currentRec.filePath)
    }

    fun downloadFullAlbum(album: com.audiovault.app.core.online.OnlineAlbumModel) {
        if (albumDownloadJobs.containsKey(album.id)) {
            albumDownloadJobs[album.id]?.cancel()
            albumDownloadJobs.remove(album.id)
            _albumDownloadProgress.update { it - album.id }
            return
        }

        val job = viewModelScope.launch {
            try {
                _albumDownloadProgress.update { it + (album.id to 0.01f) }
                val tracks = if (album.songs.isNotEmpty()) {
                    album.songs
                } else {
                    val full = com.audiovault.app.core.online.OnlineMusicSearchService.fetchAlbumSoundtrack(album.id)
                    full?.songs ?: com.audiovault.app.core.online.OnlineMusicSearchService.searchSongs(album.title, limit = 15)
                }

                if (tracks.isEmpty()) {
                    _albumDownloadProgress.update { it - album.id }
                    return@launch
                }

                val total = tracks.size
                var completed = 0
                for (song in tracks) {
                    if (!isActive) break
                    if (!isOnlineSongSavedInLibrary(song)) {
                        onlineSongDownloader.downloadAndSave(song)
                    }
                    completed++
                    val prog = (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    _albumDownloadProgress.update { it + (album.id to prog) }
                }
            } catch (e: Exception) {
                android.util.Log.e("Recordify/Album", "Album download error", e)
            } finally {
                albumDownloadJobs.remove(album.id)
                _albumDownloadProgress.update { it - album.id }
            }
        }
        albumDownloadJobs[album.id] = job
    }

    fun isAlbumDownloading(albumId: String): Boolean = albumDownloadJobs.containsKey(albumId)

    fun findMatchingLocalRecording(song: OnlineSongModel): RecordingEntity? {
        val songTitle = song.title.trim()
        val songArtist = song.artist.trim()
        return allRecordings.value.firstOrNull { local ->
            !local.filePath.startsWith("http") && (
                local.title.equals(songTitle, ignoreCase = true) ||
                (local.title.contains(songTitle, ignoreCase = true) && !songArtist.isBlank() && (local.artist ?: "").contains(songArtist.take(8), ignoreCase = true))
            )
        }
    }

    fun isOnlineSongSavedInLibrary(song: OnlineSongModel): Boolean {
        return findMatchingLocalRecording(song) != null
    }

    fun isSongDownloaded(title: String, artist: String? = null): Boolean {
        val t = title.trim()
        return allRecordings.value.any { local ->
            !local.filePath.startsWith("http") && local.title.equals(t, ignoreCase = true)
        }
    }

    fun playPauseRecording(recording: RecordingEntity, isManualAction: Boolean = true) {
        val item = QueueItem.fromRecording(recording)
        if (playerState.value.currentRecording?.id == item.toRecordingEntity().id) {
            playerEngine.togglePlayPause()
        } else {
            playbackQueueManager.playTrackExplicitly(item, isDataSaver = userSettings.value.isDataSaverEnabled)
            if (isManualAction) {
                viewModelScope.launch {
                    userPreferencesRepository.addRecentSearchItem(
                        com.audiovault.app.data.repository.RecentlyPlayedSearchItem(
                            id = item.id,
                            title = recording.title,
                            artist = recording.artist ?: "Unknown Artist",
                            albumArtPath = recording.albumArtPath,
                            isOnline = item.isOnline,
                            localRecordingId = if (!item.isOnline) recording.id else null
                        )
                    )
                }
            }
        }
    }

    fun playNextTrack() {
        playbackQueueManager.skipToNext(isUserInitiated = true, isDataSaver = userSettings.value.isDataSaverEnabled)
    }

    fun playPreviousTrack() {
        playbackQueueManager.skipToPrevious(isDataSaver = userSettings.value.isDataSaverEnabled)
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

    fun deleteArtist(artistName: String) {
        // Safe removal: Never delete song files from device, only remove artist from manual & saved list
        playlistRepository.removeArtist(artistName)
    }

    fun addManualArtist(artistName: String) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return
        playlistRepository.addManualArtist(cleanName)

        // Automatically scan library for matching songs and fetch HD portrait in background
        viewModelScope.launch(Dispatchers.IO) {
            val matchingSong = recordings.value.firstOrNull { song ->
                val songArtist = song.artist ?: ""
                songArtist.contains(cleanName, ignoreCase = true) ||
                song.title.contains(cleanName, ignoreCase = true)
            }
            com.audiovault.app.core.intelligence.CoverArtFetcher.fetchAndCacheArtistArt(
                context = getApplication<Application>(),
                artistName = cleanName,
                songHint = matchingSong?.title
            )
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
