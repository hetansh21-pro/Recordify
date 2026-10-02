package com.audiovault.app.ui.search

import androidx.activity.compose.BackHandler
import com.audiovault.app.ui.components.TrackCoverArt
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
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

data class SearchCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val filterPredicate: (RecordingEntity) -> Boolean
)

@Composable
fun SearchScreen(
    viewModel: RecordingsViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val focusManager = LocalFocusManager.current

    var activeCategoryFilter by remember { mutableStateOf<SearchCategory?>(null) }
    var isRecentExpanded by remember { mutableStateOf(false) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    // Step-by-step BackHandler for search filters and active query
    BackHandler(enabled = activeCategoryFilter != null) {
        activeCategoryFilter = null
    }
    BackHandler(enabled = activeCategoryFilter == null && searchQuery.isNotEmpty()) {
        viewModel.onSearchQueryChange("")
    }

    // Dynamic categories: Keep only "All Music Tracks" and "Recent Sessions"
    val categories = remember(recordings) {
        listOf(
            SearchCategory(
                id = "music_all",
                title = "All Music Tracks",
                subtitle = "${recordings.size} tracks captured",
                icon = Icons.Default.MusicNote,
                gradientColors = listOf(Color(0xFFE91E63), Color(0xFF880E4F)),
                filterPredicate = { true }
            ),
            SearchCategory(
                id = "recent_sessions",
                title = "Recent Sessions",
                subtitle = "${recordings.take(10).size} recent tracks",
                icon = Icons.Default.Headphones,
                gradientColors = listOf(Color(0xFF8E24AA), Color(0xFF4A148C)),
                filterPredicate = { true }
            )
        )
    }

    if (activeCategoryFilter != null) {
        val cat = activeCategoryFilter!!
        val filteredSongs = remember(recordings, cat) {
            recordings.filter(cat.filterPredicate)
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

                // Top Header: Circular Logo in Top-Left, "Search", Camera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_recordify_logo),
                            contentDescription = "Recordify",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
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

                // Spotify White Search Input Pill Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
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

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "What do you want to listen to?",
                                    style = TextStyle(
                                        color = Color(0xFF555555),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                textStyle = TextStyle(
                                    color = Color(0xFF121212),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFF121212)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF121212),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // If user typed a search query: show instant search results
                if (searchQuery.isNotBlank()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(recordings, key = { it.id }) { recording ->
                            val isPlaying = playerState.isPlaying && playerState.currentRecording?.id == recording.id
                            SearchResultItem(
                                recording = recording,
                                isPlaying = isPlaying,
                                onClick = { viewModel.playPauseRecording(recording) }
                            )
                        }
                    }
                } else {
                    // Browse by Functional Categories & Recent Captures
                    LazyColumn(
                        state = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        if (recordings.isNotEmpty()) {
                            val recentLimit = 8
                            val displayedRecordings = if (isRecentExpanded) recordings else recordings.take(recentLimit)

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Captures",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )

                                    if (recordings.size > recentLimit) {
                                        Text(
                                            text = if (isRecentExpanded) "Show Less" else "See More (${recordings.size})",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = SpotifyGreenBright
                                            ),
                                            modifier = Modifier.clickable { isRecentExpanded = !isRecentExpanded }
                                        )
                                    }
                                }
                            }

                            items(displayedRecordings, key = { it.id }) { recording ->
                                val isPlaying = playerState.isPlaying && playerState.currentRecording?.id == recording.id
                                RecentSearchItem(
                                    recording = recording,
                                    isPlaying = isPlaying,
                                    onClick = { viewModel.playPauseRecording(recording) },
                                    onDeleteClick = { recordingToDelete = recording }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            if (recordings.size > recentLimit && !isRecentExpanded) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = SpotifyCardSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable { isRecentExpanded = true }
                                    ) {
                                        Text(
                                            text = "See All ${recordings.size} Captured Songs",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = SpotifyGreenBright
                                            ),
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(20.dp))
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
                                                    .clickable { activeCategoryFilter = category }
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
            Text(
                text = "Song • ${recording.artist ?: "Unknown Artist"}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            Text(
                text = "Song • ${recording.artist ?: "Unknown Artist"}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
