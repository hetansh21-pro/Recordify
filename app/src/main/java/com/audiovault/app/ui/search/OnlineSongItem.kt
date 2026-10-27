package com.audiovault.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.ui.components.SwipeToQueueContainer
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.theme.SpotifyGreenBright

@Composable
fun OnlineSongItem(
    song: OnlineSongModel,
    isPlaying: Boolean,
    isSavedInLibrary: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    versionBadge: String? = null,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit = {},
    onSwipeToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SwipeToQueueContainer(
        onSwipeToQueue = { onSwipeToQueue?.invoke() },
        enabled = onSwipeToQueue != null,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { onPlayClick() }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // High-Resolution Square Cover Art (Identical to normal songs)
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            TrackCoverArt(
                albumArtPath = song.highResArtUrl,
                title = song.title,
                artist = song.artist,
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

        // Normal Song Typography & Layout
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) SpotifyGreenBright else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!versionBadge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x3300E676),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = versionBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SpotifyGreenBright,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "Song • ${song.artist}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xB3FFFFFF),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action Buttons: Stream Playback / Download / Saved Checkmark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (isDownloading) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onCancelDownload() }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgress ?: 0.5f },
                        modifier = Modifier.size(24.dp),
                        color = SpotifyGreenBright,
                        strokeWidth = 2.dp,
                        trackColor = Color(0x33FFFFFF)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Download",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else if (isSavedInLibrary) {
                Surface(
                    shape = CircleShape,
                    color = SpotifyGreenBright.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Saved in Library",
                            tint = SpotifyGreenBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save to Library",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
}
