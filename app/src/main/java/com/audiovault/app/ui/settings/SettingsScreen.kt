package com.audiovault.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.ui.components.SubtleJellyContainer
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright
import com.audiovault.app.ui.theme.StateRecordingRed
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import com.audiovault.app.data.model.CountryInfo
import com.audiovault.app.data.model.MusicPreferenceCatalog
import java.text.DecimalFormat
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val recordingCount by viewModel.recordingCount.collectAsState()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTasteDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    SubtleJellyContainer(
        isAtTop = { scrollState.value == 0 },
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) { jellyModifier ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(jellyModifier)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Clean Header
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: MUSIC PREFERENCES & REGION
            SettingsSectionHeader(title = "MUSIC PREFERENCES & REGION")

            val countryFlag = com.audiovault.app.data.model.MusicPreferenceCatalog.COUNTRIES.firstOrNull {
                it.code.equals(settings.userCountryCode, ignoreCase = true)
            }?.flagEmoji ?: "🇮🇳"

            SettingsGroupCard {
                SettingsClickableRow(
                    icon = Icons.Default.Public,
                    title = "Country & Region",
                    subtitle = "$countryFlag ${settings.userCountryName} (${settings.userCountryCode})",
                    onClick = { showCountryDialog = true }
                )

                SettingsDivider()

                SettingsClickableRow(
                    icon = Icons.Default.Language,
                    title = "Music Languages",
                    subtitle = "Primary: ${settings.primaryLanguage} • Preferred: ${settings.preferredLanguages.joinToString(", ")}",
                    onClick = { showLanguageDialog = true }
                )

                SettingsDivider()

                SettingsClickableRow(
                    icon = Icons.Default.Favorite,
                    title = "Music Taste & Favorites",
                    subtitle = "${settings.favoriteGenres.size} Genres selected • ${settings.favoriteArtists.size} Artists",
                    onClick = { showTasteDialog = true }
                )

                SettingsDivider()

                SettingsClickableRow(
                    icon = Icons.Default.Refresh,
                    title = "Reset History & Recommendations",
                    subtitle = "Clears search history, telemetry, and resets recommendation cache",
                    titleColor = Color(0xFFFFB74D),
                    onClick = { showResetConfirmDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 0: STREAMING & DATA SAVER
            SettingsSectionHeader(title = "STREAMING & DATA SAVER")

            SettingsGroupCard {
                SettingsSwitchRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Data Saver Mode",
                    subtitle = "Stream at 96kbps with strict chunked buffering (8s target, 15s max) and 0 audio prefetch. Saves up to 70% data.",
                    checked = settings.isDataSaverEnabled,
                    onCheckedChange = { viewModel.setDataSaver(it) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Default.Timer,
                    title = "10s Smart Skip Protection",
                    subtitle = "Shows song title & art for 10s before loading audio data over internet. If you skip within 10s, 0 MB is downloaded.",
                    checked = settings.isSongPreviewSkipSaverEnabled,
                    onCheckedChange = { viewModel.setSongPreviewSkipSaver(it) }
                )

                SettingsDivider()

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Progressive Range Architecture",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (settings.isDataSaverEnabled) "Active: 96kbps Low Bandwidth • 8s Buffer • HTTP 206 Partial Content" else "Active: 320kbps High Fidelity • 15s Buffer • HTTP 206 Partial Content",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SpotifyGreenBright,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 0.5: RECORDIFY DATA & TASTE TRAINING
            SettingsSectionHeader(title = "RECORDIFY DATA & TASTE TRAINING")

            val spotifyImportState by viewModel.spotifyImportState.collectAsState()
            val importedSpotifyCount by viewModel.importedSpotifyCount.collectAsState()

            val zipPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { viewModel.importSpotifyZip(it) }
            }

            val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let { viewModel.importSpotifyFolder(it) }
            }

            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBadge(icon = Icons.Default.AutoAwesome)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Train AI Recommendation Engine",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Upload your extended listening history (.zip or folder) to train recommendations on your exact taste.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF909090)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (val state = spotifyImportState) {
                        is SpotifyImportUiState.Importing -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF162518),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = SpotifyGreenBright,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Parsing Listening History...",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${state.streamCount} streams processed so far",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = SpotifyGreenBright
                                        )
                                    )
                                }
                            }
                        }
                        is SpotifyImportUiState.Success -> {
                            val stats = state.stats
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF102615),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = SpotifyGreenBright,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Recordify Model Trained!",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = SpotifyGreenBright
                                                )
                                            )
                                        }

                                        Text(
                                            text = "${stats.totalStreamsParsed} streams",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Total Listening Time: ${String.format(Locale.US, "%.1f", stats.totalHoursListened)} hrs • Date: ${stats.earliestDate ?: ""} - ${stats.latestDate ?: ""}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFCCCCCC),
                                            fontSize = 12.sp
                                        )
                                    )

                                    if (stats.topArtists.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Top Artists: " + stats.topArtists.take(5).joinToString(", ") { it.first },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = SpotifyGreenBright,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "Re-upload",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF909090),
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier
                                                .clickable { viewModel.resetSpotifyImportState() }
                                                .padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        is SpotifyImportUiState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF281515),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Import Error: ${state.message}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = StateRecordingRed,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Dismiss",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.clickable { viewModel.resetSpotifyImportState() }
                                    )
                                }
                            }
                        }
                        SpotifyImportUiState.Idle -> {
                            if (importedSpotifyCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF122216),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(SpotifyGreenBright)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Recordify AI Synced & Active",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = SpotifyGreenBright
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$importedSpotifyCount streams powering your personalized home recommendations",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFFCCCCCC)
                                                )
                                            )
                                        }

                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = StateRecordingRed,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier
                                                .clickable { viewModel.clearSpotifyImportedData() }
                                                .padding(6.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Upload buttons: Zip File or Folder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E3A24),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            zipPickerLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = SpotifyGreenBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Upload .ZIP",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = SpotifyGreenBright
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SpotifyCardSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            folderPickerLauncher.launch(null)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Select Folder",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            // Quick Local Folder button if existing on device
                            val localSpotifyDir = java.io.File("/home/hetansh-patel/Projects/Re-songs/my_spotify_data/Spotify Extended Streaming History")
                            if (localSpotifyDir.exists() && localSpotifyDir.isDirectory) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF142416),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.importSpotifyFromPath(localSpotifyDir.absolutePath)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "⚡ Quick Train from Local History",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = SpotifyGreenBright
                                            )
                                        )
                                        Text(
                                            text = "TRAIN NOW",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: SMART CAPTURE & RECOGNITION
            SettingsSectionHeader(title = "INTELLIGENT RECOGNITION")

            SettingsGroupCard {
                SettingsSwitchRow(
                    icon = Icons.Default.MusicNote,
                    title = "Track Change Auto-Split",
                    subtitle = "Split and tag recordings automatically with song name & artist",
                    checked = settings.isSmartSongSplitEnabled,
                    onCheckedChange = { viewModel.setSmartSongSplit(it) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Default.Shield,
                    title = "Ad & Noise Suppression",
                    subtitle = "Skip non-music spoken ads, notifications, and click noises",
                    checked = settings.isNoiseAndAdFilterEnabled,
                    onCheckedChange = { viewModel.setNoiseAndAdFilter(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: YOUTUBE AD CUTTER (NATIVE ENGINE)
            SettingsSectionHeader(title = "YOUTUBE AD-CUTTER (NATIVE ENGINE)")

            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBadge(icon = Icons.Default.AutoAwesome)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Native Precision YouTube Ad Cutter",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Built-in ExoPlayer Audio Stream Filtering + Smooth 50ms Crossfade",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF909090)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF142918),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SpotifyGreenBright)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Native Engine Active",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SpotifyGreenBright
                                        )
                                    )
                                    Text(
                                        text = "Cutting in-stream ads and dead silence seamlessly",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCCCCCC)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: AUDIO FIDELITY & SILENCE TIMEOUT
            SettingsSectionHeader(title = "AUDIO FIDELITY & TIMING")

            SettingsGroupCard {
                // Bitrate Selector
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconBadge(icon = Icons.Default.Headphones)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AAC Encoding Bitrate",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Higher bitrate yields studio richness and speed accuracy",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF909090)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            192 to "192 kbps",
                            256 to "256 kbps (HQ)",
                            320 to "320 kbps"
                        ).forEach { (bitrate, label) ->
                            val isSelected = settings.audioQualityBitrateKbps == bitrate
                            SettingsSegmentButton(
                                label = label,
                                isSelected = isSelected,
                                onClick = { viewModel.setAudioBitrate(bitrate) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                SettingsDivider()

                // Silence Timeout Selector
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconBadge(icon = Icons.Default.Timer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Silence Finalize Timeout",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Seconds of continuous silence before closing track",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF909090)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 7, 10, 15).forEach { seconds ->
                            val isSelected = settings.silenceTimeoutSeconds == seconds
                            SettingsSegmentButton(
                                label = "${seconds}s",
                                isSelected = isSelected,
                                onClick = { viewModel.setSilenceTimeout(seconds) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: STORAGE MANAGEMENT
            SettingsSectionHeader(title = "STORAGE & LIBRARY")

            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBadge(icon = Icons.Default.Storage)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Library Space",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "$recordingCount tracks recorded",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF909090)
                                    )
                                )
                            }
                        }

                        Text(
                            text = formatFileSize(totalStorageBytes),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SpotifyGreenBright
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clean Delete All Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2A1515),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearConfirmDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = StateRecordingRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clear All Recordings",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StateRecordingRed
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 4: PRIVACY & SYSTEM INFO
            AboutCaptureCard()

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showClearConfirmDialog) {
        ClearAllDataDialog(
            onDismiss = { showClearConfirmDialog = false },
            onConfirmClear = {
                viewModel.clearAllRecordings()
                showClearConfirmDialog = false
            }
        )
    }

    if (showCountryDialog) {
        CountrySelectionDialog(
            currentCode = settings.userCountryCode,
            onDismiss = { showCountryDialog = false },
            onSelect = {
                viewModel.setUserCountry(it.code, it.name)
                showCountryDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentPrimary = settings.primaryLanguage,
            currentPreferred = settings.preferredLanguages,
            onDismiss = { showLanguageDialog = false },
            onSave = { primary, preferred ->
                viewModel.setMusicLanguages(primary, preferred)
            }
        )
    }

    if (showTasteDialog) {
        MusicTasteDialog(
            currentGenres = settings.favoriteGenres,
            currentArtists = settings.favoriteArtists,
            onDismiss = { showTasteDialog = false },
            onSave = { genres, artists ->
                viewModel.setFavoriteGenres(genres)
                viewModel.setFavoriteArtists(artists)
            }
        )
    }

    if (showResetConfirmDialog) {
        ResetRecommendationsDialog(
            onDismiss = { showResetConfirmDialog = false },
            onConfirm = {
                viewModel.clearListeningHistoryAndReset()
                showResetConfirmDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            color = Color(0xFFB3B3B3)
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpotifyCardSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF909090)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SpotifyGreenBright,
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF282828),
                uncheckedBorderColor = Color(0xFF383838)
            )
        )
    }
}

