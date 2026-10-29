package com.audiovault.app.core.recommendation

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.audiovault.app.core.catalog.CountryMusicCatalog
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineMusicSearchService
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.data.local.dao.ListeningHistoryDao
import com.audiovault.app.data.local.entity.ListeningHistoryEntity
import com.audiovault.app.data.repository.PlaylistRepository
import com.audiovault.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

data class HomeRecommendationFeed(
    val userVibe: UserVibe,
    val quickTiles: List<OnlineSongModel> = emptyList(),
    val heavyRotationSongs: List<OnlineSongModel> = emptyList(),
    val dailyMix1Title: String? = null,
    val dailyMix1Subtitle: String? = null,
    val dailyMix1Songs: List<OnlineSongModel> = emptyList(),
    val dailyMix2Title: String? = null,
    val dailyMix2Subtitle: String? = null,
    val dailyMix2Songs: List<OnlineSongModel> = emptyList(),
    val topArtists: List<com.audiovault.app.core.online.ArtistItemModel> = emptyList(),
    val topAlbums: List<OnlineAlbumModel> = emptyList(),
    val madeForYou: List<OnlineSongModel> = emptyList(),
    val becauseYouListenedToTitle: String? = null,
    val becauseYouListenedTo: List<OnlineSongModel> = emptyList(),
    val vibeRecommendations: List<OnlineSongModel> = emptyList(),
    val freshForYou: List<OnlineSongModel> = emptyList(),
    val trendingNow: List<OnlineSongModel> = emptyList(),
    val topArtistName: String? = null,
    val moreFromTopArtist: List<OnlineSongModel> = emptyList(),
    val isColdStart: Boolean = false,
    val userCountryName: String = "India",
    val userCountryCode: String = "IN",
    val primaryLanguage: String = "Hindi",
    val trendingInCountryTitle: String? = null,
    val trendingInCountrySongs: List<OnlineSongModel> = emptyList(),
    val languageMixTitle: String? = null,
    val languageMixSubtitle: String? = null,
    val languageMixSongs: List<OnlineSongModel> = emptyList(),
    val chillMixSongs: List<OnlineSongModel> = emptyList(),
    val romanticMixSongs: List<OnlineSongModel> = emptyList(),
    val newReleases: List<OnlineSongModel> = emptyList()
)

