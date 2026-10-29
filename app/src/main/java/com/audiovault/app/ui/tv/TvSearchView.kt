package com.audiovault.app.ui.tv

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.catalog.CountryMusicCatalog
import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Clean, Simple & Easy 10-Foot TV Search & Discovery Experience.
 * Designed for comfortable couch viewing and effortless TV remote control:
 * - Spacious Top Search Bar with instant clear button.
 * - Prominent 1-Click Trending Search Category Pills.
 * - Compact, streamlined TV remote keyboard strip.
 * - Rich, spacious Matching Albums and Song Rows with instant 1-click remote playback.
 */
@Composable
fun TvSearchView(
    viewModel: RecordingsViewModel,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<OnlineSongModel>,
    matchingAlbums: List<OnlineAlbumModel>,
    isSearching: Boolean,
    onPlaySong: (OnlineSongModel, List<OnlineSongModel>?) -> Unit = { _, _ -> },
    onPlayAlbum: (OnlineAlbumModel) -> Unit,
    modifier: Modifier = Modifier,
    capsuleFocusRequester: FocusRequester = remember { FocusRequester() },
    onNavigateUpToHeader: () -> Unit = {}
) {
    val recentSearches by viewModel.recentSearches.collectAsState()
    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val searchContainer by viewModel.searchResultContainer.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val countryProfile = remember(userSettings.userCountryCode) {
        CountryMusicCatalog.getProfile(userSettings.userCountryCode)
    }
    // Dynamic Rotating Regional Albums & Playlists (Auto-changes every session/visit with 60+ curated & regional records)
    val discoveryAlbums = remember(countryProfile.countryCode, searchContainer?.albums) {
        val regional = countryProfile.quickAccessTiles
        val curated = CuratedAlbumCatalog.curatedAlbums
        val online = searchContainer?.albums ?: emptyList()
        (online + regional + curated).distinctBy { it.id }.shuffled().take(14)
    }
    var isCapsuleFocused by remember { mutableStateOf(false) }
    var isEditingText by remember { mutableStateOf(false) }
    var selectedDivision by remember { mutableStateOf("Real Songs") }
    var showImportAlbumDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val textFieldFocusRequester = remember { FocusRequester() }
    val firstContentFocusRequester = remember { FocusRequester() }
    val clearButtonFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = isEditingText) {
        isEditingText = false
        keyboardController?.hide()
        try {
            capsuleFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler(enabled = !isEditingText && searchQuery.isNotBlank()) {
        onSearchQueryChange("")
        try {
            capsuleFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(isEditingText) {
        if (isEditingText) {
            try {
                textFieldFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        } else {
            keyboardController?.hide()
            kotlinx.coroutines.delay(60L)
            try {
                capsuleFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isSearching) {
        if (!isEditingText) {
            if (isSearching) {
                try {
                    capsuleFocusRequester.requestFocus()
                } catch (_: Exception) {}
            } else if (searchQuery.isNotBlank()) {
                kotlinx.coroutines.delay(100L)
                if (searchResults.isNotEmpty() || matchingAlbums.isNotEmpty()) {
                    try {
                        firstContentFocusRequester.requestFocus()
                    } catch (_: Exception) {
                        try { capsuleFocusRequester.requestFocus() } catch (_: Exception) {}
                    }
                } else {
                    try {
                        capsuleFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // -------------------------------------------------------------
        // 1. CAPSULE SEARCH BAR (Native Android TV OS Keyboard Input)
        // -------------------------------------------------------------
        item {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF141414),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isCapsuleFocused || isEditingText || searchQuery.isNotBlank()) Color.White else Color(0xFF444444)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .focusRequester(capsuleFocusRequester)
                    .onFocusChanged { state ->
                        isCapsuleFocused = state.isFocused
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    onNavigateUpToHeader()
                                    true
                                }
                                Key.DirectionDown -> {
                                    try {
                                        firstContentFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (searchQuery.isNotEmpty()) {
                                        try {
                                            clearButtonFocusRequester.requestFocus()
                                            true
                                        } catch (_: Exception) { false }
                                    } else false
                                }
                                Key.DirectionLeft -> {
                                    true // Left boundary locked: stay on capsule
                                }
                                else -> false
                            }
                        } else false
                    }
                    .tvRemoteFocusable(
                        onClick = {
                            isEditingText = true
                            try {
                                textFieldFocusRequester.requestFocus()
                                keyboardController?.show()
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(50),
                        focusBorderWidth = 2.dp,
                        focusBorderColor = Color.White
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotBlank() || isCapsuleFocused || isEditingText) Color.White else Color(0xFFB3B3B3),
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "What do you want to play?",
                                color = Color(0xFF757575),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            enabled = true,
                            readOnly = !isEditingText,
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                isEditingText = false
                                keyboardController?.hide()
                                try {
                                    capsuleFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                                if (searchQuery.isNotBlank()) {
                                    viewModel.submitSearch(searchQuery)
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(textFieldFocusRequester)
                                .onFocusChanged { state ->
                                    if (!state.isFocused && isEditingText) {
                                        isEditingText = false
                                    }
                                }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.Back -> {
                                                isEditingText = false
                                                keyboardController?.hide()
                                                try {
                                                    capsuleFocusRequester.requestFocus()
                                                } catch (_: Exception) {}
                                                true
                                            }
                                            Key.Enter, Key.NumPadEnter -> {
                                                isEditingText = false
                                                keyboardController?.hide()
                                                try {
                                                    capsuleFocusRequester.requestFocus()
                                                } catch (_: Exception) {}
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.submitSearch(searchQuery)
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .focusRequester(clearButtonFocusRequester)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionLeft -> {
                                                try {
                                                    capsuleFocusRequester.requestFocus()
                                                    true
                                                } catch (_: Exception) { false }
                                            }
                                            Key.DirectionRight -> {
                                                true // Right boundary locked: stay on Clear button
                                            }
                                            Key.DirectionDown -> {
                                                try {
                                                    firstContentFocusRequester.requestFocus()
                                                } catch (_: Exception) {}
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                onNavigateUpToHeader()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .tvRemoteFocusable(
                                    onClick = {
                                        onSearchQueryChange("")
                                        try { capsuleFocusRequester.requestFocus() } catch (_: Exception) {}
                                    },
                                    shape = CircleShape,
                                    focusScale = 1.2f,
                                    focusBorderWidth = 2.dp,
                                    focusBorderColor = Color.White,
                                    focusedBackgroundColor = Color.White.copy(alpha = 0.25f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. RECENT SEARCHES & CURATED COLLECTIONS (Full HD 500x500 Album Art)
        // -------------------------------------------------------------
        if (searchQuery.isBlank()) {
            val recentList = recentSearches.take(10)
            if (recentList.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Recent searches",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 22.sp
                            ),
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(recentList) { rIdx, item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF161616),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                                    modifier = Modifier
                                        .width(160.dp)
                                        .then(if (rIdx == 0) Modifier.focusRequester(firstContentFocusRequester) else Modifier)
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionUp -> {
                                                        try {
                                                            capsuleFocusRequester.requestFocus()
                                                            true
                                                        } catch (_: Exception) { false }
                                                    }
                                                    Key.DirectionLeft -> {
                                                        if (rIdx == 0) true else false // Left-locked
                                                    }
                                                    Key.DirectionRight -> {
                                                        if (rIdx == recentList.lastIndex) true else false // Right-locked
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                        .tvRemoteFocusable(
                                            onClick = {
                                                onSearchQueryChange(item.title)
                                                viewModel.submitSearch(item.title)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            focusScale = 1.04f,
                                            focusBorderWidth = 2.dp,
                                            focusBorderColor = Color.White
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            TrackCoverArt(
                                                albumArtPath = item.albumArtPath,
                                                title = item.title,
                                                artist = item.artist,
                                                size = 140.dp,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF888888),
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (discoveryAlbums.isNotEmpty()) {
                item {
                    Column {
                        TvShelfHeader(
                            title = "Popular Playlists & Trending Albums",
                            subtitle = "Official soundtracks, chart-toppers and artist essentials in ${countryProfile.countryName}"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(discoveryAlbums) { dIdx, album ->
                                TvAlbumCard(
                                    album = album,
                                    onClick = { onPlayAlbum(album) },
                                    modifier = Modifier
                                        .then(if (recentList.isEmpty() && dIdx == 0) Modifier.focusRequester(firstContentFocusRequester) else Modifier)
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionUp -> {
                                                        if (recentList.isEmpty()) {
                                                            try {
                                                                capsuleFocusRequester.requestFocus()
                                                                true
                                                            } catch (_: Exception) { false }
                                                        } else false
                                                    }
                                                    Key.DirectionLeft -> {
                                                        if (dIdx == 0) true else false // Left-locked
                                                    }
                                                    Key.DirectionRight -> {
                                                        if (dIdx == discoveryAlbums.lastIndex) true else false // Right-locked
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. LOADING STATE
        // -------------------------------------------------------------
        if (isSearching) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = SpotifyGreenBright,
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Searching Recordify Catalog...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 5. SEARCH RESULTS STATE (TWO DIVISIONS: REAL SONGS VS YOUTUBE)
        // -------------------------------------------------------------
        if (searchQuery.isNotBlank() && !isSearching) {
            // Two Main Divisions: Real Songs vs YouTube + Paste Album Link
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val divisions = listOf("Real Songs", "YouTube")
                    divisions.forEachIndexed { dIdx, div ->
                        val isSelected = selectedDivision == div
                        val label = if (div == "Real Songs") "🎵 Real Songs (Studio Audio)" else "▶️ YouTube (Video Audio)"
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) Color.White else Color(0xFF222222),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color.White else Color(0xFF383838)
                            ),
                            modifier = Modifier
                                .height(38.dp)
                                .tvRemoteFocusable(
                                    onClick = { selectedDivision = div },
                                    shape = RoundedCornerShape(50),
                                    focusScale = 1.08f,
                                    isLeftBoundary = dIdx == 0
                                )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }

                    // Paste Album Link Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1E3823),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreenBright),
                        modifier = Modifier
                            .height(38.dp)
                            .tvRemoteFocusable(
                                onClick = { showImportAlbumDialog = true },
                                shape = RoundedCornerShape(50),
                                focusScale = 1.08f
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SpotifyGreenBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Paste Album Link",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SpotifyGreenBright,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            if (selectedDivision == "Real Songs") {
                if (searchResults.isEmpty() && matchingAlbums.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No real songs found for \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Switch to the YouTube division or paste an album link above.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888))
                            )
                        }
                    }
                } else {
                    // A. Matching Artists Shelf
                    val curatedMatchingArtists = CuratedAlbumCatalog.curatedAlbums.filter { album ->
                        album.type.equals("Artist", ignoreCase = true) &&
                        (album.title.contains(searchQuery.trim(), ignoreCase = true) ||
                         (album.primaryArtists ?: "").contains(searchQuery.trim(), ignoreCase = true))
                    }
                    val dynamicArtists = searchContainer?.artists?.map { artistItem ->
                        OnlineAlbumModel(
                            id = "artist_${artistItem.name}",
                            title = artistItem.name,
                            year = "Artist",
                            artist = "Artist • ${artistItem.name}",
                            primaryArtists = artistItem.name,
                            coverArtUrl = artistItem.imageUrl,
                            type = "Artist",
                            source = "search"
                        )
                    } ?: emptyList()
                    val matchingArtists = (curatedMatchingArtists + dynamicArtists).distinctBy { it.title.lowercase() }
                    if (matchingArtists.isNotEmpty()) {
                        item {
                            Column {
                                TvShelfHeader(
                                    title = "Artists",
                                    subtitle = "Official artist essentials"
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                                ) {
                                    itemsIndexed(matchingArtists) { aIdx, artistAlbum ->
                                        TvCircularArtistCard(
                                            name = artistAlbum.title,
                                            imageUrl = artistAlbum.coverArtUrl ?: "",
                                            onClick = { onPlayAlbum(artistAlbum) },
                                            modifier = Modifier
                                                .then(if (aIdx == 0) Modifier.focusRequester(firstContentFocusRequester) else Modifier)
                                                .onKeyEvent { keyEvent ->
                                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                                        when (keyEvent.key) {
                                                            Key.DirectionUp -> {
                                                                try {
                                                                    capsuleFocusRequester.requestFocus()
                                                                    true
                                                                } catch (_: Exception) { false }
                                                            }
                                                            Key.DirectionLeft -> {
                                                                if (aIdx == 0) true else false
                                                            }
                                                            Key.DirectionRight -> {
                                                                if (aIdx == matchingArtists.lastIndex) true else false
                                                            }
                                                            else -> false
                                                        }
                                                    } else false
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // B. Matching Albums Shelf
                    if (matchingAlbums.isNotEmpty()) {
                        item {
                            Column {
                                TvShelfHeader(
                                    title = "Matching Albums & Soundtracks",
                                    subtitle = "Explore complete collections"
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                                ) {
                                    itemsIndexed(matchingAlbums) { mIdx, album ->
                                        TvAlbumCard(
                                            album = album,
                                            onClick = { onPlayAlbum(album) },
                                            modifier = Modifier
                                                .then(if (matchingArtists.isEmpty() && mIdx == 0) Modifier.focusRequester(firstContentFocusRequester) else Modifier)
                                                .onKeyEvent { keyEvent ->
                                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                                        when (keyEvent.key) {
                                                            Key.DirectionUp -> {
                                                                if (matchingArtists.isEmpty()) {
                                                                    try {
                                                                        capsuleFocusRequester.requestFocus()
                                                                        true
                                                                    } catch (_: Exception) { false }
                                                                } else false
                                                            }
                                                            Key.DirectionLeft -> {
                                                                if (mIdx == 0) true else false
                                                            }
                                                            Key.DirectionRight -> {
                                                                if (mIdx == matchingAlbums.lastIndex) true else false
                                                            }
                                                            else -> false
                                                        }
                                                    } else false
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // C. Matching Songs Section
                    if (searchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Real Studio Songs (${searchResults.size})",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        itemsIndexed(searchResults) { sIdx, song ->
                            val isLiked = likedSongIds.contains(song.id.hashCode().toLong())
                            val isFirstResult = matchingArtists.isEmpty() && matchingAlbums.isEmpty() && sIdx == 0
                            TvSimpleSearchSongRow(
                                song = song,
                                isLiked = isLiked,
                                onPlay = {
                                    val upcoming = searchResults.drop(searchResults.indexOf(song) + 1)
                                    onPlaySong(song, upcoming)
                                },
                                onToggleLike = {
                                    viewModel.toggleLikedSong(song.id.hashCode().toLong())
                                },
                                modifier = Modifier
                                    .then(if (isFirstResult) Modifier.focusRequester(firstContentFocusRequester) else Modifier)
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionUp -> {
                                                    if (isFirstResult) {
                                                        try {
                                                            capsuleFocusRequester.requestFocus()
                                                            true
                                                        } catch (_: Exception) { false }
                                                    } else false
                                                }
                                                Key.DirectionDown -> {
                                                    if (sIdx == searchResults.lastIndex) true else false
                                                }
                                                Key.DirectionLeft -> true
                                                Key.DirectionRight -> true
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                        }
                    }
                }
            } else {
                // -------------------------------------------------------------
                // YOUTUBE DIVISION
                // -------------------------------------------------------------
                val ytSongs = searchContainer?.youtubeSongs ?: emptyList()
                if (ytSongs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No YouTube audio results found for \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFF0000),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "YOUTUBE",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Video & Live Audio Tracks (${ytSongs.size})",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }

                    itemsIndexed(ytSongs) { yIdx, song ->
                        val isLiked = likedSongIds.contains(song.id.hashCode().toLong())
                        TvSimpleSearchSongRow(
                            song = song,
                            isLiked = isLiked,
                            onPlay = {
                                val upcoming = ytSongs.drop(ytSongs.indexOf(song) + 1)
                                onPlaySong(song, upcoming)
                            },
                            onToggleLike = {
                                viewModel.toggleLikedSong(song.id.hashCode().toLong())
                            },
                            modifier = Modifier
                                .then(if (yIdx == 0) Modifier.focusRequester(firstContentFocusRequester) else Modifier)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionDown -> {
                                                if (yIdx == ytSongs.lastIndex) true else false
                                            }
                                            Key.DirectionLeft -> true
                                            Key.DirectionRight -> true
                                            else -> false
                                        }
                                    } else false
                                }
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 6. DEFAULT DISCOVERY STATE (WHEN QUERY IS EMPTY)
        // -------------------------------------------------------------
        if (searchQuery.isBlank()) {
            // Browse by Mood
            item {
                Column {
                    TvShelfHeader(
                        title = "Browse by Mood",
                        subtitle = "Find the perfect vibe for your evening"
                    )
                    val moods = listOf(
                        Triple("Relaxing Melodies", "Relaxing acoustic hindi songs", listOf(Color(0xFF1E3264), Color(0xFF0F1E3D))),
                        Triple("Romantic Love", "Romantic bollywood songs", listOf(Color(0xFF8D1B44), Color(0xFF450D21))),
                        Triple("High Energy Workout", "High energy workout gym songs", listOf(Color(0xFFE13300), Color(0xFF701A00))),
                        Triple("Focus & Study", "Chill lofi study beats", listOf(Color(0xFF503750), Color(0xFF281C28))),
                        Triple("Party Dance Hits", "Party dance hits bollywood", listOf(Color(0xFFB02897), Color(0xFF58144C))),
                        Triple("90s Nostalgia", "90s bollywood evergreen hits", listOf(Color(0xFFBA5D07), Color(0xFF5D2E04)))
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(moods) { mIdx, moodItem ->
                            val (title, queryKey, colors) = moodItem
                            TvMoodCard(
                                title = title,
                                gradientColors = colors,
                                onClick = { onSearchQueryChange(queryKey) },
                                modifier = Modifier.onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionLeft -> if (mIdx == 0) true else false
                                            Key.DirectionRight -> if (mIdx == moods.lastIndex) true else false
                                            else -> false
                                        }
                                    } else false
                                }
                            )
                        }
                    }
                }
            }

            // Popular Genres
            item {
                Column {
                    TvShelfHeader(
                        title = "Popular Genres",
                        subtitle = "Explore styles across languages"
                    )
                    val genres = listOf(
                        Triple("Bollywood Classics", "Bollywood evergreen superhits", listOf(Color(0xFF8C1932), Color(0xFF460D19))),
                        Triple("Pop & Chartbusters", "Pop viral hits songs", listOf(Color(0xFF148A08), Color(0xFF0A4504))),
                        Triple("Hip-Hop & Rap", "Indian hip hop rap hits", listOf(Color(0xFF537AA1), Color(0xFF293D50))),
                        Triple("Punjabi Top Hits", "Punjabi top hits songs", listOf(Color(0xFFD84000), Color(0xFF6C2000))),
                        Triple("Acoustic & Lo-Fi", "Acoustic lofi songs", listOf(Color(0xFF7D4B32), Color(0xFF3E2519))),
                        Triple("Rock & Anthems", "Indian rock anthems", listOf(Color(0xFF477D95), Color(0xFF233E4A)))
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(genres) { gIdx, genreItem ->
                            val (title, queryKey, colors) = genreItem
                            TvMoodCard(
                                title = title,
                                gradientColors = colors,
                                onClick = { onSearchQueryChange(queryKey) },
                                modifier = Modifier.onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionLeft -> if (gIdx == 0) true else false
                                            Key.DirectionRight -> if (gIdx == genres.lastIndex) true else false
                                            Key.DirectionDown -> true // Bottom-locked on lowest shelf
                                            else -> false
                                        }
                                    } else false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImportAlbumDialog) {
        com.audiovault.app.ui.components.ImportAlbumDialog(
            viewModel = viewModel,
            onDismiss = { showImportAlbumDialog = false }
        )
    }
}

/**
 * Clean, comfortable TV Search Song Row with square artwork, title, artist, album, duration, and instant play.
 */
@Composable
private fun TvSimpleSearchSongRow(
    song: OnlineSongModel,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161616),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
        modifier = modifier
            .fillMaxWidth()
            .tvRemoteFocusable(
                onClick = onPlay,
                shape = RoundedCornerShape(10.dp),
                focusScale = 1.02f
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackCoverArt(
                albumArtPath = song.highResArtUrl,
                title = song.title,
                artist = song.artist,
                size = 46.dp,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album ?: "Recordify"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatTvDuration(song.durationMs),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .tvRemoteFocusable(
                        onClick = onToggleLike,
                        shape = CircleShape,
                        focusScale = 1.15f
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFE91E63) else Color(0xFF777777),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
