package com.audiovault.app.ui.tv

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
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
 * Dedicated 10-Foot Recently Played Screen for Recordify TV.
 * Displays real listening history from the local database.
 */
@Composable
fun TvRecentlyPlayedView(
    viewModel: RecordingsViewModel,
    onPlaySong: (OnlineSongModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentSearches by viewModel.recentSearches.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TvShelfHeader(
                    title = "Recently Played",
                    subtitle = "Real listening history from your Recordify TV session"
                )

                if (recentSearches.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E1E1E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.tvRemoteFocusable(
                            onClick = { viewModel.clearRecentSearches() },
                            shape = RoundedCornerShape(14.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear History",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE91E63)
                                )
                            )
                        }
                    }
                }
            }
        }

        if (recentSearches.isEmpty()) {
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
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.History, contentDescription = null, tint = SpotifyGreenBright, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Listening History Yet",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Songs and albums you play on Home or Search will appear here automatically.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888))
                        )
                    }
                }
            }
        } else {
            itemsIndexed(recentSearches) { index, item ->
                val songModel = OnlineSongModel(
                    id = item.id,
                    title = item.title,
                    artist = item.artist.ifBlank { "Artist" },
                    streamUrl = item.streamUrl ?: "",
                    highResArtUrl = item.albumArtPath
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
                            albumArtPath = item.albumArtPath,
                            title = item.title,
                            artist = item.artist.ifBlank { "Artist" },
                            size = 46.dp,
                            shape = RoundedCornerShape(6.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.artist.ifBlank { "Recordify Studio" },
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = SpotifyGreenBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
