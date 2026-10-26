package com.audiovault.app.ui.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.components.rememberAsyncCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * 10-Foot TV Album & Playlist Detail Experience for Recordify TV.
 * Authentically matches Spotify TV design (Reference Photos: IMG20261026111624.jpg & IMG20261026111634.jpg):
 * - Atmospheric artist backdrop image in background.
 * - Left stationary column: Large artwork, bold title, Recordify subtitle, description, shuffle/add/Play buttons.
 * - Right scrollable column: Vertical track list with focused translucent pill highlight, thumbnail white border, and quick action buttons.
 */
@Composable
fun TvAlbumDetailView(
    album: OnlineAlbumModel,
    viewModel: RecordingsViewModel,
    onBack: () -> Unit,
    onTrackPlayed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsState()
    val likedSongIds by viewModel.likedSongIds.collectAsState()

    // Pre-seed track queries to OnlineSongModel list if songs list is empty
    val trackList: List<OnlineSongModel> = if (album.songs.isNotEmpty()) {
        album.songs
    } else {
        album.trackQueries.mapIndexed { idx, query ->
            OnlineSongModel(
                id = "${album.id}_track_$idx",
                title = query.substringBefore(" - ").ifBlank { query },
                artist = if (query.contains(" - ")) query.substringAfter(" - ") else (album.primaryArtists ?: "Recordify"),
                streamUrl = "",
                highResArtUrl = album.coverArtUrl
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
            .pointerInput(Unit) {
                detectTapGestures { /* Touch isolation: consume all taps so they never leak */ }
            }
    ) {
        // Atmospheric background artist image with heavy dark gradient
        val backdropBitmap by rememberAsyncCoverArt(
            albumArtPath = album.coverArtUrl,
            title = album.title,
            artist = album.primaryArtists,
            targetSizeDp = 400.dp
        )
        if (backdropBitmap != null) {
            Image(
                bitmap = backdropBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.15f,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Two-Column Split View (Left: Header, Right: Tracks)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 12.dp)
        ) {
            // -------------------------------------------------------------
            // 1. LEFT STATIONARY COLUMN (Cover Art, Title, Info, Action Buttons)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // Large Square Cover Art
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                ) {
                    TrackCoverArt(
                        albumArtPath = album.coverArtUrl,
                        title = album.title,
                        artist = album.primaryArtists ?: "Recordify",
                        size = 210.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bold Title
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 28.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Recordify Subtitle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SpotifyGreenBright,
                        modifier = Modifier.size(12.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recordify • Playlist • ${trackList.size} songs",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = "Let these songs be the background score to your love story. Cover - Saiyaara",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row: Shuffle, Add, and Big Green Play Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Shuffle button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF222222),
                        modifier = Modifier
                            .size(44.dp)
                            .tvRemoteFocusable(
                                onClick = {
                                    viewModel.toggleShuffle()
                                    if (trackList.isNotEmpty()) {
                                        val albumToPlay = album.copy(songs = trackList)
                                        val randomIdx = trackList.indices.random()
                                        viewModel.playAlbumSoundtrack(albumToPlay, startIndex = randomIdx)
                                        onTrackPlayed()
                                    }
                                },
                                shape = CircleShape,
                                focusScale = 1.12f
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Add to Library button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF222222),
                        modifier = Modifier
                            .size(44.dp)
                            .tvRemoteFocusable(
                                onClick = {
                                    trackList.firstOrNull()?.let { firstSong ->
                                        viewModel.toggleLikedSong(firstSong.id.hashCode().toLong(), null)
                                    }
                                },
                                shape = CircleShape,
                                focusScale = 1.12f
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add to Library",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Large Spotify-Green Master Play Button
                    Surface(
                        shape = CircleShape,
                        color = SpotifyGreenBright,
                        modifier = Modifier
                            .size(54.dp)
                            .tvRemoteFocusable(
                                onClick = {
                                    if (trackList.isNotEmpty()) {
                                        val albumToPlay = album.copy(songs = trackList)
                                        viewModel.playAlbumSoundtrack(albumToPlay, startIndex = 0)
                                        onTrackPlayed()
                                    }
                                },
                                shape = CircleShape,
                                focusScale = 1.12f
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play All",
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(36.dp))

            // -------------------------------------------------------------
            // 2. RIGHT SCROLLABLE COLUMN (Vertical Track List)
            // -------------------------------------------------------------
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(trackList) { index, song ->
                    TvSpotifyTrackRow(
                        index = index,
                        song = song,
                        isPlaying = playerState.currentRecording?.title.equals(song.title, ignoreCase = true) && playerState.isPlaying,
                        isLiked = likedSongIds.contains(song.id.hashCode().toLong()),
                        onPlay = {
                            val albumToPlay = album.copy(songs = trackList)
                            viewModel.playAlbumSoundtrack(albumToPlay, startIndex = index)
                            onTrackPlayed()
                        },
                        onToggleLike = {
                            viewModel.toggleLikedSong(song.id.hashCode().toLong(), null)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Authentic Spotify TV Track Row (Reference: IMG20261026111634.jpg).
 * Features translucent pill highlight on remote focus, white thumbnail border, and quick actions.
 */
@Composable
private fun TvSpotifyTrackRow(
    index: Int,
    song: OnlineSongModel,
    isPlaying: Boolean,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isFocused) Color.White.copy(alpha = 0.12f) else if (isPlaying) SpotifyGreenBright.copy(alpha = 0.08f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 1.5.dp else 0.dp,
            color = if (isFocused) Color.White.copy(alpha = 0.6f) else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .tvRemoteFocusable(
                onClick = onPlay,
                shape = RoundedCornerShape(10.dp),
                focusScale = 1.02f
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail image with white border when focused
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        width = if (isFocused) 1.5.dp else 0.5.dp,
                        color = if (isFocused) Color.White else Color(0xFF282828),
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                TrackCoverArt(
                    albumArtPath = song.highResArtUrl,
                    title = song.title,
                    artist = song.artist,
                    size = 46.dp,
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Artists
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFA0A0A0),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Focused quick action buttons (+) and (•••)
            if (isFocused) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onToggleLike() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "Save to Library",
                            tint = if (isLiked) SpotifyGreenBright else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else if (isPlaying) {
                TvEqualizerBars(
                    isPlaying = true,
                    modifier = Modifier.size(width = 16.dp, height = 14.dp),
                    color = SpotifyGreenBright
                )
            }
        }
    }
}