@Composable
private fun SettingsIconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF242424)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SpotifyGreenBright,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsSegmentButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) SpotifyGreenBright else Color(0xFF222222),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 12.sp
            ),
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF222222))
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF909090)
                    )
                )
            }
        }
    }
}

@Composable
private fun CountrySelectionDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onSelect: (CountryInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) MusicPreferenceCatalog.COUNTRIES
        else MusicPreferenceCatalog.COUNTRIES.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) || it.code.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Country / Region",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country...", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SpotifyDarkSurface,
                        unfocusedContainerColor = SpotifyDarkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered, key = { it.code }) { country ->
                        val isSelected = country.code.equals(currentCode, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SpotifyGreenBright.copy(alpha = 0.15f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(country) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = country.flagEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = country.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) SpotifyGreenBright else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SpotifyGreenBright, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = SpotifyCardSurface
    )
}

@Composable
private fun LanguageSelectionDialog(
    currentPrimary: String,
    currentPreferred: Set<String>,
    onDismiss: () -> Unit,
    onSave: (primary: String, preferred: Set<String>) -> Unit
) {
    var selectedLangs by remember { mutableStateOf(currentPreferred.toMutableSet()) }
    var primaryLang by remember { mutableStateOf(currentPrimary) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Music Languages", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                Text("Choose preferred languages and tap to set primary:", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(MusicPreferenceCatalog.LANGUAGES, key = { it.code }) { lang ->
                        val isSelected = selectedLangs.contains(lang.name)
                        val isPrimary = primaryLang == lang.name
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SpotifyDarkSurface else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val updated = selectedLangs.toMutableSet()
                                if (updated.contains(lang.name)) {
                                    if (updated.size > 1) {
                                        updated.remove(lang.name)
                                        if (primaryLang == lang.name) primaryLang = updated.first()
                                    }
                                } else {
                                    updated.add(lang.name)
                                }
                                selectedLangs = updated
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = lang.name, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                    Text(text = lang.nativeName, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isPrimary) SpotifyGreenBright else Color(0xFF333333),
                                            modifier = Modifier.clickable { primaryLang = lang.name }.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isPrimary) "Primary" else "Set Primary",
                                                color = if (isPrimary) Color.Black else Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SpotifyGreenBright, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(primaryLang, selectedLangs)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreenBright)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = SpotifyCardSurface
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusicTasteDialog(
    currentGenres: Set<String>,
    currentArtists: Set<String>,
    onDismiss: () -> Unit,
    onSave: (genres: Set<String>, artists: Set<String>) -> Unit
) {
    var selectedGenres by remember { mutableStateOf(currentGenres.toMutableSet()) }
    var selectedArtists by remember { mutableStateOf(currentArtists.toMutableSet()) }
    var newArtist by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Music Taste & Favorites", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(380.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("FAVORITE GENRES", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MusicPreferenceCatalog.GENRES.forEach { genre ->
                            val isSel = selectedGenres.contains(genre.name)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) SpotifyGreenBright else Color(0xFF282828),
                                modifier = Modifier.clickable {
                                    val up = selectedGenres.toMutableSet()
                                    if (up.contains(genre.name)) {
                                        if (up.size > 1) up.remove(genre.name)
                                    } else up.add(genre.name)
                                    selectedGenres = up
                                }
                            ) {
                                Text(
                                    text = genre.name,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("FAVORITE ARTISTS", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newArtist,
                            onValueChange = { newArtist = it },
                            placeholder = { Text("Add artist...", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SpotifyDarkSurface,
                                unfocusedContainerColor = SpotifyDarkSurface,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (newArtist.isNotBlank()) {
                                    val up = selectedArtists.toMutableSet()
                                    up.add(newArtist.trim())
                                    selectedArtists = up
                                    newArtist = ""
                                }
                            },
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(SpotifyGreenBright)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        }
                    }
                }
                item {
                    val allArtists = (selectedArtists.toList() + MusicPreferenceCatalog.POPULAR_ARTISTS.map { it.name }).distinct()
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allArtists.forEach { name ->
                            val isSel = selectedArtists.contains(name)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) SpotifyGreenBright else Color(0xFF282828),
                                modifier = Modifier.clickable {
                                    val up = selectedArtists.toMutableSet()
                                    if (up.contains(name)) up.remove(name) else up.add(name)
                                    selectedArtists = up
                                }
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedGenres, selectedArtists)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreenBright)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = SpotifyCardSurface
    )
}

@Composable
private fun ResetRecommendationsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reset History & Recommendations?", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
        },
        text = {
            Text(
                text = "This will erase your recent search history, listening telemetry, and reset all recommendation caches back to fresh defaults. Your saved downloads and playlists will remain untouched.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB3B3B3))
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("Reset Everything", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = SpotifyCardSurface
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return "${DecimalFormat("#,##0.#").format(value)} ${units[digitGroups]}"
}

