package com.audiovault.app.ui.player

import com.audiovault.app.ui.components.TrackCoverArt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Full-Screen Spotify Player overlay with proper status bar insets, balanced layout, and shuffle support.
 */
@Composable
fun SpotifyFullScreenPlayer(
    playerState: PlayerState,
    isShuffleActive: Boolean,
    isLiked: Boolean = false,
    playlists: List<com.audiovault.app.data.repository.UserPlaylist> = emptyList(),
    onToggleLiked: () -> Unit = {},
    onTogglePlaylist: (String) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onToggleShuffle: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recording = playerState.currentRecording ?: return

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPosition by remember { mutableFloatStateOf(0f) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    val isSavedAnywhere = isLiked || playlists.any { it.songIds.contains(recording.id) }

    val currentPosition = if (isDraggingSlider) {
        sliderDragPosition.toLong()
    } else {
        playerState.currentPositionMs
    }

    // Spotify warm amber-dark gradient background
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Consume touch events so background screen is not touchable */ }
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF5D2800), // Rich amber brown top
                        Color(0xFF331400),
                        Color(0xFF180A02),
                        Color(0xFF121212)  // Dark Spotify base
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header: Down Arrow, Header Title, 3-Dots Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM RECORDINGS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    )
                    Text(
                        text = "Recent Captures",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                IconButton(onClick = { /* Menu options */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Large Album Artwork Card (Adaptive, centered with high-res cover art)
            TrackCoverArt(
                albumArtPath = recording.albumArtPath,
                title = recording.title,
                artist = recording.artist,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(14.dp),
                placeholderIconSize = 88.dp,
                contentDescription = recording.title
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Song Title, Artist & Plus Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recording.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFB3B3B3),
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showPlaylistSheet = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isSavedAnywhere) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        contentDescription = "Save to playlist",
                        tint = if (isSavedAnywhere) SpotifyGreenBright else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Scrub Bar Slider & Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                @OptIn(ExperimentalMaterial3Api::class)
                Slider(
                    value = currentPosition.toFloat().coerceIn(0f, playerState.durationMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = {
                        isDraggingSlider = true
                        sliderDragPosition = it
                    },
                    onValueChangeFinished = {
                        onSeekTo(sliderDragPosition.toLong())
                        isDraggingSlider = false
                    },
                    valueRange = 0f..playerState.durationMs.toFloat().coerceAtLeast(1f),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.height(4.dp)
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFB3B3B3),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = formatDuration(playerState.durationMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFB3B3B3),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Main Playback Controls: Shuffle, Prev, Big Play/Pause, Next, Speed/Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle (Toggles random track progression)
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleActive) SpotifyGreenBright else Color(0xFFB3B3B3),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Track
                IconButton(onClick = onPlayPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Center Large Play/Pause Button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(68.dp),
                    onClick = onTogglePlayPause
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Next Track (Plays next sequential or random if shuffle on)
                IconButton(onClick = onPlayNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Speed / Timer Menu
                Box {
                    IconButton(onClick = { speedMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Playback Speed",
                            tint = if (playerState.playbackSpeed != 1.0f) SpotifyGreenBright else Color(0xFFB3B3B3),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = speedMenuExpanded,
                        onDismissRequest = { speedMenuExpanded = false }
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x Speed") },
                                onClick = {
                                    speedMenuExpanded = false
                                    onSpeedSelected(speed)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Bottom Device & Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recordify HD • ${recording.bitrateKbps}kbps",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SpotifyGreenBright,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFFB3B3B3),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { /* Queue */ }) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color(0xFFB3B3B3),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        if (showPlaylistSheet) {
            com.audiovault.app.ui.components.AddToPlaylistBottomSheet(
                recording = recording,
                isLiked = isLiked,
                playlists = playlists,
                onToggleLiked = onToggleLiked,
                onTogglePlaylist = onTogglePlaylist,
                onCreatePlaylist = onCreatePlaylist,
                onDismiss = { showPlaylistSheet = false }
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
