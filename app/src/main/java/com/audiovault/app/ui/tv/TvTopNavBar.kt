package com.audiovault.app.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Global Top Navigation Bar for Recordify TV.
 * Authentically replicated from Spotify TV design (Reference Photos):
 * - Top-Left: Interactive Now Playing Mini Widget Pill with artwork, title/artist, and live equalizer bars.
 * - Top-Center: Horizontal Nav Tabs (Home, Search, Your Library) with pill focus states.
 * - Top-Right: Green Action Pill ("Settings") and User Profile Initial Circle ("H").
 */
@Composable
fun TvTopNavBar(
    selectedTab: TvNavTab,
    onTabSelected: (TvNavTab) -> Unit,
    activeTrackTitle: String?,
    activeTrackArtist: String?,
    activeTrackArtUrl: String?,
    isPlaying: Boolean,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(Color(0xFF0C0C0C))
            .padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // -----------------------------------------------------------------
        // 1. TOP-LEFT: NOW PLAYING PILL WIDGET / BRAND LOGO
        // -----------------------------------------------------------------
        Box(
            modifier = Modifier.width(260.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (!activeTrackTitle.isNullOrBlank()) {
                // Interactive Now Playing Mini Capsule
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E1E1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier
                        .height(48.dp)
                        .tvRemoteFocusable(
                            onClick = onOpenNowPlaying,
                            shape = RoundedCornerShape(24.dp),
                            focusScale = 1.05f
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackCoverArt(
                            albumArtPath = activeTrackArtUrl,
                            title = activeTrackTitle,
                            artist = activeTrackArtist,
                            size = 36.dp,
                            shape = RoundedCornerShape(6.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = activeTrackTitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeTrackArtist ?: "Recordify TV",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Animated Green Audio Equalizer Bars
                        TvEqualizerBars(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(width = 16.dp, height = 14.dp),
                            color = SpotifyGreenBright
                        )
                    }
                }
            } else {
                // Branded Recordify TV Logo Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SpotifyGreenBright,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "R",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Recordify",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // 2. TOP-CENTER: HORIZONTAL NAVIGATION TABS
        // -----------------------------------------------------------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val navTabs = listOf(
                TvNavTab.HOME to "Home" to Icons.Default.Home,
                TvNavTab.SEARCH to "Search" to Icons.Default.Search,
                TvNavTab.LIBRARY to "Your Library" to Icons.Default.LibraryMusic
            )

            navTabs.forEach { (pair, icon) ->
                val (tab, label) = pair
                val isSelected = selectedTab == tab

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) Color(0xFF282828) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .tvRemoteFocusable(
                            onClick = { onTabSelected(tab) },
                            shape = RoundedCornerShape(50),
                            focusScale = 1.08f
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Color(0xFFB3B3B3),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFB3B3B3),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 3. TOP-RIGHT: SETTINGS GREEN PILL + PROFILE AVATAR "H"
        // -----------------------------------------------------------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Green Pill Action Button ("Settings" matching Spotify TV's green pill)
            Surface(
                shape = RoundedCornerShape(50),
                color = SpotifyGreenBright,
                modifier = Modifier
                    .height(38.dp)
                    .tvRemoteFocusable(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(50),
                        focusScale = 1.08f
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // User Profile Circle ("H" from user's TV photos)
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFA07A), // Peach/salmon color matching photo
                modifier = Modifier
                    .size(40.dp)
                    .tvRemoteFocusable(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        focusScale = 1.15f
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "H",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }
    }
}
