package com.audiovault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.ui.theme.SpotifyGreenBright

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsBottomSheet(
    recording: RecordingEntity,
    isLiked: Boolean,
    onViewAlbum: ((albumName: String) -> Unit)? = null,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val albumName = recording.album?.trim()?.ifBlank { null }
        ?: if (recording.title.contains(" - ")) recording.title.split(" - ").firstOrNull()?.trim() else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181818),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF555555))
            )
        },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Cover Art + Song Title & Artist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackCoverArt(
                    albumArtPath = recording.albumArtPath,
                    title = recording.title,
                    artist = recording.artist,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    placeholderIconSize = 28.dp,
                    contentDescription = recording.title
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${recording.artist ?: "Unknown Artist"}${if (!albumName.isNullOrBlank()) " • $albumName" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB3B3B3)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Action: View Movie / Album Soundtrack
            if (!albumName.isNullOrBlank() && onViewAlbum != null) {
                MenuOptionItem(
                    icon = Icons.Default.MusicNote,
                    title = "View Movie / Album Soundtrack",
                    subtitle = albumName,
                    onClick = {
                        onDismiss()
                        onViewAlbum(albumName)
                    }
                )
            }

            // Action: Add to Playlist
            MenuOptionItem(
                icon = Icons.Default.QueueMusic,
                title = "Add to playlist",
                subtitle = "Save to custom playlists or queue",
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                }
            )

            // Action: Share
            MenuOptionItem(
                icon = Icons.Default.Share,
                title = "Share Song",
                subtitle = "Send audio or details to friends",
                onClick = {
                    onDismiss()
                    onShare()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Song & Movie Details
            Text(
                text = "Song & Movie Details",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details List
            DetailInfoCard(
                label = "Song Title",
                value = recording.title,
                icon = Icons.Default.MusicNote
            )

            DetailInfoCard(
                label = "Movie / Album",
                value = albumName ?: "Single Release",
                icon = Icons.Default.Info
            )

            DetailInfoCard(
                label = "Artist / Singers",
                value = recording.artist ?: "Unknown Artist",
                icon = Icons.Default.Person
            )

            DetailInfoCard(
                label = "Duration",
                value = formatDuration(recording.durationMs),
                icon = Icons.Default.Timer
            )

            DetailInfoCard(
                label = "Audio Quality",
                value = "${recording.bitrateKbps} kbps • High Definition",
                icon = Icons.Default.Headphones
            )

            DetailInfoCard(
                label = "Playback Source",
                value = if (recording.filePath.startsWith("http")) "Online Stream (Progressive Chunks)" else "Downloaded Offline Storage",
                icon = Icons.Default.Info
            )

            DetailInfoCard(
                label = "Library Status",
                value = if (isLiked) "Saved in Liked Songs" else "Not Liked",
                icon = Icons.Default.CheckCircle,
                highlight = isLiked
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MenuOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF282828)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB3B3B3)
                    )
                )
            }
        }
    }
}

@Composable
private fun DetailInfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    highlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF222222),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highlight) SpotifyGreenBright else Color(0xFF888888),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFB3B3B3),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) SpotifyGreenBright else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
