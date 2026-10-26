package com.audiovault.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.intelligence.SongMetadata
import com.audiovault.app.core.service.MonitoringState
import com.audiovault.app.ui.theme.SpotifyBorderDivider
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright
import com.audiovault.app.ui.theme.StateIdleGray
import com.audiovault.app.ui.theme.StateRecordingRed
import com.audiovault.app.ui.theme.StateWaitingAmber

@Composable
fun MonitoringStateCard(
    state: MonitoringState,
    detectedSong: SongMetadata? = null,
    modifier: Modifier = Modifier
) {
    val isRecording = state is MonitoringState.Recording
    val isMonitoring = state is MonitoringState.Monitoring || state is MonitoringState.WaitingForAudio

    val statusColor = when (state) {
        is MonitoringState.Recording -> StateRecordingRed
        is MonitoringState.WaitingForAudio, is MonitoringState.Monitoring -> SpotifyGreenBright
        is MonitoringState.Stopping -> StateWaitingAmber
        is MonitoringState.Error -> StateRecordingRed
        is MonitoringState.Idle -> StateIdleGray
    }

    val statusText = when (state) {
        is MonitoringState.Idle -> "STANDBY"
        is MonitoringState.Monitoring, is MonitoringState.WaitingForAudio -> "LISTENING FOR MUSIC"
        is MonitoringState.Recording -> "RECORDING IN STUDIO QUALITY"
        is MonitoringState.Stopping -> "SAVING TRACK..."
        is MonitoringState.Error -> "CAPTURE ERROR"
    }

    val songTitle = if (detectedSong != null && detectedSong.isAdvertisement) {
        "Advertisement"
    } else if (detectedSong != null && detectedSong.title.isNotBlank() && !detectedSong.title.equals("Unknown", ignoreCase = true)) {
        detectedSong.title
    } else if (isRecording) {
        "Capturing Audio Stream"
    } else if (isMonitoring) {
        "Ready & Waiting..."
    } else {
        "Ready to Capture"
    }

    val songArtist = if (detectedSong != null && detectedSong.isAdvertisement) {
        "Audio recording paused (Ad Skipped)"
    } else if (detectedSong != null && !detectedSong.artist.isNullOrBlank()) {
        detectedSong.artist
    } else if (isRecording) {
        "Auto-Splitting & Tagging Active"
    } else if (isMonitoring) {
        "Play any song on Recordify or media players to start recording"
    } else {
        "Tap start below to monitor background music"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SpotifyDarkSurface
        ),
        border = BorderStroke(1.dp, if (isRecording) StateRecordingRed.copy(alpha = 0.35f) else SpotifyBorderDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clean animated sound visualizer
            LiveAudioVisualizerIndicator(state = state)

            Spacer(modifier = Modifier.height(14.dp))

            // Minimalist Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = statusColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Song Title & Artist Display
            Text(
                text = songTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (detectedSong?.isAdvertisement == true) StateRecordingRed else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = songArtist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFB3B3B3),
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sleek subtitle metadata info
            Text(
                text = "Studio 320 kbps AAC • Smart Ad Filter • Auto-Split",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF777777),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
