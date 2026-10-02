package com.audiovault.app.ui.library

import androidx.activity.compose.BackHandler
import com.audiovault.app.ui.components.ArtistAvatar
import com.audiovault.app.ui.components.TrackCoverArt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.ui.components.SubtleJellyContainer
import androidx.compose.foundation.lazy.rememberLazyListState
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.data.repository.SortOption
import com.audiovault.app.ui.recordings.DeleteConfirmDialog
import com.audiovault.app.ui.recordings.LyricsDialog
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.recordings.RenameDialog
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryTab {
    ALL,
    ARTISTS,
    SONGS,
    PLAYLISTS
}

@Composable
fun LibraryScreen(
    viewModel: RecordingsViewModel,
    onNavigateToSearch: () -> Unit,
    onArtistDetailStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()
    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var selectedTab by remember { mutableStateOf(LibraryTab.ALL) }
    var selectedArtistForDetail by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistForDetail by remember { mutableStateOf<String?>(null) } // "LIKED" or playlistId
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<com.audiovault.app.data.repository.UserPlaylist?>(null) }
    var playlistToDelete by remember { mutableStateOf<com.audiovault.app.data.repository.UserPlaylist?>(null) }

    LaunchedEffect(selectedArtistForDetail, selectedPlaylistForDetail) {
        onArtistDetailStateChange(selectedArtistForDetail != null || selectedPlaylistForDetail != null)
    }

    // Step-by-step BackHandler for Artist Detail, Playlist Detail, and Filter Tabs
    BackHandler(enabled = selectedArtistForDetail != null) {
        selectedArtistForDetail = null
    }
    BackHandler(enabled = selectedPlaylistForDetail != null) {
        selectedPlaylistForDetail = null
    }
    BackHandler(enabled = selectedArtistForDetail == null && selectedPlaylistForDetail == null && selectedTab != LibraryTab.ALL) {
        selectedTab = LibraryTab.ALL
    }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Action dialog states
    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingForLyrics by remember { mutableStateOf<RecordingEntity?>(null) }

    // Automatic Multi-Artist Grouping: Splits collaborating artists (e.g. "Artist1, Artist2") so songs appear under all individual artists
    val artistGroups = remember(recordings) {
        val map = mutableMapOf<String, MutableList<RecordingEntity>>()
        recordings.forEach { song ->
            val individualArtists = splitArtists(song.artist)
            individualArtists.forEach { artistName ->
                map.getOrPut(artistName) { mutableListOf() }.add(song)
            }
        }
        map.toList().sortedByDescending { it.second.size }
    }

    if (selectedArtistForDetail != null) {
        val artistName = selectedArtistForDetail!!
        val artistSongs = artistGroups.firstOrNull { it.first == artistName }?.second ?: emptyList()
        ArtistDetailView(
            artistName = artistName,
            songs = artistSongs,
            currentPlayingId = playerState.currentRecording?.id,
            isPlaying = playerState.isPlaying,
            onBack = { selectedArtistForDetail = null },
            onSongClick = { viewModel.playPauseRecording(it) },
            onRename = { recordingToRename = it },
            onDelete = { recordingToDelete = it },
            onLyrics = { recordingForLyrics = it }
        )
    } else if (selectedPlaylistForDetail != null) {
        val isLikedView = selectedPlaylistForDetail == "LIKED"
        val playlist = if (!isLikedView) playlists.find { it.id == selectedPlaylistForDetail } else null
        val playlistSongs = if (isLikedView) {
            recordings.filter { likedSongIds.contains(it.id) }
        } else {
            val songIdSet = playlist?.songIds?.toSet() ?: emptySet()
            recordings.filter { songIdSet.contains(it.id) }
        }

        PlaylistDetailView(
            title = if (isLikedView) "Liked Songs" else (playlist?.name ?: "Playlist"),
            isLikedPlaylist = isLikedView,
            songs = playlistSongs,
            currentPlayingId = playerState.currentRecording?.id,
            isPlaying = playerState.isPlaying,
            onBack = { selectedPlaylistForDetail = null },
            onSongClick = { viewModel.playPauseRecording(it) },
            onRenamePlaylist = { playlist?.let { playlistToRename = it } },
            onDeletePlaylist = { playlist?.let { playlistToDelete = it } },
            onRemoveSong = { song ->
                if (isLikedView) {
                    viewModel.toggleLikedSong(song.id)
                } else if (playlist != null) {
                    viewModel.toggleSongInPlaylist(playlist.id, song.id)
                }
            },
            onRenameSong = { recordingToRename = it },
            onDeleteSong = { recordingToDelete = it },
            onLyricsSong = { recordingForLyrics = it }
        )
    } else {
        val libraryListState = rememberLazyListState()
        SubtleJellyContainer(
            isAtTop = { libraryListState.firstVisibleItemIndex == 0 && libraryListState.firstVisibleItemScrollOffset == 0 },
            modifier = modifier
                .fillMaxSize()
                .background(SpotifyBlack)
        ) { jellyModifier ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(jellyModifier)
                    .padding(horizontal = 16.dp)
            ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Spotify Top Header: User Avatar, "Your Library", Search & Add
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circular App Logo in top-left
                    Image(
                        painter = painterResource(id = com.audiovault.app.R.drawable.ic_recordify_logo),
                        contentDescription = "Recordify",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips: All, Playlists, Artists, Songs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(LibraryTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    val label = when (tab) {
                        LibraryTab.ALL -> "All"
                        LibraryTab.PLAYLISTS -> "Playlists"
                        LibraryTab.ARTISTS -> "Artists"
                        LibraryTab.SONGS -> "Songs"
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SpotifyCardSurface,
                            labelColor = Color.White,
                            selectedContainerColor = SpotifyGreenBright,
                            selectedLabelColor = Color.Black
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subheader: Recents sort & Grid toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { sortMenuExpanded = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (currentSort) {
                                SortOption.NEWEST_FIRST -> "Recents"
                                SortOption.TITLE_A_TO_Z -> "Alphabetical"
                                SortOption.DURATION_LONGEST -> "Longest"
                                SortOption.DURATION_SHORTEST -> "Shortest"
                                SortOption.OLDEST_FIRST -> "Oldest"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (option) {
                                            SortOption.NEWEST_FIRST -> "Recents"
                                            SortOption.TITLE_A_TO_Z -> "Title (A-Z)"
                                            SortOption.DURATION_LONGEST -> "Longest Duration"
                                            SortOption.DURATION_SHORTEST -> "Shortest Duration"
                                            SortOption.OLDEST_FIRST -> "Oldest"
                                        }
                                    )
                                },
                                onClick = {
                                    sortMenuExpanded = false
                                    viewModel.onSortOptionChange(option)
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = { /* Toggle view */ }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "View",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Area
            if (recordings.isEmpty() && playlists.isEmpty() && likedSongIds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No captured tracks yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start monitoring to capture and automatically organize songs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = libraryListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Liked Songs Playlist Tile (if in All or Playlists)
                    if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.PLAYLISTS) {
                        item {
                            LikedSongsTile(
                                trackCount = likedSongIds.size,
                                onClick = { selectedPlaylistForDetail = "LIKED" }
                            )
                        }

                        // User Custom Playlists
                        items(playlists, key = { it.id }) { playlist ->
                            val playlistSongEntities = recordings.filter { playlist.songIds.contains(it.id) }
                            PlaylistLibraryRow(
                                playlist = playlist,
                                songs = playlistSongEntities,
                                onClick = { selectedPlaylistForDetail = playlist.id },
                                onRename = { playlistToRename = playlist },
                                onDelete = { playlistToDelete = playlist }
                            )
                        }
                    }

                    // Artist Group Rows (if in All or Artists)
                    if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.ARTISTS) {
                        items(artistGroups) { (artistName, songs) ->
                            val songTitle = songs.firstOrNull()?.title
                            ArtistLibraryRow(
                                artistName = artistName,
                                songCount = songs.size,
                                songHint = songTitle,
                                onClick = { selectedArtistForDetail = artistName }
                            )
                        }
                    }

                    // Individual Song Rows (if in Songs tab)
                    if (selectedTab == LibraryTab.SONGS) {
                        items(recordings, key = { it.id }) { recording ->
                            val isCurrentTrackPlaying = playerState.isPlaying && playerState.currentRecording?.id == recording.id
                            SongLibraryRow(
                                recording = recording,
                                isPlaying = isCurrentTrackPlaying,
                                onPlayClick = { viewModel.playPauseRecording(recording) },
                                onRename = { recordingToRename = recording },
                                onDelete = { recordingToDelete = recording },
                                onLyrics = { recordingForLyrics = recording }
                            )
                        }
                    }
                }
            }
        }
        }
    }

    // Dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    playlistToRename?.let { playlist ->
        RenamePlaylistDialog(
            currentName = playlist.name,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                playlistToRename = null
            }
        )
    }

    playlistToDelete?.let { playlist ->
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                viewModel.deletePlaylist(playlist.id)
                if (selectedPlaylistForDetail == playlist.id) {
                    selectedPlaylistForDetail = null
                }
                playlistToDelete = null
            }
        )
    }

    recordingToRename?.let { recording ->
        RenameDialog(
            recording = recording,
            onDismiss = { recordingToRename = null },
            onConfirmRename = { newTitle ->
                viewModel.renameRecording(recording, newTitle)
                recordingToRename = null
            }
        )
    }

    recordingToDelete?.let { recording ->
        DeleteConfirmDialog(
            recording = recording,
            onDismiss = { recordingToDelete = null },
            onConfirmDelete = {
                viewModel.deleteRecording(recording)
                recordingToDelete = null
            }
        )
    }

    recordingForLyrics?.let { recording ->
        LyricsDialog(
            recording = recording,
            onDismiss = { recordingForLyrics = null },
            onSaveLyrics = { updatedLyrics ->
                viewModel.updateLyrics(recording, updatedLyrics)
                recordingForLyrics = null
            }
        )
    }
}

