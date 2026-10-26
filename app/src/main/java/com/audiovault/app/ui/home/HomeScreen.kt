package com.audiovault.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.ArtistItemModel
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.online.RadioMixModel
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.ui.components.ProfileAvatarButton
import com.audiovault.app.ui.components.SubtleJellyContainer
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    recordingsViewModel: RecordingsViewModel,
    userName: String = "",
    onOpenDrawer: () -> Unit = {},
    onNavigateToSearch: (String?) -> Unit = {},
    onNavigateToRecord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playerState by recordingsViewModel.playerState.collectAsState()
    val allRecordings by recordingsViewModel.allRecordings.collectAsState()
    val recentSearches by recordingsViewModel.recentSearches.collectAsState()
    val homeContent by recordingsViewModel.homeContent.collectAsState()
    val isHomeLoading by recordingsViewModel.isHomeLoading.collectAsState()

    val homeFeed by recordingsViewModel.homeFeed.collectAsState()
    val currentVibe by recordingsViewModel.currentVibe.collectAsState()
    val userProfile by recordingsViewModel.userProfile.collectAsState()
    val smartQueue by recordingsViewModel.smartQueue.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var showDebugDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        recordingsViewModel.loadHomeContent(forceRefresh = true)
    }

    val scrollState = rememberScrollState()

    if (showDebugDialog) {
        RecommendationDebugDialog(
            profile = userProfile,
            currentVibe = currentVibe,
            queueSize = smartQueue.size,
            onDismiss = { showDebugDialog = false },
            onChangeVibe = { vibe ->
                recordingsViewModel.changeVibe(vibe)
            },
            onRefreshHome = {
                recordingsViewModel.loadHomeContent(forceRefresh = true)
            }
        )
    }

    Scaffold(
        containerColor = SpotifyBlack,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        modifier = modifier
    ) { paddingValues ->
        SubtleJellyContainer(
            isAtTop = { scrollState.value == 0 },
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
                .padding(paddingValues)
        ) { jellyModifier ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(jellyModifier)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Top Header: User Profile avatar + Filter Pills (All, Music, Podcasts)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatarButton(
                        userName = userName,
                        onClick = onOpenDrawer,
                        size = 36.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Category Filter Pills: All, Music, Podcasts
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Music", "Podcasts").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) SpotifyGreenBright else Color(0xFF282828))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = filter,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // DYNAMIC GREETING & VIBE HERO HEADER
                val greetingName = if (userName.isNotBlank()) ", $userName" else ""
                val greetingTitle = "${currentVibe.greeting}$greetingName"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greetingTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 22.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            val flag = com.audiovault.app.data.model.MusicPreferenceCatalog.COUNTRIES.firstOrNull {
                                it.code.equals(homeFeed.userCountryCode, ignoreCase = true)
                            }?.flagEmoji ?: "🇮🇳"
                            Text(
                                text = "$flag ${homeFeed.userCountryName} • ${recordingsViewModel.userSettings.value.primaryLanguage}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF888888),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Interactive Vibe Badge (Tap to refresh vibe, long-press for intelligence diagnostics)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF202020))
                            .combinedClickable(
                                onClick = { recordingsViewModel.refreshVibe() },
                                onLongClick = { showDebugDialog = true }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${currentVibe.category.emoji} ${currentVibe.vibeName}",
                            color = SpotifyGreenBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Change Vibe",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2x3 "Recently Played" Quick Grid (Prioritizes actual recently played tracks)
                val quickTiles = remember(recentSearches, allRecordings, homeFeed.quickTiles) {
                    val list = mutableListOf<QuickTileItem>()
                    val seenTitles = mutableSetOf<String>()

                    // 1. User's actual recently played/searched songs
                    for (search in recentSearches) {
                        val key = search.title.trim().lowercase()
                        if (key.isNotBlank() && seenTitles.add(key)) {
                            list.add(
                                QuickTileItem(
                                    title = search.title,
                                    coverArt = search.albumArtPath,
                                    streamUrl = search.streamUrl,
                                    onClick = { recordingsViewModel.playRecentSearchItem(search) }
                                )
                            )
                            if (list.size == 6) break
                        }
                    }

                    // 2. Supplement with downloaded/local recordings (newest first)
                    if (list.size < 6) {
                        val sortedLocal = allRecordings.sortedByDescending { it.createdAt }
                        for (rec in sortedLocal) {
                            val key = rec.title.trim().lowercase()
                            if (key.isNotBlank() && seenTitles.add(key)) {
                                list.add(
                                    QuickTileItem(
                                        title = rec.title,
                                        coverArt = rec.albumArtPath,
                                        streamUrl = rec.filePath,
                                        onClick = { recordingsViewModel.playPauseRecording(rec) }
                                    )
                                )
                                if (list.size == 6) break
                            }
                        }
                    }

                    // 3. Fallback to recommendation quick tiles if listening history is sparse
                    if (list.size < 6) {
                        for (song in homeFeed.quickTiles) {
                            val key = song.title.trim().lowercase()
                            if (key.isNotBlank() && seenTitles.add(key)) {
                                list.add(
                                    QuickTileItem(
                                        title = song.title,
                                        coverArt = song.highResArtUrl,
                                        streamUrl = song.streamUrl,
                                        onClick = { recordingsViewModel.playOnlineSong(song) }
                                    )
                                )
                                if (list.size == 6) break
                            }
                        }
                    }

                    list
                }

                if (quickTiles.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rows = quickTiles.chunked(2)
                        for (row in rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (tile in row) {
                                    val isPlaying = playerState.isPlaying && (
                                        (tile.streamUrl != null && playerState.currentRecording?.filePath == tile.streamUrl) ||
                                        playerState.currentRecording?.title.equals(tile.title, ignoreCase = true)
                                    )
                                    SpotifyQuickTile(
                                        title = tile.title,
                                        coverArt = tile.coverArt,
                                        isPlaying = isPlaying,
                                        onClick = tile.onClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Loading Indicator
                if (isHomeLoading && homeFeed.madeForYou.isEmpty() && homeContent.popularAlbumsAndSingles.isEmpty() && homeFeed.heavyRotationSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SpotifyGreenBright,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // DYNAMIC SPOTIFY SECTION 1: Your Heavy Rotation (Top Most Played Tracks)
                if (homeFeed.heavyRotationSongs.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "Your Heavy Rotation",
                        subtitle = "Your all-time most played songs on Recordify"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.heavyRotationSongs, key = { "hr_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC SPOTIFY SECTION 2: Daily Mix 1
                if (homeFeed.dailyMix1Songs.isNotEmpty() && !homeFeed.dailyMix1Title.isNullOrBlank()) {
                    HomeSectionHeader(
                        title = homeFeed.dailyMix1Title ?: "Daily Mix 1",
                        subtitle = homeFeed.dailyMix1Subtitle ?: "Personalized mix based on your favorites"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.dailyMix1Songs, key = { "dm1_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC SPOTIFY SECTION 3: Daily Mix 2
                if (homeFeed.dailyMix2Songs.isNotEmpty() && !homeFeed.dailyMix2Title.isNullOrBlank()) {
                    HomeSectionHeader(
                        title = homeFeed.dailyMix2Title ?: "Daily Mix 2",
                        subtitle = homeFeed.dailyMix2Subtitle ?: "More favorites from your listening data"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.dailyMix2Songs, key = { "dm2_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC SPOTIFY SECTION 4: Your Top Artists
                if (homeFeed.topArtists.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "Your Top Artists",
                        subtitle = "Artists you stream the most"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(homeFeed.topArtists, key = { "spot_art_${it.name}" }) { artist ->
                            SpotifyArtistCircleCard(
                                artist = artist,
                                onClick = { onNavigateToSearch(artist.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC SPOTIFY SECTION 5: Albums & Soundtracks You Love
                if (homeFeed.topAlbums.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "Albums & Soundtracks You Love",
                        subtitle = "Frequently streamed albums from your library"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.topAlbums, key = { "spot_alb_${it.id}" }) { album ->
                            SpotifyAlbumCard(
                                album = album,
                                onClick = { recordingsViewModel.openAlbumDetails(album) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC RECOMMENDATION SECTION: Made For You
                if (homeFeed.madeForYou.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "Made For You",
                        subtitle = "Personalized tracks tuned to your taste"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.madeForYou, key = { "mfy_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC RECOMMENDATION SECTION: Because You Listened To [Seed]
                if (homeFeed.becauseYouListenedTo.isNotEmpty()) {
                    val seed = homeFeed.becauseYouListenedToTitle ?: "Your Favorites"
                    HomeSectionHeader(
                        title = "Because You Listened To $seed",
                        subtitle = "Similar artists, sounds, and vibes"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.becauseYouListenedTo, key = { "byl_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC RECOMMENDATION SECTION: Your [Vibe] Vibe
                if (homeFeed.vibeRecommendations.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "${currentVibe.category.emoji} ${currentVibe.vibeName}",
                        subtitle = "Curated for your current mood & time of day"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.vibeRecommendations, key = { "vibe_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC RECOMMENDATION SECTION: Fresh For You (Discoveries)
                if (homeFeed.freshForYou.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "Fresh For You",
                        subtitle = "Unexplored gems tailored for discovery"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.freshForYou, key = { "fresh_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // DYNAMIC RECOMMENDATION SECTION: Trending Now
                if (homeFeed.trendingNow.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "Trending Now",
                        subtitle = "What everyone is streaming right now"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.trendingNow, key = { "tr_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
                // DYNAMIC SECTION: Trending in Your Country
                if (homeFeed.trendingInCountrySongs.isNotEmpty()) {
                    HomeSectionHeader(
                        title = homeFeed.trendingInCountryTitle ?: "Trending in ${homeFeed.userCountryName}",
                        subtitle = "The most popular songs streaming in ${homeFeed.userCountryName}"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.trendingInCountrySongs, key = { "trend_country_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // DYNAMIC SECTION: Your Language Mix
                if (homeFeed.languageMixSongs.isNotEmpty()) {
                    HomeSectionHeader(
                        title = homeFeed.languageMixTitle ?: "Your Language Mix",
                        subtitle = homeFeed.languageMixSubtitle ?: "Personalized from your language preferences"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.languageMixSongs, key = { "lang_mix_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // DYNAMIC SECTION: Romantic Mix
                if (homeFeed.romanticMixSongs.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "Romantic Mix",
                        subtitle = "Heartfelt melodies and love songs"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.romanticMixSongs, key = { "rom_mix_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // DYNAMIC SECTION: Chill Mix
                if (homeFeed.chillMixSongs.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "Chill Mix",
                        subtitle = "Lo-Fi, acoustic & calming sounds"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(homeFeed.chillMixSongs, key = { "chill_mix_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // DYNAMIC SECTION: New Releases
                val newReleasesList = if (homeFeed.newReleases.isNotEmpty()) homeFeed.newReleases else homeContent.latestReleases
                if (newReleasesList.isNotEmpty()) {
                    HomeSectionHeader(
                        title = "New Releases",
                        subtitle = "Fresh official singles and albums"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(newReleasesList, key = { "new_rel_${it.id}" }) { song ->
                            val isPlaying = playerState.isPlaying && (
                                playerState.currentRecording?.filePath == song.streamUrl ||
                                playerState.currentRecording?.title.equals(song.title, ignoreCase = true)
                            )
                            val isSaved = recordingsViewModel.isOnlineSongSavedInLibrary(song)
                            SpotifySongCard(
                                song = song,
                                isPlaying = isPlaying,
                                isSaved = isSaved,
                                onClick = { recordingsViewModel.playOnlineSong(song) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // SECTION: Recommended for today Albums
                val recommendedAlbums = homeContent.recommendedToday.ifEmpty { homeContent.popularAlbumsAndSingles }
                if (recommendedAlbums.isNotEmpty()) {
                    HomeSectionHeader(title = "Recommended for today")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recommendedAlbums, key = { "rec_${it.id}" }) { album ->
                            SpotifyAlbumCard(
                                album = album,
                                onClick = { recordingsViewModel.openAlbumDetails(album) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // SECTION: Popular albums and singles
                val popularAlbums = homeContent.popularAlbumsAndSingles
                if (popularAlbums.isNotEmpty()) {
                    HomeSectionHeader(title = "Popular albums and singles")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(popularAlbums, key = { "pop_${it.id}" }) { album ->
                            SpotifyAlbumCard(
                                album = album,
                                onClick = { recordingsViewModel.openAlbumDetails(album) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // SECTION: Popular radio
                val radioMixes = homeContent.popularRadios
                if (radioMixes.isNotEmpty()) {
                    HomeSectionHeader(title = "Popular radio")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(radioMixes, key = { "rad_${it.id}" }) { radio ->
                            SpotifyRadioCard(
                                radio = radio,
                                onClick = { onNavigateToSearch(radio.title) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                // SECTION: Your favourite artists
                val favouriteArtists = homeContent.favouriteArtists
                if (favouriteArtists.isNotEmpty()) {
                    HomeSectionHeader(title = "Your favourite artists")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(favouriteArtists, key = { "fav_${it.id}" }) { artist ->
                            SpotifyArtistCircleCard(
                                artist = artist,
                                onClick = { onNavigateToSearch(artist.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }



                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp
                )
            )
        }
    }
}

// 2x3 Quick Tile
@Composable
private fun SpotifyQuickTile(
    title: String,
    coverArt: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = SpotifyDarkSurface),
        modifier = modifier.height(54.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackCoverArt(
                albumArtPath = coverArt,
                title = title,
                size = 54.dp,
                shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) SpotifyGreenBright else Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
        }
    }
}

// Dynamic Recommended Song Card (1-tap instant play)
@Composable
private fun SpotifySongCard(
    song: OnlineSongModel,
    isPlaying: Boolean,
    isSaved: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(148.dp)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(148.dp)) {
            TrackCoverArt(
                albumArtPath = song.highResArtUrl,
                title = song.title,
                artist = song.artist,
                size = 148.dp,
                shape = RoundedCornerShape(8.dp)
            )

            // Equalizer / Play indicator
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreenBright),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isSaved) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Check,
                        contentDescription = "Downloaded",
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isPlaying) SpotifyGreenBright else Color.White,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Artist subtitle
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Standard Spotify Album / Single Card
@Composable
private fun SpotifyAlbumCard(
    album: OnlineAlbumModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(152.dp)
            .clickable(onClick = onClick)
    ) {
        TrackCoverArt(
            albumArtPath = album.coverArtUrl,
            title = album.title,
            artist = album.artist,
            size = 152.dp,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Type label (Album, Compilation, Single)
        Text(
            text = album.type,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFFB3B3B3),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Title
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Artist subtitle
        Text(
            text = album.primaryArtists ?: album.artist ?: "Various Artists",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Spotify Radio Card
@Composable
private fun SpotifyRadioCard(
    radio: RadioMixModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(152.dp)
            .clickable(onClick = onClick)
    ) {
        // Colored Card with Radio badge and Overlapping Circle Cutouts
        Box(
            modifier = Modifier
                .size(152.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(radio.bgHexColor))
                .padding(10.dp)
        ) {
            // Top Row: Spotify icon + "RADIO"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                Text(
                    text = "RADIO",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            // Center Circular Photos
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                radio.artistImages.forEachIndexed { index, imgUrl ->
                    TrackCoverArt(
                        albumArtPath = imgUrl,
                        title = radio.title,
                        size = if (index == 0) 56.dp else 44.dp,
                        shape = CircleShape,
                        modifier = Modifier.border(2.dp, Color(radio.bgHexColor), CircleShape)
                    )
                }
            }

            // Bottom Name
            Text(
                text = radio.title,
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Artists subtitle under the card
        Text(
            text = radio.artists,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Circular Artist Card
@Composable
private fun SpotifyArtistCircleCard(
    artist: ArtistItemModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(112.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrackCoverArt(
            albumArtPath = artist.imageUrl,
            title = artist.name,
            size = 112.dp,
            shape = CircleShape
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private data class QuickTileItem(
    val title: String,
    val coverArt: String?,
    val streamUrl: String?,
    val onClick: () -> Unit
)
