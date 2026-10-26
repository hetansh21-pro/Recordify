package com.audiovault.app.ui.tv

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Interactive Seek Timeline Bar for Android TV Remote.
 * Supports D-Pad Left (-10s) and Right (+10s) navigation with glowing scrubber thumb.
 */
@Composable
fun TvInteractiveProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progressFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    val thumbScale by animateFloatAsState(
        targetValue = if (isFocused) 1.25f else 0.85f,
        label = "thumb_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
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
                shape = RoundedCornerShape(12.dp),
                focusScale = 1.02f,
                focusBorderWidth = 1.5.dp,
                focusBorderColor = SpotifyGreenBright.copy(alpha = 0.6f)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Timeline Track with Animated Scrubber Thumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 16.dp else 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) 8.dp else 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF282828))
            )

            // Active progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progressFraction)
                    .height(if (isFocused) 8.dp else 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SpotifyGreenBright.copy(alpha = 0.8f),
                                SpotifyGreenBright
                            )
                        )
                    )
            )

            // Scrubber Thumb (glowing on remote focus)
            Box(
                modifier = Modifier.fillMaxWidth(fraction = progressFraction)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isFocused) 3.dp else 2.dp,
                        SpotifyGreenBright
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(if (isFocused) 16.dp else 12.dp)
                        .scale(thumbScale)
                        .shadow(if (isFocused) 8.dp else 2.dp, shape = CircleShape, spotColor = SpotifyGreenBright)
                ) {}
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Time labels and remote hints
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTvDuration(positionMs),
                color = if (isFocused) SpotifyGreenBright else Color(0xFF888888),
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )

            if (isFocused) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SpotifyGreenBright.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreenBright.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "◀ D-Pad: Left (-10s) • Right (+10s) ▶",
                        color = SpotifyGreenBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = formatTvDuration(safeDuration),
                color = if (isFocused) SpotifyGreenBright else Color(0xFF888888),
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Animated 7-bar audio frequency visualizer.
 */
@Composable
fun TvEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = SpotifyGreenBright
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    val heights = List(7) { i ->
        if (isPlaying) {
            infiniteTransition.animateFloat(
                initialValue = 0.22f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 320 + (i * 85), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eq_bar_$i"
            ).value
        } else {
            0.22f
        }
    }

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((18 * fraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Remote-focusable circular control button with halo scale and border.
 */
@Composable
fun TvCircleControl(
    icon: ImageVector,
    tint: Color,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF222222),
        modifier = modifier
            .size(size)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = CircleShape,
                focusScale = 1.15f
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/**
 * Section Header for horizontal TV shelves.
 */
@Composable
fun TvShelfHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            )
        }
    }
}

/**
 * Horizontal Shelf Song Card (10-foot TV optimized).
 */
@Composable
fun TvSongCard(
    song: OnlineSongModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161616),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
        modifier = modifier
            .width(170.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                focusScale = 1.08f
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                TrackCoverArt(
                    albumArtPath = song.highResArtUrl,
                    title = song.title,
                    artist = song.artist,
                    size = 150.dp,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Horizontal Shelf Album / Playlist Card.
 */
@Composable
fun TvAlbumCard(
    album: OnlineAlbumModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161616),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
        modifier = modifier
            .width(180.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                focusScale = 1.08f
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                TrackCoverArt(
                    albumArtPath = album.coverArtUrl,
                    title = album.title,
                    artist = album.primaryArtists ?: "Recordify",
                    size = 160.dp,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = album.title,
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
                text = album.primaryArtists ?: "Recordify Vault",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Circular Artist Card (Spotify TV design: IMG20261026111721.jpg & IMG20261026111737.jpg).
 */
@Composable
fun TvCircularArtistCard(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier
            .width(160.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                focusScale = 1.08f
            )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF282828), CircleShape)
            ) {
                TrackCoverArt(
                    albumArtPath = imageUrl,
                    title = name,
                    artist = "Artist",
                    size = 136.dp,
                    shape = CircleShape
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = name,
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
                text = "Artist",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Colored Mood or Genre Card for Discovery.
 */
@Composable
fun TvMoodCard(
    title: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .width(150.dp)
            .height(80.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                focusScale = 1.08f
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 15.sp
                )
            )
        }
    }
}

/**
 * Track Row for Album Detail & Playlists.
 */
@Composable
fun TvTrackRow(
    index: Int,
    song: OnlineSongModel,
    isPlaying: Boolean,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isPlaying) SpotifyGreenBright.copy(alpha = 0.12f) else Color(0xFF141414),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) SpotifyGreenBright.copy(alpha = 0.4f) else Color(0xFF242424)
        ),
        modifier = modifier
            .fillMaxWidth()
            .tvRemoteFocusable(
                onClick = onPlay,
                shape = RoundedCornerShape(10.dp),
                focusScale = 1.02f
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    TvEqualizerBars(isPlaying = true)
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF888888),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            TrackCoverArt(
                albumArtPath = song.highResArtUrl,
                title = song.title,
                artist = song.artist,
                size = 46.dp,
                shape = RoundedCornerShape(6.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isPlaying) SpotifyGreenBright else Color.White,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatTvDuration(song.durationMs),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onToggleLike() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFE91E63) else Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Helper to format milliseconds to mm:ss.
 */
fun formatTvDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
