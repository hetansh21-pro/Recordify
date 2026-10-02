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
import com.audiovault.app.ui.theme.SpotifyGreenBright
import com.audiovault.app.ui.theme.StateRecordingRed
import java.text.DecimalFormat

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val recordingCount by viewModel.recordingCount.collectAsState()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
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

            // SECTION 2: AUDIO FIDELITY & SILENCE TIMEOUT
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

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return "${DecimalFormat("#,##0.#").format(value)} ${units[digitGroups]}"
}

