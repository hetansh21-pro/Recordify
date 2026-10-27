package com.audiovault.app.ui.album

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.components.SwipeToQueueContainer
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyGreenBright

@Composable
fun AlbumDetailScreen(
    album: OnlineAlbumModel,
    viewModel: RecordingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val playerState by viewModel.playerState.collectAsState()
    val isAlbumLoading by viewModel.isAlbumLoading.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val albumDownloadProgress by viewModel.albumDownloadProgress.collectAsState()
    val selectedAlbumState by viewModel.selectedAlbum.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentAlbum = (selectedAlbumState?.takeIf { it.id == album.id }) ?: album

    val isAlbumDownloading = albumDownloadProgress.containsKey(currentAlbum.id)
    val albumProgress = albumDownloadProgress[currentAlbum.id] ?: 0f

    val currentPlayingTitle = playerState.currentRecording?.title
    val isPlaying = playerState.isPlaying

    // Vibrant moody gradient background
    val gradientTopColor = Color(0xFF332338)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            gradientTopColor,
            Color(0xFF1E1424),
            SpotifyBlack,
            SpotifyBlack
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Top App Bar with Back Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Large Central Album Cover
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .shadow(elevation = 20.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black)
                    ) {
                        TrackCoverArt(
                            albumArtPath = currentAlbum.coverArtUrl,
                            title = currentAlbum.title,
                            artist = currentAlbum.artist,
                            size = 240.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Album Title
                    Text(
                        text = currentAlbum.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Artist Chips / Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrackCoverArt(
                            albumArtPath = currentAlbum.coverArtUrl,
                            title = currentAlbum.title,
                            artist = currentAlbum.artist,
                            size = 24.dp,
                            shape = CircleShape
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentAlbum.primaryArtists ?: currentAlbum.artist ?: "Various Artists",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle: Album • Date / Year
                    val typeStr = currentAlbum.type
                    val dateStr = currentAlbum.releaseDate ?: currentAlbum.year ?: "Soundtrack"
                    val countStr = if (currentAlbum.songs.isNotEmpty()) " • ${currentAlbum.songs.size} songs" else ""
                    Text(
                        text = "$typeStr • $dateStr$countStr",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Controls Row: Add, Download, More, Shuffle, Big Play Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Add button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color(0xFFB3B3B3), CircleShape)
                                    .clickable { },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to library",
                                    tint = Color(0xFFB3B3B3),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Download Full Album button
                            if (isAlbumDownloading) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.downloadFullAlbum(currentAlbum) }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { albumProgress },
                                        modifier = Modifier.size(28.dp),
                                        color = SpotifyGreenBright,
                                        strokeWidth = 2.dp,
                                        trackColor = Color(0x33FFFFFF)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Album Download",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color(0xFFB3B3B3), CircleShape)
                                        .clickable { viewModel.downloadFullAlbum(currentAlbum) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Download Album",
                                        tint = Color(0xFFB3B3B3),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // More 3 dots
                            IconButton(
                                onClick = { },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color(0xFFB3B3B3),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Shuffle Toggle
                            IconButton(
                                onClick = { viewModel.toggleShuffle() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleEnabled) SpotifyGreenBright else Color(0xFFB3B3B3),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Big Spotify Green Play / Pause Button
                            val isThisAlbumPlaying = currentAlbum.songs.any { it.title.equals(currentPlayingTitle, ignoreCase = true) } && isPlaying
                            FloatingActionButton(
                                onClick = {
                                    if (isThisAlbumPlaying) {
                                        viewModel.playerEngine.togglePlayPause()
                                    } else {
                                        viewModel.playAlbumSoundtrack(currentAlbum, 0)
                                    }
                                },
                                containerColor = SpotifyGreenBright,
                                contentColor = Color.Black,
                                shape = CircleShape,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    imageVector = if (isThisAlbumPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isThisAlbumPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Loading State
            if (isAlbumLoading && currentAlbum.songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SpotifyGreenBright,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else if (!isAlbumLoading && currentAlbum.songs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No songs loaded for this collection",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF888888))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SpotifyGreenBright,
                            modifier = Modifier.clickable { viewModel.openAlbumDetails(currentAlbum) }
                        ) {
                            Text(
                                text = "Retry Loading Songs",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Tracklist
            itemsIndexed(currentAlbum.songs, key = { index, song -> "${song.id}_$index" }) { index, track ->
                val isTrackPlaying = track.title.equals(currentPlayingTitle, ignoreCase = true) && isPlaying
                val isTrackSaved = viewModel.isOnlineSongSavedInLibrary(track)
                val isTrackDownloading = viewModel.downloadingSongIds.collectAsState().value.contains(track.id)
                val trackProgress = viewModel.downloadProgress.collectAsState().value[track.id] ?: 0f

                AlbumTrackRow(
                    track = track,
                    isPlaying = isTrackPlaying,
                    isSavedInLibrary = isTrackSaved,
                    isDownloading = isTrackDownloading,
                    downloadProgress = trackProgress,
                    onClick = { viewModel.playAlbumSoundtrack(currentAlbum, index) },
                    onDownload = { viewModel.downloadOnlineSong(track) },
                    onCancelDownload = { viewModel.cancelDownload(track.id) },
                    onSwipeToQueue = {
                        viewModel.addSongToQueue(track)
                        android.widget.Toast.makeText(context, "Added to queue", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun AlbumTrackRow(
    track: OnlineSongModel,
    isPlaying: Boolean,
    isSavedInLibrary: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    onClick: () -> Unit,
    onDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onSwipeToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SwipeToQueueContainer(
        onSwipeToQueue = { onSwipeToQueue?.invoke() },
        enabled = onSwipeToQueue != null,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (isPlaying) "... ${track.title}" else track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPlaying) SpotifyGreenBright else Color.White,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB3B3B3),
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Download / Saved / Cancel Action
        if (isDownloading) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onCancelDownload() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { downloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(20.dp),
                    color = SpotifyGreenBright,
                    strokeWidth = 2.dp,
                    trackColor = Color(0x33FFFFFF)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Download",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        } else if (isSavedInLibrary) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                contentDescription = "Downloaded",
                tint = SpotifyGreenBright,
                modifier = Modifier
                    .size(32.dp)
                    .padding(6.dp)
            )
        } else {
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Download,
                    contentDescription = "Download Song",
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(
            onClick = { /* Track Options Menu */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFFB3B3B3),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
}
