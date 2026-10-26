package com.audiovault.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.core.queue.PlaybackQueueState
import com.audiovault.app.core.queue.PlaybackRepeatMode
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Full Now Playing Cinema Stage Overlay for Recordify TV.
 * Built according to Section 10 of the Spotify TV specification.
 * Fully synchronized with the shared playback engine and bottom mini-player.
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
    onBack: () -> Unit = {}
) {
    val rec = playerState.currentRecording
    val positionMs = playerState.currentPositionMs
    val durationMs = (playerState.durationMs).coerceAtLeast(1L)
    var showLyrics by remember { mutableStateOf(false) }

    val playPauseFocusRequester = remember { FocusRequester() }

    // Auto-focus the Big Master Play/Pause button for effortless TV remote control
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        try {
            playPauseFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Cinema Header Bar (Clean Minimal Back Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Focusable Back Pill Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1C1C1C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.tvRemoteFocusable(
                        onClick = onBack,
                        shape = CircleShape,
                        focusScale = 1.05f
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // 2. Main Stage: Left Grand Full Cover Page, Right Track Deck & Interactive Scrubber
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Grand Full Square Album Cover Page
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(280.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF282828), RoundedCornerShape(16.dp))
                    ) {
                        TrackCoverArt(
                            albumArtPath = rec?.albumArtPath,
                            title = rec?.title ?: "Music",
                            artist = rec?.artist ?: "Artist",
                            size = 280.dp,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF18221B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreenBright.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "RECORDIFY STUDIO MASTER • 24-BIT FLAC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SpotifyGreenBright,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Right Column: Track Metadata, Scrubber, Controls, Lyrics & Up-Next Shelf
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showLyrics) {
                        // Lyrics Overlay View
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF141414),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "LYRICS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SpotifyGreenBright,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = rec?.lyrics?.ifBlank { null }
                                        ?: "Synchronized lyrics are not available for this track.\nEnjoy the pure lossless master on Recordify TV.",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.White,
                                        lineHeight = 24.sp
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Streaming Overline Pill
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SpotifyGreenBright))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NOW PLAYING ON RECORDIFY TV",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SpotifyGreenBright,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = rec?.title ?: "No Song Playing",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 30.sp,
                                lineHeight = 36.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = rec?.artist ?: "Select any song from Home, Search, or Library to begin",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFB3B3B3),
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Interactive TV Progress Scrubber (D-Pad Left/Right seek)
                    TvInteractiveProgressBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeekForward = { viewModel.seekForward(10000L) },
                        onSeekBackward = { viewModel.seekBackward(10000L) },
                        onTogglePlay = onTogglePlay
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Remote D-Pad Navigation Controls Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TvCircleControl(
                            icon = Icons.Default.Shuffle,
                            tint = if (isShuffle) SpotifyGreenBright else Color(0xFFB3B3B3),
                            size = 42.dp,
                            onClick = onToggleShuffle
                        )

                        TvCircleControl(
                            icon = Icons.Default.SkipPrevious,
                            tint = Color.White,
                            size = 44.dp,
                            onClick = onPrevious
                        )

                        TvCircleControl(
                            icon = Icons.Default.FastRewind,
                            tint = Color.White,
                            size = 44.dp,
                            onClick = { viewModel.seekBackward(10000L) }
                        )

                        // Big Master Play/Pause Dial (Auto-focused for remote control with halo shadow)
                        Surface(
                            shape = CircleShape,
                            color = SpotifyGreenBright,
                            modifier = Modifier
                                .size(66.dp)
                                .tvRemoteFocusable(
                                    onClick = onTogglePlay,
                                    shape = CircleShape,
                                    focusScale = 1.08f,
                                    focusRequester = playPauseFocusRequester
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        TvCircleControl(
                            icon = Icons.Default.FastForward,
                            tint = Color.White,
                            size = 44.dp,
                            onClick = { viewModel.seekForward(10000L) }
                        )

                        TvCircleControl(
                            icon = Icons.Default.SkipNext,
                            tint = Color.White,
                            size = 44.dp,
                            onClick = onNext
                        )

                        TvCircleControl(
                            icon = if (repeatMode == PlaybackRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            tint = if (repeatMode != PlaybackRepeatMode.OFF) SpotifyGreenBright else Color(0xFFB3B3B3),
                            size = 42.dp,
                            onClick = onCycleRepeat
                        )

                        TvCircleControl(
                            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            tint = if (isLiked) Color(0xFFE91E63) else Color(0xFFB3B3B3),
                            size = 42.dp,
                            onClick = onToggleLike
                        )

                        TvCircleControl(
                            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                            tint = Color.White,
                            size = 42.dp,
                            onClick = onOpenQueue
                        )

                        TvCircleControl(
                            icon = Icons.Default.Lyrics,
                            tint = if (showLyrics) SpotifyGreenBright else Color(0xFFB3B3B3),
                            size = 42.dp,
                            onClick = { showLyrics = !showLyrics }
                        )
                    }

                    // Up-Next Queue Preview or Recommended Tracks
                    val homeFeedState by viewModel.homeFeed.collectAsState()
                    val upcomingItems = queueState.upcomingQueue.take(8)
                    val displayList = if (upcomingItems.isNotEmpty()) {
                        upcomingItems.map { up ->
                            OnlineSongModel(
                                id = up.id,
                                title = up.title,
                                artist = up.artist,
                                streamUrl = up.streamUrl,
                                highResArtUrl = up.albumArtPath
                            )
                        }
                    } else {
                        homeFeedState.trendingNow.ifEmpty { homeFeedState.quickTiles }.take(8)
                    }

                    if (displayList.isNotEmpty() && !showLyrics) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (upcomingItems.isNotEmpty()) "UP NEXT IN RECORDIFY VAULT (${queueState.upcomingQueue.size})" else "RECOMMENDED IN YOUR COUNTRY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF888888),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(displayList) { songItem ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF181818),
                                    modifier = Modifier
                                        .width(190.dp)
                                        .tvRemoteFocusable(
                                            onClick = { onPlaySong(songItem) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TrackCoverArt(
                                            albumArtPath = songItem.highResArtUrl,
                                            title = songItem.title,
                                            artist = songItem.artist,
                                            size = 38.dp,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = songItem.title,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = songItem.artist,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF888888),
                                                    fontSize = 9.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
