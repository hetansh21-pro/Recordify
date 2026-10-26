package com.audiovault.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.online.SpotifyHomeContent
import com.audiovault.app.core.recommendation.HomeRecommendationFeed
import com.audiovault.app.core.recommendation.VibeCategory
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright
import java.util.Calendar

/**
 * 10-Foot TV Home Screen for Recordify TV.
 * Packed with rich, fresh song suggestions across genres, eras, and moods:
 * - Dynamic personalized greeting & audio master indicator.
 * - Interactive Vibe & Mood filter row that recalculates recommendations.
 * - Recently Played shelf (actual listening history).
 * - "New Releases & Fresh Hits": Latest trending tracks.
 * - "Top Chartbusters in Your Country": Regional trending hits.
 * - "Made For You • Daily Mix": Personalized discovery songs.
 * - "Romantic & Soulful Melodies": High-fidelity romantic tracks.
 * - "Relaxing Lo-Fi & Chill Lounge": Calm study and relaxation beats.
 * - "Evergreen 90s & Bollywood Classics": Timeless golden era tracks.
 * - "Punjabi & High-Energy Hits": Party and workout fuel.
 * - "Featured Albums & Soundtracks": Full complete albums.
 * - "Legendary Collections & Golden Eras": Curated retro discography.
 */
