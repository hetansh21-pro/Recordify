package com.audiovault.app.ui.library

import androidx.activity.compose.BackHandler
import com.audiovault.app.ui.components.ArtistAvatar
import com.audiovault.app.ui.components.SwipeToQueueContainer
import com.audiovault.app.ui.components.TrackCoverArt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.ui.text.style.TextAlign

enum class LibraryTab {
    ALL,
    DOWNLOADED,
    PLAYLISTS,
    ARTISTS,
    SONGS
}

@Composable
fun LibraryScreen(
    viewModel: RecordingsViewModel,
    onNavigateToSearch: () -> Unit,
    userName: String = "",
    onArtistDetailStateChange: (Boolean) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    isCurrentPage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()
    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val savedArtists by viewModel.savedArtists.collectAsState()
    val manualArtists by viewModel.manualArtists.collectAsState()

    var selectedTab by remember { mutableStateOf(LibraryTab.ALL) }
    var selectedArtistForDetail by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistForDetail by remember { mutableStateOf<String?>(null) } // "LIKED" or playlistId

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            selectedPlaylistForDetail = null
            selectedArtistForDetail = null
        }
    }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddArtistDialog by remember { mutableStateOf(false) }
    var showImportAlbumDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<com.audiovault.app.data.repository.UserPlaylist?>(null) }
    var playlistToDelete by remember { mutableStateOf<com.audiovault.app.data.repository.UserPlaylist?>(null) }
    var artistToDelete by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var isLibrarySearchOpen by remember { mutableStateOf(false) }
    var librarySearchQuery by remember { mutableStateOf("") }
    var isGridView by rememberSaveable { mutableStateOf(false) }
    val libraryGridState = rememberLazyGridState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(selectedArtistForDetail, selectedPlaylistForDetail) {
        onArtistDetailStateChange(selectedArtistForDetail != null || selectedPlaylistForDetail != null)
    }

    // Step-by-step BackHandler for Artist Detail, Playlist Detail, Search, and Filter Tabs
    BackHandler(enabled = selectedArtistForDetail != null) {
        selectedArtistForDetail = null
    }
    BackHandler(enabled = selectedPlaylistForDetail != null) {
        selectedPlaylistForDetail = null
    }
    BackHandler(enabled = isLibrarySearchOpen) {
        isLibrarySearchOpen = false
        librarySearchQuery = ""
    }
    BackHandler(enabled = selectedArtistForDetail == null && selectedPlaylistForDetail == null && !isLibrarySearchOpen && selectedTab != LibraryTab.ALL) {
        selectedTab = LibraryTab.ALL
    }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Action dialog states
    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingForLyrics by remember { mutableStateOf<RecordingEntity?>(null) }

    // All Multi-Artist Grouping: Splits collaborating artists & includes manually added & saved artists scanning existing songs
    val allArtistGroups = remember(recordings, savedArtists, manualArtists) {
        val map = LinkedHashMap<String, MutableList<RecordingEntity>>()
        recordings.forEach { song ->
            val individualArtists = splitArtists(song.artist)
            individualArtists.forEach { artistName ->
                val clean = artistName.trim()
                if (clean.isNotBlank()) {
                    val existingKey = map.keys.firstOrNull { it.equals(clean, ignoreCase = true) }
                    if (existingKey != null) {
                        map[existingKey]?.add(song)
                    } else {
                        map[clean] = mutableListOf(song)
                    }
                }
            }
        }
        // Also ensure any manual or saved artist without songs yet or matched by substring is included
        val extraArtists = (manualArtists + savedArtists)
        extraArtists.forEach { extraArtist ->
            val cleanExtra = extraArtist.trim()
            if (cleanExtra.isNotBlank()) {
                val existingKey = map.keys.firstOrNull { it.equals(cleanExtra, ignoreCase = true) }
                if (existingKey == null) {
                    val matching = recordings.filter { song ->
                        val songArtist = song.artist ?: ""
                        songArtist.contains(cleanExtra, ignoreCase = true) ||
                        song.title.contains(cleanExtra, ignoreCase = true)
                    }
                    map[cleanExtra] = matching.toMutableList()
                } else {
                    val currentList = map[existingKey] ?: mutableListOf()
                    val matching = recordings.filter { song ->
                        !currentList.any { it.id == song.id } &&
                        ((song.artist ?: "").contains(cleanExtra, ignoreCase = true) || song.title.contains(cleanExtra, ignoreCase = true))
                    }
                    currentList.addAll(matching)
                }
            }
        }
        map.toList().sortedByDescending { it.second.size }
    }

    // Saved Artist Groups: Only artists the user has saved/followed
    val savedArtistGroups = remember(allArtistGroups, savedArtists) {
        allArtistGroups.filter { (artistName, _) ->
            savedArtists.any { it.equals(artistName, ignoreCase = true) }
        }
    }

    // Real-time Search Filtering
    val query = librarySearchQuery.trim()
    val filteredSavedArtistGroups = remember(savedArtistGroups, query) {
        if (query.isEmpty()) savedArtistGroups
        else savedArtistGroups.filter { (artistName, songs) ->
            artistName.contains(query, ignoreCase = true) ||
            songs.any { it.title.contains(query, ignoreCase = true) }
        }
    }

    val filteredAllArtistGroups = remember(allArtistGroups, query) {
        if (query.isEmpty()) allArtistGroups
        else allArtistGroups.filter { (artistName, songs) ->
            artistName.contains(query, ignoreCase = true) ||
            songs.any { it.title.contains(query, ignoreCase = true) }
        }
    }

    val filteredPlaylists = remember(playlists, query) {
        if (query.isEmpty()) playlists
        else playlists.filter { it.name.contains(query, ignoreCase = true) }
    }

    val filteredRecordings = remember(recordings, query) {
        if (query.isEmpty()) recordings
        else recordings.filter {
            it.title.contains(query, ignoreCase = true) ||
            (it.artist ?: "").contains(query, ignoreCase = true)
        }
    }

    val activeLikedCount = remember(recordings, likedSongIds) {
        val idSet = likedSongIds.toSet()
        recordings.count { idSet.contains(it.id) }
    }

    val totalDownloadedBytes = remember(recordings) {
        recordings.sumOf { it.fileSizeBytes }
    }

    val formattedDownloadedSize = remember(totalDownloadedBytes) {
        val mb = totalDownloadedBytes.toDouble() / (1024.0 * 1024.0)
        if (mb >= 1000.0) {
            String.format(Locale.getDefault(), "%.1f GB", mb / 1024.0)
        } else {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        }
    }

    val showLikedTile = (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.PLAYLISTS) &&
        (query.isEmpty() || "liked songs".contains(query, ignoreCase = true) || "liked".contains(query, ignoreCase = true))

    if (selectedArtistForDetail != null) {
        val artistName = selectedArtistForDetail!!
        val artistSongs = allArtistGroups.firstOrNull { it.first.equals(artistName, ignoreCase = true) }?.second ?: emptyList()
        val isArtistSaved = savedArtists.any { it.equals(artistName, ignoreCase = true) }
        ArtistDetailView(
            artistName = artistName,
            songs = artistSongs,
            currentPlayingId = playerState.currentRecording?.id,
            isPlaying = playerState.isPlaying,
            isSaved = isArtistSaved,
            onToggleSave = { viewModel.toggleSavedArtist(artistName) },
            onBack = { selectedArtistForDetail = null },
            onSongClick = { viewModel.playPauseRecording(it) },
            onDeleteArtist = { artistToDelete = Pair(artistName, artistSongs.size) },
            onRename = { recordingToRename = it },
            onDelete = { recordingToDelete = it },
            onLyrics = { recordingForLyrics = it },
            onSwipeToQueue = { song ->
                viewModel.addRecordingToQueue(song)
                android.widget.Toast.makeText(context, "Added to queue", android.widget.Toast.LENGTH_SHORT).show()
            }
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
            onLyricsSong = { recordingForLyrics = it },
            onSwipeToQueue = { song ->
                viewModel.addRecordingToQueue(song)
                android.widget.Toast.makeText(context, "Added to queue", android.widget.Toast.LENGTH_SHORT).show()
            }
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

            // Spotify Top Header: Normal Header vs In-Library Search Box
            if (isLibrarySearchOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SpotifyCardSurface)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = librarySearchQuery,
                        onValueChange = { librarySearchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(SpotifyGreenBright),
                        decorationBox = { innerTextField ->
                            if (librarySearchQuery.isEmpty()) {
                                Text(
                                    text = "Search artists, songs, playlists...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF888888)
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (librarySearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { librarySearchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFFB3B3B3),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isLibrarySearchOpen = false
                            librarySearchQuery = ""
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenDrawer() }
                    ) {
                        com.audiovault.app.ui.components.ProfileAvatarButton(
                            userName = userName,
                            onClick = onOpenDrawer,
                            size = 36.dp
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
                        IconButton(onClick = { isLibrarySearchOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Library",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        var plusMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = {
                                when (selectedTab) {
                                    LibraryTab.ARTISTS -> showAddArtistDialog = true
                                    LibraryTab.PLAYLISTS -> showCreatePlaylistDialog = true
                                    else -> plusMenuExpanded = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = plusMenuExpanded,
                                onDismissRequest = { plusMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Import Album from Link") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = SpotifyGreenBright)
                                    },
                                    onClick = {
                                        plusMenuExpanded = false
                                        showImportAlbumDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Create Playlist") },
                                    leadingIcon = {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreenBright)
                                    },
                                    onClick = {
                                        plusMenuExpanded = false
                                        showCreatePlaylistDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add Artist") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = SpotifyGreenBright)
                                    },
                                    onClick = {
                                        plusMenuExpanded = false
                                        showAddArtistDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips: All, Playlists, Artists, Songs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(LibraryTab.values(), key = { it.name }) { tab ->
                    val isSelected = selectedTab == tab
                    val label = when (tab) {
                        LibraryTab.ALL -> "All"
                        LibraryTab.DOWNLOADED -> "Downloaded"
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

                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                        tint = if (isGridView) SpotifyGreenBright else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Area with Smooth Fade Transition (Always showing category cards by default)
            Crossfade(
                targetState = Pair(selectedTab, isGridView),
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                label = "LibraryTabFade"
            ) { (_, inGrid) ->
                if (inGrid) {
                    // Spotify-style 2-column Grid View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = libraryGridState,
                        modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Top Colorful Compact Category Cards (if in ALL tab and not searching)
                    if (selectedTab == LibraryTab.ALL && query.isEmpty()) {
                        item {
                            CategorySquareCard(
                                title = "Downloaded",
                                countText = if (recordings.isNotEmpty()) "${recordings.size} tracks • $formattedDownloadedSize" else "0 tracks",
                                icon = Icons.Default.Download,
                                gradientColors = listOf(Color(0xFF0072FF), Color(0xFF00C6FF)),
                                onClick = { selectedTab = LibraryTab.DOWNLOADED }
                            )
                        }
                        item {
                            CategorySquareCard(
                                title = "Liked Songs",
                                countText = "$activeLikedCount liked",
                                icon = Icons.Default.Favorite,
                                gradientColors = listOf(Color(0xFFEA4C89), Color(0xFFFF6B6B)),
                                onClick = { selectedPlaylistForDetail = "LIKED" }
                            )
                        }
                        item {
                            CategorySquareCard(
                                title = "Playlists",
                                countText = "${playlists.size} created",
                                icon = Icons.Default.QueueMusic,
                                gradientColors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
                                onClick = { selectedTab = LibraryTab.PLAYLISTS }
                            )
                        }
                        item {
                            CategorySquareCard(
                                title = "Artists",
                                countText = "${allArtistGroups.size} artists",
                                icon = Icons.Default.Person,
                                gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                                onClick = { selectedTab = LibraryTab.ARTISTS }
                            )
                        }
                    }

                    // Downloaded Songs Cards (if in Downloaded tab)
                    if (selectedTab == LibraryTab.DOWNLOADED) {
                        if (filteredRecordings.isEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp, bottom = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No downloaded tracks yet",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Download songs from Search or Player to listen offline anytime.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onNavigateToSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreenBright)
                                    ) {
                                        Text("Browse Music", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            items(filteredRecordings, key = { "dl_${it.id}" }) { recording ->
                                val isCurrentTrackPlaying = playerState.isPlaying && playerState.currentRecording?.id == recording.id
                                SongGridCard(
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

                    // Liked Songs Playlist Tile (if in Playlists tab)
                    if (selectedTab == LibraryTab.PLAYLISTS && showLikedTile) {
                        item {
                            LikedSongsGridCard(
                                trackCount = activeLikedCount,
                                onClick = { selectedPlaylistForDetail = "LIKED" }
                            )
                        }
                    }

                    // User Custom Playlists
                    if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.PLAYLISTS) {
                        items(filteredPlaylists, key = { it.id }) { playlist ->
                            val playlistSongEntities = recordings.filter { playlist.songIds.contains(it.id) }
                            PlaylistGridCard(
                                playlist = playlist,
                                songs = playlistSongEntities,
                                onClick = { selectedPlaylistForDetail = playlist.id },
                                onRename = { playlistToRename = playlist },
                                onDelete = { playlistToDelete = playlist }
                            )
                        }
                    }

                    // Saved Artist Cards (if in ALL tab - only saved/followed artists)
                    if (selectedTab == LibraryTab.ALL) {
                        items(filteredSavedArtistGroups, key = { it.first }) { (artistName, songs) ->
                            val songTitle = songs.firstOrNull()?.title
                            val isArtistSaved = savedArtists.any { it.equals(artistName, ignoreCase = true) }
                            ArtistGridCard(
                                artistName = artistName,
                                songCount = songs.size,
                                songHint = songTitle,
                                isSaved = isArtistSaved,
                                onToggleSave = { viewModel.toggleSavedArtist(artistName) },
                                onClick = { selectedArtistForDetail = artistName },
                                onDeleteArtist = { artistToDelete = Pair(artistName, songs.size) }
                            )
                        }
                    }

                    // All Artist Cards (in ARTISTS tab - all artists with toggleable save button)
                    if (selectedTab == LibraryTab.ARTISTS) {
                        if (query.isEmpty()) {
                            item {
                                AddArtistGridCard(onClick = { showAddArtistDialog = true })
                            }
                        }
                        items(filteredAllArtistGroups, key = { it.first }) { (artistName, songs) ->
                            val songTitle = songs.firstOrNull()?.title
                            val isArtistSaved = savedArtists.any { it.equals(artistName, ignoreCase = true) }
                            ArtistGridCard(
                                artistName = artistName,
                                songCount = songs.size,
                                songHint = songTitle,
                                isSaved = isArtistSaved,
                                onToggleSave = { viewModel.toggleSavedArtist(artistName) },
                                onClick = { selectedArtistForDetail = artistName },
                                onDeleteArtist = { artistToDelete = Pair(artistName, songs.size) }
                            )
                        }
                    }

                    // Individual Song Cards (if in Songs tab)
                    if (selectedTab == LibraryTab.SONGS) {
                        items(filteredRecordings, key = { it.id }) { recording ->
                            val isCurrentTrackPlaying = playerState.isPlaying && playerState.currentRecording?.id == recording.id
                            SongGridCard(
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
            } else {
                LazyColumn(
                    state = libraryListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Colorful Compact Category Hub (if in ALL tab and not searching)
                    if (selectedTab == LibraryTab.ALL && query.isEmpty()) {
                        item {
                            LibraryCategoryHub(
                                downloadedCount = recordings.size,
                                downloadedSizeText = formattedDownloadedSize,
                                playlistCount = playlists.size,
                                artistCount = allArtistGroups.size,
                                songCount = recordings.size,
                                likedCount = activeLikedCount,
                                onDownloadedClick = { selectedTab = LibraryTab.DOWNLOADED },
                                onPlaylistsClick = { selectedTab = LibraryTab.PLAYLISTS },
                                onArtistsClick = { selectedTab = LibraryTab.ARTISTS },
                                onSongsClick = { selectedTab = LibraryTab.SONGS },
                                onLikedClick = { selectedPlaylistForDetail = "LIKED" }
                            )
                        }
                    }

                    // Downloaded Songs Section (if in Downloaded tab)
                    if (selectedTab == LibraryTab.DOWNLOADED) {
                        if (filteredRecordings.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp, bottom = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No downloaded tracks yet",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Download songs from Search or Player to listen offline anytime.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onNavigateToSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreenBright)
                                    ) {
                                        Text("Browse Music", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = SpotifyGreenBright,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${filteredRecordings.size} tracks • $formattedDownloadedSize downloaded on device",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFB3B3B3),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            items(filteredRecordings, key = { "dl_row_${it.id}" }) { recording ->
                                val isCurrentTrackPlaying = playerState.isPlaying && playerState.currentRecording?.id == recording.id
                                SongLibraryRow(
                                    recording = recording,
                                    isPlaying = isCurrentTrackPlaying,
                                    onPlayClick = { viewModel.playPauseRecording(recording) },
                                    onRename = { recordingToRename = recording },
                                    onDelete = { recordingToDelete = recording },
                                    onLyrics = { recordingForLyrics = recording },
                                    onSwipeToQueue = {
                                        viewModel.addRecordingToQueue(recording)
                                        android.widget.Toast.makeText(context, "Added to queue", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Liked Songs Playlist Tile (if in Playlists tab)
                    if (selectedTab == LibraryTab.PLAYLISTS && showLikedTile) {
                        item {
                            LikedSongsTile(
                                trackCount = activeLikedCount,
                                onClick = { selectedPlaylistForDetail = "LIKED" }
                            )
                        }
                    }

                    // User Custom Playlists
                    if (selectedTab == LibraryTab.ALL || selectedTab == LibraryTab.PLAYLISTS) {
                        items(filteredPlaylists, key = { it.id }) { playlist ->
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

                    // Saved Artist Rows (if in ALL tab - only saved/followed artists)
                    if (selectedTab == LibraryTab.ALL) {
                        items(filteredSavedArtistGroups, key = { it.first }) { (artistName, songs) ->
                            val songTitle = songs.firstOrNull()?.title
                            val isArtistSaved = savedArtists.any { it.equals(artistName, ignoreCase = true) }
                            ArtistLibraryRow(
                                artistName = artistName,
                                songCount = songs.size,
                                songHint = songTitle,
                                isSaved = isArtistSaved,
                                onToggleSave = { viewModel.toggleSavedArtist(artistName) },
                                onClick = { selectedArtistForDetail = artistName },
                                onDeleteArtist = { artistToDelete = Pair(artistName, songs.size) }
                            )
                        }
                    }

                    // All Artist Rows (in ARTISTS tab - all artists with toggleable save button)
                    if (selectedTab == LibraryTab.ARTISTS) {
                        if (query.isEmpty()) {
                            item {
                                AddArtistLibraryRow(onClick = { showAddArtistDialog = true })
                            }
                        }
                        items(filteredAllArtistGroups, key = { it.first }) { (artistName, songs) ->
                            val songTitle = songs.firstOrNull()?.title
                            val isArtistSaved = savedArtists.any { it.equals(artistName, ignoreCase = true) }
                            ArtistLibraryRow(
                                artistName = artistName,
                                songCount = songs.size,
                                songHint = songTitle,
                                isSaved = isArtistSaved,
                                onToggleSave = { viewModel.toggleSavedArtist(artistName) },
                                onClick = { selectedArtistForDetail = artistName },
                                onDeleteArtist = { artistToDelete = Pair(artistName, songs.size) }
                            )
                        }
                    }

                    // Individual Song Rows (if in Songs tab)
                    if (selectedTab == LibraryTab.SONGS) {
                        items(filteredRecordings, key = { it.id }) { recording ->
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
    }

    // Dialogs
    if (showImportAlbumDialog) {
        com.audiovault.app.ui.components.ImportAlbumDialog(
            viewModel = viewModel,
            onDismiss = { showImportAlbumDialog = false }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showAddArtistDialog) {
        AddArtistDialog(
            onDismiss = { showAddArtistDialog = false },
            onConfirm = { name ->
                viewModel.addManualArtist(name)
                showAddArtistDialog = false
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

    artistToDelete?.let { (artistName, count) ->
        DeleteArtistConfirmDialog(
            artistName = artistName,
            songCount = count,
            onDismiss = { artistToDelete = null },
            onConfirmDelete = {
                viewModel.deleteArtist(artistName)
                if (selectedArtistForDetail == artistName) {
                    selectedArtistForDetail = null
                }
                artistToDelete = null
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
                    text = " • $trackCount ${if (trackCount == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB3B3B3)
                    )
                )
            }
        }
    }
}

/**
 * Dedicated Add Artist Row for Artists Tab.
 */
@Composable
fun AddArtistLibraryRow(onClick: () -> Unit) {
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
                .clip(CircleShape)
                .background(SpotifyDarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Artist",
                tint = SpotifyGreenBright,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Add Artist",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Scan & group songs by artist name",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB3B3B3)
                )
            )
        }
    }
}

/**
 * Circular Artist row matching Spotify screenshot #2.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistLibraryRow(
    artistName: String,
    songCount: Int,
    songHint: String? = null,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onClick: () -> Unit,
    onDeleteArtist: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteArtist
            )
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

        // Save / Follow Button
        IconButton(
            onClick = onToggleSave,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                contentDescription = if (isSaved) "Saved Artist" else "Save Artist",
                tint = if (isSaved) SpotifyGreenBright else Color(0xFF888888),
                modifier = Modifier.size(22.dp)
            )
        }

        // 3-dots Menu for Delete
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Artist Options",
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (isSaved) "Remove from Saved" else "Save Artist") },
                    onClick = {
                        menuExpanded = false
                        onToggleSave()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Artist", color = Color(0xFFFF4444)) },
                    onClick = {
                        menuExpanded = false
                        onDeleteArtist()
                    }
                )
            }
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
    onLyrics: () -> Unit,
    onSwipeToQueue: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    SwipeToQueueContainer(
        onSwipeToQueue = { onSwipeToQueue?.invoke() },
        enabled = onSwipeToQueue != null
    ) {
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

        com.audiovault.app.ui.components.ProcessingTimelineBadge(
            isProcessing = recording.isProcessing,
            durationMs = recording.durationMs,
            textColor = Color(0xFF888888),
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
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onBack: () -> Unit,
    onSongClick: (RecordingEntity) -> Unit,
    onDeleteArtist: () -> Unit,
    onRename: (RecordingEntity) -> Unit,
    onDelete: (RecordingEntity) -> Unit,
    onLyrics: (RecordingEntity) -> Unit,
    onSwipeToQueue: ((RecordingEntity) -> Unit)? = null
) {
    var artistMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Back Arrow Header with Options & Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                    text = "Artist",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        contentDescription = if (isSaved) "Saved Artist" else "Save Artist",
                        tint = if (isSaved) SpotifyGreenBright else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box {
                    IconButton(onClick = { artistMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Artist Options",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = artistMenuExpanded,
                        onDismissRequest = { artistMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Remove from Saved" else "Save Artist") },
                            onClick = {
                                artistMenuExpanded = false
                                onToggleSave()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Artist", color = Color(0xFFFF4444)) },
                            onClick = {
                                artistMenuExpanded = false
                                onDeleteArtist()
                            }
                        )
                    }
                }
            }
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
                    onLyrics = { onLyrics(song) },
                    onSwipeToQueue = onSwipeToQueue?.let { cb -> { cb(song) } }
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
 * Liked Songs Grid Card for Spotify-style Grid View.
 */
@Composable
fun LikedSongsGridCard(
    trackCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Liked Songs",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "📌 Playlist • $trackCount ${if (trackCount == 1) "song" else "songs"}",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Playlist Grid Card for Spotify-style Grid View.
 */
@Composable
fun PlaylistGridCard(
    playlist: com.audiovault.app.data.repository.UserPlaylist,
    songs: List<RecordingEntity>,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val songCount = playlist.songIds.size
    val firstArt = songs.firstOrNull { !it.albumArtPath.isNullOrBlank() }?.albumArtPath

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF282828)),
            contentAlignment = Alignment.Center
        ) {
            if (firstArt != null) {
                TrackCoverArt(
                    albumArtPath = firstArt,
                    title = playlist.name,
                    artist = null,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Playlist",
                    tint = SpotifyGreenBright,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Playlist • $songCount ${if (songCount == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFFB3B3B3),
                        modifier = Modifier.size(16.dp)
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
}

/**
 * Dedicated Add Artist Card for Grid View.
 */
@Composable
fun AddArtistGridCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(SpotifyDarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Artist",
                tint = SpotifyGreenBright,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Add Artist",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Scan library songs",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Artist Grid Card for Spotify-style Grid View.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistGridCard(
    artistName: String,
    songCount: Int,
    songHint: String? = null,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onClick: () -> Unit,
    onDeleteArtist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteArtist
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Circular Artist Avatar
        ArtistAvatar(
            artistName = artistName,
            songHint = songHint,
            size = 120.dp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleSmall.copy(
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
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleSave,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    contentDescription = if (isSaved) "Saved Artist" else "Save Artist",
                    tint = if (isSaved) SpotifyGreenBright else Color(0xFF888888),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Song Grid Card for Spotify-style Grid View.
 */
@Composable
fun SongGridCard(
    recording: RecordingEntity,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onLyrics: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onPlayClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            TrackCoverArt(
                albumArtPath = recording.albumArtPath,
                title = recording.title,
                artist = recording.artist,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp)
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Playing",
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                    text = recording.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFFB3B3B3),
                        modifier = Modifier.size(16.dp)
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
    onLyricsSong: (RecordingEntity) -> Unit,
    onSwipeToQueue: ((RecordingEntity) -> Unit)? = null
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

                    SwipeToQueueContainer(
                        onSwipeToQueue = { onSwipeToQueue?.invoke(song) },
                        enabled = onSwipeToQueue != null
                    ) {
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

                        com.audiovault.app.ui.components.ProcessingTimelineBadge(
                            isProcessing = song.isProcessing,
                            durationMs = song.durationMs,
                            textColor = Color(0xFF888888),
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
}

@Composable
fun DeleteArtistConfirmDialog(
    artistName: String,
    songCount: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCardSurface,
        title = {
            Text(
                text = "Remove \"$artistName\"?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        },
        text = {
            Text(
                text = "Remove \"$artistName\" from your Artists list? Your $songCount recorded track${if (songCount == 1) "" else "s"} will remain safely in your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB3B3B3)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreenBright,
                    contentColor = Color.Black
                )
            ) {
                Text("Remove Artist", fontWeight = FontWeight.Bold)
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
fun AddArtistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var artistName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCardSurface,
        title = {
            Text(
                text = "Add Artist",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Type an artist name. Recordify will automatically scan your library, group all their songs, and fetch their HD portrait.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB3B3B3)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = artistName,
                    onValueChange = { artistName = it },
                    placeholder = { Text("e.g. Arijit Singh, Taylor Swift...", color = Color(0xFF888888)) },
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
                            if (artistName.isNotBlank()) {
                                onConfirm(artistName.trim())
                            }
                        }
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (artistName.isNotBlank()) {
                        onConfirm(artistName.trim())
                    }
                },
                enabled = artistName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreenBright,
                    contentColor = Color.Black
                )
            ) {
                Text("Add Artist", fontWeight = FontWeight.Bold)
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

/**
 * Compact Category Card for Library hub navigation.
 */
@Composable
fun CategorySquareCard(
    title: String,
    countText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = modifier
            .height(64.dp)
            .shadow(3.dp, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular frosted icon badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title & count
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact Category Hub displayed at the top of the ALL tab in List View.
 */
@Composable
fun LibraryCategoryHub(
    downloadedCount: Int,
    downloadedSizeText: String,
    playlistCount: Int,
    artistCount: Int,
    songCount: Int,
    likedCount: Int,
    onDownloadedClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onSongsClick: () -> Unit,
    onLikedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CategorySquareCard(
                title = "Downloaded",
                countText = if (downloadedCount > 0) "$downloadedCount tracks • $downloadedSizeText" else "0 tracks",
                icon = Icons.Default.Download,
                gradientColors = listOf(Color(0xFF0072FF), Color(0xFF00C6FF)),
                onClick = onDownloadedClick,
                modifier = Modifier.weight(1f)
            )
            CategorySquareCard(
                title = "Liked Songs",
                countText = "$likedCount liked",
                icon = Icons.Default.Favorite,
                gradientColors = listOf(Color(0xFFEA4C89), Color(0xFFFF6B6B)),
                onClick = onLikedClick,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CategorySquareCard(
                title = "Playlists",
                countText = "$playlistCount created",
                icon = Icons.Default.QueueMusic,
                gradientColors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
                onClick = onPlaylistsClick,
                modifier = Modifier.weight(1f)
            )
            CategorySquareCard(
                title = "Artists",
                countText = "$artistCount artists",
                icon = Icons.Default.Person,
                gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                onClick = onArtistsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
