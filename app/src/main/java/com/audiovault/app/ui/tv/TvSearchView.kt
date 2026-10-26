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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onPlaySong: (OnlineSongModel) -> Unit,
    onPlayAlbum: (OnlineAlbumModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentSearches by viewModel.recentSearches.collectAsState()
    val likedSongIds by viewModel.likedSongIds.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // -------------------------------------------------------------
        // 1. CLEAN TOP SEARCH BAR DISPLAY
        // -------------------------------------------------------------
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161616),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (searchQuery.isNotBlank()) SpotifyGreenBright else Color(0xFF2C2C2C)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotBlank()) SpotifyGreenBright else Color(0xFF888888),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = if (searchQuery.isBlank()) "Search any song, artist, album, or era (e.g. Arijit Singh, 90s Bollywood)..." else searchQuery,
                        color = if (searchQuery.isBlank()) Color(0xFF666666) else Color.White,
                        fontSize = 16.sp,
                        fontWeight = if (searchQuery.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .tvRemoteFocusable(
                                    onClick = { onSearchQueryChange("") },
                                    shape = CircleShape,
                                    focusScale = 1.15f
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = Color(0xFFB3B3B3),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. ONE-CLICK TRENDING CATEGORY PILLS (Effortless TV Discovery)
        // -------------------------------------------------------------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Discovery",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                )

                val quickCategories = listOf(
                    "🔥 Trending Hits" to "Latest trending Bollywood chartbusters",
                    "📻 90s Bollywood" to "90s Bollywood evergreen hits",
                    "💖 Romantic Melodies" to "Romantic Bollywood love songs",
                    "👑 Arijit Singh" to "Arijit Singh",
                    "⚡ Punjabi Top 50" to "Punjabi top hits songs",
                    "🎙 Sidhu Moose Wala" to "Sidhu Moose Wala",
                    "🌟 Diljit Dosanjh" to "Diljit Dosanjh",
                    "🎤 Kishore Kumar" to "Kishore Kumar golden hits",
                    "☕ Chill Lo-Fi" to "Acoustic lofi chill songs",
                    "🎉 Party Anthems" to "Party dance hits songs"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickCategories) { (displayLabel, queryKey) ->
                        val isSelected = searchQuery.equals(queryKey, ignoreCase = true) || searchQuery.equals(displayLabel, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) SpotifyGreenBright.copy(alpha = 0.25f) else Color(0xFF1C1C1C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SpotifyGreenBright else Color(0xFF2C2C2C)
                            ),
                            modifier = Modifier.tvRemoteFocusable(
                                onClick = { onSearchQueryChange(queryKey) },
                                shape = RoundedCornerShape(20.dp),
                                focusScale = 1.08f
                            )
                        ) {
                            Text(
                                text = displayLabel,
                                color = if (isSelected) SpotifyGreenBright else Color(0xFFE0E0E0),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. STREAMLINED, SIMPLE ON-SCREEN KEYPAD STRIP
        // -------------------------------------------------------------
        item {
            TvStreamlinedKeypad(
                searchQuery = searchQuery,
                onAppendLetter = { letter -> onSearchQueryChange(searchQuery + letter) },
                onBackspace = {
                    if (searchQuery.isNotEmpty()) onSearchQueryChange(searchQuery.dropLast(1))
                },
                onSpace = {
                    if (searchQuery.isNotEmpty() && !searchQuery.endsWith(" ")) {
                        onSearchQueryChange("$searchQuery ")
                    }
                },
                onClear = { onSearchQueryChange("") }
            )
        }

        // -------------------------------------------------------------
        // 4. LOADING STATE
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
        // 5. SEARCH RESULTS STATE (ALBUMS & SONGS)
        // -------------------------------------------------------------
        if (searchQuery.isNotBlank() && !isSearching) {
            if (searchResults.isEmpty() && matchingAlbums.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try searching for an artist, movie soundtrack, or select a quick category pill above.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888))
                        )
                    }
                }
            } else {
                // A. Matching Albums Shelf
                if (matchingAlbums.isNotEmpty()) {
                    item {
                        Column {
                            TvShelfHeader(
                                title = "Matching Albums & Soundtracks",
                                subtitle = "Explore complete collections"
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(matchingAlbums) { album ->
                                    TvAlbumCard(album = album, onClick = { onPlayAlbum(album) })
                                }
                            }
                        }
                    }
                }

                // B. Matching Songs Section
                if (searchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs (${searchResults.size})",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(searchResults) { song ->
                        val isLiked = likedSongIds.contains(song.id.hashCode().toLong())
                        TvSimpleSearchSongRow(
                            song = song,
                            isLiked = isLiked,
                            onPlay = { onPlaySong(song) },
                            onToggleLike = {
                                viewModel.toggleLikedSong(song.id.hashCode().toLong())
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
            // Recent Searches Shelf
            if (recentSearches.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvShelfHeader(
                                title = "Recent Searches",
                                subtitle = "Jump back in"
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF222222),
                                modifier = Modifier.tvRemoteFocusable(
                                    onClick = { viewModel.clearRecentSearches() },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = Color(0xFF888888),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(recentSearches.take(8)) { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF181818),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
                                    modifier = Modifier.tvRemoteFocusable(
                                        onClick = { onSearchQueryChange(item.title) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = SpotifyGreenBright,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(moods) { (title, queryKey, colors) ->
                            TvMoodCard(
                                title = title,
                                gradientColors = colors,
                                onClick = { onSearchQueryChange(queryKey) }
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(genres) { (title, queryKey, colors) ->
                            TvMoodCard(
                                title = title,
                                gradientColors = colors,
                                onClick = { onSearchQueryChange(queryKey) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Streamlined 2-Row Keyboard Strip for effortless TV remote typing.
 * Row 1: A B C D E F G H I J K L M
 * Row 2: N O P Q R S T U V W X Y Z [SPACE] [DEL] [CLEAR]
 */
@Composable
private fun TvStreamlinedKeypad(
    searchQuery: String,
    onAppendLetter: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val row1 = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M")
    val row2 = listOf("N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF121212), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: A - M
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            for (key in row1) {
                TvSimpleKeyButton(
                    text = key,
                    onClick = { onAppendLetter(key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: N - Z + Space, Del, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (key in row2) {
                TvSimpleKeyButton(
                    text = key,
                    onClick = { onAppendLetter(key) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Space
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF202020),
                modifier = Modifier
                    .weight(1.8f)
                    .height(34.dp)
                    .tvRemoteFocusable(
                        onClick = onSpace,
                        shape = RoundedCornerShape(6.dp),
                        focusScale = 1.08f
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("SPACE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Del
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF202020),
                modifier = Modifier
                    .weight(1.2f)
                    .height(34.dp)
                    .tvRemoteFocusable(
                        onClick = onBackspace,
                        shape = RoundedCornerShape(6.dp),
                        focusScale = 1.08f
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Clear
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF202020),
                modifier = Modifier
                    .weight(1.2f)
                    .height(34.dp)
                    .tvRemoteFocusable(
                        onClick = onClear,
                        shape = RoundedCornerShape(6.dp),
                        focusScale = 1.08f
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("CLEAR", color = Color(0xFFE91E63), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Individual remote-focusable Keypad key.
 */
@Composable
private fun TvSimpleKeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF1E1E1E),
        modifier = modifier
            .height(34.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(6.dp),
                focusScale = 1.15f
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
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
    onToggleLike: () -> Unit
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
