package com.audiovault.app.ui.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.queue.PlaybackRepeatMode
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Persistent Fixed Bottom Mini-Player Bar for Recordify TV.
 * Built strictly according to Section 9 of the Spotify TV Specification:
 * - Remains stationary across all primary navigation screens (Home, Search, Library, Playlists, etc.).
 * - Left Region: Artwork, title, artist, favorite toggle (clicking opens Full Player).
 * - Center Region: Shuffle, Previous, Master Play/Pause Dial, Next, Repeat.
 * - Right Region: Elapsed/Total Duration, D-Pad interactive seek scrubber, Queue button, Fullscreen button.
 */
@Composable
fun TvMiniPlayerBar(
    title: String,
    artist: String,
    artUrl: String?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: PlaybackRepeatMode,
    isLiked: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onOpenQueue: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progressFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        color = Color(0xFF141414),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // -------------------------------------------------------------
            // 1. LEFT REGION: CURRENT TRACK METADATA & FAVORITE BUTTON
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(RoundedCornerShape(10.dp))
                    .tvRemoteFocusable(
                        onClick = onExpand,
                        shape = RoundedCornerShape(10.dp),
                        focusScale = 1.04f
                    )
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackCoverArt(
                    albumArtPath = artUrl,
                    title = title,
                    artist = artist,
                    size = 52.dp,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Favorite Toggle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .tvRemoteFocusable(
                            onClick = onToggleLike,
                            shape = CircleShape,
                            focusScale = 1.15f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFE91E63) else Color(0xFF888888),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // -------------------------------------------------------------
            // 2. CENTER REGION: PLAYBACK CONTROLS
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.weight(1.0f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Shuffle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .tvRemoteFocusable(
                            onClick = onToggleShuffle,
                            shape = CircleShape,
                            focusScale = 1.15f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) SpotifyGreenBright else Color(0xFF888888),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Previous
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .tvRemoteFocusable(
                            onClick = onPrevious,
                            shape = CircleShape,
                            focusScale = 1.15f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Master Play/Pause Dial (Prominent emerald button)
                Surface(
                    shape = CircleShape,
                    color = SpotifyGreenBright,
                    modifier = Modifier
                        .size(48.dp)
                        .tvRemoteFocusable(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            focusScale = 1.08f
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Next
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .tvRemoteFocusable(
                            onClick = onNext,
                            shape = CircleShape,
                            focusScale = 1.15f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Repeat
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .tvRemoteFocusable(
                            onClick = onCycleRepeat,
                            shape = CircleShape,
                            focusScale = 1.15f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (repeatMode == PlaybackRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != PlaybackRepeatMode.OFF) SpotifyGreenBright else Color(0xFF888888),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // -------------------------------------------------------------
            // 3. RIGHT REGION: SEEKBAR, TIME, QUEUE & FULLSCREEN BUTTONS
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.weight(1.3f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Interactive Mini Scrubber Bar
                var isScrubberFocused by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isScrubberFocused = it.isFocused }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionRight -> {
                                        onSeekForward()
                                        true
                                    }
                                    Key.DirectionLeft -> {
                                        onSeekBackward()
                                        true
                                    }
                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                        onTogglePlay()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .tvRemoteFocusable(
                            onClick = onTogglePlay,
                            shape = RoundedCornerShape(8.dp),
                            focusScale = 1.02f,
                            focusBorderColor = SpotifyGreenBright.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    // Track Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2E2E2E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progressFraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isScrubberFocused) SpotifyGreenBright else Color(0xFFCCCCCC)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTvDuration(positionMs),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isScrubberFocused) SpotifyGreenBright else Color(0xFF888888),
                                fontSize = 11.sp
                            )
                        )
                        if (isScrubberFocused) {
                            Text(
                                text = "◀ -10s | +10s ▶",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SpotifyGreenBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            text = formatTvDuration(safeDuration),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF888888),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Queue Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .tvRemoteFocusable(
                            onClick = onOpenQueue,
                            shape = CircleShape,
                            focusScale = 1.15f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = "Playback Queue",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Fullscreen Now Playing Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .tvRemoteFocusable(
                            onClick = onExpand,
                            shape = CircleShape,
                            focusScale = 1.15f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Expand Full Player",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
