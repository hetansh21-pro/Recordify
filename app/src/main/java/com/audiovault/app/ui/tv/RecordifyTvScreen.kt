package com.audiovault.app.ui.tv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.recordings.RecordingsViewModel

/**
 * 10-Foot UI Experience for Recordify TV.
 * Built according to the Spotify TV Architecture Specification:
 * - Persistent Stationary Left Sidebar Navigation Rail.
 * - Central Independently-Scrollable Content Container.
 * - Persistent Fixed Bottom Mini-Player Bar (never stops playback during navigation).
 * - Full-Screen Cinema Stage & Queue Side Panel Overlays.
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
    var isQueueOpen by remember { mutableStateOf(false) }
    var isSidebarExpanded by remember { mutableStateOf(false) }
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

    // Level 4: Fullscreen Cinema Player open -> Exit full screen
    BackHandler(enabled = !isQueueOpen && isNowPlayingFullScreen) {
        isNowPlayingFullScreen = false
        if (selectedTab == TvNavTab.NOW_PLAYING) {
            selectedTab = TvNavTab.HOME
        }
    }

    // Level 3: Album / Playlist Detail open -> Close album detail
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum != null) {
        viewModel.closeAlbumDetails()
    }

    // Level 2.5: Left Sidebar Drawer Expanded -> Collapse sidebar rail back to circular icon buttons
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && isSidebarExpanded) {
        isSidebarExpanded = false
    }

    // Level 2 (Library): Inside Library Subview -> Return to 4 category cards
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && !isSidebarExpanded && librarySubView != null) {
        librarySubView = null
    }

    // Level 2 (Search): Search query active -> Clear search query
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && !isSidebarExpanded && librarySubView == null && selectedTab == TvNavTab.SEARCH && searchQuery.isNotBlank()) {
        searchQuery = ""
        viewModel.onSearchQueryChange("")
    }

    // Level 1: In any secondary tab -> Return to Home tab
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && !isSidebarExpanded && librarySubView == null && (selectedTab != TvNavTab.SEARCH || searchQuery.isBlank()) && selectedTab != TvNavTab.HOME) {
        selectedTab = TvNavTab.HOME
    }

    // Level 0: On Home tab -> Finish activity / exit app cleanly
    BackHandler(enabled = !isQueueOpen && !isNowPlayingFullScreen && selectedAlbum == null && !isSidebarExpanded && librarySubView == null && selectedTab == TvNavTab.HOME) {
        activity?.finish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // -------------------------------------------------------------
            // 1. LEFT SIDEBAR NAVIGATION RAIL (TV Remote Navigable)
            // -------------------------------------------------------------
            TvSidebar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    isSidebarExpanded = false
                    if (tab == TvNavTab.NOW_PLAYING) {
                        isNowPlayingFullScreen = true
                    } else {
                        selectedTab = tab
                        if (selectedAlbum != null) {
                            viewModel.closeAlbumDetails()
                        }
                    }
                },
                userCountryCode = userSettings.userCountryCode,
                primaryLanguage = userSettings.primaryLanguage,
                isExpanded = isSidebarExpanded,
                onToggleExpand = { isSidebarExpanded = !isSidebarExpanded },
                modifier = Modifier.fillMaxHeight()
            )

            // Vertical divider line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF222222))
            )

            // -------------------------------------------------------------
            // 2. MAIN CONTENT AREA (Scrollable with Bottom Padding for Mini-Player)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(bottom = 96.dp)
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
        // 3. PERSISTENT FIXED BOTTOM MINI-PLAYER
        // Remains visible across ALL screens when a song is active.
        // -------------------------------------------------------------
        if (hasActiveTrack && !isNowPlayingFullScreen) {
            val isLiked = currentRecording?.id?.let { likedSongIds.contains(it) } ?: false
            TvMiniPlayerBar(
                title = currentRecording?.title ?: "Playing Music",
                artist = currentRecording?.artist ?: "Recordify TV",
                artUrl = currentRecording?.albumArtPath,
                positionMs = playerState.currentPositionMs,
                durationMs = playerState.durationMs,
                isPlaying = playerState.isPlaying,
                isShuffle = isShuffleActive,
                repeatMode = repeatMode,
                isLiked = isLiked,
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
                onSeekForward = { viewModel.seekForward(10000L) },
                onSeekBackward = { viewModel.seekBackward(10000L) },
                onOpenQueue = { isQueueOpen = true },
                onExpand = { isNowPlayingFullScreen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = if (isSidebarExpanded) 240.dp else 74.dp,
                        end = 0.dp,
                        bottom = 0.dp
                    )
            )
        }

        // -------------------------------------------------------------
        // 4. PLAYBACK QUEUE SIDE PANEL OVERLAY
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isQueueOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
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
                onClose = { isQueueOpen = false }
            )
        }

        // -------------------------------------------------------------
        // 5. FULL-SCREEN NOW PLAYING CINEMA STAGE OVERLAY
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
                    if (selectedTab == TvNavTab.NOW_PLAYING) {
                        selectedTab = TvNavTab.HOME
                    }
                }
            )
        }
    }
}
