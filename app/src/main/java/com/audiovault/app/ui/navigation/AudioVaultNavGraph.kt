package com.audiovault.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import com.audiovault.app.ui.theme.SpotifyBlack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audiovault.app.ui.home.HomeScreen
import com.audiovault.app.ui.home.HomeViewModel
import com.audiovault.app.ui.library.LibraryScreen
import com.audiovault.app.ui.player.SpotifyFullScreenPlayer
import com.audiovault.app.ui.player.SpotifyMiniPlayer
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.search.SearchScreen
import com.audiovault.app.ui.settings.SettingsScreen
import com.audiovault.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun AudioVaultNavGraph(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { Screen.items.size })
    val currentRoute = Screen.items.getOrNull(pagerState.currentPage)?.route

    val recordingsViewModel: RecordingsViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val playerState by recordingsViewModel.playerState.collectAsState()
    val isShuffleActive by recordingsViewModel.isShuffleEnabled.collectAsState()
    val likedSongIds by recordingsViewModel.likedSongIds.collectAsState()
    val playlists by recordingsViewModel.playlists.collectAsState()

    var showFullScreenPlayer by remember { mutableStateOf(false) }
    var isArtistDetailOpen by remember { mutableStateOf(false) }

    // One-by-one hierarchical Back navigation:
    // 1. If full screen player is open, back closes player first
    BackHandler(enabled = showFullScreenPlayer) {
        showFullScreenPlayer = false
    }

    // 2. If on a sub-tab (Search, Library, Settings), back smoothly swipes back to Home screen (tab 0)
    BackHandler(enabled = !showFullScreenPlayer && !isArtistDetailOpen && pagerState.currentPage != 0) {
        scope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    // Global Floating Spotify Mini Player
                    if (playerState.currentRecording != null) {
                        SpotifyMiniPlayer(
                            playerState = playerState,
                            onTogglePlayPause = { recordingsViewModel.playerEngine.togglePlayPause() },
                            onClickMiniPlayer = { showFullScreenPlayer = true }
                        )
                    }

                    // Bottom Navigation Bar synchronized with HorizontalPager
                    AudioVaultBottomBar(
                        currentRoute = currentRoute,
                        onNavigateToRoute = { route ->
                            val targetIndex = Screen.items.indexOfFirst { it.route == route }
                            if (targetIndex >= 0) {
                                scope.launch {
                                    // Direct instant open on click as requested
                                    pagerState.scrollToPage(targetIndex)
                                }
                            }
                        }
                    )
                }
            },
            containerColor = SpotifyBlack,
            contentWindowInsets = WindowInsets.statusBars,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isArtistDetailOpen && !showFullScreenPlayer,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToRecordings = {
                            scope.launch {
                                pagerState.scrollToPage(2)
                            }
                        }
                    )
                    1 -> SearchScreen(
                        viewModel = recordingsViewModel
                    )
                    2 -> LibraryScreen(
                        viewModel = recordingsViewModel,
                        onNavigateToSearch = {
                            scope.launch {
                                pagerState.scrollToPage(1)
                            }
                        },
                        onArtistDetailStateChange = { isArtistDetailOpen = it }
                    )
                    3 -> {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        SettingsScreen(viewModel = settingsViewModel)
                    }
                }
            }
        }

        // Full Screen Spotify Player Overlay
        AnimatedVisibility(
            visible = showFullScreenPlayer && playerState.currentRecording != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume clicks to prevent background pass-through */ }
                )
        ) {
            val currentRec = playerState.currentRecording
            SpotifyFullScreenPlayer(
                playerState = playerState,
                isShuffleActive = isShuffleActive,
                isLiked = currentRec?.let { likedSongIds.contains(it.id) } == true,
                playlists = playlists,
                onToggleLiked = { currentRec?.let { recordingsViewModel.toggleLikedSong(it.id) } },
                onTogglePlaylist = { playlistId -> currentRec?.let { recordingsViewModel.toggleSongInPlaylist(playlistId, it.id) } },
                onCreatePlaylist = { name -> recordingsViewModel.createPlaylist(name, currentRec?.id) },
                onToggleShuffle = { recordingsViewModel.toggleShuffle() },
                onPlayNext = { recordingsViewModel.playNextTrack() },
                onPlayPrevious = { recordingsViewModel.playPreviousTrack() },
                onTogglePlayPause = { recordingsViewModel.playerEngine.togglePlayPause() },
                onSeekTo = { recordingsViewModel.playerEngine.seekTo(it) },
                onSpeedSelected = { recordingsViewModel.playerEngine.setPlaybackSpeed(it) },
                onShare = {
                    if (currentRec != null) {
                        val shareIntent = recordingsViewModel.createShareIntent(currentRec)
                        if (shareIntent != null) {
                            context.startActivity(shareIntent)
                        }
                    }
                },
                onClose = { showFullScreenPlayer = false }
            )
        }
    }
}
