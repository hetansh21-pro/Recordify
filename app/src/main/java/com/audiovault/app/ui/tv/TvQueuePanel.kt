package com.audiovault.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.queue.PlaybackQueueState
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.theme.SpotifyGreenBright
import kotlinx.coroutines.delay

/**
 * TV Playback Queue Management Side Panel.
 * Allows TV remote users to inspect, reorder, play, remove, and clear upcoming tracks.
 * - Auto-focuses on open.
 * - Un-nested side-by-side D-Pad navigation (Up/Down scrolls songs, Right/Left selects delete).
 * - Back key on remote smoothly closes panel.
 * - Touch isolated from background views.
 */
@Composable
fun TvQueuePanel(
    queueState: PlaybackQueueState,
    currentRecordingTitle: String?,
    currentRecordingArtist: String?,
    currentRecordingArt: String?,
    isPlaying: Boolean,
    onPlayQueueIndex: (Int) -> Unit,
    onRemoveQueueIndex: (Int) -> Unit,
    onMoveQueueItemUp: ((Int) -> Unit)? = null,
    onMoveQueueItemDown: ((Int) -> Unit)? = null,
    onClearQueue: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    var activeEditIndex by remember { mutableStateOf<Int?>(null) }

    // Auto-focus on entry for TV remote
    LaunchedEffect(Unit) {
        delay(150)
        try {
            if (queueState.upcomingQueue.isNotEmpty()) {
                firstItemFocusRequester.requestFocus()
            } else {
                closeFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
        color = Color(0xFF121212),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282828)),
        shadowElevation = 16.dp,
        modifier = modifier
            .width(460.dp)
            .fillMaxHeight()
            // Touch Isolation: Consume all taps inside panel so nothing touches background
            .pointerInput(Unit) {
                detectTapGestures { /* Consume touches inside queue panel */ }
            }
            // Direct Remote Back button handling
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Back) {
                    onClose()
                    true
                } else false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Playback Queue (${queueState.upcomingQueue.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (queueState.upcomingQueue.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF242424),
                            modifier = Modifier.tvRemoteFocusable(
                                onClick = onClearQueue,
                                shape = RoundedCornerShape(14.dp),
                                focusScale = 1.05f
                            )
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE91E63),
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .focusRequester(closeFocusRequester)
                            .tvRemoteFocusable(
                                onClick = onClose,
                                shape = CircleShape,
                                focusScale = 1.15f,
                                focusBorderWidth = 2.dp,
                                focusBorderColor = Color.White,
                                focusedBackgroundColor = Color.White.copy(alpha = 0.25f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Queue",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Now Playing Section Card
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = SpotifyGreenBright,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpotifyGreenBright.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreenBright.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackCoverArt(
                        albumArtPath = currentRecordingArt,
                        title = currentRecordingTitle ?: "Now Playing",
                        artist = currentRecordingArtist ?: "Recordify",
                        size = 48.dp,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentRecordingTitle ?: "No Song Playing",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentRecordingArtist ?: "Recordify Studio",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFB3B3B3),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TvEqualizerBars(isPlaying = isPlaying)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Up Next Section
            Text(
                text = "NEXT IN RECORDIFY VAULT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF888888),
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (queueState.upcomingQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Your queue is empty",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Play any song or album to populate your upcoming queue automatically.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF777777)),
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(queueState.upcomingQueue) { index, song ->
                        val isEditing = activeEditIndex == index
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Track Item Surface (Clicking plays the track index)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isEditing) Color(0xFF242424) else Color(0xFF1A1A1A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEditing) Color(0xFF444444) else Color(0xFF282828)),
                                modifier = Modifier
                                    .weight(1f)
                                    .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                                    .tvRemoteFocusable(
                                        onClick = { onPlayQueueIndex(index) },
                                        shape = RoundedCornerShape(10.dp),
                                        focusScale = 1.02f,
                                        focusBorderWidth = 2.dp,
                                        focusBorderColor = Color.White
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF888888),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.width(24.dp)
                                    )

                                    TrackCoverArt(
                                        albumArtPath = song.albumArtPath,
                                        title = song.title,
                                        artist = song.artist,
                                        size = 40.dp,
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF888888),
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (isEditing) {
                                // Three Action Buttons on edit mode: Up, Down, Close
                                // 1. Move Up Button (enabled if index > 0)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .tvRemoteFocusable(
                                            onClick = { onMoveQueueItemUp?.invoke(index) },
                                            shape = CircleShape,
                                            focusScale = 1.2f,
                                            focusBorderWidth = 2.dp,
                                            focusBorderColor = Color.White,
                                            focusedBackgroundColor = Color.White.copy(alpha = 0.25f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up in Queue",
                                        tint = if (index > 0) Color.White else Color(0xFF555555),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // 2. Move Down Button (enabled if index < queueState.upcomingQueue.size - 1)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .tvRemoteFocusable(
                                            onClick = { onMoveQueueItemDown?.invoke(index) },
                                            shape = CircleShape,
                                            focusScale = 1.2f,
                                            focusBorderWidth = 2.dp,
                                            focusBorderColor = Color.White,
                                            focusedBackgroundColor = Color.White.copy(alpha = 0.25f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down in Queue",
                                        tint = if (index < queueState.upcomingQueue.size - 1) Color.White else Color(0xFF555555),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // 3. Remove Button (deletes track index)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .tvRemoteFocusable(
                                            onClick = {
                                                onRemoveQueueIndex(index)
                                                activeEditIndex = null
                                            },
                                            shape = CircleShape,
                                            focusScale = 1.2f,
                                            focusBorderWidth = 2.dp,
                                            focusBorderColor = Color.White,
                                            focusedBackgroundColor = Color.White.copy(alpha = 0.25f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove from Queue",
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                // Edit Action Trigger Button
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .tvRemoteFocusable(
                                            onClick = { activeEditIndex = index },
                                            shape = CircleShape,
                                            focusScale = 1.2f,
                                            focusBorderWidth = 2.dp,
                                            focusBorderColor = Color.White,
                                            focusedBackgroundColor = Color.White.copy(alpha = 0.25f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Edit Track in Queue",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