class RecommendationService(
    private val context: Context,
    private val listeningHistoryDao: ListeningHistoryDao,
    private val playlistRepository: PlaylistRepository,
    val smartQueueEngine: SmartQueueEngine = SmartQueueEngine(),
    val config: RecommendationConfig = RecommendationConfig()
) {
    companion object {
        private const val TAG = "Recordify/Recommendation"
    }

    private var currentSessionId: String = UUID.randomUUID().toString()

    private val _userProfile = MutableStateFlow(UserMusicProfile())
    val userProfile: StateFlow<UserMusicProfile> = _userProfile.asStateFlow()

    private val _currentVibe = MutableStateFlow(VibeDetector.detectCurrentVibe(emptyList()))
    val currentVibe: StateFlow<UserVibe> = _currentVibe.asStateFlow()

    private val _homeFeed = MutableStateFlow(HomeRecommendationFeed(userVibe = _currentVibe.value))
    val homeFeed: StateFlow<HomeRecommendationFeed> = _homeFeed.asStateFlow()

    // Memory cache for sub-100ms instant loading
    private val feedCache = LruCache<String, HomeRecommendationFeed>(10)

    suspend fun recordEvent(event: ListeningEvent) = withContext(Dispatchers.IO) {
        val weight = when (event.type) {
            ListeningEventType.PLAY_STARTED -> config.weightPlayStarted
            ListeningEventType.PLAY_25_PERCENT -> config.weightPlay25Percent
            ListeningEventType.PLAY_50_PERCENT -> config.weightPlay50Percent
            ListeningEventType.PLAY_75_PERCENT -> config.weightPlay75Percent
            ListeningEventType.PLAY_COMPLETED -> config.weightPlayCompleted
            ListeningEventType.SONG_REPLAYED -> config.weightSongReplayed
            ListeningEventType.SONG_LIKED -> config.weightSongLiked
            ListeningEventType.ADDED_TO_PLAYLIST -> config.weightAddedToPlaylist
            ListeningEventType.SEARCHED -> config.weightSongSearched
            ListeningEventType.ARTIST_OPENED -> config.weightArtistOpened
            ListeningEventType.ALBUM_OPENED -> config.weightAlbumOpened
            ListeningEventType.SONG_SKIPPED -> config.penaltySongSkipped
            ListeningEventType.SONG_SHORT_SKIP -> config.penaltyShortPlaySkipped
            ListeningEventType.SONG_UNLIKED -> config.penaltySongUnliked
            ListeningEventType.REMOVED_FROM_PLAYLIST -> config.penaltyRemovedFromPlaylist
            ListeningEventType.MANUAL_SONG_SELECTION -> config.weightSongSearched
            ListeningEventType.RECOMMENDATION_ACCEPTED -> config.weightPlayStarted + 2.0f
            ListeningEventType.RECOMMENDATION_REJECTED -> config.penaltySongSkipped
            ListeningEventType.STREAM_FAILED -> 0.0f
        }

        val entity = ListeningHistoryEntity(
            songId = event.songId,
            title = event.title,
            artist = event.artist,
            album = event.album,
            albumArtPath = event.albumArtPath,
            streamUrl = event.streamUrl,
            eventType = event.type.name,
            eventWeight = weight,
            durationPlayedMs = event.durationPlayedMs,
            songDurationMs = event.songDurationMs,
            completionPercentage = event.completionPercentage,
            timestamp = event.timestamp,
            sessionId = currentSessionId,
            isOnline = event.isOnline,
            genre = event.genre,
            language = event.language
        )

        try {
            listeningHistoryDao.insertEvent(entity)
            refreshProfile()
        } catch (e: Exception) {
            Log.e(TAG, "Error recording listening event", e)
        }
    }

    suspend fun refreshProfile() = withContext(Dispatchers.IO) {
        try {
            val history = listeningHistoryDao.getRecentHistorySync(5000)
            val likedSongs = playlistRepository.likedSongIds.value.map { it.toString() }.toSet()
            val newProfile = UserMusicProfile.buildFromHistory(history, likedSongs, config)
            _userProfile.value = newProfile

            val vibe = VibeDetector.detectCurrentVibe(history, _currentVibe.value.category)
            _currentVibe.value = vibe
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing user music profile", e)
        }
    }

    suspend fun reloadProfileAfterImport() = withContext(Dispatchers.IO) {
        feedCache.evictAll()
        refreshProfile()
        loadHomeFeed(forceRefresh = true)
    }

    suspend fun clearHistoryAndResetRecommendations() = withContext(Dispatchers.IO) {
        listeningHistoryDao.clearAllHistory()
        feedCache.evictAll()
        _userProfile.value = UserMusicProfile()
        loadHomeFeed(forceRefresh = true)
    }

    /**
     * Retrieves songs played within the 1-hour anti-repeat cooldown window.
     */
    suspend fun getRecentCooldownSongs(): Pair<Set<String>, Set<String>> = withContext(Dispatchers.IO) {
        try {
            val cooldownMillis = config.songCooldownMinutes * 60L * 1000L // 1 Hour (3,600,000 ms)
            val since = System.currentTimeMillis() - cooldownMillis
            val recentEvents = listeningHistoryDao.getHistorySince(since)
            val songIds = recentEvents.map { it.songId }.toSet()
            val normalizedKeys = recentEvents.map { DiversityFilter.normalize(it.title, it.artist) }.toSet()
            Pair(songIds, normalizedKeys)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching 1-hour cooldown songs", e)
            Pair(emptySet(), emptySet())
        }
    }

    suspend fun changeVibe(targetVibe: VibeCategory? = null) = withContext(Dispatchers.IO) {
        val next = targetVibe ?: VibeDetector.getNextVibe(_currentVibe.value.category)
        val (greeting, _) = VibeDetector.getTimeGreeting()
        _currentVibe.value = UserVibe(
            category = next,
            confidence = 0.85f,
            greeting = greeting,
            subtitle = "Your ${next.displayName}"
        )
        loadHomeFeed(forceRefresh = true)
    }

    suspend fun getUserArtistUniverse(): List<String> = withContext(Dispatchers.IO) {
        val saved = playlistRepository.savedArtists.value.toList()
        val userPrefs = UserPreferencesRepository(context)
        val searchArtists = try {
            userPrefs.recentSearchesFlow.firstOrNull()?.map { it.artist }?.filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val topArtistsTuples = try {
            listeningHistoryDao.getTopArtists(20).map { it.artist }
        } catch (_: Exception) {
            emptyList()
        }
        val profileArtists = _userProfile.value.topArtists.keys.toList()

        (saved + searchArtists + topArtistsTuples + profileArtists).distinct()
    }

    suspend fun loadHomeFeed(
        userName: String = "",
        forceRefresh: Boolean = false,
        countryCode: String = "IN",
        countryName: String = "India",
        primaryLang: String = "Hindi",
        preferredLangs: Set<String> = setOf("Hindi", "Gujarati", "English"),
        favoriteGenres: Set<String> = setOf("Bollywood", "Romantic", "Pop"),
        favoriteArtists: Set<String> = emptySet()
    ): HomeRecommendationFeed = withContext(Dispatchers.IO) {
        val vibeKey = "${_currentVibe.value.category.name}_${userName}_${countryCode}_${primaryLang}"
        if (!forceRefresh) {
            feedCache.get(vibeKey)?.let { cached ->
                _homeFeed.value = cached
                return@withContext cached
            }
        }

        refreshProfile()
        val profile = _userProfile.value
        val history = listeningHistoryDao.getRecentHistorySync(50)
        val vibe = _currentVibe.value
        val isColdStart = profile.totalPlays < 3

        val (cooldownIds, cooldownNormalized) = getRecentCooldownSongs()

        val topSongsTuples = listeningHistoryDao.getTopMostPlayedSongs(20)
        val topArtistsTuples = listeningHistoryDao.getTopArtists(12)
        val topAlbumsTuples = listeningHistoryDao.getTopAlbums(8)

        val countryProfile = CountryMusicCatalog.getProfile(countryCode)
        val effectiveCountryName = countryProfile.countryName
        val effectivePrimaryLang = if (countryCode.equals("IN", ignoreCase = true)) primaryLang else countryProfile.defaultPrimaryLanguage
        val trendingShelf = countryProfile.shelves.firstOrNull { it.category == "trending" }

        val allUserArtists = getUserArtistUniverse()
        val shuffledPool = (favoriteArtists.toList() + allUserArtists).distinct()
        val countryPopular = countryProfile.popularArtists.map { it.name }

        val a1 = favoriteArtists.firstOrNull() ?: shuffledPool.getOrNull(0) ?: topArtistsTuples.firstOrNull()?.artist ?: countryPopular.getOrNull(0) ?: "Taylor Swift"
        val a2 = favoriteArtists.drop(1).firstOrNull() ?: shuffledPool.getOrNull(1) ?: topArtistsTuples.getOrNull(1)?.artist ?: countryPopular.getOrNull(1) ?: "The Weeknd"
        val a3 = shuffledPool.getOrNull(2) ?: topArtistsTuples.getOrNull(2)?.artist ?: countryPopular.getOrNull(2) ?: "Drake"
        val a4 = shuffledPool.getOrNull(3) ?: topArtistsTuples.getOrNull(3)?.artist ?: countryPopular.getOrNull(3) ?: "Billie Eilish"
        val preferredGenre = favoriteGenres.firstOrNull() ?: if (countryCode.equals("IN", ignoreCase = true)) "Bollywood" else "Pop"
        val madeForYouArtist = a1

        val discoveryKeywords = listOf(
            "new releases $effectivePrimaryLang $effectiveCountryName 2024",
            "trending pop viral hits $effectiveCountryName",
            "$effectivePrimaryLang acoustic romantic unplugged",
            "viral hits 2024",
            "indie folk acoustic chill $effectivePrimaryLang",
            "lofi chillhop midnight study",
            "global viral top charts"
        ).shuffled()
        val freshKeyword = discoveryKeywords.getOrNull(0) ?: "new releases $effectivePrimaryLang 2024"
        val trendingKeyword = discoveryKeywords.getOrNull(1) ?: "trending viral hits $effectiveCountryName"

        coroutineScope {
            // 0. "Your Spotify Heavy Rotation" (Resolved in parallel in <400ms)
            val heavyRotationDeferred = async {
                if (topSongsTuples.isEmpty()) return@async emptyList<OnlineSongModel>()
                val topQueries = topSongsTuples.shuffled().take(10).map { "${it.title} ${it.artist}" }
                OnlineMusicSearchService.resolveSongsFastParallel(topQueries).take(8)
            }

            // 1. "Daily Mix 1" (Top Artists Cluster #1 - dynamic pairings each time)
            val dailyMix1Title = "Daily Mix 1 • $a1, $a2"
            val dailyMix1Subtitle = "Curated from your listening taste"
            val dailyMix1Deferred = async {
                try {
                    val query = "$a1 $a2 top hits"
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(query, limit = 10)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 2. "Daily Mix 2" (Top Artists Cluster #2 - dynamic pairings each time)
            val dailyMix2Title = "Daily Mix 2 • $a3, $a4"
            val dailyMix2Subtitle = "More favorites from your music data"
            val dailyMix2Deferred = async {
                try {
                    val query = "$a3 $a4 hits"
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(query, limit = 10)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 3. "Top Artists"
            val topArtistsDeferred = async {
                val pool = (shuffledPool.take(5) + topArtistsTuples.map { it.artist }).distinct().take(8)
                pool.mapIndexed { index, name ->
                    com.audiovault.app.core.online.ArtistItemModel(
                        id = "spotify_art_${name.hashCode()}",
                        name = name,
                        imageUrl = com.audiovault.app.core.catalog.ArtistImageCatalog.getImageUrl(name),
                        role = "Favorite Artist"
                    )
                }
            }

            // 4. "Top Albums" (Resolved in parallel in <400ms)
            val topAlbumsDeferred = async {
                val albumQueries = topAlbumsTuples.take(4).map {
                    if (!it.artist.isNullOrBlank()) "${it.album} ${it.artist}" else it.album
                }
                OnlineMusicSearchService.resolveAlbumsFastParallel(albumQueries)
            }

            // 5. "Made For You" (Spotlight artist rotating each session)
            val madeForYouDeferred = async {
                try {
                    val query = "$madeForYouArtist top songs hits"
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(query, limit = 15)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 6. "Because You Listened To" (Context from most recent finished song or dynamic artist)
            val lastListened = history.firstOrNull { it.eventType == ListeningEventType.PLAY_COMPLETED.name || it.eventType == ListeningEventType.PLAY_STARTED.name }
            val becauseTitle = lastListened?.title ?: a1

            val becauseDeferred = async {
                try {
                    val query = if (lastListened != null) "${lastListened.artist} ${lastListened.title}" else "$a1 best songs"
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(query, limit = 15)
                    val filtered = DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = setOf(lastListened?.songId ?: ""),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    )
                    filtered.take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 7. "Your Vibe" (e.g. Chill, Lo-fi, Romantic, Energetic)
            val vibeDeferred = async {
                try {
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(vibe.category.searchKeyword, limit = 15)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 8. "Fresh For You" (Discovery rotating themes)
            val freshDeferred = async {
                try {
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(freshKeyword, limit = 15)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 9. "Trending Now" (Discovery rotating themes)
            val trendingDeferred = async {
                try {
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(trendingKeyword, limit = 15)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 10. "Trending in Your Country"
            val trendingInCountryTitle = trendingShelf?.title ?: "Trending in $effectiveCountryName"
            val trendingInCountryDeferred = async {
                try {
                    val query = trendingShelf?.searchQuery ?: "top trending $effectiveCountryName $primaryLang hits 2024"
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(query, limit = 15)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(10)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 11. "Your Language Mix"
            val secondaryLang = preferredLangs.firstOrNull { !it.equals(effectivePrimaryLang, ignoreCase = true) }
            val languageMixTitle = if (secondaryLang != null) "$effectivePrimaryLang & $secondaryLang Mix" else "Your $effectivePrimaryLang Mix"
            val languageMixSubtitle = "Top hits in your preferred languages"
            val languageMixDeferred = async {
                try {
                    val query = if (secondaryLang != null) "$effectivePrimaryLang $secondaryLang top hits songs" else "$effectivePrimaryLang top songs hits"
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs(query, limit = 15)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(10)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 12. "Chill Mix"
            val chillMixDeferred = async {
                try {
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs("chill acoustic lofi $primaryLang", limit = 12)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 13. "Romantic Mix"
            val romanticMixDeferred = async {
                try {
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs("romantic love songs $primaryLang hits", limit = 12)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            // 14. "New Releases"
            val newReleasesDeferred = async {
                try {
                    val songs = OnlineMusicSearchService.searchDirectCdnSongs("latest new song releases $primaryLang 2024", limit = 12)
                    DiversityFilter.filterAndDiversify(
                        candidates = songs,
                        excludeIds = emptySet(),
                        currentPlayingSongId = null,
                        recentPlayedSongIds = cooldownIds,
                        recentPlayedNormalizedKeys = cooldownNormalized,
                        config = config
                    ).take(8)
                } catch (_: Exception) {
                    emptyList<OnlineSongModel>()
                }
            }

            val heavyRotation = heavyRotationDeferred.await()
            val mfy = madeForYouDeferred.await()
            val quickTiles = if (heavyRotation.isNotEmpty()) heavyRotation.take(6) else mfy.take(6)

            val feed = HomeRecommendationFeed(
                userVibe = vibe,
                quickTiles = quickTiles,
                heavyRotationSongs = heavyRotation,
                dailyMix1Title = dailyMix1Title,
                dailyMix1Subtitle = dailyMix1Subtitle,
                dailyMix1Songs = dailyMix1Deferred.await(),
                dailyMix2Title = dailyMix2Title,
                dailyMix2Subtitle = dailyMix2Subtitle,
                dailyMix2Songs = dailyMix2Deferred.await(),
                topArtists = topArtistsDeferred.await(),
                topAlbums = topAlbumsDeferred.await(),
                madeForYou = mfy,
                becauseYouListenedToTitle = becauseTitle,
                becauseYouListenedTo = becauseDeferred.await(),
                vibeRecommendations = vibeDeferred.await(),
                freshForYou = freshDeferred.await(),
                trendingNow = trendingDeferred.await(),
                topArtistName = profile.topArtists.keys.firstOrNull(),
                moreFromTopArtist = emptyList(),
                isColdStart = isColdStart,
                userCountryName = countryName,
                userCountryCode = countryCode,
                primaryLanguage = primaryLang,
                trendingInCountryTitle = trendingInCountryTitle,
                trendingInCountrySongs = trendingInCountryDeferred.await(),
                languageMixTitle = languageMixTitle,
                languageMixSubtitle = languageMixSubtitle,
                languageMixSongs = languageMixDeferred.await(),
                chillMixSongs = chillMixDeferred.await(),
                romanticMixSongs = romanticMixDeferred.await(),
                newReleases = newReleasesDeferred.await()
            )

            feedCache.put(vibeKey, feed)
            _homeFeed.value = feed
            feed
        }
    }
}
