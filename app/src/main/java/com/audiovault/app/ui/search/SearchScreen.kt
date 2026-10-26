package com.audiovault.app.ui.search

import androidx.activity.compose.BackHandler
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.components.ProcessingTimelineBadge
import com.audiovault.app.ui.search.OnlineSongItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import com.audiovault.app.core.identity.RecordingVersionType
import com.audiovault.app.core.search.SearchTargetEntity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.R
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.ui.components.SubtleJellyContainer
import com.audiovault.app.ui.recordings.DeleteConfirmDialog
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.audiovault.app.ui.components.ArtistAvatar
import com.audiovault.app.ui.library.ArtistDetailView
import com.audiovault.app.ui.library.splitArtists
import com.audiovault.app.ui.recordings.LyricsDialog
import com.audiovault.app.ui.recordings.RenameDialog

data class SearchCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val queryKeyword: String? = null,
    val filterPredicate: ((RecordingEntity) -> Boolean)? = null
)

@Composable
fun SearchScreen(
    viewModel: RecordingsViewModel,
    userName: String = "",
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val allRecordings by viewModel.allRecordings.collectAsState()
    val savedArtists by viewModel.savedArtists.collectAsState()
    val manualArtists by viewModel.manualArtists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onlineSearchResults by viewModel.onlineSearchResults.collectAsState()
    val isOnlineSearching by viewModel.isOnlineSearching.collectAsState()
    val matchingAlbums by viewModel.matchingAlbums.collectAsState()
    val downloadingSongIds by viewModel.downloadingSongIds.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val isSearchOriginalOnly by viewModel.isSearchOriginalOnly.collectAsState()
    val searchContainer by viewModel.searchResultContainer.collectAsState()
    val selectedDecade by viewModel.selectedDecade.collectAsState()
    val selectedLanguageFilter by viewModel.selectedLanguageFilter.collectAsState()
    val selectedVersionFilter by viewModel.selectedVersionFilter.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    var selectedSearchTab by remember { mutableStateOf("All") }
    var showDecadeMenu by remember { mutableStateOf(false) }
    var showVersionMenu by remember { mutableStateOf(false) }
    var activeCategoryFilter by remember { mutableStateOf<SearchCategory?>(null) }
    var selectedArtistForDetail by remember { mutableStateOf<String?>(null) }
    var isRecentExpanded by remember { mutableStateOf(false) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingForLyrics by remember { mutableStateOf<RecordingEntity?>(null) }
    var artistToDelete by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // Request focus when entering active search mode
    LaunchedEffect(isSearchActive) {
        if (isSearchActive && searchQuery.isEmpty()) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Throwable) {}
        }
    }

    // Step-by-step BackHandler for artist detail, search filters, and active query
    BackHandler(enabled = selectedArtistForDetail != null) {
        selectedArtistForDetail = null
    }
    BackHandler(enabled = selectedArtistForDetail == null && activeCategoryFilter != null) {
        activeCategoryFilter = null
    }
    BackHandler(enabled = selectedArtistForDetail == null && activeCategoryFilter == null && isSearchActive) {
        isSearchActive = false
        if (searchQuery.isNotEmpty()) {
            viewModel.onSearchQueryChange("")
        }
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    // Multi-artist grouping computed across entire library
    val allArtistGroups = remember(allRecordings, savedArtists, manualArtists) {
        val map = LinkedHashMap<String, MutableList<RecordingEntity>>()
        allRecordings.forEach { song ->
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
        val extraArtists = (manualArtists + savedArtists)
        extraArtists.forEach { extraArtist ->
            val cleanExtra = extraArtist.trim()
            if (cleanExtra.isNotBlank()) {
                val existingKey = map.keys.firstOrNull { it.equals(cleanExtra, ignoreCase = true) }
                if (existingKey == null) {
                    val matching = allRecordings.filter { song ->
                        val songArtist = song.artist ?: ""
                        songArtist.contains(cleanExtra, ignoreCase = true) ||
                        song.title.contains(cleanExtra, ignoreCase = true)
                    }
                    map[cleanExtra] = matching.toMutableList()
                } else {
                    val currentList = map[existingKey] ?: mutableListOf()
                    val matching = allRecordings.filter { song ->
                        !currentList.any { it.id == song.id } &&
                        ((song.artist ?: "").contains(cleanExtra, ignoreCase = true) || song.title.contains(cleanExtra, ignoreCase = true))
                    }
                    currentList.addAll(matching)
                }
            }
        }
        map.toList().sortedByDescending { it.second.size }
    }

    val trimmedQuery = searchQuery.trim()
    val matchingArtists = remember(allArtistGroups, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList()
        else allArtistGroups.filter { (artistName, _) ->
            artistName.contains(trimmedQuery, ignoreCase = true)
        }
    }

    val categories = remember(allRecordings) {
        listOf(
            SearchCategory(
                id = "cat_bollywood",
                title = "Bollywood",
                subtitle = "Hindi movie soundtracks & hits",
                icon = Icons.Default.MusicNote,
                gradientColors = listOf(Color(0xFFE50914), Color(0xFFB20710)),
                queryKeyword = "Bollywood hits"
            ),
            SearchCategory(
                id = "cat_gujarati",
                title = "Gujarati",
                subtitle = "Folk, romantic & urban hits",
                icon = Icons.Default.LibraryMusic,
                gradientColors = listOf(Color(0xFFFF6F00), Color(0xFFE65100)),
                queryKeyword = "Gujarati romantic songs"
            ),
            SearchCategory(
                id = "cat_romantic",
                title = "Romantic",
                subtitle = "Heartfelt love songs",
                icon = Icons.Default.MusicNote,
                gradientColors = listOf(Color(0xFFE91E63), Color(0xFF880E4F)),
                queryKeyword = "Romantic songs"
            ),
            SearchCategory(
                id = "cat_pop",
                title = "Pop Hits",
                subtitle = "Chart-toppers worldwide",
                icon = Icons.Default.Headphones,
                gradientColors = listOf(Color(0xFF1DB954), Color(0xFF191414)),
                queryKeyword = "Pop viral hits"
            ),
            SearchCategory(
                id = "cat_punjabi",
                title = "Punjabi",
                subtitle = "High energy & bhangra beats",
                icon = Icons.Default.Album,
                gradientColors = listOf(Color(0xFFFBC02D), Color(0xFFF57F17)),
                queryKeyword = "Punjabi hits"
            ),
            SearchCategory(
                id = "cat_retro",
                title = "90s Retro",
                subtitle = "Golden era classics",
                icon = Icons.Default.Timer,
                gradientColors = listOf(Color(0xFF8E24AA), Color(0xFF4A148C)),
                queryKeyword = "90s Bollywood hits"
            ),
            SearchCategory(
                id = "cat_hiphop",
                title = "Hip-Hop & Rap",
                subtitle = "Beats, bars & flow",
                icon = Icons.Default.Mic,
                gradientColors = listOf(Color(0xFFFF9800), Color(0xFFF57C00)),
                queryKeyword = "Hip hop rap"
            ),
            SearchCategory(
                id = "cat_chill",
                title = "Chill & Lo-Fi",
                subtitle = "Acoustic, calm & study vibes",
                icon = Icons.Default.Headphones,
                gradientColors = listOf(Color(0xFF00ACC1), Color(0xFF006064)),
                queryKeyword = "Chill lofi acoustic"
            ),
            SearchCategory(
                id = "cat_sad",
                title = "Sad Songs",
                subtitle = "Deep & soulful melodies",
                icon = Icons.Default.MusicNote,
                gradientColors = listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)),
                queryKeyword = "Sad emotional songs"
            ),
            SearchCategory(
                id = "cat_devotional",
                title = "Devotional",
                subtitle = "Bhajans, aarti & spiritual",
                icon = Icons.Default.LibraryMusic,
                gradientColors = listOf(Color(0xFFFB8C00), Color(0xFFE65100)),
                queryKeyword = "Devotional bhajan"
            ),
            SearchCategory(
                id = "cat_rock",
                title = "Rock Classics",
                subtitle = "Guitars & anthems",
                icon = Icons.Default.MusicNote,
                gradientColors = listOf(Color(0xFFD32F2F), Color(0xFF7B1FA2)),
                queryKeyword = "Rock hits"
            ),
            SearchCategory(
                id = "cat_dance",
                title = "EDM / Dance",
                subtitle = "Club & party anthems",
                icon = Icons.Default.Album,
                gradientColors = listOf(Color(0xFF00E676), Color(0xFF00B0FF)),
                queryKeyword = "EDM dance party"
            ),
            SearchCategory(
                id = "music_all",
                title = "Your Captures",
                subtitle = "${allRecordings.size} tracks captured",
                icon = Icons.Default.MusicNote,
                gradientColors = listOf(Color(0xFF3949AB), Color(0xFF1A237E)),
                filterPredicate = { true }
            )
        )
    }

    if (selectedArtistForDetail != null) {
        val artistName = selectedArtistForDetail!!
        var onlineArtistSongs by remember(artistName) { mutableStateOf<List<RecordingEntity>>(emptyList()) }
        val localSongs = allArtistGroups.firstOrNull { it.first.equals(artistName, ignoreCase = true) }?.second ?: emptyList()

        LaunchedEffect(artistName) {
            if (localSongs.isEmpty()) {
                try {
                    val songs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.audiovault.app.core.online.OnlineMusicSearchService.fetchArtistAllSongs(artistName, limit = 40)
                    }
                    onlineArtistSongs = songs.map { com.audiovault.app.core.queue.QueueItem.fromOnlineSong(it).toRecordingEntity() }
                } catch (_: Exception) {}
            }
        }

        val displayedSongs = localSongs.ifEmpty { onlineArtistSongs }
        val isArtistSaved = savedArtists.any { it.equals(artistName, ignoreCase = true) }
        ArtistDetailView(
            artistName = artistName,
            songs = displayedSongs,
            currentPlayingId = playerState.currentRecording?.id,
            isPlaying = playerState.isPlaying,
            isSaved = isArtistSaved,
            onToggleSave = { viewModel.toggleSavedArtist(artistName) },
            onBack = { selectedArtistForDetail = null },
            onSongClick = { viewModel.playPauseRecording(it) },
            onDeleteArtist = { artistToDelete = Pair(artistName, displayedSongs.size) },
            onRename = { recordingToRename = it },
            onDelete = { recordingToDelete = it },
            onLyrics = { recordingForLyrics = it }
        )
    } else if (activeCategoryFilter != null) {
        val cat = activeCategoryFilter!!
        val filteredSongs = remember(allRecordings, cat) {
            val pred = cat.filterPredicate ?: { true }
            allRecordings.filter(pred)
        }

        CategoryDetailView(
            category = cat,
            songs = filteredSongs,
            currentPlayingId = playerState.currentRecording?.id,
            isPlaying = playerState.isPlaying,
            onBack = { activeCategoryFilter = null },
            onSongClick = { viewModel.playPauseRecording(it) }
        )
    } else {
        val searchListState = rememberLazyListState()
        SubtleJellyContainer(
            isAtTop = { searchListState.firstVisibleItemIndex == 0 && searchListState.firstVisibleItemScrollOffset == 0 },
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

                if (!isSearchActive) {
                    // Top Header: Circular Logo in Top-Left, "Search", Camera (Landing Browse state - Image 1)
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
                                text = "Search",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }

                        IconButton(onClick = { /* Camera scan */ }) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Spotify White Search Input Pill Box (Clickable landing pill)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { isSearchActive = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF121212),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "What do you want to listen to?",
                                style = TextStyle(
                                    color = Color(0xFF555555),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                } else {
                    // Active Search Top Bar (Back arrow + search input + clear button - Image 2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.onSearchQueryChange("")
                                }
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF282828))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "What do you want to listen to?",
                                            style = TextStyle(
                                                color = Color(0xFFAAAAAA),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }

                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChange(it) },
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        singleLine = true,
                                        cursorBrush = SolidColor(SpotifyGreenBright),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.onSearchQueryChange("")
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // If user typed a search query: show instant search results (Online Worldwide, Artists & Library)
                if (searchQuery.isNotBlank()) {
                    // Filter Chips: All, Songs, Albums, Artists, Playlists
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Songs", "Albums", "Artists", "Playlists").forEach { tab ->
                            val isSelected = selectedSearchTab == tab
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) SpotifyGreenBright else SpotifyCardSurface,
                                modifier = Modifier.clickable { selectedSearchTab = tab }
                            ) {
                                Text(
                                    text = tab,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Advanced Filter Bar: Official Only, Decade, Version
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Official / Original Only Toggle
                        Surface(
                            shape = CircleShape,
                            color = if (isSearchOriginalOnly) Color(0xFF1E3823) else SpotifyCardSurface,
                            modifier = Modifier.clickable { viewModel.toggleSearchOriginalOnly() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSearchOriginalOnly) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SpotifyGreenBright,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "Official Only",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSearchOriginalOnly) SpotifyGreenBright else Color.White
                                    )
                                )
                            }
                        }

                        // 2. Decade Filter Menu
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = if (selectedDecade != null) Color(0xFF1E3823) else SpotifyCardSurface,
                                modifier = Modifier.clickable { showDecadeMenu = true }
                            ) {
                                val label = when (selectedDecade) {
                                    1970..1979 -> "70s"
                                    1980..1989 -> "80s"
                                    1990..1999 -> "90s"
                                    2000..2009 -> "2000s"
                                    2010..2019 -> "2010s"
                                    2020..2029 -> "2020s"
                                    null -> "Decade"
                                    else -> "${selectedDecade!!.first}-${selectedDecade!!.last}"
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDecade != null) SpotifyGreenBright else Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            androidx.compose.material3.DropdownMenu(
                                expanded = showDecadeMenu,
                                onDismissRequest = { showDecadeMenu = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("All Decades") },
                                    onClick = {
                                        showDecadeMenu = false
                                        viewModel.setSelectedDecade(null)
                                    }
                                )
                                listOf(
                                    "2020s" to 2020..2029,
                                    "2010s" to 2010..2019,
                                    "2000s" to 2000..2009,
                                    "90s" to 1990..1999,
                                    "80s" to 1980..1989,
                                    "70s" to 1970..1979
                                ).forEach { (name, range) ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            showDecadeMenu = false
                                            viewModel.setSelectedDecade(range)
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Version Filter Menu
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = if (selectedVersionFilter != null) Color(0xFF1E3823) else SpotifyCardSurface,
                                modifier = Modifier.clickable { showVersionMenu = true }
                            ) {
                                val label = selectedVersionFilter?.badge ?: "Version"
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedVersionFilter != null) SpotifyGreenBright else Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            androidx.compose.material3.DropdownMenu(
                                expanded = showVersionMenu,
                                onDismissRequest = { showVersionMenu = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("All Versions") },
                                    onClick = {
                                        showVersionMenu = false
                                        viewModel.setSelectedVersionFilter(null)
                                    }
                                )
                                listOf(
                                    RecordingVersionType.ORIGINAL_STUDIO,
                                    RecordingVersionType.OFFICIAL_SOUNDTRACK,
                                    RecordingVersionType.REMIX,
                                    RecordingVersionType.ACOUSTIC,
                                    RecordingVersionType.LIVE,
                                    RecordingVersionType.KARAOKE_INSTRUMENTAL
                                ).forEach { version ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(version.displayName) },
                                        onClick = {
                                            showVersionMenu = false
                                            viewModel.setSelectedVersionFilter(version)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val resultsListState = rememberLazyListState()
                    LaunchedEffect(resultsListState.isScrollInProgress) {
                        if (resultsListState.isScrollInProgress) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }

                    val canonicalSongs = searchContainer?.songs ?: emptyList()
                    val displayedCanonical = canonicalSongs.filter { song ->
                        val decadeOk = selectedDecade == null || (song.releaseYear != null && song.releaseYear in selectedDecade!!)
                        val versionOk = selectedVersionFilter == null || song.recordingVersion == selectedVersionFilter
                        decadeOk && versionOk
                    }
                    val displayedOnlineSongs = if (displayedCanonical.isNotEmpty()) {
                        displayedCanonical.map { it.toOnlineSongModel() }
                    } else {
                        onlineSearchResults
                    }

                    val displayedAlbums = (searchContainer?.albums ?: matchingAlbums).filter { album ->
                        val albumYear = album.year?.filter { it.isDigit() }?.toIntOrNull()
                        selectedDecade == null || (albumYear != null && albumYear in selectedDecade!!)
                    }

                    val isAlbumIntent = searchContainer?.intent?.targetEntity == SearchTargetEntity.ALBUMS ||
                            searchContainer?.intent?.queryType == com.audiovault.app.core.search.SearchQueryType.ALBUM_SEARCH ||
                            searchContainer?.intent?.queryType == com.audiovault.app.core.search.SearchQueryType.SOUNDTRACK_SEARCH

                    val searchSuggestions = remember(searchQuery) {
                        com.audiovault.app.core.search.SearchSuggestionEngine.getLocalSuggestions(searchQuery)
                    }

                    val qLower = searchQuery.trim().lowercase()
                    val isOldSongsQuery = qLower.contains("old") || qLower == "olds" || qLower.contains("puran") || qLower.contains("retro")
                    val isSadSongsQuery = qLower.contains("sad") || qLower.contains("dard")
                    val onlineArtists = searchContainer?.artists ?: emptyList()

                    LazyColumn(
                        state = resultsListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        // 1. TOP SPOTIFY AUTOCOMPLETE SUGGESTIONS (Search icon on left, Up-right arrow on right)
                        if (searchSuggestions.isNotEmpty() && (selectedSearchTab == "All" || selectedSearchTab == "Songs")) {
                            items(searchSuggestions) { suggestionText ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            viewModel.onSearchQueryChange(suggestionText)
                                            viewModel.searchOnlineMusic(suggestionText)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFFB3B3B3),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = suggestionText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.onSearchQueryChange(suggestionText)
                                            viewModel.searchOnlineMusic(suggestionText)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text(
                                            text = "↗",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color(0xFFB3B3B3),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Light
                                            )
                                        )
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // 2. DISPATCH CONTENT BY TAB AND INTENT
                        if (selectedSearchTab == "All") {
                            when {
                                isSadSongsQuery -> {
                                    // Sad songs intent: Playlists at top, then songs
                                    val sadPlaylists = displayedAlbums.filter { it.type == "Playlist" || it.title.contains("Sad", ignoreCase = true) || it.title.contains("Arijit", ignoreCase = true) }.take(2)
                                    items(sadPlaylists, key = { "sad_pl_${it.id}" }) { album ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    viewModel.openAlbumDetails(album)
                                                }
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TrackCoverArt(
                                                albumArtPath = album.coverArtUrl,
                                                title = album.title,
                                                artist = album.artist ?: "",
                                                size = 52.dp,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = album.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 15.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = album.artist ?: "Playlist • Recordify",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = Color(0xFFB3B3B3),
                                                        fontSize = 13.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    items(displayedOnlineSongs, key = { "online_${it.id}" }) { onlineSong ->
                                        val isCurrentPlayingOnline = playerState.isPlaying &&
                                                (playerState.currentRecording?.filePath == onlineSong.streamUrl)
                                        val isSaved = viewModel.isOnlineSongSavedInLibrary(onlineSong)
                                        val isDownloading = downloadingSongIds.contains(onlineSong.id)
                                        val progress = downloadProgress[onlineSong.id]
                                        val versionBadge = displayedCanonical.firstOrNull { it.providerTrackId == onlineSong.id }?.recordingVersion?.badge

                                        OnlineSongItem(
                                            song = onlineSong,
                                            isPlaying = isCurrentPlayingOnline,
                                            isSavedInLibrary = isSaved,
                                            isDownloading = isDownloading,
                                            downloadProgress = progress,
                                            versionBadge = versionBadge,
                                            onPlayClick = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                viewModel.playOnlineSong(onlineSong)
                                            },
                                            onDownloadClick = { viewModel.downloadOnlineSong(onlineSong) },
                                            onCancelDownload = { viewModel.cancelDownload(onlineSong.id) }
                                        )
                                    }
                                }

                                isOldSongsQuery -> {
                                    // Old songs intent: 2 songs -> 90s bollywood playlist -> 1 song -> Old is Gold playlist -> Kishore Kumar artist -> remaining songs
                                    val top2Songs = displayedOnlineSongs.take(2)
                                    val playlist90s = displayedAlbums.firstOrNull { it.id.contains("90s") || it.title.contains("90s", ignoreCase = true) } ?: displayedAlbums.getOrNull(0)
                                    val song3 = displayedOnlineSongs.getOrNull(2)
                                    val playlistOldIsGold = displayedAlbums.firstOrNull { it.id.contains("gold") || it.title.contains("Gold", ignoreCase = true) } ?: displayedAlbums.getOrNull(1)
                                    val kishoreArtist = onlineArtists.firstOrNull { it.name.contains("Kishore", ignoreCase = true) } ?: onlineArtists.firstOrNull()
                                    val remainingSongs = displayedOnlineSongs.drop(3)

                                    items(top2Songs, key = { "old_top_${it.id}" }) { onlineSong ->
                                        val isCurrentPlayingOnline = playerState.isPlaying && (playerState.currentRecording?.filePath == onlineSong.streamUrl)
                                        val isSaved = viewModel.isOnlineSongSavedInLibrary(onlineSong)
                                        val isDownloading = downloadingSongIds.contains(onlineSong.id)
                                        val progress = downloadProgress[onlineSong.id]
                                        val versionBadge = displayedCanonical.firstOrNull { it.providerTrackId == onlineSong.id }?.recordingVersion?.badge

                                        OnlineSongItem(
                                            song = onlineSong,
                                            isPlaying = isCurrentPlayingOnline,
                                            isSavedInLibrary = isSaved,
                                            isDownloading = isDownloading,
                                            downloadProgress = progress,
                                            versionBadge = versionBadge,
                                            onPlayClick = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                viewModel.playOnlineSong(onlineSong)
                                            },
                                            onDownloadClick = { viewModel.downloadOnlineSong(onlineSong) },
                                            onCancelDownload = { viewModel.cancelDownload(onlineSong.id) }
                                        )
                                    }

                                    if (playlist90s != null) {
                                        item(key = "old_pl_90s") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        viewModel.openAlbumDetails(playlist90s)
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TrackCoverArt(
                                                    albumArtPath = playlist90s.coverArtUrl,
                                                    title = playlist90s.title,
                                                    artist = playlist90s.artist ?: "",
                                                    size = 52.dp,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = playlist90s.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 15.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = playlist90s.artist ?: "Playlist • Recordify",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFFB3B3B3),
                                                            fontSize = 13.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (song3 != null) {
                                        item(key = "old_s3_${song3.id}") {
                                            val isCurrentPlayingOnline = playerState.isPlaying && (playerState.currentRecording?.filePath == song3.streamUrl)
                                            val isSaved = viewModel.isOnlineSongSavedInLibrary(song3)
                                            val isDownloading = downloadingSongIds.contains(song3.id)
                                            val progress = downloadProgress[song3.id]
                                            val versionBadge = displayedCanonical.firstOrNull { it.providerTrackId == song3.id }?.recordingVersion?.badge

                                            OnlineSongItem(
                                                song = song3,
                                                isPlaying = isCurrentPlayingOnline,
                                                isSavedInLibrary = isSaved,
                                                isDownloading = isDownloading,
                                                downloadProgress = progress,
                                                versionBadge = versionBadge,
                                                onPlayClick = {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    viewModel.playOnlineSong(song3)
                                                },
                                                onDownloadClick = { viewModel.downloadOnlineSong(song3) },
                                                onCancelDownload = { viewModel.cancelDownload(song3.id) }
                                            )
                                        }
                                    }

                                    if (playlistOldIsGold != null) {
                                        item(key = "old_pl_gold") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        viewModel.openAlbumDetails(playlistOldIsGold)
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TrackCoverArt(
                                                    albumArtPath = playlistOldIsGold.coverArtUrl,
                                                    title = playlistOldIsGold.title,
                                                    artist = playlistOldIsGold.artist ?: "",
                                                    size = 52.dp,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = playlistOldIsGold.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 15.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = playlistOldIsGold.artist ?: "Playlist • Recordify",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFFB3B3B3),
                                                            fontSize = 13.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (kishoreArtist != null) {
                                        item(key = "old_art_${kishoreArtist.id}") {
                                            ArtistSearchResultItem(
                                                artistName = kishoreArtist.name,
                                                songCount = 0,
                                                songHint = "Artist",
                                                onClick = {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    selectedArtistForDetail = kishoreArtist.name
                                                }
                                            )
                                        }
                                    }

                                    items(remainingSongs, key = { "old_rem_${it.id}" }) { onlineSong ->
                                        val isCurrentPlayingOnline = playerState.isPlaying && (playerState.currentRecording?.filePath == onlineSong.streamUrl)
                                        val isSaved = viewModel.isOnlineSongSavedInLibrary(onlineSong)
                                        val isDownloading = downloadingSongIds.contains(onlineSong.id)
                                        val progress = downloadProgress[onlineSong.id]
                                        val versionBadge = displayedCanonical.firstOrNull { it.providerTrackId == onlineSong.id }?.recordingVersion?.badge

                                        OnlineSongItem(
                                            song = onlineSong,
                                            isPlaying = isCurrentPlayingOnline,
                                            isSavedInLibrary = isSaved,
                                            isDownloading = isDownloading,
                                            downloadProgress = progress,
                                            versionBadge = versionBadge,
                                            onPlayClick = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                viewModel.playOnlineSong(onlineSong)
                                            },
                                            onDownloadClick = { viewModel.downloadOnlineSong(onlineSong) },
                                            onCancelDownload = { viewModel.cancelDownload(onlineSong.id) }
                                        )
                                    }
                                }

                                else -> {
                                    // General All tab search
                                    if (isAlbumIntent && displayedAlbums.isNotEmpty()) {
                                        items(displayedAlbums, key = { "album_${it.id}" }) { album ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        viewModel.openAlbumDetails(album)
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TrackCoverArt(
                                                    albumArtPath = album.coverArtUrl,
                                                    title = album.title,
                                                    artist = album.artist ?: "",
                                                    size = 52.dp,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = album.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 15.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = album.artist ?: "Album",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFFB3B3B3),
                                                            fontSize = 13.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        if (displayedAlbums.isNotEmpty()) {
                                            items(displayedAlbums.take(2), key = { "compact_album_${it.id}" }) { album ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                            viewModel.openAlbumDetails(album)
                                                        }
                                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TrackCoverArt(
                                                        albumArtPath = album.coverArtUrl,
                                                        title = album.title,
                                                        artist = album.artist ?: "",
                                                        size = 48.dp,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = album.title,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = album.artist ?: "Album",
                                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        items(displayedOnlineSongs, key = { "online_${it.id}" }) { onlineSong ->
                                            val isCurrentPlayingOnline = playerState.isPlaying && (playerState.currentRecording?.filePath == onlineSong.streamUrl)
                                            val isSaved = viewModel.isOnlineSongSavedInLibrary(onlineSong)
                                            val isDownloading = downloadingSongIds.contains(onlineSong.id)
                                            val progress = downloadProgress[onlineSong.id]
                                            val versionBadge = displayedCanonical.firstOrNull { it.providerTrackId == onlineSong.id }?.recordingVersion?.badge

                                            OnlineSongItem(
                                                song = onlineSong,
                                                isPlaying = isCurrentPlayingOnline,
                                                isSavedInLibrary = isSaved,
                                                isDownloading = isDownloading,
                                                downloadProgress = progress,
                                                versionBadge = versionBadge,
                                                onPlayClick = {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    viewModel.playOnlineSong(onlineSong)
                                                },
                                                onDownloadClick = { viewModel.downloadOnlineSong(onlineSong) },
                                                onCancelDownload = { viewModel.cancelDownload(onlineSong.id) }
                                            )
                                        }

                                        if (onlineArtists.isNotEmpty() || matchingArtists.isNotEmpty()) {
                                            items(matchingArtists.take(2), key = { "art_match_${it.first}" }) { (artistName, songs) ->
                                                ArtistSearchResultItem(
                                                    artistName = artistName,
                                                    songCount = songs.size,
                                                    songHint = songs.firstOrNull()?.title,
                                                    onClick = {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        selectedArtistForDetail = artistName
                                                    }
                                                )
                                            }
                                            items(onlineArtists.take(2), key = { "online_art_${it.id}" }) { artist ->
                                                ArtistSearchResultItem(
                                                    artistName = artist.name,
                                                    songCount = 0,
                                                    songHint = "Popular Artist",
                                                    onClick = {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        selectedArtistForDetail = artist.name
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (selectedSearchTab == "Songs") {
                            items(displayedOnlineSongs, key = { "online_song_tab_${it.id}" }) { onlineSong ->
                                val isCurrentPlayingOnline = playerState.isPlaying && (playerState.currentRecording?.filePath == onlineSong.streamUrl)
                                val isSaved = viewModel.isOnlineSongSavedInLibrary(onlineSong)
                                val isDownloading = downloadingSongIds.contains(onlineSong.id)
                                val progress = downloadProgress[onlineSong.id]
                                val versionBadge = displayedCanonical.firstOrNull { it.providerTrackId == onlineSong.id }?.recordingVersion?.badge

                                OnlineSongItem(
                                    song = onlineSong,
                                    isPlaying = isCurrentPlayingOnline,
                                    isSavedInLibrary = isSaved,
                                    isDownloading = isDownloading,
                                    downloadProgress = progress,
                                    versionBadge = versionBadge,
                                    onPlayClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        viewModel.playOnlineSong(onlineSong)
                                    },
                                    onDownloadClick = { viewModel.downloadOnlineSong(onlineSong) },
                                    onCancelDownload = { viewModel.cancelDownload(onlineSong.id) }
                                )
                            }
                        } else if (selectedSearchTab == "Albums" || selectedSearchTab == "Playlists") {
                            val filteredAlbums = if (selectedSearchTab == "Playlists") {
                                displayedAlbums.filter { it.type == "Playlist" || it.source == "curated" }
                            } else {
                                displayedAlbums
                            }
                            items(filteredAlbums, key = { "album_tab_${it.id}" }) { album ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            viewModel.openAlbumDetails(album)
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TrackCoverArt(
                                        albumArtPath = album.coverArtUrl,
                                        title = album.title,
                                        artist = album.artist ?: "",
                                        size = 52.dp,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = album.artist ?: if (selectedSearchTab == "Playlists") "Playlist • Recordify" else "Album",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFB3B3B3),
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else if (selectedSearchTab == "Artists") {
                            items(matchingArtists, key = { "artist_tab_${it.first}" }) { (artistName, songs) ->
                                ArtistSearchResultItem(
                                    artistName = artistName,
                                    songCount = songs.size,
                                    songHint = songs.firstOrNull()?.title,
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        selectedArtistForDetail = artistName
                                    }
                                )
                            }
                            items(onlineArtists, key = { "online_art_tab_${it.id}" }) { artist ->
                                ArtistSearchResultItem(
                                    artistName = artist.name,
                                    songCount = 0,
                                    songHint = "Popular Artist",
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        selectedArtistForDetail = artist.name
                                    }
                                )
                            }
                        }

                        // Playlists Section
                        if (selectedSearchTab == "All" || selectedSearchTab == "Playlists") {
                            val matchingPlaylists = playlists.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
                            if (matchingPlaylists.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Playlists",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                items(matchingPlaylists, key = { "playlist_${it.id}" }) { playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SpotifyCardSurface,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.LibraryMusic,
                                                    contentDescription = null,
                                                    tint = SpotifyGreenBright,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Playlist • ${playlist.songIds.size} songs",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Empty State if no results found
                        val hasAnyResult = displayedOnlineSongs.isNotEmpty() ||
                                displayedAlbums.isNotEmpty() ||
                                matchingArtists.isNotEmpty()

                        if (!hasAnyResult && !isOnlineSearching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No results found for \"$searchQuery\"",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Try searching for a song name, movie, artist or album.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF999999),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!isSearchActive) {
                    // 1. LANDING BROWSE PAGE (Image 1) - Search bar not clicked
                    // Curated albums (90s, old songs, classics) & Explore categories
                    LazyColumn(
                        state = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
                    ) {
                        // Curated Internet Albums: 90s, Old Songs, Classics
                        item {
                            Column(modifier = Modifier.padding(bottom = 18.dp)) {
                                Text(
                                    text = "Timeless Eras & Curated Albums",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    com.audiovault.app.core.catalog.CuratedAlbumCatalog.curatedAlbums.forEach { curated ->
                                        CuratedAlbumCard(
                                            album = curated,
                                            onClick = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                viewModel.openAlbumDetails(curated)
                                            },
                                            onPlayClick = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                viewModel.playAlbumSoundtrack(curated, 0)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Explore Categories",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                categories.chunked(2).forEach { rowCats ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowCats.forEach { category ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(96.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Brush.linearGradient(category.gradientColors))
                                                    .clickable {
                                                        if (category.queryKeyword != null) {
                                                            isSearchActive = true
                                                            viewModel.onSearchQueryChange(category.queryKeyword)
                                                            viewModel.searchOnlineMusic(category.queryKeyword)
                                                        } else if (category.filterPredicate != null) {
                                                            activeCategoryFilter = category
                                                        }
                                                    }
                                                    .padding(12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = category.title,
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Bottom
                                                    ) {
                                                        Text(
                                                            text = category.subtitle,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = Color(0xCCFFFFFF),
                                                                fontSize = 11.sp
                                                            )
                                                        )
                                                        Icon(
                                                            imageVector = category.icon,
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (rowCats.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 2. ACTIVE SEARCH RECENTS (Search bar clicked, query is empty - Image 2)
                    LazyColumn(
                        state = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recents",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 20.sp
                                    )
                                )

                                if (recentSearches.isNotEmpty()) {
                                    Text(
                                        text = "Clear all",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFB3B3B3),
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                                    )
                                }
                            }
                        }

                        if (recentSearches.isEmpty() && recordings.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Play songs or search to build your recents.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF888888)),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // 1. Online / recent search items
                            items(recentSearches.take(30), key = { "recent_${it.id}" }) { item ->
                                val isPlaying = playerState.isPlaying && (
                                    (item.isOnline && playerState.currentRecording?.title.equals(item.title, ignoreCase = true)) ||
                                    (!item.isOnline && playerState.currentRecording?.id == item.localRecordingId)
                                )
                                RecentSearchHistoryRow(
                                    item = item,
                                    isPlaying = isPlaying,
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        viewModel.playRecentSearchItem(item)
                                    },
                                    onRemoveClick = { viewModel.removeRecentSearch(item.id) },
                                    onAddClick = {
                                        val id = item.localRecordingId ?: (item.id.hashCode().toLong().let { if (it > 0) -it else it })
                                        val fallback = if (item.isOnline) {
                                            RecordingEntity(
                                                id = id,
                                                fileName = "${item.id}.m4a",
                                                filePath = item.streamUrl ?: "",
                                                title = item.title,
                                                artist = item.artist,
                                                albumArtPath = item.albumArtPath,
                                                createdAt = item.timestamp,
                                                durationMs = 0L,
                                                fileSizeBytes = 0L,
                                                mimeType = "audio/mp4"
                                            )
                                        } else null
                                        viewModel.toggleLikedSong(id, fallback)
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            // 2. If recentSearches is small, also show recently captured recordings
                            if (recentSearches.size < 6 && recordings.isNotEmpty()) {
                                items(recordings.take(8), key = { "rec_${it.id}" }) { recording ->
                                    val isPlaying = playerState.isPlaying && playerState.currentRecording?.id == recording.id
                                    RecentSearchItem(
                                        recording = recording,
                                        isPlaying = isPlaying,
                                        onClick = { viewModel.playPauseRecording(recording) },
                                        onDeleteClick = { recordingToDelete = recording }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Permanent Delete Confirmation Dialog
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

    // Rename Song Dialog
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

    // Lyrics Dialog
    recordingForLyrics?.let { recording ->
        LyricsDialog(
            recording = recording,
            onDismiss = { recordingForLyrics = null },
            onSaveLyrics = { lyrics ->
                viewModel.updateLyrics(recording, lyrics)
                recordingForLyrics = null
            }
        )
    }

    // Remove Artist Dialog
    artistToDelete?.let { (artistName, count) ->
        AlertDialog(
            onDismissRequest = { artistToDelete = null },
            title = {
                Text(
                    text = "Remove Artist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '$artistName'? All $count songs by this artist will remain safe and intact in your library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB3B3B3)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteArtist(artistName)
                        artistToDelete = null
                        if (selectedArtistForDetail == artistName) {
                            selectedArtistForDetail = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { artistToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = SpotifyCardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Dedicated category drill-down view showing filtered tracks with instant playback.
 */
@Composable
fun CategoryDetailView(
    category: SearchCategory,
    songs: List<RecordingEntity>,
    currentPlayingId: Long?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (RecordingEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Back bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(category.gradientColors))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xEEFFFFFF)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${songs.size} tracks available",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Track List
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracks in this category yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF888888)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    val isPlayingTrack = isPlaying && currentPlayingId == song.id
                    SearchResultItem(
                        recording = song,
                        isPlaying = isPlayingTrack,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    recording: RecordingEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            TrackCoverArt(
                albumArtPath = recording.albumArtPath,
                title = recording.title,
                artist = recording.artist,
                size = 48.dp,
                shape = RoundedCornerShape(4.dp)
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Playing",
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recording.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) SpotifyGreenBright else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Song • ${recording.artist ?: "Unknown Artist"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (recording.isProcessing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ProcessingTimelineBadge(isProcessing = true, durationMs = recording.durationMs)
                }
            }
        }

        if (isPlaying) {
            Surface(
                shape = CircleShape,
                color = SpotifyGreenBright,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentSearchItem(
    recording: RecordingEntity,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            TrackCoverArt(
                albumArtPath = recording.albumArtPath,
                title = recording.title,
                artist = recording.artist,
                size = 46.dp,
                shape = RoundedCornerShape(4.dp)
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Playing",
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recording.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) SpotifyGreenBright else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Song • ${recording.artist ?: "Unknown Artist"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (recording.isProcessing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ProcessingTimelineBadge(isProcessing = true, durationMs = recording.durationMs)
                }
            }
        }

        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete Song",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ArtistSearchResultItem(
    artistName: String,
    songCount: Int,
    songHint: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistAvatar(
            artistName = artistName,
            songHint = songHint,
            size = 52.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (songCount > 0) "Artist • $songCount ${if (songCount == 1) "track" else "tracks"}" else "Artist",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = CircleShape,
            color = SpotifyCardSurface,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = SpotifyGreenBright,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun RecentSearchHistoryRow(
    item: com.audiovault.app.data.repository.RecentlyPlayedSearchItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onAddClick: () -> Unit = {}
) {
    val isArtist = item.artist.equals("Artist", ignoreCase = true) || item.artist.isBlank()
    val shape = if (isArtist) CircleShape else RoundedCornerShape(4.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            TrackCoverArt(
                albumArtPath = item.albumArtPath,
                title = item.title,
                artist = item.artist,
                size = 48.dp,
                shape = shape
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(shape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Playing",
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) SpotifyGreenBright else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = when {
                item.isAlbum -> "Album • ${item.artist}"
                isArtist -> "Artist"
                else -> "Song • ${item.artist}"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color(0xFFB3B3B3),
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onRemoveClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove Search",
                tint = Color(0xFFB3B3B3),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
