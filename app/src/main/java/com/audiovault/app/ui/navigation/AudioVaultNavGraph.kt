package com.audiovault.app.ui.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.ui.drawer.EditProfileNameDialog
import com.audiovault.app.ui.drawer.ProfileDrawerSheet
import com.audiovault.app.ui.home.HomeScreen
import com.audiovault.app.ui.home.HomeViewModel
import com.audiovault.app.ui.library.LibraryScreen
import com.audiovault.app.ui.player.SpotifyFullScreenPlayer
import com.audiovault.app.ui.player.SpotifyMiniPlayer
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.search.SearchScreen
import com.audiovault.app.data.repository.UserSettings
import com.audiovault.app.ui.onboarding.OnboardingScreen
import com.audiovault.app.ui.settings.SettingsScreen
import com.audiovault.app.ui.settings.SettingsViewModel
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.videos.VideosScreen
import com.audiovault.app.ui.videos.VideosViewModel
import kotlinx.coroutines.launch

@Composable
fun AudioVaultNavGraph(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val userPreferencesRepository = remember {
        (context.applicationContext as AudioVaultApplication).container.userPreferencesRepository
    }
    val userName by userPreferencesRepository.userNameFlow.collectAsState(initial = "")
    val hasPromptedNameState by userPreferencesRepository.hasPromptedNameFlow.collectAsState(initial = null)
    val isOnboardingCompleted by userPreferencesRepository.isOnboardingCompletedFlow.collectAsState(initial = null)
    val userSettings by userPreferencesRepository.userSettingsFlow.collectAsState(initial = UserSettings())

    var showNamePromptDialog by remember { mutableStateOf(false) }
    var showOnboardingFlow by remember { mutableStateOf(false) }

    // On first launch, prompt for name only once when DataStore confirms never prompted before
    LaunchedEffect(hasPromptedNameState) {
        if (hasPromptedNameState == false && userName.isBlank()) {
            showNamePromptDialog = true
            userPreferencesRepository.setHasPromptedName(true)
        }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { Screen.items.size })
    val currentRoute = Screen.items.getOrNull(pagerState.currentPage)?.route

    val recordingsViewModel: RecordingsViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val videosViewModel: VideosViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val playerState by recordingsViewModel.playerState.collectAsState()
    val queueState by recordingsViewModel.queueState.collectAsState()
    val repeatMode by recordingsViewModel.repeatMode.collectAsState()
    val isShuffleActive by recordingsViewModel.isShuffleEnabled.collectAsState()
    val likedSongIds by recordingsViewModel.likedSongIds.collectAsState()
    val playlists by recordingsViewModel.playlists.collectAsState()
    val downloadingSongIds by recordingsViewModel.downloadingSongIds.collectAsState()
    val downloadProgressMap by recordingsViewModel.downloadProgress.collectAsState()
    val activePlayingVideo by videosViewModel.activePlayingVideo.collectAsState()
    val selectedAlbum by recordingsViewModel.selectedAlbum.collectAsState()
    val isPendingSongPreview by recordingsViewModel.isPendingSongPreview.collectAsState()
    val pendingSongCountdown by recordingsViewModel.pendingSongCountdown.collectAsState()

    var showFullScreenPlayer by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showSettingsOverlay by remember { mutableStateOf(false) }
    var showRecordOverlay by remember { mutableStateOf(false) }
    var isArtistDetailOpen by remember { mutableStateOf(false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(showFullScreenPlayer) {
        if (showFullScreenPlayer) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    // Hierarchical Back Navigation
    BackHandler(enabled = activePlayingVideo != null) {
        videosViewModel.closePlayer()
    }

    BackHandler(enabled = drawerState.isOpen && activePlayingVideo == null) {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = showSettingsOverlay && !drawerState.isOpen && activePlayingVideo == null) {
        showSettingsOverlay = false
    }

    BackHandler(enabled = showRecordOverlay && !showSettingsOverlay && !drawerState.isOpen && activePlayingVideo == null) {
        showRecordOverlay = false
    }

    BackHandler(enabled = selectedAlbum != null && !showSettingsOverlay && !showRecordOverlay && !drawerState.isOpen && activePlayingVideo == null) {
        recordingsViewModel.closeAlbumDetails()
    }

    BackHandler(enabled = showFullScreenPlayer && selectedAlbum == null && !showSettingsOverlay && !showRecordOverlay && !drawerState.isOpen && activePlayingVideo == null) {
        showFullScreenPlayer = false
    }

    BackHandler(
        enabled = !drawerState.isOpen && !showSettingsOverlay && !showRecordOverlay && !showFullScreenPlayer && selectedAlbum == null &&
                !isArtistDetailOpen && activePlayingVideo == null && pagerState.currentPage != 0
    ) {
        scope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    val allLocalRecordings by recordingsViewModel.allRecordings.collectAsState()

    val currentRec = playerState.currentRecording
    val isCurrentRecLiked = currentRec?.let { rec ->
        likedSongIds.contains(rec.id) || allLocalRecordings.any { local ->
            likedSongIds.contains(local.id) && local.title.equals(rec.title, ignoreCase = true)
        }
    } == true
    val isCurrentRecInPlaylist = currentRec?.let { rec ->
        playlists.any { pl ->
            pl.songIds.contains(rec.id) || allLocalRecordings.any { local ->
                pl.songIds.contains(local.id) && local.title.equals(rec.title, ignoreCase = true)
            }
        }
    } == true
    val isCurrentRecSaved = isCurrentRecLiked || isCurrentRecInPlaylist
    val isCurrentRecDownloading = currentRec?.let {
        val idStr = it.id.toString()
        val hashStr = it.filePath.hashCode().toString()
        downloadingSongIds.contains(idStr) || downloadingSongIds.contains(hashStr) || downloadingSongIds.contains(it.filePath)
    } == true
    val currentRecDownloadProgress = currentRec?.let {
        val idStr = it.id.toString()
        val hashStr = it.filePath.hashCode().toString()
        downloadProgressMap[idStr] ?: downloadProgressMap[hashStr] ?: downloadProgressMap[it.filePath] ?: 0f
    } ?: 0f
    val isCurrentRecDownloaded = currentRec?.let { rec ->
        !rec.filePath.startsWith("http") || allLocalRecordings.any { local ->
            !local.filePath.startsWith("http") && local.title.equals(rec.title, ignoreCase = true)
        }
    } == true

    if (isOnboardingCompleted == false || showOnboardingFlow) {
        OnboardingScreen(
            initialCountryCode = userSettings.userCountryCode,
            initialCountryName = userSettings.userCountryName,
            initialPrimaryLanguage = userSettings.primaryLanguage,
            initialPreferredLanguages = userSettings.preferredLanguages,
            initialFavoriteGenres = userSettings.favoriteGenres,
            initialFavoriteArtists = userSettings.favoriteArtists,
            onFinishOnboarding = { countryCode, countryName, primaryLang, preferredLangs, favoriteGenres, favoriteArtists ->
                recordingsViewModel.setUserCountry(countryCode, countryName)
                recordingsViewModel.setMusicLanguages(primaryLang, preferredLangs)
                recordingsViewModel.setFavoriteGenres(favoriteGenres)
                recordingsViewModel.setFavoriteArtists(favoriteArtists)
                recordingsViewModel.setOnboardingCompleted(true)
                showOnboardingFlow = false
            }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawerSheet(
                userName = userName,
                onEditProfile = {
                    showNamePromptDialog = true
                },
                onNavigateToRecord = {
                    scope.launch {
                        drawerState.close()
                        showRecordOverlay = true
                    }
                },
                onNavigateToSettings = {
                    scope.launch {
                        drawerState.close()
                        showSettingsOverlay = true
                    }
                },
                onNavigateToRecents = {
                    scope.launch {
                        drawerState.close()
                        pagerState.scrollToPage(2) // Jump to Library
                    }
                },
                onNavigateToUpdates = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (activePlayingVideo == null) {
                        Column {
                            // Global Floating Spotify Mini Player
                            if (currentRec != null) {
                                SpotifyMiniPlayer(
                                    playerState = playerState,
                                    isSaved = isCurrentRecSaved,
                                    isPendingPreview = isPendingSongPreview,
                                    previewCountdown = pendingSongCountdown,
                                    onTogglePlayPause = {
                                        if (isPendingSongPreview) {
                                            recordingsViewModel.playImmediatelyWithoutWaiting()
                                        } else {
                                            recordingsViewModel.playerEngine.togglePlayPause()
                                        }
                                    },
                                    onClickMiniPlayer = { showFullScreenPlayer = true },
                                    onSaveClick = { showAddToPlaylistSheet = true }
                                )
                            }

                            // Bottom Navigation Bar synchronized with HorizontalPager
                            AudioVaultBottomBar(
                                currentRoute = currentRoute,
                                onNavigateToRoute = { route ->
                                    // Dismiss open album/playlist details and overlays cleanly
                                    recordingsViewModel.closeAlbumDetails()
                                    showRecordOverlay = false
                                    showSettingsOverlay = false
                                    isArtistDetailOpen = false

                                    val targetIndex = Screen.items.indexOfFirst { it.route == route }
                                    if (targetIndex >= 0) {
                                        scope.launch {
                                            pagerState.scrollToPage(targetIndex)
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                containerColor = SpotifyBlack,
                contentWindowInsets = if (activePlayingVideo != null) WindowInsets(0.dp, 0.dp, 0.dp, 0.dp) else WindowInsets.statusBars,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = selectedAlbum == null && !isArtistDetailOpen && !showFullScreenPlayer && !showSettingsOverlay && !showRecordOverlay && activePlayingVideo == null,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> HomeScreen(
                                recordingsViewModel = recordingsViewModel,
                                userName = userName,
                                onOpenDrawer = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                },
                                onNavigateToSearch = { query ->
                                    scope.launch {
                                        if (!query.isNullOrBlank()) {
                                            recordingsViewModel.onSearchQueryChange(query)
                                        }
                                        pagerState.scrollToPage(1)
                                    }
                                },
                                onNavigateToRecord = {
                                    showRecordOverlay = true
                                }
                            )
                            1 -> SearchScreen(
                                viewModel = recordingsViewModel,
                                userName = userName,
                                isCurrentPage = pagerState.currentPage == 1,
                                onOpenDrawer = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                            )
                            2 -> LibraryScreen(
                                viewModel = recordingsViewModel,
                                userName = userName,
                                isCurrentPage = pagerState.currentPage == 2,
                                onNavigateToSearch = {
                                    scope.launch {
                                        pagerState.scrollToPage(1)
                                    }
                                },
                                onArtistDetailStateChange = { isArtistDetailOpen = it },
                                onOpenDrawer = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                            )
                            3 -> VideosScreen(
                                viewModel = videosViewModel,
                                userName = userName,
                                onOpenDrawer = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                            )
                        }
                    }

                    // Album & Movie Soundtrack Detail Full Screen Screen (Matching Spotify with Bottom Nav & MiniPlayer)
                    AnimatedVisibility(
                        visible = selectedAlbum != null,
                        enter = slideInHorizontally(initialOffsetX = { it }),
                        exit = slideOutHorizontally(targetOffsetX = { it }),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (selectedAlbum != null) {
                            com.audiovault.app.ui.album.AlbumDetailScreen(
                                album = selectedAlbum!!,
                                viewModel = recordingsViewModel,
                                onBack = { recordingsViewModel.closeAlbumDetails() }
                            )
                        }
                    }
                }
            }

            // Record Full Screen Modal Overlay (Accessible from Profile Drawer)
            AnimatedVisibility(
                visible = showRecordOverlay,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    topBar = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            IconButton(onClick = { showRecordOverlay = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Song Recorder",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    },
                    containerColor = SpotifyBlack,
                    contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier.fillMaxSize()
                ) { recordPadding ->
                    Box(modifier = Modifier.padding(recordPadding).fillMaxSize()) {
                        com.audiovault.app.ui.record.RecordScreen(
                            viewModel = homeViewModel,
                            userName = userName,
                            onNavigateToRecordings = {
                                showRecordOverlay = false
                                scope.launch {
                                    pagerState.scrollToPage(2)
                                }
                            },
                            onOpenDrawer = {
                                showRecordOverlay = false
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                }
            }

            // Settings Full Screen Modal Overlay (Accessible from Profile Drawer)
            AnimatedVisibility(
                visible = showSettingsOverlay,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    topBar = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            IconButton(onClick = { showSettingsOverlay = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    },
                    containerColor = SpotifyBlack,
                    modifier = Modifier.fillMaxSize()
                ) { settingsPadding ->
                    Box(modifier = Modifier.padding(settingsPadding)) {
                        SettingsScreen(viewModel = settingsViewModel)
                    }
                }
            }

            // Edit Profile Name Dialog (First Launch or manual edit)
            if (showNamePromptDialog) {
                EditProfileNameDialog(
                    currentName = userName,
                    onSaveName = { newName ->
                        scope.launch {
                            userPreferencesRepository.setUserName(newName)
                            showNamePromptDialog = false
                        }
                    },
                    onDismiss = {
                        scope.launch {
                            userPreferencesRepository.setHasPromptedName(true)
                            showNamePromptDialog = false
                        }
                    }
                )
            }

            // Add to Playlist Bottom Sheet modal for MiniPlayer
            if (showAddToPlaylistSheet && currentRec != null) {
                com.audiovault.app.ui.components.AddToPlaylistBottomSheet(
                    recording = currentRec,
                    isLiked = isCurrentRecLiked,
                    playlists = playlists,
                    onToggleLiked = { recordingsViewModel.toggleLikedSong(currentRec.id, currentRec) },
                    onTogglePlaylist = { playlistId -> recordingsViewModel.toggleSongInPlaylist(playlistId, currentRec.id, currentRec) },
                    onCreatePlaylist = { name -> recordingsViewModel.createPlaylist(name, currentRec.id, currentRec) },
                    onDismiss = { showAddToPlaylistSheet = false }
                )
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
                SpotifyFullScreenPlayer(
                    playerState = playerState,
                    isShuffleActive = isShuffleActive,
                    isLiked = isCurrentRecLiked,
                    isDownloading = isCurrentRecDownloading,
                    downloadProgress = currentRecDownloadProgress,
                    isDownloaded = isCurrentRecDownloaded,
                    playlists = playlists,
                    isPendingPreview = isPendingSongPreview,
                    previewCountdown = pendingSongCountdown,
                    queueState = queueState,
                    repeatMode = repeatMode,
                    onToggleRepeat = { recordingsViewModel.toggleRepeatMode() },
                    onPlayQueueItem = { recordingsViewModel.playFromQueue(it) },
                    onRemoveQueueItem = { recordingsViewModel.removeFromQueue(it) },
                    onMoveQueueItemUp = { recordingsViewModel.moveQueueItemUp(it) },
                    onMoveQueueItemDown = { recordingsViewModel.moveQueueItemDown(it) },
                    onMoveQueueItem = { from, to -> recordingsViewModel.moveQueueItem(from, to) },
                    onPromoteToUserQueue = { recordingsViewModel.promoteToUserQueue(it) },
                    onClearUserQueue = { recordingsViewModel.clearUserQueue() },
                    onClearQueue = { recordingsViewModel.clearQueue() },
                    onRefreshQueue = { recordingsViewModel.refreshVibeFlowQueue() },
                    onToggleAutoQueue = { recordingsViewModel.toggleAutoQueue() },
                    onTogglePersonalization = { recordingsViewModel.togglePersonalization() },
                    onToggleLiked = { currentRec?.let { recordingsViewModel.toggleLikedSong(it.id, it) } },
                    onDownload = { recordingsViewModel.downloadCurrentPlayingSong() },
                    onCancelDownload = { recordingsViewModel.cancelCurrentPlayingDownload() },
                    onViewAlbum = { albumName ->
                        showFullScreenPlayer = false
                        recordingsViewModel.openAlbumByName(
                            albumName = albumName,
                            artistName = currentRec?.artist,
                            coverArtUrl = currentRec?.albumArtPath
                        )
                    },
                    onTogglePlaylist = { playlistId -> currentRec?.let { recordingsViewModel.toggleSongInPlaylist(playlistId, it.id, it) } },
                    onCreatePlaylist = { name -> recordingsViewModel.createPlaylist(name, currentRec?.id, currentRec) },
                    onToggleShuffle = { recordingsViewModel.toggleShuffle() },
                    onPlayNext = { recordingsViewModel.playNextTrack() },
                    onPlayPrevious = { recordingsViewModel.playPreviousTrack() },
                    onTogglePlayPause = {
                        if (isPendingSongPreview) {
                            recordingsViewModel.playImmediatelyWithoutWaiting()
                        } else {
                            recordingsViewModel.playerEngine.togglePlayPause()
                        }
                    },
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

            // In-App HD Video Player Modal (Full-Screen Overlay on top of all Navigation)
            if (activePlayingVideo != null) {
                val video = activePlayingVideo!!
                com.audiovault.app.ui.videos.VideoPlayerModal(
                    video = video,
                    onClose = { videosViewModel.closePlayer() },
                    onShare = {
                        val shareIntent = videosViewModel.createShareIntent(video)
                        if (shareIntent != null) {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        }
                    }
                )
            }
        }
    }
}
