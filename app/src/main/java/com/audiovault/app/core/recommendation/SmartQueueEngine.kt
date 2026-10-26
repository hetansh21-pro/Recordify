package com.audiovault.app.core.recommendation

import com.audiovault.app.core.online.OnlineMusicSearchService
import com.audiovault.app.core.online.OnlineSongModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SmartQueueEngine(
    private val config: RecommendationConfig = RecommendationConfig()
) {
    private val _queue = MutableStateFlow<List<OnlineSongModel>>(emptyList())
    val queue: StateFlow<List<OnlineSongModel>> = _queue.asStateFlow()

    private val mutex = Mutex()
    private var isGenerating = false

    fun setQueue(newQueue: List<OnlineSongModel>) {
        _queue.value = newQueue
    }

    fun popNext(): OnlineSongModel? {
        val current = _queue.value
        if (current.isEmpty()) return null
        val next = current.first()
        _queue.value = current.drop(1)
        return next
    }

    fun removeAt(index: Int) {
        val current = _queue.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _queue.value = current
        }
    }

    fun clear() {
        _queue.value = emptyList()
    }

    suspend fun generateInitialQueue(
        seedSong: OnlineSongModel,
        profile: UserMusicProfile,
        vibe: VibeCategory = VibeCategory.CHILL,
        recentPlayedSongIds: Set<String> = emptySet(),
        recentPlayedNormalizedKeys: Set<String> = emptySet(),
        recentPlayedArtists: Set<String> = emptySet(),
        userArtistPool: List<String> = emptyList(),
        targetSize: Int = 12
    ): List<OnlineSongModel> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val generated = buildSmartQueue(
                seedSong = seedSong,
                profile = profile,
                vibe = vibe,
                targetCount = targetSize,
                existingQueue = emptyList(),
                recentPlayedSongIds = recentPlayedSongIds,
                recentPlayedNormalizedKeys = recentPlayedNormalizedKeys,
                recentPlayedArtists = recentPlayedArtists,
                userArtistPool = userArtistPool
            )
            _queue.value = generated
            generated
        }
    }

    suspend fun checkAndRefillQueue(
        currentSong: OnlineSongModel,
        profile: UserMusicProfile,
        vibe: VibeCategory = VibeCategory.CHILL,
        recentPlayedSongIds: Set<String> = emptySet(),
        recentPlayedNormalizedKeys: Set<String> = emptySet(),
        recentPlayedArtists: Set<String> = emptySet(),
        userArtistPool: List<String> = emptyList()
    ) = withContext(Dispatchers.IO) {
        if (_queue.value.size <= config.queueRefillThreshold && !isGenerating) {
            mutex.withLock {
                if (_queue.value.size <= config.queueRefillThreshold && !isGenerating) {
                    try {
                        isGenerating = true
                        val newSongs = buildSmartQueue(
                            seedSong = currentSong,
                            profile = profile,
                            vibe = vibe,
                            targetCount = config.queueBatchSize,
                            existingQueue = _queue.value,
                            recentPlayedSongIds = recentPlayedSongIds,
                            recentPlayedNormalizedKeys = recentPlayedNormalizedKeys,
                            recentPlayedArtists = recentPlayedArtists,
                            userArtistPool = userArtistPool
                        )
                        if (newSongs.isNotEmpty()) {
                            _queue.update { it + newSongs }
                        }
                    } finally {
                        isGenerating = false
                    }
                }
            }
        }
    }

    private suspend fun buildSmartQueue(
        seedSong: OnlineSongModel,
        profile: UserMusicProfile,
        vibe: VibeCategory,
        targetCount: Int,
        existingQueue: List<OnlineSongModel>,
        recentPlayedSongIds: Set<String> = emptySet(),
        recentPlayedNormalizedKeys: Set<String> = emptySet(),
        recentPlayedArtists: Set<String> = emptySet(),
        userArtistPool: List<String> = emptyList()
    ): List<OnlineSongModel> = coroutineScope {
        val similarCount = (targetCount * config.similarRatio).toInt().coerceAtLeast(2)
        val favoritesCount = (targetCount * config.favoritesRatio).toInt().coerceAtLeast(1)
        val discoveryCount = (targetCount * config.discoveryRatio).toInt().coerceAtLeast(1)
        val trendingCount = (targetCount * config.trendingRatio).toInt().coerceAtLeast(1)

        val excludeIds = existingQueue.map { it.id }.toMutableSet()
        excludeIds.add(seedSong.id)

        val seedArtist = seedSong.artist.split(",", "&", "feat.", "/", "ft.").firstOrNull()?.trim() ?: ""
        val pool = (userArtistPool.filter { !it.equals(seedArtist, ignoreCase = true) } +
                    profile.topArtists.keys.filter { !it.equals(seedArtist, ignoreCase = true) })
            .distinct()
            .shuffled()

        val artist1 = pool.getOrNull(0) ?: "Arijit Singh"
        val artist2 = pool.getOrNull(1) ?: "Pritam"
        val artist3 = pool.getOrNull(2) ?: "Shreya Ghoshal"

        // 1. Diverse Artist #1 from user's history/data (strictly different from current song)
        val candidate1Deferred = async {
            try {
                OnlineMusicSearchService.searchDirectCdnSongs(artist1, limit = 8)
            } catch (_: Exception) {
                emptyList<OnlineSongModel>()
            }
        }

        // 2. Diverse Artist #2 from user's history/data
        val candidate2Deferred = async {
            try {
                OnlineMusicSearchService.searchDirectCdnSongs(artist2, limit = 8)
            } catch (_: Exception) {
                emptyList<OnlineSongModel>()
            }
        }

        // 3. Diverse Artist #3 from user's history/data
        val candidate3Deferred = async {
            try {
                OnlineMusicSearchService.searchDirectCdnSongs(artist3, limit = 8)
            } catch (_: Exception) {
                emptyList<OnlineSongModel>()
            }
        }

        // 4. Vibe & Discovery
        val vibeDeferred = async {
            try {
                OnlineMusicSearchService.searchDirectCdnSongs(vibe.searchKeyword, limit = 10)
            } catch (_: Exception) {
                emptyList<OnlineSongModel>()
            }
        }

        val c1 = candidate1Deferred.await()
        val c2 = candidate2Deferred.await()
        val c3 = candidate3Deferred.await()
        val cVibe = vibeDeferred.await()

        val activeRecentArtists = (recentPlayedArtists + setOf(seedArtist)).toSet()

        // Filter each bucket with anti-repeat
        val f1 = DiversityFilter.filterAndDiversify(
            candidates = c1,
            excludeIds = excludeIds,
            currentPlayingSongId = seedSong.id,
            recentPlayedSongIds = recentPlayedSongIds,
            recentPlayedNormalizedKeys = recentPlayedNormalizedKeys,
            recentPlayedArtists = activeRecentArtists,
            config = config
        ).take(3)

        val f2 = DiversityFilter.filterAndDiversify(
            candidates = c2,
            excludeIds = excludeIds + f1.map { it.id }.toSet(),
            currentPlayingSongId = seedSong.id,
            recentPlayedSongIds = recentPlayedSongIds,
            recentPlayedNormalizedKeys = recentPlayedNormalizedKeys,
            recentPlayedArtists = activeRecentArtists,
            config = config
        ).take(3)

        val f3 = DiversityFilter.filterAndDiversify(
            candidates = c3,
            excludeIds = excludeIds + (f1 + f2).map { it.id }.toSet(),
            currentPlayingSongId = seedSong.id,
            recentPlayedSongIds = recentPlayedSongIds,
            recentPlayedNormalizedKeys = recentPlayedNormalizedKeys,
            recentPlayedArtists = activeRecentArtists,
            config = config
        ).take(3)

        val fVibe = DiversityFilter.filterAndDiversify(
            candidates = cVibe,
            excludeIds = excludeIds + (f1 + f2 + f3).map { it.id }.toSet(),
            currentPlayingSongId = seedSong.id,
            recentPlayedSongIds = recentPlayedSongIds,
            recentPlayedNormalizedKeys = recentPlayedNormalizedKeys,
            recentPlayedArtists = activeRecentArtists,
            config = config
        ).take(3)

        // Round-robin interleave to strictly alternate artists on every consecutive song
        val interleaved = mutableListOf<OnlineSongModel>()
        val buckets = listOf(f1, f2, f3, fVibe).filter { it.isNotEmpty() }
        val maxLen = buckets.maxOfOrNull { it.size } ?: 0
        for (i in 0 until maxLen) {
            for (bucket in buckets) {
                if (i < bucket.size) {
                    interleaved.add(bucket[i])
                }
            }
        }

        DiversityFilter.filterAndDiversify(
            candidates = interleaved,
            excludeIds = emptySet(),
            currentPlayingSongId = seedSong.id,
            recentPlayedSongIds = recentPlayedSongIds,
            recentPlayedNormalizedKeys = recentPlayedNormalizedKeys,
            recentPlayedArtists = activeRecentArtists,
            config = config
        ).take(targetCount)
    }
}
