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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel

/**
 * Dedicated 10-Foot Playlists Screen for Recordify TV.
 * Built according to Sections 5 and 12 of the Spotify TV specification.
 */
@Composable
fun TvPlaylistsView(
    viewModel: RecordingsViewModel,
    onOpenAlbum: (OnlineAlbumModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val curatedPlaylists = CuratedAlbumCatalog.curatedAlbums
    val userPlaylists by viewModel.playlists.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            TvShelfHeader(
                title = "Playlists & Curated Collections",
                subtitle = "Browse hand-picked master soundsets and user-curated playlists"
            )
        }

        // Curated Collections Shelf
        item {
            Column {
                Text(
                    text = "RECORDIFY EDITORIAL PLAYLISTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(curatedPlaylists) { playlist ->
                        TvAlbumCard(
                            album = playlist,
                            onClick = { onOpenAlbum(playlist) }
                        )
                    }
                }
            }
        }

        // Full Detailed Playlist List
        item {
            Text(
                text = "ALL AVAILABLE PLAYLISTS (${curatedPlaylists.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF888888),
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(curatedPlaylists) { playlist ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161616),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier
                    .fillMaxWidth()
                    .tvRemoteFocusable(
                        onClick = { onOpenAlbum(playlist) },
                        shape = RoundedCornerShape(12.dp),
                        focusScale = 1.02f
                    )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackCoverArt(
                        albumArtPath = playlist.coverArtUrl,
                        title = playlist.title,
                        artist = playlist.primaryArtists ?: "Recordify",
                        size = 58.dp,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Studio Master Playlist • ${playlist.trackQueries.size.coerceAtLeast(playlist.songCount)} Tracks",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${playlist.trackQueries.size} Tracks",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}
