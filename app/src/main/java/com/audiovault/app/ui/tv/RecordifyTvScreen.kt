package com.audiovault.app.ui.tv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.recordings.RecordingsViewModel

/**
 * 10-Foot UI Experience for Recordify TV.
 * Built according to the real Spotify TV Design Reference Photos:
 * - Shared Horizontal Top Navigation Bar (Now Playing Pill Widget on Left, Tabs in Center, Settings + "H" on Right).
 * - Full-Width Independently Scrollable Main Content Container.
 * - Split 2-Column Album & Playlist Details Stage.
 * - Cinematic Full-Screen Now Playing Stage Overlay with Backdrops & Scrubber.
 * - Side Queue Panel Overlay.
 * - Step-by-Step Back Navigation Hierarchy.
 */
@Composable
fun RecordifyTvScreen(
    viewModel: RecordingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedTab by remember { mutableStateOf(TvNavTab.HOME) }
    var isNowPlayingFullScreen by remember { mutableStateOf(false) }
    var isNowPlayingControlsVisible by remember { mutableStateOf(true) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var librarySubView by remember { mutableStateOf<String?>(null) }

    val vmSearchQuery by viewModel.searchQuery.collectAsState()
    LaunchedEffect(vmSearchQuery) {
        if (vmSearchQuery != searchQuery) {
            searchQuery = vmSearchQuery
        }
    }

    val playerState by viewModel.playerState.collectAsState()
    val queueState by viewModel.queueState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val homeContent by viewModel.homeContent.collectAsState()
    val homeFeed by viewModel.homeFeed.collectAsState()
    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val isShuffleActive by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val onlineSearchResults by viewModel.onlineSearchResults.collectAsState()
    val matchingAlbums by viewModel.matchingAlbums.collectAsState()
    val isOnlineSearching by viewModel.isOnlineSearching.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()

    val currentRecording = playerState.currentRecording
    val hasActiveTrack = currentRecording != null

    LaunchedEffect(Unit) {
        viewModel.loadHomeContent(forceRefresh = false)
    }

    // -------------------------------------------------------------
    // STEP-BY-STEP BACK NAVIGATION SYSTEM
    // -------------------------------------------------------------
    // Level 5: Queue Side Panel open -> Close Queue
    BackHandler(enabled = isQueueOpen) {
        isQueueOpen = false
    }

    // Level 4b: Fullscreen Cinema Player open & controls visible -> Hide controls to ambient mode first
    BackHandler(enabled = !isQueueOpen && isNowPlayingFullScreen && isNowPlayingControlsVisible) {
        isNowPlayingControlsVisible = false
    }

    // Level 4a: Fullscreen Cinema Player open & controls already hidden -> Exit full screen
    BackHandler(enabled = !isQueueOpen && isNowPlayingFullScreen && !isNowPlayingControlsVisible) {
        isNowPlayingFullScreen = false
        isNowPlayingControlsVisible = true
        if (selectedTab == TvNavTab.NOW_PLAYING) {
            selectedTab = TvNavTab.HOME
        }
    }

    // Level 3: Album / Playlist Detail open -> Close album detail
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum != null) {
        viewModel.closeAlbumDetails()
    }

    // Level 2 (Library): Inside Library Subview -> Return to main library shelf
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && librarySubView != null) {
        librarySubView = null
    }

    // Level 2 (Search): Search query active -> Clear search query
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && librarySubView == null && selectedTab == TvNavTab.SEARCH && searchQuery.isNotBlank()) {
        searchQuery = ""
        viewModel.onSearchQueryChange("")
    }

    // Level 1: In any secondary tab -> Return to Home tab
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && librarySubView == null && (selectedTab != TvNavTab.SEARCH || searchQuery.isBlank()) && selectedTab != TvNavTab.HOME) {
        selectedTab = TvNavTab.HOME
    }

    // Level 0: On Home tab -> Finish activity / exit app cleanly
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && librarySubView == null && selectedTab == TvNavTab.HOME) {
        activity?.finish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // -------------------------------------------------------------
            // 1. GLOBAL TOP NAVIGATION BAR (Matching Spotify TV Photos)
            // Left: Now Playing Mini Widget Pill with Equalizer Bars
            // Center: Nav Tabs (Home, Search, Your Library)
            // Right: Settings Green Pill + "H" Profile Circle
            // -------------------------------------------------------------
            TvTopNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    if (selectedAlbum != null) {
                        viewModel.closeAlbumDetails()
                    }
                },
                activeTrackTitle = currentRecording?.title,
                activeTrackArtist = currentRecording?.artist,
                activeTrackArtUrl = currentRecording?.albumArtPath,
                isPlaying = playerState.isPlaying,
                onOpenNowPlaying = { isNowPlayingFullScreen = true },
                onOpenSettings = { selectedTab = TvNavTab.SETTINGS }
            )

            // -------------------------------------------------------------
            // 2. MAIN CONTENT CONTAINER (Full Width, Scrollable)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedAlbum != null) {
                    TvAlbumDetailView(
                        album = selectedAlbum!!,
                        viewModel = viewModel,
                        onBack = { viewModel.closeAlbumDetails() },
                        onTrackPlayed = { isNowPlayingFullScreen = true }
                    )
                } else {
                    when (selectedTab) {
                        TvNavTab.HOME -> {
                            TvHomeView(
                                viewModel = viewModel,
                                homeFeed = homeFeed,
                                homeContent = homeContent,
                                onPlaySong = { song ->
                                    viewModel.playOnlineSong(song)
                                    isNowPlayingFullScreen = true
                                },
                                onPlayAlbum = { album -> viewModel.openAlbumDetails(album) },
                                onSearchMood = { query ->
                                    selectedTab = TvNavTab.SEARCH
                                    searchQuery = query
                                    viewModel.onSearchQueryChange(query)
                                    viewModel.searchOnlineMusic(query)
                                }
                            )
                        }
                        TvNavTab.SEARCH -> {
                            TvSearchView(
                                viewModel = viewModel,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { q ->
                                    searchQuery = q
                                    viewModel.onSearchQueryChange(q)
                                    viewModel.searchOnlineMusic(q)
                                },
                                searchResults = onlineSearchResults,
                                matchingAlbums = matchingAlbums,
                                isSearching = isOnlineSearching,
                                onPlaySong = { song ->
                                    viewModel.playOnlineSong(song)
                                    isNowPlayingFullScreen = true
                                },
                                onPlayAlbum = { album -> viewModel.openAlbumDetails(album) }
                            )
                        }
                        TvNavTab.LIBRARY -> {
                            TvLibraryView(
                                viewModel = viewModel,
                                librarySubView = librarySubView,
                                onLibrarySubViewChange = { librarySubView = it },
                                likedSongIds = likedSongIds,
                                onPlaySong = { song ->
                                    viewModel.playOnlineSong(song)
                                    isNowPlayingFullScreen = true
                                },
                                onOpenAlbum = { album -> viewModel.openAlbumDetails(album) }
                            )
                        }
                        TvNavTab.PLAYLISTS -> {
                            TvPlaylistsView(
                                viewModel = viewModel,
                                onOpenAlbum = { album -> viewModel.openAlbumDetails(album) }
                            )
                        }
                        TvNavTab.RECENTLY_PLAYED -> {
                            TvRecentlyPlayedView(
                                viewModel = viewModel,
                                onPlaySong = { song ->
                                    viewModel.playOnlineSong(song)
                                    isNowPlayingFullScreen = true
                                }
                            )
                        }
                        TvNavTab.FAVORITES -> {
                            TvFavoritesView(
                                viewModel = viewModel,
                                onPlaySong = { song ->
                                    viewModel.playOnlineSong(song)
                                    isNowPlayingFullScreen = true
                                }
                            )
                        }
                        TvNavTab.NOW_PLAYING -> {
                            LaunchedEffect(Unit) {
                                isNowPlayingFullScreen = true
                            }
                        }
                        TvNavTab.SETTINGS -> {
                            TvSettingsView(
                                viewModel = viewModel,
                                userSettings = userSettings,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. FULL-SCREEN NOW PLAYING CINEMA STAGE OVERLAY
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isNowPlayingFullScreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TvNowPlayingView(
                viewModel = viewModel,
                playerState = playerState,
                queueState = queueState,
                isShuffle = isShuffleActive,
                repeatMode = repeatMode,
                isLiked = currentRecording?.id?.let { likedSongIds.contains(it) } ?: false,
                onTogglePlay = { viewModel.togglePlayback() },
                onNext = { viewModel.playNextTrack() },
                onPrevious = { viewModel.playPreviousTrack() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onToggleLike = {
                    currentRecording?.let { rec ->
                        viewModel.toggleLikedSong(rec.id, rec)
                    }
                },
                onOpenQueue = { isQueueOpen = true },
                onPlaySong = { song -> viewModel.playOnlineSong(song) },
                onBack = {
                    isNowPlayingFullScreen = false
                    isNowPlayingControlsVisible = true
                    if (selectedTab == TvNavTab.NOW_PLAYING) {
                        selectedTab = TvNavTab.HOME
                    }
                },
                areControlsVisible = isNowPlayingControlsVisible,
                onControlsVisibilityChange = { isNowPlayingControlsVisible = it },
                isQueueOpen = isQueueOpen
            )
        }

        // -------------------------------------------------------------
        // 4. PLAYBACK QUEUE SIDE PANEL OVERLAY (Renders on top of full-screen player with Touch Isolation)
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isQueueOpen,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                isQueueOpen = false
                            }
                        )
                    }
            ) {
                TvQueuePanel(
                    queueState = queueState,
                    currentRecordingTitle = currentRecording?.title,
                    currentRecordingArtist = currentRecording?.artist,
                    currentRecordingArt = currentRecording?.albumArtPath,
                    isPlaying = playerState.isPlaying,
                    onPlayQueueIndex = { idx -> viewModel.playFromQueue(idx) },
                    onRemoveQueueIndex = { idx -> viewModel.removeFromQueue(idx) },
                    onClearQueue = { viewModel.clearQueue() },
                    onClose = { isQueueOpen = false },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}
