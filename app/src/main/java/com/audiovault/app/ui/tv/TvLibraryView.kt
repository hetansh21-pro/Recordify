package com.audiovault.app.ui.tv

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * 10-Foot Your Library Screen for Recordify TV.
 * Built according to Sections 5 and 12 of the Spotify TV specification:
 * - 4 Primary Category Cards: Saved Songs, Favorite Tracks, Playlists, Artists.
 * - Drill-down subviews with immediate remote playback.
 */
@Composable
fun TvLibraryView(
    viewModel: RecordingsViewModel,
    librarySubView: String?,
    onLibrarySubViewChange: (String?) -> Unit,
    likedSongIds: Set<Long>,
    onPlaySong: (OnlineSongModel) -> Unit,
    onOpenAlbum: (OnlineAlbumModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState(initial = emptyList())
    val playlists by viewModel.playlists.collectAsState()
    val savedArtists by viewModel.savedArtists.collectAsState()

    if (librarySubView != null) {
        // Detailed Subview (Saved Songs, Liked Songs, Playlists, Artists)
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            // Header with Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E1E1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.tvRemoteFocusable(
                        onClick = { onLibrarySubViewChange(null) },
                        shape = CircleShape,
                        focusScale = 1.12f
                    )
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = when (librarySubView) {
                        "SAVED" -> "Saved Songs (${recordings.size})"
                        "FAVORITES" -> "Favorite Tracks (${likedSongIds.size})"
                        "PLAYLISTS" -> "Playlists & Curated Vault"
                        "ARTISTS" -> "Followed Artists (${savedArtists.size})"
                        else -> "Library Subview"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (librarySubView) {
                "SAVED" -> {
                    if (recordings.isEmpty()) {
                        TvEmptyLibraryState(
                            title = "No Saved Songs Found",
                            subtitle = "Download online songs or import local MP3s in Settings to view them here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            itemsIndexed(recordings) { index, rec ->
                                TvSavedSongRow(
                                    rec = rec,
                                    onPlay = {
                                        onPlaySong(
                                            OnlineSongModel(
                                                id = rec.id.toString(),
                                                title = rec.title,
                                                artist = rec.artist ?: "Unknown",
                                                streamUrl = rec.filePath,
                                                highResArtUrl = rec.albumArtPath
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                "FAVORITES" -> {
                    val likedSongs = recordings.filter { likedSongIds.contains(it.id) }
                    if (likedSongs.isEmpty()) {
                        TvEmptyLibraryState(
                            title = "No Favorite Songs Yet",
                            subtitle = "Press the heart icon on any playing song or search result to build your favorites collection."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            itemsIndexed(likedSongs) { index, rec ->
                                TvSavedSongRow(
                                    rec = rec,
                                    onPlay = {
                                        onPlaySong(
                                            OnlineSongModel(
                                                id = rec.id.toString(),
                                                title = rec.title,
                                                artist = rec.artist ?: "Unknown",
                                                streamUrl = rec.filePath,
                                                highResArtUrl = rec.albumArtPath
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                "PLAYLISTS" -> {
                    val allPlaylists = CuratedAlbumCatalog.curatedAlbums
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(allPlaylists) { album ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF161616),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvRemoteFocusable(
                                        onClick = { onOpenAlbum(album) },
                                        shape = RoundedCornerShape(10.dp),
                                        focusScale = 1.02f
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TrackCoverArt(
                                        albumArtPath = album.coverArtUrl,
                                        title = album.title,
                                        artist = album.primaryArtists ?: "Recordify",
                                        size = 54.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Curated Playlist • ${album.trackQueries.size.coerceAtLeast(album.songCount)} Tracks",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888)),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "ARTISTS" -> {
                    if (savedArtists.isEmpty()) {
                        TvEmptyLibraryState(
                            title = "No Followed Artists",
                            subtitle = "Follow artists while browsing or playing songs to keep track of their releases here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(savedArtists.toList()) { artistName ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF161616),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tvRemoteFocusable(
                                            onClick = {},
                                            shape = RoundedCornerShape(10.dp),
                                            focusScale = 1.02f
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF262626),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = SpotifyGreenBright)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = artistName,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // 4 Category Cards Main View
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                TvShelfHeader(
                    title = "Your Library",
                    subtitle = "All your saved music, downloaded tracks, and curated collections in one place"
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvLibraryCategoryCard(
                        title = "Saved Songs",
                        subtitle = "${recordings.size} tracks",
                        icon = Icons.Default.Download,
                        accentColor = SpotifyGreenBright,
                        onClick = { onLibrarySubViewChange("SAVED") },
                        modifier = Modifier.weight(1f)
                    )
                    TvLibraryCategoryCard(
                        title = "Favorite Tracks",
                        subtitle = "${likedSongIds.size} songs",
                        icon = Icons.Default.Favorite,
                        accentColor = Color(0xFFE91E63),
                        onClick = { onLibrarySubViewChange("FAVORITES") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvLibraryCategoryCard(
                        title = "Playlists",
                        subtitle = "${CuratedAlbumCatalog.curatedAlbums.size} playlists",
                        icon = Icons.Default.Album,
                        accentColor = Color(0xFF2196F3),
                        onClick = { onLibrarySubViewChange("PLAYLISTS") },
                        modifier = Modifier.weight(1f)
                    )
                    TvLibraryCategoryCard(
                        title = "Artists",
                        subtitle = "${savedArtists.size} followed",
                        icon = Icons.Default.Person,
                        accentColor = Color(0xFFFF9800),
                        onClick = { onLibrarySubViewChange("ARTISTS") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TvLibraryCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF161616),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
        modifier = modifier
            .height(130.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(14.dp),
                focusScale = 1.05f
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                )
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
}

@Composable
private fun TvSavedSongRow(
    rec: RecordingEntity,
    onPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161616),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
        modifier = Modifier
            .fillMaxWidth()
            .tvRemoteFocusable(
                onClick = onPlay,
                shape = RoundedCornerShape(10.dp),
                focusScale = 1.02f
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        color = Color.White
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
        }
    }
}

@Composable
private fun TvEmptyLibraryState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888)),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
