package com.audiovault.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * 10-Foot TV Album & Playlist Detail Experience for Recordify TV.
 * Built according to Section 12 of the Spotify TV specification:
 * - Large square album artwork with atmospheric gradient.
 * - Primary Play and Shuffle buttons.
 * - Complete remote-navigable track list with immediate playback.
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF162A1D),
                        Color(0xFF0F1813),
                        Color(0xFF0C0C0C)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Top Bar: Focusable Back Button
            item {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E1E1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.tvRemoteFocusable(
                        onClick = onBack,
                        shape = CircleShape,
                        focusScale = 1.1f
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
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

            // 2. Grand Album Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .shadow(20.dp, RoundedCornerShape(14.dp), spotColor = SpotifyGreenBright.copy(alpha = 0.35f))
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0xFF2C2C2C), RoundedCornerShape(14.dp))
                    ) {
                        TrackCoverArt(
                            albumArtPath = album.coverArtUrl,
                            title = album.title,
                            artist = album.primaryArtists ?: "Recordify",
                            size = 190.dp,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SpotifyGreenBright.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreenBright.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "ALBUM • RECORDIFY VAULT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SpotifyGreenBright,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 32.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${album.primaryArtists ?: "Recordify Vault"} • ${album.year ?: "Studio Master"} • ${trackList.size} Songs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFCCCCCC),
                                fontSize = 15.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Studio Master Soundtrack • ${album.type} • Lossless Audio",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Buttons: Play All & Shuffle
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Button(
                                onClick = {
                                    viewModel.playAlbumSoundtrack(album, startIndex = 0)
                                    onTrackPlayed()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreenBright),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Album", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF222222),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                                modifier = Modifier
                                    .height(44.dp)
                                    .tvRemoteFocusable(
                                        onClick = {
                                            viewModel.toggleShuffle()
                                            viewModel.playAlbumSoundtrack(album, startIndex = 0)
                                            onTrackPlayed()
                                        },
                                        shape = RoundedCornerShape(24.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Track List Header
            item {
                Text(
                    text = "TRACKS IN THIS SOUNDTRACK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // 4. Track List Items
            itemsIndexed(trackList) { index, song ->
                val isCurrentPlaying = playerState.currentRecording?.title?.equals(song.title, ignoreCase = true) == true
                val isLiked = likedSongIds.contains(song.id.hashCode().toLong())

                TvTrackRow(
                    index = index,
                    song = song,
                    isPlaying = isCurrentPlaying && playerState.isPlaying,
                    isLiked = isLiked,
                    onPlay = {
                        viewModel.playAlbumSoundtrack(album, startIndex = index)
                        onTrackPlayed()
                    },
                    onToggleLike = {
                        viewModel.toggleLikedSong(song.id.hashCode().toLong())
                    }
                )
            }
        }
    }
}
