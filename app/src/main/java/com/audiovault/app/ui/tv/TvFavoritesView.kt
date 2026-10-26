package com.audiovault.app.ui.tv

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Dedicated 10-Foot Favorites / Liked Songs Screen for Recordify TV.
 * Built according to Section 5 of the Spotify TV specification.
 */
@Composable
fun TvFavoritesView(
    viewModel: RecordingsViewModel,
    onPlaySong: (OnlineSongModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState(initial = emptyList())
    val likedSongIds by viewModel.likedSongIds.collectAsState()

    val likedSongs = recordings.filter { likedSongIds.contains(it.id) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE91E63).copy(alpha = 0.2f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Favorite Tracks",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${likedSongs.size} songs saved to your private vault",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888))
                        )
                    }
                }

                if (likedSongs.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val first = likedSongs.first()
                                onPlaySong(
                                    OnlineSongModel(
                                        id = first.id.toString(),
                                        title = first.title,
                                        artist = first.artist ?: "Unknown",
                                        streamUrl = first.filePath,
                                        highResArtUrl = first.albumArtPath
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreenBright),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF222222),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                            modifier = Modifier.tvRemoteFocusable(
                                onClick = {
                                    val shuffled = likedSongs.shuffled()
                                    if (shuffled.isNotEmpty()) {
                                        val first = shuffled.first()
                                        onPlaySong(
                                            OnlineSongModel(
                                                id = first.id.toString(),
                                                title = first.title,
                                                artist = first.artist ?: "Unknown",
                                                streamUrl = first.filePath,
                                                highResArtUrl = first.albumArtPath
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        if (likedSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Favorite Songs Yet",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the heart icon on any song while playing or browsing to add it here.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888))
                        )
                    }
                }
            }
        } else {
            itemsIndexed(likedSongs) { index, rec ->
                val songModel = OnlineSongModel(
                    id = rec.id.toString(),
                    title = rec.title,
                    artist = rec.artist ?: "Unknown",
                    streamUrl = rec.filePath,
                    highResArtUrl = rec.albumArtPath
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF161616),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvRemoteFocusable(
                            onClick = { onPlaySong(songModel) },
                            shape = RoundedCornerShape(10.dp),
                            focusScale = 1.02f
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF888888),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(32.dp)
                        )

                        TrackCoverArt(
                            albumArtPath = rec.albumArtPath,
                            title = rec.title,
                            artist = rec.artist ?: "Unknown",
                            size = 46.dp,
                            shape = RoundedCornerShape(6.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = rec.artist ?: "Recordify Studio",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = formatTvDuration(rec.durationMs),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888)),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorited",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