@Composable
fun TvHomeView(
    viewModel: RecordingsViewModel,
    homeFeed: HomeRecommendationFeed,
    homeContent: SpotifyHomeContent,
    onPlaySong: (OnlineSongModel, List<OnlineSongModel>?) -> Unit = { _, _ -> },
    onPlayAlbum: (OnlineAlbumModel) -> Unit,
    onSearchMood: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val userName by viewModel.userName.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currentVibe by viewModel.currentVibe.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val curatedAlbums = CuratedAlbumCatalog.curatedAlbums

    var selectedFilterPill by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All") }
    val heroFocusRequester = remember { FocusRequester() }

    // Auto-focus on primary featured content when entering Home
    LaunchedEffect(Unit) {
        repeat(3) {
            delay(50L)
            try {
                heroFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // -----------------------------------------------------------------
        // 1. HERO FEATURED HEADER BANNER (Spotify TV Match: Bollywood Mush)
        // -----------------------------------------------------------------
        val featuredAlbum = curatedAlbums.find { it.id == "curated_bollywood_mush" }
            ?: curatedAlbums.firstOrNull()

        if (featuredAlbum != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141414),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(heroFocusRequester)
                        .tvRemoteFocusable(
                            onClick = { onPlayAlbum(featuredAlbum) },
                            shape = RoundedCornerShape(16.dp),
                            focusScale = 1.02f
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Column: Big Bold Title, Recordify Subtitle & Description
                        Column(
                            modifier = Modifier
                                .weight(1.3f)
                                .padding(end = 24.dp)
                        ) {
                            Text(
                                text = featuredAlbum.title,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 34.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = SpotifyGreenBright,
                                    modifier = Modifier.size(14.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Playlist • Recordify",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Let these songs be the background score to your love story. Cover - Saiyaara",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFB3B3B3),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right Column: Stylized Artwork Card with Waveform Graphic
                        Box(
                            modifier = Modifier
                                .weight(0.7f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Subtle equalizer / waveform lines
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val barHeights = listOf(14.dp, 28.dp, 44.dp, 22.dp, 36.dp, 18.dp)
                                    barHeights.forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(h)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF333333))
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E1E1E),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                                    modifier = Modifier.size(160.dp)
                                ) {
                                    TrackCoverArt(
                                        albumArtPath = featuredAlbum.coverArtUrl,
                                        title = featuredAlbum.title,
                                        artist = featuredAlbum.primaryArtists ?: "Recordify",
                                        size = 160.dp,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 2. FILTER PILLS ROW ([All], [Music], [Podcasts & Shows])
        // -----------------------------------------------------------------
        item {
            val filterOptions = listOf("All", "Music", "Podcasts & Shows")
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterOptions.forEach { filterLabel ->
                    val isSelected = selectedFilterPill == filterLabel
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) Color.White else Color(0xFF202020),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color.White else Color(0xFF333333)
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .tvRemoteFocusable(
                                onClick = { selectedFilterPill = filterLabel },
                                shape = RoundedCornerShape(50),
                                focusScale = 1.08f
                            )
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filterLabel,
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

        // -----------------------------------------------------------------
        // 3. SIGNATURE 8 QUICK-ACCESS RECTANGULAR TILES (2 Rows x 4 Columns)
        // Authentically matches Spotify TV Home (IMG20261026111711.jpg):
        // Bollywood Mush, My songs, Aditya Gadhvi, Kumar Sanu All Time Hits,
        // Padmaavat, Love song, Michael Jackson, Golden 90s Hits.
        // -----------------------------------------------------------------
        item {
            val quickTiles = listOf(
                curatedAlbums.find { it.id == "curated_bollywood_mush" } ?: curatedAlbums.getOrNull(0),
                curatedAlbums.find { it.id == "curated_my_songs" } ?: curatedAlbums.getOrNull(1),
                curatedAlbums.find { it.id == "curated_aditya_gadhvi" } ?: curatedAlbums.getOrNull(2),
                curatedAlbums.find { it.id == "curated_kumar_sanu_all_time_hits" } ?: curatedAlbums.getOrNull(3),
                curatedAlbums.find { it.id == "curated_padmaavat" } ?: curatedAlbums.getOrNull(4),
                curatedAlbums.find { it.id == "curated_love_songs_emoji" } ?: curatedAlbums.getOrNull(5),
                curatedAlbums.find { it.id == "curated_90s_pop_rock" } ?: curatedAlbums.getOrNull(6),
                curatedAlbums.find { it.id == "curated_90s_bollywood" } ?: curatedAlbums.getOrNull(7)
            ).filterNotNull()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // First Row (4 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    quickTiles.take(4).forEach { item ->
                        TvQuickAccessTile(
                            title = item.title,
                            artUrl = item.coverArtUrl,
                            onClick = { onPlayAlbum(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Second Row (4 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    quickTiles.drop(4).take(4).forEach { item ->
                        TvQuickAccessTile(
                            title = item.title,
                            artUrl = item.coverArtUrl,
                            onClick = { onPlayAlbum(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 4. ALBUMS FEATURING SONGS YOU LIKE (Matching Spotify TV Shelf)
        // -----------------------------------------------------------------
        item {
            Column {
                TvShelfHeader(
                    title = "Albums featuring songs you like",
                    subtitle = "Complete studio records, curated playlists & legendary soundtracks"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(curatedAlbums) { album ->
                        TvAlbumCard(
                            album = album,
                            onClick = { onPlayAlbum(album) }
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 3. RECENTLY PLAYED SHELF (Actual listening history)
        // -----------------------------------------------------------------
        val historyItems = recentSearches.take(10)
        if (historyItems.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Recently Played",
                        subtitle = "Jump straight back into your recent music"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(historyItems) { item ->
                            val songModel = OnlineSongModel(
                                id = item.id,
                                title = item.title,
                                artist = item.artist.ifBlank { "Artist" },
                                streamUrl = item.streamUrl ?: "",
                                highResArtUrl = item.albumArtPath
                            )
                            TvSongCard(
                                song = songModel,
                                onClick = { onPlaySong(songModel, null) }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 4. NEW RELEASES & FRESH HITS (Top suggestions)
        // -----------------------------------------------------------------
        val newReleasesList = homeFeed.newReleases
            .ifEmpty { homeContent.latestReleases }
            .ifEmpty { TvCatalogFallbacks.newReleases }
            .take(12)

        if (newReleasesList.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "New Releases & Fresh Hits",
                        subtitle = "Today's newest tracks and viral chartbusters"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(newReleasesList) { song ->
                            TvSongCard(
                                song = song,
                                onClick = {
                                    val upcoming = newReleasesList.drop(newReleasesList.indexOf(song) + 1)
                                    onPlaySong(song, upcoming)
                                }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 5. TOP CHARTBUSTERS IN YOUR REGION
        // -----------------------------------------------------------------
        val countryHits = homeFeed.trendingInCountrySongs
            .ifEmpty { homeFeed.trendingNow }
            .ifEmpty { TvCatalogFallbacks.topHitsIndia }
            .take(12)

        if (countryHits.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Popular in ${userSettings.userCountryCode}",
                        subtitle = "Top streamed music across India and your region"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(countryHits) { song ->
                            TvSongCard(
                                song = song,
                                onClick = {
                                    val upcoming = countryHits.drop(countryHits.indexOf(song) + 1)
                                    onPlaySong(song, upcoming)
                                }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 6. MADE FOR YOU • DAILY MIX
        // -----------------------------------------------------------------
        val madeForYouList = homeFeed.madeForYou
            .ifEmpty { homeFeed.dailyMix1Songs }
            .ifEmpty { homeFeed.quickTiles }
            .ifEmpty { TvCatalogFallbacks.madeForYou }
            .take(12)

        if (madeForYouList.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Made for You • Daily Mix",
                        subtitle = "Personalized selections inspired by your music taste"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(madeForYouList) { song ->
                            TvSongCard(
                                song = song,
                                onClick = {
                                    val upcoming = madeForYouList.drop(madeForYouList.indexOf(song) + 1)
                                    onPlaySong(song, upcoming)
                                }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 7. ROMANTIC & SOULFUL MELODIES
        // -----------------------------------------------------------------
        val romanticList = homeFeed.romanticMixSongs
            .ifEmpty { homeFeed.chillMixSongs }
            .ifEmpty { TvCatalogFallbacks.romanticMelodies }
            .take(12)

        if (romanticList.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Romantic & Soulful Melodies",
                        subtitle = "Timeless love songs, slow ballads, and acoustic warmth"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(romanticList) { song ->
                            TvSongCard(
                                song = song,
                                onClick = {
                                    val upcoming = romanticList.drop(romanticList.indexOf(song) + 1)
                                    onPlaySong(song, upcoming)
                                }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 8. RELAXING LO-FI & CHILL LOUNGE
        // -----------------------------------------------------------------
        val chillList = homeFeed.chillMixSongs
            .ifEmpty { homeFeed.vibeRecommendations }
            .ifEmpty { TvCatalogFallbacks.chillLofi }
            .take(12)

        if (chillList.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Relaxing Lo-Fi & Chill Lounge",
                        subtitle = "Soothing acoustic and ambient beats for evening unwinding"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(chillList) { song ->
                            TvSongCard(
                                song = song,
                                onClick = {
                                    val upcoming = chillList.drop(chillList.indexOf(song) + 1)
                                    onPlaySong(song, upcoming)
                                }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 9. EVERGREEN 90S & BOLLYWOOD CLASSICS
        // -----------------------------------------------------------------
        val bollywoodList = homeFeed.languageMixSongs
            .ifEmpty { homeFeed.becauseYouListenedTo }
            .ifEmpty { TvCatalogFallbacks.golden90sBollywood }
            .take(12)

        if (bollywoodList.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Evergreen 90s & Bollywood Classics",
                        subtitle = "Kumar Sanu, Alka Yagnik, Udit Narayan, and golden era soundtracks"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(bollywoodList) { song ->
                            TvSongCard(
                                song = song,
                                onClick = {
                                    val upcoming = bollywoodList.drop(bollywoodList.indexOf(song) + 1)
                                    onPlaySong(song, upcoming)
                                }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 10. PUNJABI & HIGH-ENERGY HITS
        // -----------------------------------------------------------------
        val partyList = homeFeed.heavyRotationSongs
            .ifEmpty { homeFeed.trendingNow }
            .ifEmpty { TvCatalogFallbacks.punjabiParty }
            .take(12)

        if (partyList.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Punjabi Top 50 & High-Energy Hits",
                        subtitle = "Diljit Dosanjh, Sidhu Moose Wala, Karan Aujla, and party anthems"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(partyList) { song ->
                            TvSongCard(
                                song = song,
                                onClick = {
                                    val upcoming = partyList.drop(partyList.indexOf(song) + 1)
                                    onPlaySong(song, upcoming)
                                }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 11. FEATURED ALBUMS & SOUNDTRACKS
        // -----------------------------------------------------------------
        val albumsList = (homeContent.popularAlbumsAndSingles + curatedAlbums).distinctBy { it.id }.take(10)
        if (albumsList.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Featured Albums & Soundtracks",
                        subtitle = "Complete collections and iconic soundtracks"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(albumsList) { album ->
                            TvAlbumCard(
                                album = album,
                                onClick = { onPlayAlbum(album) }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 12. LEGENDARY COLLECTIONS & GOLDEN ERAS
        // -----------------------------------------------------------------
        if (curatedAlbums.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "Legendary Eras & Vault Classics",
                        subtitle = "Full discographies from 70s, 80s, 90s, and retro Ghazals"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(curatedAlbums) { album ->
                            TvAlbumCard(
                                album = album,
                                onClick = { onPlayAlbum(album) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pre-compiled fallback songs for Android TV Home Screen.
 * Ensures the screen is NEVER empty or waiting when the TV app opens,
 * providing instant playable songs with real stream search keys and high-res art.
 */
object TvCatalogFallbacks {
    val newReleases: List<OnlineSongModel> = listOf(
        OnlineSongModel(
            id = "tv_new_1",
            title = "Aaj Ki Raat",
            artist = "Sachin-Jigar, Madhubanti Bagchi",
            album = "Stree 2",
            durationMs = 228000L,
            highResArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_new_2",
            title = "Angaaron",
            artist = "Shreya Ghoshal",
            album = "Pushpa 2 The Rule",
            durationMs = 260000L,
            highResArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_new_3",
            title = "Tauba Tauba",
            artist = "Karan Aujla",
            album = "Bad Newz",
            durationMs = 208000L,
            highResArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_new_4",
            title = "Illuminati",
            artist = "Sushin Shyam",
            album = "Aavesham",
            durationMs = 193000L,
            highResArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_new_5",
            title = "Arjan Vailly",
            artist = "Bhupinder Babbal",
            album = "Animal",
            durationMs = 182000L,
            highResArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_new_6",
            title = "O Maahi",
            artist = "Arijit Singh",
            album = "Dunki",
            durationMs = 233000L,
            highResArtUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_new_7",
            title = "Chaleya",
            artist = "Arijit Singh, Shilpa Rao",
            album = "Jawan",
            durationMs = 200000L,
            highResArtUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_new_8",
            title = "Satranga",
            artist = "Arijit Singh",
            album = "Animal",
            durationMs = 271000L,
            highResArtUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80"
        )
    )

    val topHitsIndia: List<OnlineSongModel> = listOf(
        OnlineSongModel(
            id = "tv_top_1",
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            album = "Aashiqui 2",
            durationMs = 262000L,
            highResArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_top_2",
            title = "Kesariya",
            artist = "Arijit Singh",
            album = "Brahmastra",
            durationMs = 268000L,
            highResArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_top_3",
            title = "Heeriye",
            artist = "Jasleen Royal, Arijit Singh",
            album = "Heeriye",
            durationMs = 194000L,
            highResArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_top_4",
            title = "Apna Bana Le",
            artist = "Arijit Singh",
            album = "Bhediya",
            durationMs = 261000L,
            highResArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_top_5",
            title = "Pehle Bhi Main",
            artist = "Vishal Mishra",
            album = "Animal",
            durationMs = 250000L,
            highResArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_top_6",
            title = "Maan Meri Jaan",
            artist = "King",
            album = "Champagne Talk",
            durationMs = 194000L,
            highResArtUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_top_7",
            title = "Raabta",
            artist = "Arijit Singh",
            album = "Agent Vinod",
            durationMs = 243000L,
            highResArtUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_top_8",
            title = "Ranjha",
            artist = "B Praak, Jasleen Royal",
            album = "Shershaah",
            durationMs = 228000L,
            highResArtUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80"
        )
    )

    val madeForYou: List<OnlineSongModel> = listOf(
        OnlineSongModel(
            id = "tv_mfy_1",
            title = "Saiyaara",
            artist = "Mohit Chauhan, Taraannum Mallik",
            album = "Ek Tha Tiger",
            durationMs = 253000L,
            highResArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_mfy_2",
            title = "Agar Tum Saath Ho",
            artist = "Alka Yagnik, Arijit Singh",
            album = "Tamasha",
            durationMs = 341000L,
            highResArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_mfy_3",
            title = "Hawayein",
            artist = "Arijit Singh",
            album = "Jab Harry Met Sejal",
            durationMs = 290000L,
            highResArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_mfy_4",
            title = "Kalank Title Track",
            artist = "Arijit Singh",
            album = "Kalank",
            durationMs = 311000L,
            highResArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_mfy_5",
            title = "Tere Hawaale",
            artist = "Arijit Singh, Shilpa Rao",
            album = "Laal Singh Chaddha",
            durationMs = 346000L,
            highResArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_mfy_6",
            title = "Dil Diyan Gallan",
            artist = "Atif Aslam",
            album = "Tiger Zinda Hai",
            durationMs = 260000L,
            highResArtUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80"
        )
    )

    val romanticMelodies: List<OnlineSongModel> = listOf(
        OnlineSongModel(
            id = "tv_rom_1",
            title = "Jeene Laga Hoon",
            artist = "Atif Aslam, Shreya Ghoshal",
            album = "Ramaiya Vastavaiya",
            durationMs = 237000L,
            highResArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_rom_2",
            title = "Sun Saathiya",
            artist = "Priya Saraiya, Divya Kumar",
            album = "ABCD 2",
            durationMs = 220000L,
            highResArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_rom_3",
            title = "Kaun Tujhe",
            artist = "Palak Muchhal",
            album = "M.S. Dhoni",
            durationMs = 241000L,
            highResArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_rom_4",
            title = "Hasi Ban Gaye",
            artist = "Shreya Ghoshal",
            album = "Hamari Adhuri Kahani",
            durationMs = 192000L,
            highResArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_rom_5",
            title = "Tera Ban Jaunga",
            artist = "Akhil Sachdeva, Tulsi Kumar",
            album = "Kabir Singh",
            durationMs = 236000L,
            highResArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_rom_6",
            title = "Raataan Lambiyan",
            artist = "Jubin Nautiyal, Asees Kaur",
            album = "Shershaah",
            durationMs = 230000L,
            highResArtUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80"
        )
    )

    val chillLofi: List<OnlineSongModel> = listOf(
        OnlineSongModel(
            id = "tv_lofi_1",
            title = "Iktara (Lo-Fi Ambient)",
            artist = "Kavita Seth, Amitabh Bhattacharya",
            album = "Wake Up Sid",
            durationMs = 253000L,
            highResArtUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_lofi_2",
            title = "Kabira (Acoustic Study)",
            artist = "Tochi Raina, Rekha Bhardwaj",
            album = "Yeh Jawaani Hai Deewani",
            durationMs = 223000L,
            highResArtUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_lofi_3",
            title = "Kun Faya Kun",
            artist = "A.R. Rahman, Mohit Chauhan",
            album = "Rockstar",
            durationMs = 473000L,
            highResArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_lofi_4",
            title = "Phir Le Aya Dil (Reprise)",
            artist = "Arijit Singh",
            album = "Barfi",
            durationMs = 304000L,
            highResArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_lofi_5",
            title = "Kho Gaye Hum Kahan",
            artist = "Jasleen Royal, Prateek Kuhad",
            album = "Baar Baar Dekho",
            durationMs = 213000L,
            highResArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_lofi_6",
            title = "Safarnama",
            artist = "Lucky Ali",
            album = "Tamasha",
            durationMs = 251000L,
            highResArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        )
    )

    val golden90sBollywood: List<OnlineSongModel> = listOf(
        OnlineSongModel(
            id = "tv_90s_1",
            title = "Tujhe Dekha To",
            artist = "Kumar Sanu, Lata Mangeshkar",
            album = "Dilwale Dulhania Le Jayenge",
            durationMs = 304000L,
            highResArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_90s_2",
            title = "Pehla Nasha",
            artist = "Udit Narayan, Sadhana Sargam",
            album = "Jo Jeeta Wohi Sikandar",
            durationMs = 292000L,
            highResArtUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_90s_3",
            title = "Dheere Dheere Se Meri Zindagi",
            artist = "Kumar Sanu, Anuradha Paudwal",
            album = "Aashiqui",
            durationMs = 331000L,
            highResArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_90s_4",
            title = "Tip Tip Barsa Paani",
            artist = "Alka Yagnik, Udit Narayan",
            album = "Mohra",
            durationMs = 356000L,
            highResArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_90s_5",
            title = "Chura Ke Dil Mera",
            artist = "Kumar Sanu, Alka Yagnik",
            album = "Main Khiladi Tu Anari",
            durationMs = 472000L,
            highResArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_90s_6",
            title = "Mera Dil Bhi Kitna Pagal Hai",
            artist = "Kumar Sanu, Alka Yagnik",
            album = "Saajan",
            durationMs = 328000L,
            highResArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_90s_7",
            title = "Bahon Ke Darmiyan",
            artist = "Hariharan, Alka Yagnik",
            album = "Khamoshi",
            durationMs = 371000L,
            highResArtUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_90s_8",
            title = "Pal Pal Dil Ke Paas",
            artist = "Kishore Kumar",
            album = "Blackmail",
            durationMs = 328000L,
            highResArtUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80"
        )
    )

    val punjabiParty: List<OnlineSongModel> = listOf(
        OnlineSongModel(
            id = "tv_punj_1",
            title = "Softly",
            artist = "Karan Aujla",
            album = "Making Memories",
            durationMs = 155000L,
            highResArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_punj_2",
            title = "Born to Shine",
            artist = "Diljit Dosanjh",
            album = "G.O.A.T.",
            durationMs = 214000L,
            highResArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_punj_3",
            title = "Lover",
            artist = "Diljit Dosanjh",
            album = "MoonChild Era",
            durationMs = 186000L,
            highResArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_punj_4",
            title = "295",
            artist = "Sidhu Moose Wala",
            album = "Moosetape",
            durationMs = 270000L,
            highResArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_punj_5",
            title = "So High",
            artist = "Sidhu Moose Wala",
            album = "PBX 1",
            durationMs = 237000L,
            highResArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        ),
        OnlineSongModel(
            id = "tv_punj_6",
            title = "White Brown Black",
            artist = "Karan Aujla, Avvy Sra",
            album = "Single",
            durationMs = 179000L,
            highResArtUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80"
        )
    )
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}

/**
 * Rectangular Quick-Access Tile (Spotify TV Home design: IMG20261026111711.jpg).
 */
@Composable
fun TvQuickAccessTile(
    title: String,
    artUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF222222),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
        modifier = modifier
            .height(58.dp)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(6.dp),
                focusScale = 1.04f
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
            ) {
                TrackCoverArt(
                    albumArtPath = artUrl,
                    title = title,
                    artist = "Recordify",
                    size = 58.dp,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
        }
    }
}
