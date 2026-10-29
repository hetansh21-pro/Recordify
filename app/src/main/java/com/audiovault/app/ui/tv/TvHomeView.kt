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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.catalog.CountryMusicCatalog
import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.online.SpotifyHomeContent
import com.audiovault.app.core.recommendation.HomeRecommendationFeed
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright
import java.util.Calendar

/**
 * 10-Foot TV Home Screen for Recordify TV.
 * Exclusively focused on complete ALBUMS & SOUNDTRACKS for big-screen couch viewing:
 * - Blockbuster Spotlight Hero Album Banner with instant 1-click playback.
 * - Quick-Access rectangular album tiles.
 * - "🎬 Blockbuster Bollywood Soundtracks": Animal, Kabir Singh, Aashiqui 2, Rockstar, Brahmāstra.
 * - "🎤 Iconic Artist Discographies": Arijit Singh, Diljit Dosanjh, Sidhu Moose Wala, Shreya Ghoshal.
 * - "📻 Golden Era & 90s Classics": Kishore Kumar, Lata Mangeshkar, Kumar Sanu, Mohammed Rafi.
 * - "🔥 Punjabi & High-Energy Albums": Moosetape, Making Memories.
 * - "🌧️ Romantic & Soulful Collections": Bollywood Mush, 90s Romance, Love songs.
 * - "🎧 Lo-Fi, Chill & Soulful Ghazals".
 * - "🌍 Trending & Popular Albums".
 */
