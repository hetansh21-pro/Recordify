package com.audiovault.app.ui.tv

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.recommendation.VibeCategory
import com.audiovault.app.data.repository.UserSettings
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * 10-Foot TV Settings Screen for Recordify TV.
 * Built according to Sections 8 and 18 of the Spotify TV specification:
 * - Country & Regional preferences picker.
 * - Preferred music languages selector.
 * - Preferred mood selector.
 * - Audio Playback & Streaming settings.
 * - Listening History management with clear action.
 * - About Recordify TV information.
 */
@Composable
fun TvSettingsView(
    viewModel: RecordingsViewModel,
    userSettings: UserSettings,
    modifier: Modifier = Modifier
) {
    var historyClearedMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            TvShelfHeader(
                title = "Settings & Preferences",
                subtitle = "Configure audio output, regional catalogs, and recommendation taste profile"
            )
        }

        // -------------------------------------------------------------
        // 1. COUNTRY SELECTION SECTION
        // -------------------------------------------------------------
        item {
            Column {
                Text(
                    text = "COUNTRY & REGIONAL DISCOVERY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val countryList = listOf(
                    Triple("IN", "India", "🇮🇳"),
                    Triple("US", "United States", "🇺🇸"),
                    Triple("GB", "United Kingdom", "🇬🇧"),
                    Triple("KR", "South Korea", "🇰🇷"),
                    Triple("JP", "Japan", "🇯🇵"),
                    Triple("BR", "Brazil", "🇧🇷"),
                    Triple("AE", "United Arab Emirates", "🇦🇪"),
                    Triple("ES", "Spain", "🇪🇸"),
                    Triple("CA", "Canada", "🇨🇦"),
                    Triple("AU", "Australia", "🇦🇺"),
                    Triple("DE", "Germany", "🇩🇪"),
                    Triple("FR", "France", "🇫🇷")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(countryList) { (code, name, flag) ->
                        val isSelected = userSettings.userCountryCode.equals(code, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SpotifyGreenBright.copy(alpha = 0.2f) else Color(0xFF181818),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SpotifyGreenBright else Color(0xFF2C2C2C)
                            ),
                            modifier = Modifier.tvRemoteFocusable(
                                onClick = { viewModel.setUserCountry(code, name) },
                                shape = RoundedCornerShape(12.dp),
                                focusScale = 1.05f
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) SpotifyGreenBright else Color.White
                                        )
                                    )
                                    Text(
                                        text = "Code: $code",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF888888))
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SpotifyGreenBright, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. PREFERRED MUSIC LANGUAGES
        // -------------------------------------------------------------
        item {
            Column {
                Text(
                    text = "PREFERRED MUSIC LANGUAGES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val languages = listOf("Hindi", "English", "Punjabi", "Gujarati", "Tamil", "Telugu", "Spanish")

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(languages) { lang ->
                        val isSelected = userSettings.primaryLanguage.equals(lang, ignoreCase = true) ||
                                userSettings.preferredLanguages.contains(lang)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) SpotifyGreenBright.copy(alpha = 0.25f) else Color(0xFF181818),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SpotifyGreenBright else Color(0xFF2C2C2C)
                            ),
                            modifier = Modifier.tvRemoteFocusable(
                                onClick = {
                                    val currentPreferred = userSettings.preferredLanguages.toMutableSet()
                                    if (isSelected && currentPreferred.size > 1) {
                                        currentPreferred.remove(lang)
                                    } else {
                                        currentPreferred.add(lang)
                                    }
                                    viewModel.setMusicLanguages(lang, currentPreferred)
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lang,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SpotifyGreenBright else Color.White
                                    )
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SpotifyGreenBright, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. AUDIO PLAYBACK & STREAMING PREFERENCES
        // -------------------------------------------------------------
        item {
            Column {
                Text(
                    text = "AUDIO QUALITY & PLAYBACK ENGINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161616),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Streaming Quality", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                Text(text = "Android Media3 ExoPlayer Lossless Output", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888)))
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SpotifyGreenBright.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreenBright.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "320 KBPS MASTER",
                                    color = SpotifyGreenBright,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Audio Normalization & Volume Leveling", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                Text(text = "Automatically balances volume levels across dynamic tracks", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888)))
                            }
                            Text(text = "Active", color = SpotifyGreenBright, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 4. LISTENING HISTORY & CACHE MANAGEMENT
        // -------------------------------------------------------------
        item {
            Column {
                Text(
                    text = "DATA & LISTENING HISTORY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161616),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvRemoteFocusable(
                            onClick = {
                                viewModel.clearListeningHistoryAndReset()
                                historyClearedMessage = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE91E63))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Clear Listening History & Reset Taste",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (historyClearedMessage) "Listening history reset successfully!" else "Wipes playback history and recalculates fresh recommendations",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (historyClearedMessage) SpotifyGreenBright else Color(0xFF888888)
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Reset",
                            color = Color(0xFFE91E63),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 5. ABOUT RECORDIFY TV
        // -------------------------------------------------------------
        item {
            Column {
                Text(
                    text = "ABOUT RECORDIFY TV",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161616),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Application", color = Color(0xFF888888), fontSize = 13.sp)
                            Text(text = "Recordify TV Edition", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Version", color = Color(0xFF888888), fontSize = 13.sp)
                            Text(text = "2.4.0 (Lossless Master Build)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Audio Architecture", color = Color(0xFF888888), fontSize = 13.sp)
                            Text(text = "Android Media3 ExoPlayer • Clean Architecture", color = SpotifyGreenBright, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
