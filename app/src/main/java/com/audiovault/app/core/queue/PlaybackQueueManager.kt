package com.audiovault.app.core.queue

import android.content.Context
import android.util.Log
import com.audiovault.app.core.identity.TrackQualityFilter
import com.audiovault.app.core.identity.TrackVersionResolver
import com.audiovault.app.core.intelligence.TrackMatcher
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineMusicSearchService
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.core.player.PlayerPlaybackStatus
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.core.recommendation.ListeningEvent
import com.audiovault.app.core.recommendation.ListeningEventType
import com.audiovault.app.core.recommendation.RecommendationService
import com.audiovault.app.core.vibeflow.SongMoodProfile
import com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer
import com.audiovault.app.core.vibeflow.VibeFlowScorer
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.data.repository.UserPreferencesRepository
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.data.repository.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

/**
 * Single authoritative playback queue and track progression manager.
 * Thread-safe, non-blocking, and guarantees exactly-once track transitions.
 */
class PlaybackQueueManager(
    private val context: Context,
    val playerEngine: AudioPlayerEngine,
    val recommendationService: RecommendationService,
    val recordingRepository: RecordingRepository? = null
) {
    companion object {
        private const val TAG = "Recordify/QueueManager"
        private const val REFILL_THRESHOLD = 3
        private const val TARGET_QUEUE_SIZE = 12
        private const val MAX_CONSECUTIVE_FAILURES = 3
        private const val MIN_SKIP_PENALTY_DURATION_MS = 15000L // 15 seconds min before counting as negative skip
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private val transitionMutex = Mutex()
    private val userPreferencesRepository = UserPreferencesRepository(context)

    private val _queueState = MutableStateFlow(PlaybackQueueState())
    val queueState: StateFlow<PlaybackQueueState> = _queueState.asStateFlow()

    private val currentGenerationId = AtomicLong(1L)
    private var lastAdvanceTimestamp = 0L
    private var consecutiveFailureCount = 0
    private var activeReplenishJob: Job? = null

    @Volatile
    private var cachedPreferredLanguages: Set<String> = setOf("Hindi", "Gujarati")
    @Volatile
    private var cachedUserCountry: String = "IN"

    init {
        // Wire completion listener directly to queue manager
        playerEngine.onSongCompleted = { finishedRecording ->
            scope.launch {
                handleTrackCompletion(finishedRecording)
            }
        }

        playerEngine.onPlaybackError = { failedRec, errorMsg ->
            scope.launch {
                Log.w(TAG, "Playback error: $errorMsg for ${failedRec?.title}. Advancing safely.")
                skipToNext(isUserInitiated = false)
            }
        }

        // Cache user language and country preferences safely in the background
        try {
            scope.launch(Dispatchers.IO) {
                try {
                    userPreferencesRepository.userSettingsFlow.collect { settings ->
                        cachedPreferredLanguages = settings.preferredLanguages
                        cachedUserCountry = settings.userCountryCode
                    }
                } catch (_: Throwable) {
                    // Ignore DataStore errors in unit tests with mock contexts
                }
            }
        } catch (_: Throwable) {
            // Ignore in tests
        }
    }

    /**
     * Start playing a specific track immediately as a manual user action.
     * Replaces the current track and sets up a new VibeFlow queue context.
     */
    fun playTrackExplicitly(
        item: QueueItem,
        newUpcoming: List<QueueItem>? = null,
        isDataSaver: Boolean = false,
        mode: QueueMode = QueueMode.VIBE_RADIO
    ) {
        val genId = currentGenerationId.incrementAndGet()
        activeReplenishJob?.cancel()

        scope.launch {
            transitionMutex.withLock {
                consecutiveFailureCount = 0
                val prevTrack = _queueState.value.currentTrack
                val history = if (prevTrack != null && prevTrack.id != item.id) {
                    (_queueState.value.playbackHistory + prevTrack).takeLast(50)
                } else {
                    _queueState.value.playbackHistory
                }

                val updatedRecent = (listOf(item.canonicalTrackId) + _queueState.value.recentCanonicalIds).distinct().take(50)

                // Intelligent auto-queue continuity:
                // When playing a single track, immediately seed the upcoming queue matching
                // that exact era and vibe (old -> old classics, new -> new chartbusters, romantic -> romantic, etc.)
                // and strictly respecting user's preferred languages and country
                val resolvedUpcoming = newUpcoming ?: if (mode == QueueMode.VIBE_RADIO || mode == QueueMode.USER_QUEUE) {
                    SmartVibeQueueEngine.getInstantSeedQueue(item, cachedPreferredLanguages, cachedUserCountry)
                } else {
                    _queueState.value.upcomingQueue.filter { q -> q.id != item.id }
                }

                _queueState.update {
                    it.copy(
                        currentTrack = item,
                        upcomingQueue = resolvedUpcoming,
                        playbackHistory = history,
                        recentCanonicalIds = updatedRecent,
                        queueMode = mode,
                        queueGenerationId = genId,
                        activeVibeDescription = item.moodProfile?.moodLabels?.firstOrNull()?.displayName
                            ?: "Smart Flow • ${SmartVibeQueueEngine.detectVibeType(item.title, item.artist, item.album).name.replace('_', ' ')}"
                    )
                }

                // Record Manual Selection Listening Event
                recommendationService.recordEvent(
                    ListeningEvent(
                        songId = item.id,
                        title = item.title,
                        artist = item.artist,
                        album = item.album,
                        albumArtPath = item.albumArtPath,
                        streamUrl = item.streamUrl,
                        type = ListeningEventType.PLAY_STARTED,
                        durationPlayedMs = 0L,
                        songDurationMs = item.durationMs,
                        completionPercentage = 0f,
                        isOnline = item.isOnline
                    )
                )

                // Start playback
                executePlay(item, isDataSaver, isManual = true)

                // Auto-replenish if queue is low (NEVER in ALBUM mode while album is playing)
                if (mode != QueueMode.ALBUM && mode != QueueMode.SINGLE_TRACK &&
                    _queueState.value.isAutoQueueEnabled && (newUpcoming == null || newUpcoming.size < REFILL_THRESHOLD)) {
                    triggerVibeFlowQueueReplenish(seed = item, genId = genId)
                }
            }
        }
    }

    /**
     * Plays an entire album in strict track order (QueueMode.ALBUM).
     */
    fun playAlbum(album: OnlineAlbumModel, startIndex: Int = 0, isDataSaver: Boolean = false) {
        if (album.songs.isEmpty()) return
        val target = album.songs.getOrNull(startIndex) ?: album.songs.first()
        val queueItems = album.songs.map { QueueItem.fromOnlineSong(it, "Album • ${album.title}") }
        val targetIndex = album.songs.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
        val targetItem = queueItems[targetIndex]
        val remainingSongs = if (_queueState.value.isShuffleEnabled) {
            queueItems.filter { it.id != targetItem.id }.shuffled()
        } else {
            queueItems.drop(targetIndex + 1)
        }
        playTrackExplicitly(targetItem, remainingSongs, isDataSaver, mode = QueueMode.ALBUM)
    }

    /**
     * Handles normal completion when ExoPlayer reaches STATE_ENDED.
     * Strictly guaranteed to advance exactly once.
     */
    suspend fun handleTrackCompletion(finishedRecording: RecordingEntity) {
        val now = System.currentTimeMillis()
        if (now - lastAdvanceTimestamp < 800L) {
            Log.d(TAG, "Suppressed duplicate track completion event within 800ms debounce window.")
            return
        }

        transitionMutex.withLock {
            lastAdvanceTimestamp = System.currentTimeMillis()
            val state = _queueState.value
            val current = state.currentTrack

            // Record completed event
            recommendationService.recordEvent(
                ListeningEvent(
                    songId = finishedRecording.id.toString(),
                    title = finishedRecording.title,
                    artist = finishedRecording.artist ?: "Unknown Artist",
                    album = finishedRecording.album,
                    albumArtPath = finishedRecording.albumArtPath,
                    streamUrl = finishedRecording.filePath,
                    type = ListeningEventType.PLAY_COMPLETED,
                    durationPlayedMs = finishedRecording.durationMs,
                    songDurationMs = finishedRecording.durationMs,
                    completionPercentage = 1.0f,
                    isOnline = finishedRecording.sourcePackage == "com.audiovault.online"
                )
            )

            // 1. Repeat ONE mode
            if (state.repeatMode == PlaybackRepeatMode.ONE && current != null) {
                playerEngine.seekTo(0L)
                playerEngine.resume()
                return
            }

            advanceNextInternal(isUserInitiated = false)
        }
    }

    /**
     * Advances to the next track in queue.
     * Can be invoked manually (Skip Next) or automatically upon track end.
     */
    fun skipToNext(isUserInitiated: Boolean = true, isDataSaver: Boolean = false) {
        val now = System.currentTimeMillis()
        if (now - lastAdvanceTimestamp < 500L) {
            Log.d(TAG, "Debounced rapid SkipNext click.")
            return
        }
        lastAdvanceTimestamp = now

        scope.launch {
            transitionMutex.withLock {
                val current = _queueState.value.currentTrack
                val playedDuration = playerEngine.playerState.value.currentPositionMs

                // If user deliberately skipped after minimum listening duration, record negative feedback
                if (isUserInitiated && current != null && playedDuration >= MIN_SKIP_PENALTY_DURATION_MS) {
                    val frac = if (current.durationMs > 0) playedDuration.toFloat() / current.durationMs.toFloat() else 0.2f
                    recommendationService.recordEvent(
                        ListeningEvent(
                            songId = current.id,
                            title = current.title,
                            artist = current.artist,
                            album = current.album,
                            albumArtPath = current.albumArtPath,
                            streamUrl = current.streamUrl,
                            type = ListeningEventType.SONG_SKIPPED,
                            durationPlayedMs = playedDuration,
                            songDurationMs = current.durationMs,
                            completionPercentage = frac,
                            isOnline = current.isOnline
                        )
                    )
                }

                advanceNextInternal(isUserInitiated = isUserInitiated, isDataSaver = isDataSaver)
            }
        }
    }

    /**
     * Internal implementation of queue advancement. Must be called under transitionMutex.
     */
    private suspend fun advanceNextInternal(isUserInitiated: Boolean, isDataSaver: Boolean = false) {
        val state = _queueState.value
        val queue = state.upcomingQueue
        val history = state.playbackHistory
        val current = state.currentTrack

        if (queue.isNotEmpty()) {
            val nextTrack = queue.first()
            val remainingQueue = queue.drop(1)
            val updatedHistory = if (current != null) (history + current).takeLast(50) else history
            val updatedRecent = (listOf(nextTrack.canonicalTrackId) + state.recentCanonicalIds).distinct().take(50)

            _queueState.update {
                it.copy(
                    currentTrack = nextTrack,
                    upcomingQueue = remainingQueue,
                    playbackHistory = updatedHistory,
                    recentCanonicalIds = updatedRecent
                )
            }

            // Check if queue needs background replenishment (NEVER in ALBUM mode while album is playing!)
            if (state.queueMode != QueueMode.ALBUM && state.queueMode != QueueMode.SINGLE_TRACK &&
                state.isAutoQueueEnabled && remainingQueue.size <= REFILL_THRESHOLD) {
                triggerVibeFlowQueueReplenish(seed = nextTrack, genId = state.queueGenerationId)
            }

            executePlay(nextTrack, isDataSaver, isManual = isUserInitiated)
        } else {
            // Queue is empty! Check Repeat ALL mode
            if (state.repeatMode == PlaybackRepeatMode.ALL && (history.isNotEmpty() || current != null)) {
                val fullLoop = (history + listOfNotNull(current)).distinctBy { it.id }
                if (fullLoop.isNotEmpty()) {
                    val firstTrack = fullLoop.first()
                    val loopUpcoming = fullLoop.drop(1)
                    val updatedRecent = (listOf(firstTrack.canonicalTrackId) + state.recentCanonicalIds).distinct().take(50)
                    _queueState.update {
                        it.copy(
                            currentTrack = firstTrack,
                            upcomingQueue = loopUpcoming,
                            playbackHistory = emptyList(),
                            recentCanonicalIds = updatedRecent
                        )
                    }
                    executePlay(firstTrack, isDataSaver, isManual = false)
                    return
                }
            }

            // When album queue completes and auto-queue is enabled, transition to VibeFlow mood continuation!
            if (state.isAutoQueueEnabled && current != null) {
                val freshCandidates = generateVibeFlowCandidates(current)
                if (freshCandidates.isNotEmpty()) {
                    val nextTrack = freshCandidates.first()
                    val remaining = freshCandidates.drop(1)
                    val updatedRecent = (listOf(nextTrack.canonicalTrackId) + state.recentCanonicalIds).distinct().take(50)
                    _queueState.update {
                        it.copy(
                            currentTrack = nextTrack,
                            upcomingQueue = remaining,
                            playbackHistory = if (current != null) (history + current).takeLast(50) else history,
                            recentCanonicalIds = updatedRecent,
                            queueMode = QueueMode.VIBE_RADIO
                        )
                    }
                    executePlay(nextTrack, isDataSaver, isManual = false)
                    return
                }
            }

            // No tracks available, end playback gracefully without crashes
            Log.d(TAG, "Queue exhausted. Playback stopped cleanly.")
            _queueState.update { it.copy(currentTrack = null) }
            playerEngine.stopAndClose()
        }
    }

    /**
     * Navigates to the previous track in history, or restarts the current track if >3s.
     */
    fun skipToPrevious(isDataSaver: Boolean = false) {
        scope.launch {
            transitionMutex.withLock {
                val currentPos = playerEngine.playerState.value.currentPositionMs
                if (currentPos > 3000L) {
                    playerEngine.seekTo(0L)
                    playerEngine.resume()
                    return@withLock
                }

                val history = _queueState.value.playbackHistory
                val current = _queueState.value.currentTrack

                if (history.isNotEmpty()) {
                    val prevTrack = history.last()
                    val newHistory = history.dropLast(1)
                    val newUpcoming = if (current != null) listOf(current) + _queueState.value.upcomingQueue else _queueState.value.upcomingQueue

                    _queueState.update {
                        it.copy(
                            currentTrack = prevTrack,
                            upcomingQueue = newUpcoming,
                            playbackHistory = newHistory
                        )
                    }

                    executePlay(prevTrack, isDataSaver, isManual = true)
                } else {
                    playerEngine.seekTo(0L)
                }
            }
        }
    }

    /**
     * Checks if this track has been downloaded locally on device for offline-first instant playback.
     */
    private suspend fun findMatchingDownloadedRecording(title: String, artist: String): RecordingEntity? {
        val repo = recordingRepository ?: (context.applicationContext as? AudioVaultApplication)?.container?.recordingRepository ?: return null
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        return try {
            val allLocal = repo.getAllRecordings()
            allLocal.firstOrNull { local ->
                !local.filePath.startsWith("http://") && !local.filePath.startsWith("https://") && (
                    local.title.equals(cleanTitle, ignoreCase = true) ||
                    (local.title.contains(cleanTitle, ignoreCase = true) && cleanArtist.isNotBlank() && (local.artist ?: "").contains(cleanArtist.take(8), ignoreCase = true)) ||
                    (cleanTitle.contains(local.title, ignoreCase = true) && cleanArtist.isNotBlank() && (local.artist ?: "").contains(cleanArtist.take(8), ignoreCase = true))
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking downloaded recordings for '$title'", e)
            null
        }
    }

    /**
     * Resolves the playable stream URL before starting playback and executes smoothly.
     * If stream resolution fails, automatically skips to the next track safely (max 3 retries).
     */
    private suspend fun executePlay(item: QueueItem, isDataSaver: Boolean, isManual: Boolean) {
        val virtualRecording = item.toRecordingEntity()

        // 0. OFFLINE-FIRST: If item is a direct local file path on disk, play immediately
        if (!item.isOnline && item.streamUrl.isNotBlank() && !item.streamUrl.startsWith("http://") && !item.streamUrl.startsWith("https://")) {
            val localFile = java.io.File(item.streamUrl)
            if (localFile.exists() && localFile.length() > 0L) {
                Log.i(TAG, "Playing direct local track for '${item.title}': ${item.streamUrl}")
                playerEngine.play(virtualRecording)
                consecutiveFailureCount = 0
                return
            }
        }

        // 0.1 OFFLINE-FIRST: Check if track is downloaded locally on device
        val downloadedMatch = findMatchingDownloadedRecording(item.title, item.artist)
        if (downloadedMatch != null) {
            val localFile = java.io.File(downloadedMatch.filePath)
            if (localFile.exists() && localFile.length() > 0L) {
                Log.i(TAG, "Playing local downloaded copy for '${item.title}': ${downloadedMatch.filePath}")
                playerEngine.play(downloadedMatch)
                consecutiveFailureCount = 0
                return
            }
        }

        // 1. Direct playable stream
        if (item.streamUrl.isNotBlank() && !OnlineMusicSearchService.isShortPreview(item.streamUrl)) {
            val resolvedUrl = if (isDataSaver && item.streamUrl.contains("_320.mp4")) {
                item.streamUrl.replace("_320.mp4", "_96.mp4")
            } else {
                item.streamUrl
            }
            playerEngine.play(virtualRecording.copy(filePath = resolvedUrl))
            consecutiveFailureCount = 0
            return
        }

        // 2. Stream needs asynchronous resolution
        _queueState.update { it.copy(isResolvingStream = true) }
        playerEngine.setPlaceholderPlaying(virtualRecording)

        val resolvedUrl = withContext(Dispatchers.IO) {
            try {
                OnlineMusicSearchService.resolveFullAudioStream(
                    song = item.toOnlineSongModel(),
                    isDataSaver = isDataSaver,
                    allowYouTubeFallback = isManual
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving full stream for ${item.title}", e)
                ""
            }
        }

        _queueState.update { it.copy(isResolvingStream = false) }

        if (resolvedUrl.isNotBlank()) {
            consecutiveFailureCount = 0
            val finalUrl = if (isDataSaver && resolvedUrl.contains("_320.mp4")) resolvedUrl.replace("_320.mp4", "_96.mp4") else resolvedUrl
            playerEngine.play(virtualRecording.copy(filePath = finalUrl))
        } else {
            consecutiveFailureCount++
            Log.w(TAG, "Stream resolution failed for ${item.title}. Consecutive failures: $consecutiveFailureCount")

            if (consecutiveFailureCount < MAX_CONSECUTIVE_FAILURES) {
                // Auto-advance to next track in queue on dead stream
                advanceNextInternal(isUserInitiated = false, isDataSaver = isDataSaver)
            } else {
                Log.e(TAG, "Reached max stream failures ($MAX_CONSECUTIVE_FAILURES). Halting queue advancement.")
                playerEngine.stopAndClose()
            }
        }
    }

    /**
     * Background replenishment of the VibeFlow queue.
     */
    private fun triggerVibeFlowQueueReplenish(seed: QueueItem, genId: Long) {
        activeReplenishJob?.cancel()
        activeReplenishJob = scope.launch(Dispatchers.IO) {
            val newCandidates = generateVibeFlowCandidates(seed)
            if (currentGenerationId.get() != genId) return@launch // Stale generation discarded

            transitionMutex.withLock {
                if (currentGenerationId.get() == genId && newCandidates.isNotEmpty()) {
                    val existing = _queueState.value.upcomingQueue
                    val existingKeys = existing.map { it.id }.toSet()
                    val filtered = newCandidates.filter { !existingKeys.contains(it.id) && it.id != seed.id }

                    _queueState.update {
                        it.copy(upcomingQueue = it.upcomingQueue + filtered)
                    }
                }
            }
        }
    }

    private suspend fun generateVibeFlowCandidates(seed: QueueItem): List<QueueItem> = withContext(Dispatchers.IO) {
        try {
            val profile = recommendationService.userProfile.value
            val currentVibe = recommendationService.currentVibe.value.category
            val (cooldownIds, cooldownNormalized) = recommendationService.getRecentCooldownSongs()

            val preferredLangs = cachedPreferredLanguages
            val primaryLang = if (preferredLangs.isNotEmpty()) preferredLangs.first() else "Hindi"
            val isEnglishAllowed = preferredLangs.any { it.equals("English", ignoreCase = true) }

            // Target language lock: if seed is Hindi -> Hindi only; if Gujarati -> Gujarati only
            val seedLang = seed.language
                ?: seed.moodProfile?.language
                ?: VibeFlowMoodAnalyzer.inferLanguage("${seed.title} ${seed.artist}")
                ?: primaryLang
            val targetLanguage = seedLang.ifBlank { primaryLang }

            val seedArtist = seed.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""
            // Intelligently generate queries tailored to exact era (old vs new) and style (romantic, party, lofi, etc.)
            val queryKeywords = SmartVibeQueueEngine.generateIntelligentSearchQueries(seed, targetLanguage)

            val rawCandidates = mutableListOf<OnlineSongModel>()
            for (query in queryKeywords) {
                val songs = OnlineMusicSearchService.searchDirectCdnSongs(query, limit = 8)
                rawCandidates.addAll(songs)
            }

            // Filter out remixes, edits, slowed/sped-up, and non-original uploads
            val canonicalCandidates = TrackQualityFilter.filterAndDeduplicate(rawCandidates, isOriginalOnly = true)

            // Strict anti-repeat, strict language isolation, and strict era consistency:
            // 1. Exclude the seed song and all its alternate uploads/remixes
            // 2. Exclude recent 50 canonical songs
            // 3. Exclude songs already in upcoming queue
            // 4. Strict language lock (Hindi -> Hindi only, Gujarati -> Gujarati only)
            // 5. Hard exclusion of English if user did not select English in preferences
            // 6. Strict ERA consistency: Old songs only queue old/retro tracks; New songs only queue modern/trending tracks!
            // 7. Strict TITLE DEDUPLICATION: catches songs uploaded under composer vs lyricist or duplicate recordings
            val seedCanonical = seed.canonicalTrackId
            val recentCanonical = _queueState.value.recentCanonicalIds.toSet()
            val upcomingCanonical = _queueState.value.upcomingQueue.map { it.canonicalTrackId }.toSet()

            val seenNormalizedTitles = mutableSetOf<String>()
            val seedNormTitle = TrackMatcher.normalize(seed.title)
            if (seedNormTitle.isNotBlank()) seenNormalizedTitles.add(seedNormTitle)
            _queueState.value.upcomingQueue.forEach { item ->
                val t = TrackMatcher.normalize(item.title)
                if (t.isNotBlank()) seenNormalizedTitles.add(t)
            }

            val filtered = canonicalCandidates.filter { cand ->
                if (cand.canonicalTrackId == seedCanonical ||
                    recentCanonical.contains(cand.canonicalTrackId) ||
                    upcomingCanonical.contains(cand.canonicalTrackId) ||
                    cooldownIds.contains(cand.providerTrackId)) {
                    return@filter false
                }

                // Strict title root deduplication (e.g. Apna Bana Le or Gehra Hua across different artist credits)
                val candNormTitle = TrackMatcher.normalize(cand.normalizedTitle)
                if (candNormTitle.isBlank()) return@filter false
                val isDuplicateTitle = seenNormalizedTitles.any { existing ->
                    existing == candNormTitle ||
                    (existing.length >= 4 && candNormTitle.length >= 4 && (
                        existing.contains(candNormTitle) ||
                        candNormTitle.contains(existing) ||
                        TrackMatcher.similarity(existing, candNormTitle) >= 0.80
                    ))
                }
                if (isDuplicateTitle) {
                    return@filter false
                }

                // Rule A: Strict ERA consistency
                if (!SmartVibeQueueEngine.isCandidateEraCompatible(seed, cand.normalizedTitle, cand.normalizedPrimaryArtist, cand.albumTitle)) {
                    return@filter false
                }

                val candLang = cand.language
                    ?: VibeFlowMoodAnalyzer.inferLanguage("${cand.normalizedTitle} ${cand.normalizedPrimaryArtist}")

                // Rule B: If English is not in user's preferences, strictly exclude English tracks and Western artists
                if (!isEnglishAllowed) {
                    if (candLang?.equals("English", ignoreCase = true) == true ||
                        cand.language?.equals("english", ignoreCase = true) == true ||
                        VibeFlowMoodAnalyzer.isEnglishArtistOrTitle(cand.normalizedTitle, cand.normalizedPrimaryArtist)) {
                        return@filter false
                    }
                }

                // Rule C: Strictly enforce language connection
                if (candLang != null) {
                    if (!candLang.equals(targetLanguage, ignoreCase = true) && !preferredLangs.any { it.equals(candLang, ignoreCase = true) }) {
                        return@filter false
                    }
                } else {
                    if (!isEnglishAllowed && VibeFlowMoodAnalyzer.isEnglishArtistOrTitle(cand.normalizedTitle, cand.normalizedPrimaryArtist)) {
                        return@filter false
                    }
                }

                true
            }

            val seedMood = seed.moodProfile ?: VibeFlowMoodAnalyzer.analyze(seed.toRecordingEntity())

            // Score and rank candidates using VibeFlowScorer
            val scored = filtered.map { candidate ->
                val candMood = VibeFlowMoodAnalyzer.analyzeFromMetadata(
                    trackId = candidate.providerTrackId,
                    title = candidate.normalizedTitle,
                    artist = candidate.normalizedPrimaryArtist,
                    album = candidate.albumTitle,
                    lyrics = candidate.lyrics,
                    year = candidate.releaseYear?.toString(),
                    durationMs = candidate.durationMs
                )
                val scoreResult = VibeFlowScorer.scoreCandidate(
                    candidate = candidate.toOnlineSongModel(),
                    candidateProfile = candMood,
                    seedProfile = seedMood,
                    userProfile = profile,
                    currentVibe = currentVibe,
                    recentPlayedSongIds = cooldownIds,
                    recentPlayedNormalizedKeys = cooldownNormalized
                )
                Pair(QueueItem.fromCanonicalTrack(candidate, scoreResult.explanation), scoreResult.score)
            }.sortedByDescending { it.second }

            // Apply Spotify-style diversity rules:
            // 1. Title root uniqueness across chosen candidates (strictly NO duplicates)
            // 2. Max 2 tracks per primary artist in the 12-track queue
            // 3. ZERO consecutive tracks by the same artist
            val diversified = mutableListOf<QueueItem>()
            val artistTrackCount = mutableMapOf<String, Int>()
            var lastArtist = seedArtist.lowercase().trim()

            for ((cand, _) in scored) {
                val candNormTitle = TrackMatcher.normalize(cand.title)
                if (seenNormalizedTitles.any { existing ->
                    existing == candNormTitle ||
                    (existing.length >= 4 && candNormTitle.length >= 4 && (
                        existing.contains(candNormTitle) ||
                        candNormTitle.contains(existing) ||
                        TrackMatcher.similarity(existing, candNormTitle) >= 0.80
                    ))
                }) {
                    continue
                }

                val primaryCandArtist = cand.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""
                val lowerCandArtist = primaryCandArtist.lowercase()

                // Rule A: Zero consecutive tracks by the same artist
                if (lowerCandArtist.isNotBlank() && lowerCandArtist == lastArtist) {
                    continue
                }

                // Rule B: Max 2 tracks per artist across the 12-track queue
                val currentCount = artistTrackCount.getOrDefault(lowerCandArtist, 0)
                if (currentCount >= 2 && lowerCandArtist.isNotBlank()) {
                    continue
                }

                diversified.add(cand)
                seenNormalizedTitles.add(candNormTitle)
                if (lowerCandArtist.isNotBlank()) {
                    artistTrackCount[lowerCandArtist] = currentCount + 1
                    lastArtist = lowerCandArtist
                }

                if (diversified.size >= TARGET_QUEUE_SIZE) break
            }

            // Fallback pass if queue size is under target without violating title uniqueness
            if (diversified.size < TARGET_QUEUE_SIZE) {
                for ((cand, _) in scored) {
                    val candNormTitle = TrackMatcher.normalize(cand.title)
                    if (seenNormalizedTitles.any { existing ->
                        existing == candNormTitle ||
                        (existing.length >= 4 && candNormTitle.length >= 4 && (
                            existing.contains(candNormTitle) ||
                            candNormTitle.contains(existing) ||
                            TrackMatcher.similarity(existing, candNormTitle) >= 0.80
                        ))
                    }) {
                        continue
                    }
                    val primaryCandArtist = cand.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""
                    val lowerCandArtist = primaryCandArtist.lowercase()
                    if (lowerCandArtist.isNotBlank() && lowerCandArtist == lastArtist) continue

                    diversified.add(cand)
                    seenNormalizedTitles.add(candNormTitle)
                    if (lowerCandArtist.isNotBlank()) lastArtist = lowerCandArtist
                    if (diversified.size >= TARGET_QUEUE_SIZE) break
                }
            }

            diversified
        } catch (e: Exception) {
            Log.e(TAG, "Error generating VibeFlow candidates", e)
            emptyList()
        }
    }

    // Queue mutations
    fun addToQueue(item: QueueItem) {
        val current = _queueState.value.upcomingQueue.toMutableList()
        val userItem = item.copy(isUserQueued = true)
        // First swiped song inserts right after current song;
        // Subsequent swiped songs stack directly underneath existing user-queued songs,
        // and all user-queued songs sit ahead of album/playlist context tracks.
        val insertIndex = current.indexOfFirst { !it.isUserQueued }.let { if (it == -1) current.size else it }
        current.add(insertIndex, userItem)
        _queueState.update { it.copy(upcomingQueue = current) }
    }

    fun promoteToUserQueue(index: Int) {
        val current = _queueState.value.upcomingQueue.toMutableList()
        if (index in current.indices) {
            val item = current.removeAt(index).copy(isUserQueued = true)
            val insertIndex = current.indexOfFirst { !it.isUserQueued }.let { if (it == -1) current.size else it }
            current.add(insertIndex, item)
            _queueState.update { it.copy(upcomingQueue = current) }
        }
    }

    fun clearUserQueue() {
        val current = _queueState.value.upcomingQueue.filter { !it.isUserQueued }
        _queueState.update { it.copy(upcomingQueue = current) }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val current = _queueState.value.upcomingQueue.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            var item = current.removeAt(fromIndex)
            val userQueueEndIndex = current.indexOfFirst { !it.isUserQueued }.let { if (it == -1) current.size else it }
            if (toIndex <= userQueueEndIndex) {
                item = item.copy(isUserQueued = true)
            }
            current.add(toIndex, item)
            _queueState.update { it.copy(upcomingQueue = current) }
        }
    }

    fun removeAt(index: Int) {
        val current = _queueState.value.upcomingQueue.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _queueState.update { it.copy(upcomingQueue = current) }
        }
    }

    fun playImmediatelyFromQueue(index: Int, isDataSaver: Boolean = false) {
        val current = _queueState.value.upcomingQueue.toMutableList()
        if (index in current.indices) {
            val selected = current.removeAt(index)
            playTrackExplicitly(selected, current, isDataSaver)
        }
    }

    fun clearUpcoming() {
        _queueState.update { it.copy(upcomingQueue = emptyList()) }
    }

    fun setRepeatMode(mode: PlaybackRepeatMode) {
        _queueState.update { it.copy(repeatMode = mode) }
    }

    fun toggleRepeatMode(): PlaybackRepeatMode {
        val nextMode = when (_queueState.value.repeatMode) {
            PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
            PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
            PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
        }
        _queueState.update { it.copy(repeatMode = nextMode) }
        return nextMode
    }

    fun toggleShuffle(): Boolean {
        val next = !_queueState.value.isShuffleEnabled
        val upcoming = _queueState.value.upcomingQueue
        val newQueue = if (next) upcoming.shuffled() else upcoming
        _queueState.update { it.copy(isShuffleEnabled = next, upcomingQueue = newQueue) }
        return next
    }

    fun toggleAutoQueue(): Boolean {
        val next = !_queueState.value.isAutoQueueEnabled
        _queueState.update { it.copy(isAutoQueueEnabled = next) }
        return next
    }

    fun togglePersonalization(): Boolean {
        val next = !_queueState.value.isPersonalizationEnabled
        _queueState.update { it.copy(isPersonalizationEnabled = next) }
        return next
    }

    fun refreshVibeFlowQueue() {
        val current = _queueState.value.currentTrack ?: return
        val genId = currentGenerationId.incrementAndGet()
        triggerVibeFlowQueueReplenish(seed = current, genId = genId)
    }
}