@Composable
fun TvHomeView(
    viewModel: RecordingsViewModel,
    homeFeed: HomeRecommendationFeed,
    homeContent: SpotifyHomeContent,
    onPlaySong: (OnlineSongModel, List<OnlineSongModel>?) -> Unit = { _, _ -> },
    onPlayAlbum: (OnlineAlbumModel) -> Unit,
    onSearchMood: (String) -> Unit,
    modifier: Modifier = Modifier,
    heroFocusRequester: FocusRequester = remember { FocusRequester() },
    onNavigateUpToHeader: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val userName by viewModel.userName.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val curatedAlbums = CuratedAlbumCatalog.curatedAlbums
    val countryProfile = remember(userSettings.userCountryCode) {
        CountryMusicCatalog.getProfile(userSettings.userCountryCode)
    }

    // Pick top blockbuster hero album (e.g. Animal, Kabir Singh, Aashiqui 2 or country hero)
    val featuredHeroAlbum = remember(curatedAlbums) {
        curatedAlbums.firstOrNull { it.title == "Animal" }
            ?: curatedAlbums.firstOrNull { it.title == "Kabir Singh" }
            ?: countryProfile.heroAlbum
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // -----------------------------------------------------------------
        // 1. HERO FEATURED SPOTLIGHT ALBUM (Large 10-Foot Cinema Banner)
        // -----------------------------------------------------------------
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF141414),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(heroFocusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                            onNavigateUpToHeader()
                            true
                        } else false
                    }
                    .tvRemoteFocusable(
                        onClick = { onPlayAlbum(featuredHeroAlbum) },
                        shape = RoundedCornerShape(18.dp),
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
                    // Left Column: Big Bold Title, Badges, and Action Buttons
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .padding(end = 28.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = SpotifyGreenBright,
                                modifier = Modifier.size(12.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FEATURED ALBUM • ALL ALBUMS TV",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SpotifyGreenBright,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = featuredHeroAlbum.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 36.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${featuredHeroAlbum.year ?: "2023"} • ${featuredHeroAlbum.artist ?: featuredHeroAlbum.primaryArtists ?: "Recordify Master Soundtrack"} • ${featuredHeroAlbum.songCount} Songs",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFCCCCCC),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Complete original film soundtrack with full-length master audio. Click to play the entire album sequentially or browse all tracks.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF888888),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SpotifyGreenBright,
                                modifier = Modifier
                                    .height(40.dp)
                                    .clickable { onPlayAlbum(featuredHeroAlbum) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Play Full Album",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF242424),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF383838)),
                                modifier = Modifier
                                    .height(40.dp)
                                    .clickable { onPlayAlbum(featuredHeroAlbum) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = "Tracks",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "View Tracklist",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: Artwork
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1E1E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF363636)),
                        modifier = Modifier.size(175.dp)
                    ) {
                        TrackCoverArt(
                            albumArtPath = featuredHeroAlbum.coverArtUrl,
                            title = featuredHeroAlbum.title,
                            artist = featuredHeroAlbum.primaryArtists ?: "Recordify",
                            size = 175.dp,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 2. QUICK-ACCESS 8 ALBUMS GRID (2 Rows x 4 Columns)
        // -----------------------------------------------------------------
        item {
            val quickAlbums = curatedAlbums.take(8)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // First Row (4 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val row1 = quickAlbums.take(4)
                    row1.forEachIndexed { idx, item ->
                        TvQuickAccessTile(
                            title = item.title,
                            artUrl = item.coverArtUrl,
                            onClick = { onPlayAlbum(item) },
                            modifier = Modifier.weight(1f),
                            isLeftBoundary = idx == 0,
                            isRightBoundary = idx == row1.lastIndex
                        )
                    }
                }

                // Second Row (4 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val row2 = quickAlbums.drop(4).take(4)
                    row2.forEachIndexed { idx, item ->
                        TvQuickAccessTile(
                            title = item.title,
                            artUrl = item.coverArtUrl,
                            onClick = { onPlayAlbum(item) },
                            modifier = Modifier.weight(1f),
                            isLeftBoundary = idx == 0,
                            isRightBoundary = idx == row2.lastIndex
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 3. BLOCKBUSTER MOVIE SOUNDTRACKS
        // -----------------------------------------------------------------
        val blockbusterAlbums = curatedAlbums.filter {
            it.title in listOf("Animal", "Kabir Singh", "Aashiqui 2", "Rockstar", "Brahmāstra: Part One – Shiva", "Yeh Jawaani Hai Deewani", "Padmaavat")
        }.ifEmpty { curatedAlbums.take(6) }

        if (blockbusterAlbums.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "🎬 Blockbuster Movie Soundtracks",
                        subtitle = "Complete official soundtrack records with original film scores"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(blockbusterAlbums) { idx, album ->
                            TvAlbumCard(
                                album = album,
                                isLeftBoundary = idx == 0,
                                isRightBoundary = idx == blockbusterAlbums.lastIndex,
                                onClick = { onPlayAlbum(album) }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 4. ICONIC ARTIST DISCOGRAPHIES & ESSENTIALS
        // -----------------------------------------------------------------
        val artistAlbums = curatedAlbums.filter {
            it.type.equals("Artist", ignoreCase = true) ||
            it.title in listOf("Arijit Singh", "Diljit Dosanjh", "Pritam", "Shreya Ghoshal", "Aditya Gadhvi")
        }

        if (artistAlbums.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "🎤 Legendary Artist Discographies",
                        subtitle = "Greatest hit albums and signature collections of master vocalists"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(artistAlbums) { idx, album ->
                            TvAlbumCard(
                                album = album,
                                isLeftBoundary = idx == 0,
                                isRightBoundary = idx == artistAlbums.lastIndex,
                                onClick = { onPlayAlbum(album) }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 5. GOLDEN ERA & 90S BOLLYWOOD CLASSIC ALBUMS
        // -----------------------------------------------------------------
        val retroAlbums = curatedAlbums.filter {
            it.title in listOf(
                "Golden 90s Bollywood Hits",
                "Retro Kishore Kumar Golden Hits",
                "Evergreen Lata Mangeshkar Classics",
                "Kumar Sanu All Time Hits",
                "Immortal Mohammed Rafi Hits",
                "Evergreen 70s & 80s Classics",
                "90s bollywood",
                "Old is Gold"
            )
        }

        if (retroAlbums.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "📻 Golden Era & 90s Classic Albums",
                        subtitle = "Timeless nostalgia from Kishore Kumar, Lata Mangeshkar, and Kumar Sanu"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(retroAlbums) { idx, album ->
                            TvAlbumCard(
                                album = album,
                                isLeftBoundary = idx == 0,
                                isRightBoundary = idx == retroAlbums.lastIndex,
                                onClick = { onPlayAlbum(album) }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 6. PUNJABI POWER & HIGH-ENERGY ALBUMS
        // -----------------------------------------------------------------
        val punjabiAlbums = curatedAlbums.filter {
            it.title in listOf("Moosetape", "Making Memories", "90s High-Energy Dance Hits")
        }

        if (punjabiAlbums.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "🔥 Punjabi Power & High-Energy Albums",
                        subtitle = "Sidhu Moose Wala, Karan Aujla, Diljit Dosanjh, and party anthems"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(punjabiAlbums) { idx, album ->
                            TvAlbumCard(
                                album = album,
                                isLeftBoundary = idx == 0,
                                isRightBoundary = idx == punjabiAlbums.lastIndex,
                                onClick = { onPlayAlbum(album) }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 7. ROMANTIC & HEARTBREAK ALBUM COLLECTIONS
        // -----------------------------------------------------------------
        val romanticAlbums = curatedAlbums.filter {
            it.title in listOf(
                "Bollywood Mush",
                "90s Romantic Melodies",
                "Love song 🥰💖",
                "Arijit Singh All Sad Songs 💔🥀",
                "Sad Hindi Songs 🥺💔 (Mood Off 🥺)"
            )
        }

        if (romanticAlbums.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "🌧️ Romantic & Soulful Album Collections",
                        subtitle = "Passionate love ballads, acoustic melodies, and heartfelt poetry"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(romanticAlbums) { idx, album ->
                            TvAlbumCard(
                                album = album,
                                isLeftBoundary = idx == 0,
                                isRightBoundary = idx == romanticAlbums.lastIndex,
                                onClick = { onPlayAlbum(album) }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 8. SOULFUL GHAZALS & ACOUSTIC ALBUMS
        // -----------------------------------------------------------------
        val ghazalAlbums = curatedAlbums.filter {
            it.title in listOf("Old Is Gold: Soulful Ghazals", "90s Pop & Rock Anthems", "My songs", "Sadi ke fall sa")
        }

        if (ghazalAlbums.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "🎧 Soulful Ghazals & Acoustic Albums",
                        subtitle = "Calm evening listening, deep lyrics, and acoustic instruments"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(ghazalAlbums) { idx, album ->
                            TvAlbumCard(
                                album = album,
                                isLeftBoundary = idx == 0,
                                isRightBoundary = idx == ghazalAlbums.lastIndex,
                                onClick = { onPlayAlbum(album) }
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 9. POPULAR & REGIONAL ALBUMS FROM SPOTIFY & REGION
        // -----------------------------------------------------------------
        val popularAlbums = (homeContent.popularAlbumsAndSingles + homeContent.trendingSoundtracks)
            .distinctBy { it.id }
            .take(10)

        if (popularAlbums.isNotEmpty()) {
            item {
                Column {
                    TvShelfHeader(
                        title = "🌍 Trending Soundtracks & Popular Albums",
                        subtitle = "Worldwide favorites and trending releases streaming now"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        itemsIndexed(popularAlbums) { idx, album ->
                            TvAlbumCard(
                                album = album,
                                isLeftBoundary = idx == 0,
                                isRightBoundary = idx == popularAlbums.lastIndex,
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
 * Rectangular Quick-Access Tile (Spotify TV Home design).
 */
@Composable
fun TvQuickAccessTile(
    title: String,
    artUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLeftBoundary: Boolean = false,
    isRightBoundary: Boolean = false
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
                focusScale = 1.04f,
                isLeftBoundary = isLeftBoundary,
                isRightBoundary = isRightBoundary
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
                    artist = "Recordify Album",
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