/**
 * Liked Songs Playlist tile matching the purple gradient box in Spotify screenshot #2.
 */
@Composable
fun LikedSongsTile(
    trackCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF450AF5), Color(0xFF8E8EE5), Color(0xFFC4EFD9))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Liked Songs",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Liked Songs",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📌 Playlist • Recordify",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SpotifyGreenBright,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = " • $trackCount songs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB3B3B3)
                    )
                )
            }
        }
    }
}

/**
 * Circular Artist row matching Spotify screenshot #2.
 */
@Composable
fun ArtistLibraryRow(
    artistName: String,
    songCount: Int,
    songHint: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Artist Profile Avatar
        ArtistAvatar(
            artistName = artistName,
            songHint = songHint,
            size = 64.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Artist • $songCount ${if (songCount == 1) "song" else "songs"}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB3B3B3)
                )
            )
        }
    }
}

/**
 * Detailed song row matching Spotify layout.
 */
@Composable
fun SongLibraryRow(
    recording: RecordingEntity,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onLyrics: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onPlayClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square Track Thumbnail with Album Art
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            TrackCoverArt(
                albumArtPath = recording.albumArtPath,
                title = recording.title,
                artist = recording.artist,
                size = 54.dp,
                shape = RoundedCornerShape(6.dp)
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Playing",
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recording.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) SpotifyGreenBright else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Song • ${recording.artist ?: "Unknown Artist"}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB3B3B3)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatDuration(recording.durationMs),
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF888888)
            ),
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename Song") },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("View / Edit Lyrics") },
                    onClick = {
                        menuExpanded = false
                        onLyrics()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Song", color = Color(0xFFFF4444)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

/**
 * Artist discography detail view showing all captured songs divided under this artist.
 */
@Composable
fun ArtistDetailView(
    artistName: String,
    songs: List<RecordingEntity>,
    currentPlayingId: Long?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (RecordingEntity) -> Unit,
    onRename: (RecordingEntity) -> Unit,
    onDelete: (RecordingEntity) -> Unit,
    onLyrics: (RecordingEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Back Arrow Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Artist",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Artist Hero Banner
        val songTitle = songs.firstOrNull()?.title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtistAvatar(
                artistName = artistName,
                songHint = songTitle,
                size = 90.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${songs.size} ${if (songs.size == 1) "Track Captured" else "Tracks Captured"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SpotifyGreenBright,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Play All Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Popular Songs",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            if (songs.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = SpotifyGreenBright,
                    modifier = Modifier.size(46.dp),
                    onClick = { onSongClick(songs.first()) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play All",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                val isTrackPlaying = isPlaying && currentPlayingId == song.id
                SongLibraryRow(
                    recording = song,
                    isPlaying = isTrackPlaying,
                    onPlayClick = { onSongClick(song) },
                    onRename = { onRename(song) },
                    onDelete = { onDelete(song) },
                    onLyrics = { onLyrics(song) }
                )
            }
        }
    }
}

/**
 * Custom playlist row in Library.
 */
@Composable
fun PlaylistLibraryRow(
    playlist: com.audiovault.app.data.repository.UserPlaylist,
    songs: List<RecordingEntity>,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val songCount = playlist.songIds.size
    val firstArt = songs.firstOrNull { !it.albumArtPath.isNullOrBlank() }?.albumArtPath

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF282828)),
            contentAlignment = Alignment.Center
        ) {
            if (firstArt != null) {
                TrackCoverArt(
                    albumArtPath = firstArt,
                    title = playlist.name,
                    artist = null,
                    size = 64.dp,
                    shape = RoundedCornerShape(6.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Playlist",
                    tint = SpotifyGreenBright,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB3B3B3)
                    )
                )
                Text(
                    text = " • $songCount ${if (songCount == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB3B3B3)
                    )
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Playlist Options",
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename Playlist") },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Playlist", color = Color(0xFFFF4444)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

/**
 * Playlist detail view for Liked Songs and custom user playlists.
 */
@Composable
fun PlaylistDetailView(
    title: String,
    isLikedPlaylist: Boolean,
    songs: List<RecordingEntity>,
    currentPlayingId: Long?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (RecordingEntity) -> Unit,
    onRenamePlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveSong: (RecordingEntity) -> Unit,
    onRenameSong: (RecordingEntity) -> Unit,
    onDeleteSong: (RecordingEntity) -> Unit,
    onLyricsSong: (RecordingEntity) -> Unit
) {
    var headerMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Back Arrow Header & Playlist Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLikedPlaylist) "Liked Songs" else "Playlist",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            if (!isLikedPlaylist) {
                Box {
                    IconButton(onClick = { headerMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Playlist Options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = headerMenuExpanded,
                        onDismissRequest = { headerMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename Playlist") },
                            onClick = {
                                headerMenuExpanded = false
                                onRenamePlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Playlist", color = Color(0xFFFF4444)) },
                            onClick = {
                                headerMenuExpanded = false
                                onDeletePlaylist()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playlist Hero Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLikedPlaylist) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF450AF5), Color(0xFF8E8EE5), Color(0xFFC4EFD9))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Liked Songs",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            } else {
                val firstArt = songs.firstOrNull { !it.albumArtPath.isNullOrBlank() }?.albumArtPath
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF282828)),
                    contentAlignment = Alignment.Center
                ) {
                    if (firstArt != null) {
                        TrackCoverArt(
                            albumArtPath = firstArt,
                            title = title,
                            artist = null,
                            size = 90.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Playlist",
                            tint = SpotifyGreenBright,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SpotifyGreenBright,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Play All Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tracks",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            if (songs.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = SpotifyGreenBright,
                    modifier = Modifier.size(46.dp),
                    onClick = { onSongClick(songs.first()) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play All",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLikedPlaylist) "No liked songs yet.\nTap the + button in the player to add songs!" else "This playlist is empty.\nTap + on any song to add it here!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF888888),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    val isTrackPlaying = isPlaying && currentPlayingId == song.id
                    var menuExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSongClick(song) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackCoverArt(
                            albumArtPath = song.albumArtPath,
                            title = song.title,
                            artist = song.artist,
                            size = 54.dp,
                            shape = RoundedCornerShape(6.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTrackPlaying) SpotifyGreenBright else Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = song.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFB3B3B3)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = formatDuration(song.durationMs),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF888888)
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color(0xFFB3B3B3),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isLikedPlaylist) "Remove from Liked Songs" else "Remove from Playlist") },
                                    onClick = {
                                        menuExpanded = false
                                        onRemoveSong(song)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename Song") },
                                    onClick = {
                                        menuExpanded = false
                                        onRenameSong(song)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("View / Edit Lyrics") },
                                    onClick = {
                                        menuExpanded = false
                                        onLyricsSong(song)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Song", color = Color(0xFFFF4444)) },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteSong(song)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistTitle by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCardSurface,
        title = {
            Text(
                text = "Give your playlist a title",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        },
        text = {
            OutlinedTextField(
                value = playlistTitle,
                onValueChange = { playlistTitle = it },
                placeholder = { Text("My Playlist", color = Color(0xFF888888)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SpotifyGreenBright,
                    unfocusedBorderColor = Color(0xFF444444),
                    cursorColor = SpotifyGreenBright
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (playlistTitle.isNotBlank()) {
                            onConfirm(playlistTitle.trim())
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (playlistTitle.isNotBlank()) {
                        onConfirm(playlistTitle.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreenBright,
                    contentColor = Color.Black
                )
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB3B3B3))
            }
        }
    )
}

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCardSurface,
        title = {
            Text(
                text = "Rename Playlist",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SpotifyGreenBright,
                    unfocusedBorderColor = Color(0xFF444444),
                    cursorColor = SpotifyGreenBright
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (newTitle.isNotBlank()) {
                            onConfirm(newTitle.trim())
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onConfirm(newTitle.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreenBright,
                    contentColor = Color.Black
                )
            ) {
                Text("Rename", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB3B3B3))
            }
        }
    )
}

@Composable
fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCardSurface,
        title = {
            Text(
                text = "Delete \"$playlistName\"?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        },
        text = {
            Text(
                text = "This will delete the playlist from your library. Your recorded songs will not be deleted.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB3B3B3)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4444),
                    contentColor = Color.White
                )
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB3B3B3))
            }
        }
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun splitArtists(rawArtist: String?): List<String> {
    if (rawArtist.isNullOrBlank()) return listOf("Unknown Artist")

    // Split by comma, &, feat., ft., /, x, with
    val regex = Regex("(?i)\\s*(?:,|&|\\/|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bx\\b|\\bwith\\b)\\s*")
    val list = rawArtist.split(regex)
        .map { it.trim().trim('(', ')', '[', ']') }
        .filter { it.isNotBlank() }
        .distinct()

    return if (list.isNotEmpty()) list else listOf(rawArtist.trim())
}
