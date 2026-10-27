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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
    onPlaySong: (OnlineSongModel, List<OnlineSongModel>?) -> Unit = { _, _ -> },
    onOpenAlbum: (OnlineAlbumModel) -> Unit,
    modifier: Modifier = Modifier,
    firstPillFocusRequester: FocusRequester = remember { FocusRequester() },
    onNavigateUpToHeader: () -> Unit = {}
) {
    val recordings by viewModel.recordings.collectAsState(initial = emptyList())
    val playlists by viewModel.playlists.collectAsState()
    val savedArtists by viewModel.savedArtists.collectAsState()

    var selectedFilterPill by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All") }
    val subviewBackFocusRequester = remember { FocusRequester() }

    LaunchedEffect(librarySubView) {
        if (librarySubView != null) {
            kotlinx.coroutines.delay(100L)
            try {
                subviewBackFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    if (librarySubView != null) {
        // Detailed Subview (Saved Songs, Liked Songs, Playlists, Artists)
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 24.dp)
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
                        focusScale = 1.12f,
                        focusRequester = subviewBackFocusRequester,
                        focusBorderWidth = 2.dp,
                        focusBorderColor = Color.White
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
                        "SAVED" -> "Your Episodes & Saved Songs (${recordings.size})"
                        "FAVORITES" -> "Liked Songs (${likedSongIds.size})"
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
                                            ),
                                            null
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
                                            ),
                                            null
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
                    val allArtists = (savedArtists + listOf("Aditya Gadhvi", "Pritam", "Arijit Singh", "Kumar Sanu")).distinct()
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(allArtists) { artistName ->
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
    } else {
        // Authentically Replicated Your Library UI (Spotify TV Match: IMG20261026111737.jpg)
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // -------------------------------------------------------------
            // 1. FILTER PILLS ([All], [Playlists], [Podcasts & shows], [Artists])
            // -------------------------------------------------------------
            item {
                val libraryFilterPills = listOf("All", "Playlists", "Podcasts & shows", "Artists")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    libraryFilterPills.forEach { pillLabel ->
                        val isSelected = selectedFilterPill == pillLabel
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) Color.White else Color(0xFF202020),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color.White else Color(0xFF333333)
                            ),
                            modifier = Modifier
                                .height(34.dp)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                        onNavigateUpToHeader()
                                        true
                                    } else false
                                }
                                .tvRemoteFocusable(
                                    onClick = { selectedFilterPill = pillLabel },
                                    shape = RoundedCornerShape(50),
                                    focusScale = 1.08f,
                                    focusRequester = if (pillLabel == "All") firstPillFocusRequester else null,
                                    focusBorderWidth = 2.dp,
                                    focusBorderColor = Color.White
                                )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pillLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. SHELF OF LIBRARY CARDS (Matching Spotify TV: IMG20261026111737.jpg)
            // - Liked Songs (Purple/Blue gradient with heart)
            // - Your Episodes / Saved (Green gradient with bookmark)
            // - Aditya Gadhvi (Circular artist card)
            // - My songs (Playlist card)
            // - Sadi ke fall sa (Playlist card)
            // - Pritam (Circular artist card)
            // - Arijit Singh (Circular artist card)
            // -------------------------------------------------------------
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Special "Liked Songs" Card
                    if (selectedFilterPill == "All" || selectedFilterPill == "Playlists") {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .width(160.dp)
                                    .tvRemoteFocusable(
                                        onClick = { onLibrarySubViewChange("FAVORITES") },
                                        shape = RoundedCornerShape(12.dp),
                                        focusScale = 1.08f
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(140.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                                        listOf(Color(0xFF450AF5), Color(0xFF8E8EE5))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Liked Songs",
                                                tint = Color.White,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Liked Songs",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📌 Playlist • ${likedSongIds.size.coerceAtLeast(6)} songs",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF888888),
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // 2. Special "Your Episodes" / "Offline Vault" Card
                    if (selectedFilterPill == "All" || selectedFilterPill == "Playlists" || selectedFilterPill == "Podcasts & shows") {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .width(160.dp)
                                    .tvRemoteFocusable(
                                        onClick = { onLibrarySubViewChange("SAVED") },
                                        shape = RoundedCornerShape(12.dp),
                                        focusScale = 1.08f
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(140.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                                        listOf(Color(0xFF006450), Color(0xFF05B385))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Saved Episodes",
                                                tint = Color.White,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Your Episodes",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📌 Playlist • Recordify Vault",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF888888),
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // 3. Circular Artist Card: Aditya Gadhvi
                    if (selectedFilterPill == "All" || selectedFilterPill == "Artists") {
                        item {
                            TvCircularArtistCard(
                                name = "Aditya Gadhvi",
                                imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                                onClick = {
                                    val album = CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_aditya_gadhvi" }
                                    if (album != null) onOpenAlbum(album)
                                }
                            )
                        }
                    }

                    // 4. Playlist Card: My songs
                    if (selectedFilterPill == "All" || selectedFilterPill == "Playlists") {
                        item {
                            val album = CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_my_songs" }
                            if (album != null) {
                                TvAlbumCard(album = album, onClick = { onOpenAlbum(album) })
                            }
                        }
                    }

                    // 5. Playlist Card: Sadi ke fall sa
                    if (selectedFilterPill == "All" || selectedFilterPill == "Playlists") {
                        item {
                            val album = CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_sadi_ke_fall_sa" }
                            if (album != null) {
                                TvAlbumCard(album = album, onClick = { onOpenAlbum(album) })
                            }
                        }
                    }

                    // 6. Circular Artist Card: Pritam
                    if (selectedFilterPill == "All" || selectedFilterPill == "Artists") {
                        item {
                            TvCircularArtistCard(
                                name = "Pritam",
                                imageUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&auto=format&fit=crop&q=80",
                                onClick = {
                                    val album = CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_pritam" }
                                    if (album != null) onOpenAlbum(album)
                                }
                            )
                        }
                    }

                    // 7. Circular Artist Card: Arijit Singh
                    if (selectedFilterPill == "All" || selectedFilterPill == "Artists") {
                        item {
                            TvCircularArtistCard(
                                name = "Arijit Singh",
                                imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
                                onClick = {
                                    val album = CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_arijit_singh" }
                                    if (album != null) onOpenAlbum(album)
                                }
                            )
                        }
                    }
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
