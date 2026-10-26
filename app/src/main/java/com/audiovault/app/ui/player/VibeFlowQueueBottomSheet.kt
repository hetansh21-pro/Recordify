package com.audiovault.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.queue.PlaybackQueueState
import com.audiovault.app.core.queue.PlaybackRepeatMode
import com.audiovault.app.core.queue.QueueItem
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeFlowQueueBottomSheet(
    queueState: PlaybackQueueState,
    onPlayTrackAt: (Int) -> Unit,
    onRemoveTrackAt: (Int) -> Unit,
    onMoveQueueItemUp: ((Int) -> Unit)? = null,
    onMoveQueueItemDown: ((Int) -> Unit)? = null,
    onClearUpcoming: () -> Unit,
    onRefreshQueue: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleAutoQueue: () -> Unit,
    onTogglePersonalization: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeEditIndex by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF555555))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Header Bar with Vibe Flow Indicator & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "VibeFlow",
                        tint = SpotifyGreenBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VibeFlow Queue",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E3823)
                            ) {
                                Text(
                                    text = queueState.activeVibeDescription,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SpotifyGreenBright,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${queueState.upcomingQueue.size} upcoming tracks ready",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF999999))
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRefreshQueue) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Recommendations",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Playback Flow Options (Auto-Queue & Personalization Toggles)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SpotifyCardSurface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Mood Auto-Queue",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Plays compatible songs automatically when queue ends",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Switch(
                            checked = queueState.isAutoQueueEnabled,
                            onCheckedChange = { onToggleAutoQueue() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SpotifyGreenBright,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Personalize to My Taste",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Adapts recommendations using your likes and listening data",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Switch(
                            checked = queueState.isPersonalizationEnabled,
                            onCheckedChange = { onTogglePersonalization() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SpotifyGreenBright,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Repeat & Shuffle Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (queueState.isShuffleEnabled) Color(0xFF1E3823) else SpotifyCardSurface,
                        modifier = Modifier.clickable { onToggleShuffle() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (queueState.isShuffleEnabled) SpotifyGreenBright else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (queueState.isShuffleEnabled) "Shuffle On" else "Shuffle",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (queueState.isShuffleEnabled) SpotifyGreenBright else Color.White
                                )
                            )
                        }
                    }

                    // Repeat Mode Pill
                    val (repeatText, repeatColor, repeatIcon) = when (queueState.repeatMode) {
                        PlaybackRepeatMode.OFF -> Triple("Repeat Off", Color.White, Icons.Default.Repeat)
                        PlaybackRepeatMode.ALL -> Triple("Repeat All", SpotifyGreenBright, Icons.Default.Repeat)
                        PlaybackRepeatMode.ONE -> Triple("Repeat One", SpotifyGreenBright, Icons.Default.RepeatOne)
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (queueState.repeatMode != PlaybackRepeatMode.OFF) Color(0xFF1E3823) else SpotifyCardSurface,
                        modifier = Modifier.clickable { onToggleRepeat() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = repeatIcon,
                                contentDescription = "Repeat",
                                tint = repeatColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = repeatText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = repeatColor
                                )
                            )
                        }
                    }
                }

                if (queueState.upcomingQueue.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFB3B3B3),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { onClearUpcoming() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Queue Content List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Now Playing Track Item
                val current = queueState.currentTrack
                if (current != null) {
                    item {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SpotifyGreenBright,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        QueueTrackRow(
                            item = current,
                            isNowPlaying = true,
                            onPlay = {},
                            onRemove = {}
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Up Next Header
                item {
                    Text(
                        text = "Next in Queue",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (queueState.upcomingQueue.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Queue is clear",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (queueState.isAutoQueueEnabled) "VibeFlow will seamlessly recommend mood-matching songs next." else "Auto-Queue is off. Playback will stop after this track.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF999999))
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(queueState.upcomingQueue, key = { index, item -> "queue_${item.id}_$index" }) { index, item ->
                        QueueTrackRow(
                            item = item,
                            index = index,
                            totalCount = queueState.upcomingQueue.size,
                            isNowPlaying = false,
                            isEditing = activeEditIndex == index,
                            onToggleEdit = {
                                activeEditIndex = if (activeEditIndex == index) null else index
                            },
                            onPlay = { onPlayTrackAt(index) },
                            onMoveUp = { onMoveQueueItemUp?.invoke(index) },
                            onMoveDown = { onMoveQueueItemDown?.invoke(index) },
                            onRemove = {
                                onRemoveTrackAt(index)
                                if (activeEditIndex == index) activeEditIndex = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueTrackRow(
    item: QueueItem,
    index: Int = 0,
    totalCount: Int = 1,
    isNowPlaying: Boolean,
    isEditing: Boolean = false,
    onToggleEdit: () -> Unit = {},
    onPlay: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isNowPlaying) Color(0xFF1E2F20)
                else if (isEditing) Color(0xFF282828)
                else SpotifyCardSurface
            )
            .combinedClickable(
                enabled = !isNowPlaying,
                onClick = onPlay,
                onLongClick = onToggleEdit
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackCoverArt(
            albumArtPath = item.albumArtPath,
            title = item.title,
            artist = item.artist,
            size = 46.dp,
            shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isNowPlaying) SpotifyGreenBright else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.artist,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Transparent Recommendation Explanation
            if (!item.recommendationReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreenBright)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = item.recommendationReason,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SpotifyGreenBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isNowPlaying) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = "Playing",
                tint = SpotifyGreenBright,
                modifier = Modifier.size(20.dp)
            )
        } else if (isEditing) {
            // Three action items on long press: Up, Down, Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Up Icon
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = if (index > 0) Color.White else Color(0xFF555555),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Down Icon
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = if (index < totalCount - 1) Color.White else Color(0xFF555555),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 3. Close Icon
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from Queue",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlay, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Now",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onToggleEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Edit queue item",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
