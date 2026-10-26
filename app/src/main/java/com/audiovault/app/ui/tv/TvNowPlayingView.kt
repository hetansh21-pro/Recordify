package com.audiovault.app.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.core.queue.PlaybackQueueState
import com.audiovault.app.core.queue.PlaybackRepeatMode
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.components.rememberAsyncCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright
import kotlinx.coroutines.delay

/**
 * Authentic Spotify TV Full-Screen Now Playing View (Reference: IMG20261026111647.jpg).
 * - Full-screen cinematic artist backdrop (or pure OLED black if Dark mode is active).
 * - Top-left: "Playing from [Playlist]" pill.
 * - Lower section: Album cover, bold title, artist names, and [(+) Save] button.
 * - Timeline: White Play/Pause button, elapsed time, 5s scrubber seekbar, remaining time.
 * - Bottom controls:
 *     Left: Previous, Next, Shuffle, Repeat.
 *     Right: Lyrics, Queue, About, Dark mode pills.
 * - Auto-hide: Inactivity for 5 seconds smoothly hides controls leaving ambient bottom-left track card.
 * - D-Pad wakeup: Any remote keypress restores full controls.
 */
@Composable
fun TvNowPlayingView(
    viewModel: RecordingsViewModel,
    playerState: PlayerState,
    queueState: PlaybackQueueState,
    isShuffle: Boolean,
    repeatMode: PlaybackRepeatMode,
    isLiked: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlaySong: (OnlineSongModel) -> Unit = {},
    onBack: () -> Unit = {},
    areControlsVisible: Boolean = true,
    onControlsVisibilityChange: (Boolean) -> Unit = {},
    isQueueOpen: Boolean = false
) {
    val rec = playerState.currentRecording
    val positionMs = playerState.currentPositionMs
    val durationMs = playerState.durationMs.coerceAtLeast(1L)

    var isDarkScreenActive by rememberSaveable { mutableStateOf(false) }
    var darkToastMessage by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Dedicated FocusRequesters for every interactive remote control on screen
    val ambientFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }
    val playingFromFocusRequester = remember { FocusRequester() }
    val profileFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }
    val prevFocusRequester = remember { FocusRequester() }
    val nextFocusRequester = remember { FocusRequester() }
    val shuffleFocusRequester = remember { FocusRequester() }
    val repeatFocusRequester = remember { FocusRequester() }
    val lyricsFocusRequester = remember { FocusRequester() }
    val queueFocusRequester = remember { FocusRequester() }
    val aboutFocusRequester = remember { FocusRequester() }
    val darkModeFocusRequester = remember { FocusRequester() }

    // Auto-focus management between controls deck and ambient mode
    LaunchedEffect(areControlsVisible, isQueueOpen) {
        if (!isQueueOpen) {
            delay(100)
            if (areControlsVisible) {
                try {
                    playPauseFocusRequester.requestFocus()
                } catch (_: Exception) {}
            } else {
                try {
                    ambientFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    // 5-second inactivity auto-hide timer (paused while Queue or dialogs are open)
    LaunchedEffect(lastInteractionTime, areControlsVisible, showAboutDialog, showLyricsDialog, isQueueOpen) {
        if (areControlsVisible && !showAboutDialog && !showLyricsDialog && !isQueueOpen) {
            delay(5000L)
            onControlsVisibilityChange(false)
        }
    }

    // Automatic focus restoration when Queue panel closes
    LaunchedEffect(isQueueOpen) {
        if (!isQueueOpen && areControlsVisible) {
            delay(100)
            try {
                queueFocusRequester.requestFocus()
            } catch (_: Exception) {
                try {
                    playPauseFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    // Auto-dismiss dark mode toast after 3 seconds
    LaunchedEffect(darkToastMessage) {
        if (darkToastMessage != null) {
            delay(3000L)
            darkToastMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkScreenActive) Color.Black else Color(0xFF0C0C0C))
            // Complete Touch Isolation: Consume all touches/clicks on full player background
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        lastInteractionTime = System.currentTimeMillis()
                        if (!areControlsVisible) {
                            onControlsVisibilityChange(true)
                        }
                    }
                )
            }
            // Ambient remote wakeup: Active focus receiver when controls are hidden
            .then(
                if (!areControlsVisible) {
                    Modifier
                        .focusRequester(ambientFocusRequester)
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.key == Key.Back) {
                                    // Let BackHandler handle exiting to Home
                                    false
                                } else {
                                    // Any remote D-pad / Enter key wakes up controls instantly
                                    lastInteractionTime = System.currentTimeMillis()
                                    onControlsVisibilityChange(true)
                                    true
                                }
                            } else false
                        }
                } else Modifier
            )
    ) {
        // -----------------------------------------------------------------
        // 1. FULL-SCREEN ARTIST PHOTO BACKDROP (Hidden if Dark Screen Active)
        // -----------------------------------------------------------------
        if (!isDarkScreenActive) {
            val backdropBitmap by rememberAsyncCoverArt(
                albumArtPath = rec?.albumArtPath,
                title = rec?.title,
                artist = rec?.artist,
                targetSizeDp = 400.dp
            )
            if (backdropBitmap != null) {
                Image(
                    bitmap = backdropBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.35f
                )
            }

            // Dark gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
        }

        // -----------------------------------------------------------------
        // 2. AMBIENT BOTTOM-LEFT SONG CARD (Shown when controls are hidden)
        // Reference: media_1792999663110_c196747e.jpg & media_1792999672148_4c39bea3.jpg
        // -----------------------------------------------------------------
        AnimatedVisibility(
            visible = !areControlsVisible,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 44.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                ) {
                    TrackCoverArt(
                        albumArtPath = rec?.albumArtPath,
                        title = rec?.title ?: "Music",
                        artist = rec?.artist ?: "Artist",
                        size = 64.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = rec?.title ?: "No Song Playing",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rec?.artist ?: "Recordify TV",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFB3B3B3),
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // 3. MAIN CONTROLS DECK (Top Bar + Scrubber + Control Buttons)
        // -----------------------------------------------------------------
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: "Playing from" Pill on Left, Profile on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // "Playing from" Pill Button (Clicking returns to previous / playlist)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier
                            .height(46.dp)
                            .focusRequester(playingFromFocusRequester)
                            .focusProperties {
                                down = playPauseFocusRequester
                                right = profileFocusRequester
                            }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    lastInteractionTime = System.currentTimeMillis()
                                    when (keyEvent.key) {
                                        Key.DirectionUp, Key.DirectionLeft -> true // Clamp
                                        Key.DirectionDown -> {
                                            playPauseFocusRequester.requestFocus()
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            profileFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .tvRemoteFocusable(
                                onClick = onBack,
                                shape = RoundedCornerShape(24.dp),
                                focusScale = 1.05f
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackCoverArt(
                                albumArtPath = rec?.albumArtPath,
                                title = rec?.title ?: "Music",
                                artist = rec?.artist ?: "Artist",
                                size = 32.dp,
                                shape = RoundedCornerShape(4.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "Playing from",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = rec?.album ?: queueState.currentTrack?.album ?: "Bollywood Mush",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    // Profile Avatar Circle ("H")
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFA07A),
                        modifier = Modifier
                            .size(40.dp)
                            .focusRequester(profileFocusRequester)
                            .focusProperties {
                                down = saveFocusRequester
                                left = playingFromFocusRequester
                            }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    lastInteractionTime = System.currentTimeMillis()
                                    when (keyEvent.key) {
                                        Key.DirectionUp, Key.DirectionRight -> true // Clamp
                                        Key.DirectionDown -> {
                                            saveFocusRequester.requestFocus()
                                            true
                                        }
                                        Key.DirectionLeft -> {
                                            playingFromFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .tvRemoteFocusable(
                                onClick = onBack,
                                shape = CircleShape,
                                focusScale = 1.15f
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "H",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // -------------------------------------------------------------
                // LOWER HALF: TRACK INFO & CONTROLS DECK
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Track Info Row: Album Art + Title & Artists on Left, [(+) Save] on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Square Album Cover Card
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
                            ) {
                                TrackCoverArt(
                                    albumArtPath = rec?.albumArtPath,
                                    title = rec?.title ?: "Music",
                                    artist = rec?.artist ?: "Artist",
                                    size = 110.dp,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Title & Artists
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rec?.title ?: "No Song Playing",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 24.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rec?.artist ?: "Recordify TV",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFFB3B3B3),
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // [(+) Save] / [(✓) Saved] Pill Button
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isLiked) SpotifyGreenBright.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isLiked) SpotifyGreenBright else Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .height(40.dp)
                                .focusRequester(saveFocusRequester)
                                .focusProperties {
                                    up = profileFocusRequester
                                    down = timelineFocusRequester
                                    left = playPauseFocusRequester
                                }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        lastInteractionTime = System.currentTimeMillis()
                                        when (keyEvent.key) {
                                            Key.DirectionRight -> true // Clamp
                                            Key.DirectionUp -> {
                                                profileFocusRequester.requestFocus()
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                timelineFocusRequester.requestFocus()
                                                true
                                            }
                                            Key.DirectionLeft -> {
                                                playPauseFocusRequester.requestFocus()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .tvRemoteFocusable(
                                    onClick = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        onToggleLike()
                                    },
                                    shape = RoundedCornerShape(50),
                                    focusScale = 1.08f
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Check else Icons.Default.Favorite,
                                    contentDescription = "Save",
                                    tint = if (isLiked) SpotifyGreenBright else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLiked) "Saved" else "Save",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLiked) SpotifyGreenBright else Color.White,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // TIMELINE & PLAY/PAUSE ROW (Spotify TV layout: IMG20261026111647.jpg)
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // White Circular Play/Pause Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .size(46.dp)
                                .focusRequester(playPauseFocusRequester)
                                .focusProperties {
                                    up = playingFromFocusRequester
                                    down = prevFocusRequester
                                    right = timelineFocusRequester
                                }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        lastInteractionTime = System.currentTimeMillis()
                                        when (keyEvent.key) {
                                            Key.DirectionLeft -> true // Clamp
                                            Key.DirectionRight -> {
                                                timelineFocusRequester.requestFocus()
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                playingFromFocusRequester.requestFocus()
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                prevFocusRequester.requestFocus()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .tvRemoteFocusable(
                                    onClick = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        onTogglePlay()
                                    },
                                    shape = CircleShape,
                                    focusScale = 1.15f
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Scrubber Bar & Timestamps (5s seek steps)
                        TvInteractiveProgressBar(
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onSeekForward = {
                                lastInteractionTime = System.currentTimeMillis()
                                viewModel.seekForward(5000L)
                            },
                            onSeekBackward = {
                                lastInteractionTime = System.currentTimeMillis()
                                viewModel.seekBackward(5000L)
                            },
                            onTogglePlay = {
                                lastInteractionTime = System.currentTimeMillis()
                                onTogglePlay()
                            },
                            focusRequester = timelineFocusRequester,
                            upFocusRequester = saveFocusRequester,
                            downFocusRequester = lyricsFocusRequester,
                            leftFocusRequester = playPauseFocusRequester,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ---------------------------------------------------------
                    // BOTTOM CONTROL BUTTONS ROW
                    // Left: Previous, Next, Shuffle, Repeat.
                    // Right: Lyrics, Queue, About, Dark mode.
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Group (Circular Buttons)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TvCircleControlButton(
                                icon = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    onPrevious()
                                },
                                modifier = Modifier
                                    .focusRequester(prevFocusRequester)
                                    .focusProperties {
                                        up = playPauseFocusRequester
                                        right = nextFocusRequester
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            lastInteractionTime = System.currentTimeMillis()
                                            when (keyEvent.key) {
                                                Key.DirectionDown, Key.DirectionLeft -> true // Clamp
                                                Key.DirectionUp -> {
                                                    playPauseFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    nextFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                            TvCircleControlButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    onNext()
                                },
                                modifier = Modifier
                                    .focusRequester(nextFocusRequester)
                                    .focusProperties {
                                        up = playPauseFocusRequester
                                        left = prevFocusRequester
                                        right = shuffleFocusRequester
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            lastInteractionTime = System.currentTimeMillis()
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> true // Clamp
                                                Key.DirectionUp -> {
                                                    playPauseFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionLeft -> {
                                                    prevFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    shuffleFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                            TvCircleControlButton(
                                icon = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) SpotifyGreenBright else Color.White,
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    onToggleShuffle()
                                },
                                modifier = Modifier
                                    .focusRequester(shuffleFocusRequester)
                                    .focusProperties {
                                        up = playPauseFocusRequester
                                        left = nextFocusRequester
                                        right = repeatFocusRequester
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            lastInteractionTime = System.currentTimeMillis()
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> true // Clamp
                                                Key.DirectionUp -> {
                                                    playPauseFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionLeft -> {
                                                    nextFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    repeatFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                            TvCircleControlButton(
                                icon = if (repeatMode == PlaybackRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeatMode != PlaybackRepeatMode.OFF) SpotifyGreenBright else Color.White,
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    onCycleRepeat()
                                },
                                modifier = Modifier
                                    .focusRequester(repeatFocusRequester)
                                    .focusProperties {
                                        up = timelineFocusRequester
                                        left = shuffleFocusRequester
                                        right = lyricsFocusRequester
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            lastInteractionTime = System.currentTimeMillis()
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> true // Clamp
                                                Key.DirectionUp -> {
                                                    timelineFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionLeft -> {
                                                    shuffleFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    lyricsFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                        }

                        // Right Group (Capsule Pill Buttons)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TvCapsulePillButton(
                                label = "Lyrics",
                                icon = Icons.Default.Mic,
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    showLyricsDialog = !showLyricsDialog
                                },
                                modifier = Modifier
                                    .focusRequester(lyricsFocusRequester)
                                    .focusProperties {
                                        up = timelineFocusRequester
                                        left = repeatFocusRequester
                                        right = queueFocusRequester
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            lastInteractionTime = System.currentTimeMillis()
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> true // Clamp
                                                Key.DirectionUp -> {
                                                    timelineFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionLeft -> {
                                                    repeatFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    queueFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                            TvCapsulePillButton(
                                label = "Queue",
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    onOpenQueue()
                                },
                                modifier = Modifier
                                    .focusRequester(queueFocusRequester)
                                    .focusProperties {
                                        up = timelineFocusRequester
                                        left = lyricsFocusRequester
                                        right = aboutFocusRequester
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            lastInteractionTime = System.currentTimeMillis()
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> true // Clamp
                                                Key.DirectionUp -> {
                                                    timelineFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionLeft -> {
                                                    lyricsFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    aboutFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                            TvCapsulePillButton(
                                label = "About",
                                icon = Icons.Default.Person,
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    showAboutDialog = true
                                },
                                modifier = Modifier
                                    .focusRequester(aboutFocusRequester)
                                    .focusProperties {
                                        up = timelineFocusRequester
                                        left = queueFocusRequester
                                        right = darkModeFocusRequester
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            lastInteractionTime = System.currentTimeMillis()
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> true // Clamp
                                                Key.DirectionUp -> {
                                                    timelineFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionLeft -> {
                                                    queueFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    darkModeFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                            TvCapsulePillButton(
                                label = "Dark mode",
                                icon = Icons.Default.DarkMode,
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isDarkScreenActive = !isDarkScreenActive
                                    darkToastMessage = if (isDarkScreenActive) "🌙 Dark mode is activated" else "🌙 Dark mode is deactivated"
                                },
                                modifier = Modifier
                                    .focusRequester(darkModeFocusRequester)
                                    .focusProperties {
                                        up = saveFocusRequester
                                        left = aboutFocusRequester
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            lastInteractionTime = System.currentTimeMillis()
                                            when (keyEvent.key) {
                                                Key.DirectionDown, Key.DirectionRight -> true // Clamp
                                                Key.DirectionUp -> {
                                                    saveFocusRequester.requestFocus()
                                                    true
                                                }
                                                Key.DirectionLeft -> {
                                                    aboutFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 4. TOP-RIGHT FLOATING TOAST (For Dark mode and feedback)
        // -----------------------------------------------------------------
        AnimatedVisibility(
            visible = darkToastMessage != null,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 36.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreenBright.copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = darkToastMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // 5. SPOTIFY TV-STYLE ABOUT DIALOG MODAL
        // -----------------------------------------------------------------
        if (showAboutDialog) {
            val closeFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(100)
                try {
                    closeFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showAboutDialog = false
                            }
                        )
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Back) {
                            showAboutDialog = false
                            true
                        } else false
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181818),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E)),
                    modifier = Modifier
                        .width(520.dp)
                        .padding(24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { /* consume taps inside modal */ }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Track Cover Art (large 120dp)
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF3E3E3E), RoundedCornerShape(12.dp))
                        ) {
                            TrackCoverArt(
                                albumArtPath = rec?.albumArtPath,
                                title = rec?.title ?: "Music",
                                artist = rec?.artist ?: "Artist",
                                size = 120.dp,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = rec?.title ?: "Unknown Song",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = rec?.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = SpotifyGreenBright,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF2A2A2A))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Metadata Details
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Album", color = Color(0xFF888888), fontSize = 13.sp)
                                Text(
                                    rec?.album ?: "Recordify Premium",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Audio Stream", color = Color(0xFF888888), fontSize = 13.sp)
                                Text("320 kbps • Lossless HD Audio", color = SpotifyGreenBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Playback Engine", color = Color(0xFF888888), fontSize = 13.sp)
                                Text("Media3 ExoPlayer Studio", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Close Button
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            modifier = Modifier
                                .focusRequester(closeFocusRequester)
                                .tvRemoteFocusable(
                                    onClick = { showAboutDialog = false },
                                    shape = RoundedCornerShape(24.dp),
                                    focusScale = 1.08f
                                )
                        ) {
                            Text(
                                text = "Close",
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvCircleControlButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF222222),
        modifier = modifier
            .size(40.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = CircleShape,
                focusScale = 1.15f
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TvCapsulePillButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF202020),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
        modifier = modifier
            .height(36.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(50),
                focusScale = 1.08f
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                )
            )
        }
    }
}
