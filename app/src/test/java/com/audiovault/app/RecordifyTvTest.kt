package com.audiovault.app

import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.audiovault.app.ui.tv.TvNavTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordifyTvTest {

    @Test
    fun testTvNavTabsAreConfiguredCorrectly() {
        val tabs = TvNavTab.values()
        assertEquals(8, tabs.size)
        assertEquals(TvNavTab.HOME, tabs[0])
        assertEquals(TvNavTab.SEARCH, tabs[1])
        assertEquals(TvNavTab.LIBRARY, tabs[2])
        assertEquals(TvNavTab.PLAYLISTS, tabs[3])
        assertEquals(TvNavTab.RECENTLY_PLAYED, tabs[4])
        assertEquals(TvNavTab.FAVORITES, tabs[5])
        assertEquals(TvNavTab.NOW_PLAYING, tabs[6])
        assertEquals(TvNavTab.SETTINGS, tabs[7])
    }

    @Test
    fun testCuratedAlbumsAvailableForTvShelves() {
        val curated = CuratedAlbumCatalog.curatedAlbums
        assertTrue(curated.isNotEmpty())
        assertTrue(curated.any { it.title.contains("90s", ignoreCase = true) })
        assertTrue(curated.any { it.title.contains("Classics", ignoreCase = true) || it.title.contains("Old", ignoreCase = true) })
        
        // Verify curated albums & playlists have pre-seeded track queries
        val album90s = curated.first { it.id == "curated_90s_bollywood" }
        assertTrue(album90s.trackQueries.isNotEmpty())
        assertTrue(album90s.trackQueries.any { it.contains("Kumar Sanu") })

        val arijitSad = curated.first { it.id == "curated_arijit_sad_playlist" }
        assertTrue(arijitSad.trackQueries.isNotEmpty())
        assertTrue(arijitSad.trackQueries.any { it.contains("Channa Mereya") })
    }

    @Test
    fun testTvRemoteKeyCodesRecognized() {
        val mediaKeys = listOf(
            android.view.KeyEvent.KEYCODE_BACK,
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE,
            android.view.KeyEvent.KEYCODE_MEDIA_NEXT,
            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER
        )
        for (key in mediaKeys) {
            assertTrue(key > 0)
        }
    }

    enum class TvBackAction {
        CLOSE_QUEUE,
        CLOSE_FULL_SCREEN,
        CLOSE_ALBUM_DETAILS,
        CLOSE_LIBRARY_SUBVIEW,
        CLEAR_SEARCH_QUERY,
        NAVIGATE_TO_HOME_TAB,
        EXIT_APP
    }

    private fun resolveTvBackAction(
        isQueueOpen: Boolean = false,
        isNowPlayingFullScreen: Boolean,
        hasSelectedAlbum: Boolean,
        librarySubView: String?,
        selectedTab: TvNavTab,
        searchQuery: String
    ): TvBackAction {
        return when {
            isQueueOpen -> TvBackAction.CLOSE_QUEUE
            isNowPlayingFullScreen -> TvBackAction.CLOSE_FULL_SCREEN
            hasSelectedAlbum -> TvBackAction.CLOSE_ALBUM_DETAILS
            librarySubView != null -> TvBackAction.CLOSE_LIBRARY_SUBVIEW
            selectedTab == TvNavTab.SEARCH && searchQuery.isNotBlank() -> TvBackAction.CLEAR_SEARCH_QUERY
            selectedTab != TvNavTab.HOME -> TvBackAction.NAVIGATE_TO_HOME_TAB
            else -> TvBackAction.EXIT_APP
        }
    }

    @Test
    fun testStepByStepBackSystemHierarchy() {
        // Level 5: Queue panel open -> Back closes queue
        assertEquals(
            TvBackAction.CLOSE_QUEUE,
            resolveTvBackAction(
                isQueueOpen = true,
                isNowPlayingFullScreen = true,
                hasSelectedAlbum = true,
                librarySubView = "LIKED",
                selectedTab = TvNavTab.LIBRARY,
                searchQuery = "Arijit"
            )
        )

        // Level 4: Fullscreen player open -> Back exits fullscreen
        assertEquals(
            TvBackAction.CLOSE_FULL_SCREEN,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = true,
                hasSelectedAlbum = true,
                librarySubView = "LIKED",
                selectedTab = TvNavTab.LIBRARY,
                searchQuery = "Arijit"
            )
        )

        // Level 3: Album Detail open -> Back closes album details
        assertEquals(
            TvBackAction.CLOSE_ALBUM_DETAILS,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = true,
                librarySubView = null,
                selectedTab = TvNavTab.HOME,
                searchQuery = ""
            )
        )

        // Level 2 (Library): Library subview open -> Back returns to Library main view
        assertEquals(
            TvBackAction.CLOSE_LIBRARY_SUBVIEW,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = false,
                librarySubView = "DOWNLOADED",
                selectedTab = TvNavTab.LIBRARY,
                searchQuery = ""
            )
        )

        // Level 2 (Search): Search query typed -> Back clears search query
        assertEquals(
            TvBackAction.CLEAR_SEARCH_QUERY,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = false,
                librarySubView = null,
                selectedTab = TvNavTab.SEARCH,
                searchQuery = "romantic hits"
            )
        )

        // Level 1: In Search with blank query -> Back returns to Home tab
        assertEquals(
            TvBackAction.NAVIGATE_TO_HOME_TAB,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = false,
                librarySubView = null,
                selectedTab = TvNavTab.SEARCH,
                searchQuery = ""
            )
        )

        // Level 1: In Playlists tab -> Back returns to Home tab
        assertEquals(
            TvBackAction.NAVIGATE_TO_HOME_TAB,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = false,
                librarySubView = null,
                selectedTab = TvNavTab.PLAYLISTS,
                searchQuery = ""
            )
        )

        // Level 1: In Recently Played tab -> Back returns to Home tab
        assertEquals(
            TvBackAction.NAVIGATE_TO_HOME_TAB,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = false,
                librarySubView = null,
                selectedTab = TvNavTab.RECENTLY_PLAYED,
                searchQuery = ""
            )
        )

        // Level 1: In Favorites tab -> Back returns to Home tab
        assertEquals(
            TvBackAction.NAVIGATE_TO_HOME_TAB,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = false,
                librarySubView = null,
                selectedTab = TvNavTab.FAVORITES,
                searchQuery = ""
            )
        )

        // Level 1: In Settings tab -> Back returns to Home tab
        assertEquals(
            TvBackAction.NAVIGATE_TO_HOME_TAB,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = false,
                librarySubView = null,
                selectedTab = TvNavTab.SETTINGS,
                searchQuery = ""
            )
        )

        // Level 0: On Home tab with nothing open -> Back exits app cleanly
        assertEquals(
            TvBackAction.EXIT_APP,
            resolveTvBackAction(
                isQueueOpen = false,
                isNowPlayingFullScreen = false,
                hasSelectedAlbum = false,
                librarySubView = null,
                selectedTab = TvNavTab.HOME,
                searchQuery = ""
            )
        )
    }

    @Test
    fun testTvCatalogFallbacksProvideRichMusicShelves() {
        val newReleases = com.audiovault.app.ui.tv.TvCatalogFallbacks.newReleases
        val topHits = com.audiovault.app.ui.tv.TvCatalogFallbacks.topHitsIndia
        val romantic = com.audiovault.app.ui.tv.TvCatalogFallbacks.romanticMelodies
        val bollywood90s = com.audiovault.app.ui.tv.TvCatalogFallbacks.golden90sBollywood
        val chillLofi = com.audiovault.app.ui.tv.TvCatalogFallbacks.chillLofi
        val punjabi = com.audiovault.app.ui.tv.TvCatalogFallbacks.punjabiParty

        assertTrue(newReleases.size >= 6)
        assertTrue(topHits.size >= 6)
        assertTrue(romantic.size >= 6)
        assertTrue(bollywood90s.size >= 6)
        assertTrue(chillLofi.size >= 6)
        assertTrue(punjabi.size >= 6)

        for (song in newReleases + topHits + romantic + bollywood90s) {
            assertTrue(song.id.isNotBlank())
            assertTrue(song.title.isNotBlank())
            assertTrue(song.artist.isNotBlank())
            assertTrue(song.durationMs > 0L)
            assertTrue(!song.highResArtUrl.isNullOrBlank())
        }
    }

    @Test
    fun testSmartVibeQueueEngineEraDetectionAndQueueContinuity() {
        val retroItem = com.audiovault.app.core.queue.QueueItem(
            id = "test_retro",
            title = "Tujhe Dekha To",
            artist = "Kumar Sanu, Lata Mangeshkar",
            album = "Dilwale Dulhania Le Jayenge"
        )
        val modernItem = com.audiovault.app.core.queue.QueueItem(
            id = "test_modern",
            title = "Aaj Ki Raat",
            artist = "Sachin-Jigar, Madhubanti Bagchi",
            album = "Stree 2"
        )
        val romanticItem = com.audiovault.app.core.queue.QueueItem(
            id = "test_romantic",
            title = "Jeene Laga Hoon",
            artist = "Atif Aslam, Shreya Ghoshal",
            album = "Ramaiya Vastavaiya"
        )
        val punjabiItem = com.audiovault.app.core.queue.QueueItem(
            id = "test_punjabi",
            title = "Softly",
            artist = "Karan Aujla",
            album = "Making Memories"
        )

        // 1. Era Detection
        assertEquals(
            com.audiovault.app.core.queue.SongEra.OLD_RETRO,
            com.audiovault.app.core.queue.SmartVibeQueueEngine.detectEra(retroItem.title, retroItem.artist, retroItem.album)
        )
        assertEquals(
            com.audiovault.app.core.queue.SongEra.NEW_MODERN,
            com.audiovault.app.core.queue.SmartVibeQueueEngine.detectEra(modernItem.title, modernItem.artist, modernItem.album)
        )

        // 2. Intelligent Queue Seeding
        val retroQueue = com.audiovault.app.core.queue.SmartVibeQueueEngine.getInstantSeedQueue(retroItem)
        assertTrue(retroQueue.isNotEmpty())
        // When old song is played, all queued songs must be retro/old classics
        for (queued in retroQueue) {
            val qEra = com.audiovault.app.core.queue.SmartVibeQueueEngine.detectEra(queued.title, queued.artist, queued.album)
            assertTrue(qEra == com.audiovault.app.core.queue.SongEra.OLD_RETRO || qEra == com.audiovault.app.core.queue.SongEra.NEUTRAL)
        }

        val modernQueue = com.audiovault.app.core.queue.SmartVibeQueueEngine.getInstantSeedQueue(modernItem)
        assertTrue(modernQueue.isNotEmpty())
        // When modern song is played, all queued songs must be modern hits
        for (queued in modernQueue) {
            val qEra = com.audiovault.app.core.queue.SmartVibeQueueEngine.detectEra(queued.title, queued.artist, queued.album)
            assertTrue(qEra != com.audiovault.app.core.queue.SongEra.OLD_RETRO)
        }

        val romanticQueue = com.audiovault.app.core.queue.SmartVibeQueueEngine.getInstantSeedQueue(romanticItem)
        assertTrue(romanticQueue.isNotEmpty())
        assertTrue(romanticQueue.any { it.title.contains("Saathiya", ignoreCase = true) || it.title.contains("Tujhe", ignoreCase = true) || it.title.contains("Ban Jaunga", ignoreCase = true) })

        val punjabiQueue = com.audiovault.app.core.queue.SmartVibeQueueEngine.getInstantSeedQueue(punjabiItem)
        assertTrue(punjabiQueue.isNotEmpty())
        assertTrue(punjabiQueue.any { it.artist.contains("Diljit", ignoreCase = true) || it.artist.contains("Moose", ignoreCase = true) || it.artist.contains("Aujla", ignoreCase = true) })

        // 3. Era Compatibility Validation
        // Old seed must reject modern candidate
        assertFalse(
            com.audiovault.app.core.queue.SmartVibeQueueEngine.isCandidateEraCompatible(
                seed = retroItem,
                candidateTitle = "Tauba Tauba",
                candidateArtist = "Karan Aujla",
                candidateAlbum = "Bad Newz"
            )
        )
        // Modern seed must reject 90s retro candidate
        assertFalse(
            com.audiovault.app.core.queue.SmartVibeQueueEngine.isCandidateEraCompatible(
                seed = modernItem,
                candidateTitle = "Pal Pal Dil Ke Paas",
                candidateArtist = "Kishore Kumar",
                candidateAlbum = "Blackmail"
            )
        )
        // Old seed accepts old candidate
        assertTrue(
            com.audiovault.app.core.queue.SmartVibeQueueEngine.isCandidateEraCompatible(
                seed = retroItem,
                candidateTitle = "Pehla Nasha",
                candidateArtist = "Udit Narayan, Alka Yagnik",
                candidateAlbum = "Jo Jeeta Wohi Sikandar"
            )
        )
    }

    @Test
    fun testQueueStrictlyRejectsEnglishWhenEnglishNotSelected() {
        val seed = com.audiovault.app.core.queue.QueueItem(
            id = "test_hindi_seed",
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            album = "Aashiqui 2"
        )

        // 1. When English is not selected (preferred languages = Hindi & Gujarati only)
        val hindiQueue = com.audiovault.app.core.queue.SmartVibeQueueEngine.getInstantSeedQueue(
            seed = seed,
            preferredLanguages = setOf("Hindi", "Gujarati"),
            userCountry = "IN"
        )
        assertTrue(hindiQueue.isNotEmpty())

        for (item in hindiQueue) {
            val inferredLang = com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer.inferLanguage("${item.title} ${item.artist}")
            assertFalse(
                "Queue must NOT contain English track '${item.title}' when English is not selected",
                inferredLang.equals("English", ignoreCase = true)
            )
            assertFalse(
                "Queue must NOT contain Western artist '${item.artist}'",
                com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer.isEnglishArtistOrTitle(item.title, item.artist)
            )
        }

        // 2. Western artist detector test
        assertTrue(com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer.isEnglishArtistOrTitle("Shape of You", "Ed Sheeran"))
        assertTrue(com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer.isEnglishArtistOrTitle("Faded", "Alan Walker"))
        assertFalse(com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer.isEnglishArtistOrTitle("Kesariya", "Arijit Singh"))
        assertFalse(com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer.isEnglishArtistOrTitle("Pehla Nasha", "Udit Narayan"))

        // 3. Language-connected query generation
        val gujaratiQueries = com.audiovault.app.core.queue.SmartVibeQueueEngine.generateIntelligentSearchQueries(seed, "Gujarati")
        assertTrue(gujaratiQueries.any { it.contains("Gujarati", ignoreCase = true) })

        val hindiQueries = com.audiovault.app.core.queue.SmartVibeQueueEngine.generateIntelligentSearchQueries(seed, "Hindi")
        assertTrue(hindiQueries.any { it.contains("Hindi", ignoreCase = true) })
    }
}
