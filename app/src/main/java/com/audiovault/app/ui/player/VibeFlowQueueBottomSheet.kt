package com.audiovault.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.audiovault.app.core.queue.PlaybackQueueState
import com.audiovault.app.core.queue.PlaybackRepeatMode
import com.audiovault.app.core.queue.QueueItem
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Authentic Spotify-Identical Queue Bottom Sheet.
 * - Top Header: "Queue" + "Playing [Source]" with Spotify Green Headphones indicator.
 * - Now Playing Hero Card: Green title, artist, white circular play/pause button.
 * - Next In Queue: Square cover art, white title, grey artist subtitle, and right-hand 3-bar drag handle (`≡`).
 * - Hold & Drag reordering: Touch and drag up/down reorders items in the queue.
 * - Tap to reveal quick actions: Up, Down, Delete buttons.
 * - Bottom Bar: [🔀 Shuffle] and [⏱️ Timer] rounded pill buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeFlowQueueBottomSheet(
    queueState: PlaybackQueueState,
    isPlaying: Boolean = true,
    onTogglePlayPause: () -> Unit = {},
    onPlayTrackAt: (Int) -> Unit,
    onRemoveTrackAt: (Int) -> Unit,
    onMoveQueueItemUp: ((Int) -> Unit)? = null,
    onMoveQueueItemDown: ((Int) -> Unit)? = null,
    onMoveQueueItem: ((Int, Int) -> Unit)? = null,
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
    var showTimerMenu by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val rowThresholdPx = with(density) { 56.dp.toPx() }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var draggingDeltaY by remember { mutableFloatStateOf(0f) }

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
                .fillMaxHeight(0.90f)
                .padding(horizontal = 16.dp)
        ) {
            // -------------------------------------------------------------
            // 1. TOP HEADER BAR: "Queue" + "Playing from [Source]"
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Queue",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    )
                    val sourceName = queueState.currentTrack?.album
                        ?: queueState.currentTrack?.title
                        ?: "Recordify"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = SpotifyGreenBright,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Playing $sourceName",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFB3B3B3),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onRefreshQueue, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Queue",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // 2. QUEUE SCROLLABLE CONTENT (Now Playing + Up Next)
            // -------------------------------------------------------------
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                // 2A. NOW PLAYING SECTION HERO
                val current = queueState.currentTrack
                if (current != null) {
                    item {
                        Text(
                            text = "Now playing",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF181818),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackCoverArt(
                                    albumArtPath = current.albumArtPath,
                                    title = current.title,
                                    artist = current.artist,
                                    size = 52.dp,
                                    shape = RoundedCornerShape(6.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = current.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SpotifyGreenBright,
                                            fontSize = 15.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = current.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFB3B3B3),
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!current.recommendationReason.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = current.recommendationReason,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = SpotifyGreenBright,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Circular White Play/Pause Button (Spotify match)
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { onTogglePlayPause() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 2B. NEXT IN QUEUE HEADER
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val headerLabel = if (queueState.isShuffleEnabled) {
                            "Shuffling from: ${queueState.currentTrack?.album ?: "Playlist"}"
                        } else {
                            "Next in queue"
                        }
                        Text(
                            text = headerLabel,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        )

                        if (queueState.upcomingQueue.isNotEmpty()) {
                            Text(
                                text = "Clear all",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFB3B3B3),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onClearUpcoming() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 2C. UPCOMING TRACKS LIST
                if (queueState.upcomingQueue.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(40.dp)
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
                    itemsIndexed(
                        queueState.upcomingQueue,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val isItemDragging = draggingItemId == item.id
                        SpotifyQueueTrackRow(
                            item = item,
                            index = index,
                            totalCount = queueState.upcomingQueue.size,
                            isDragging = isItemDragging,
                            draggingOffsetY = if (isItemDragging) draggingDeltaY else 0f,
                            onDragStart = {
                                draggingItemId = item.id
                                draggingDeltaY = 0f
                            },
                            onDragEnd = {
                                draggingItemId = null
                                draggingDeltaY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggingDeltaY += dragAmount.y
                                val currentIdx = queueState.upcomingQueue.indexOfFirst { it.id == draggingItemId }
                                if (currentIdx >= 0) {
                                    if (draggingDeltaY > rowThresholdPx && currentIdx < queueState.upcomingQueue.size - 1) {
                                        onMoveQueueItem?.invoke(currentIdx, currentIdx + 1) ?: onMoveQueueItemDown?.invoke(currentIdx)
                                        draggingDeltaY -= rowThresholdPx
                                    } else if (draggingDeltaY < -rowThresholdPx && currentIdx > 0) {
                                        onMoveQueueItem?.invoke(currentIdx, currentIdx - 1) ?: onMoveQueueItemUp?.invoke(currentIdx)
                                        draggingDeltaY += rowThresholdPx
                                    }
                                }
                            },
                            isEditing = activeEditIndex == index,
                            onToggleEdit = {
                                activeEditIndex = if (activeEditIndex == index) null else index
                            },
                            onPlay = { onPlayTrackAt(index) },
                            onMoveUp = {
                                onMoveQueueItem?.invoke(index, index - 1) ?: onMoveQueueItemUp?.invoke(index)
                            },
                            onMoveDown = {
                                onMoveQueueItem?.invoke(index, index + 1) ?: onMoveQueueItemDown?.invoke(index)
                            },
                            onRemove = {
                                onRemoveTrackAt(index)
                                if (activeEditIndex == index) activeEditIndex = null
                            }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. BOTTOM STICKY CONTROLS BAR: [🔀 Shuffle] & [⏱️ Timer] & Auto-Queue
            // -------------------------------------------------------------
            Surface(
                color = Color(0xFF121212),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Group: Shuffle Pill & Timer Pill (Exact Spotify UI match)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Shuffle Pill Button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (queueState.isShuffleEnabled) SpotifyGreenBright else Color(0xFF242424),
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onToggleShuffle() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (queueState.isShuffleEnabled) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (queueState.isShuffleEnabled) "Shuffle" else "Shuffle",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (queueState.isShuffleEnabled) Color.Black else Color.White,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // 2. Timer Pill Button
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF242424),
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showTimerMenu = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Timer",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Timer",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showTimerMenu,
                                onDismissRequest = { showTimerMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("15 minutes") },
                                    onClick = { showTimerMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("30 minutes") },
                                    onClick = { showTimerMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("45 minutes") },
                                    onClick = { showTimerMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("End of track") },
                                    onClick = { showTimerMenu = false }
                                )
                            }
                        }
                    }

                    // Right: AI Auto-Queue Quick Toggle
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (queueState.isAutoQueueEnabled) Color(0xFF1E3823) else Color(0xFF242424),
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onToggleAutoQueue() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Auto Queue",
                                tint = if (queueState.isAutoQueueEnabled) SpotifyGreenBright else Color(0xFF888888),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (queueState.isAutoQueueEnabled) "Auto-Queue: ON" else "Auto: OFF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (queueState.isAutoQueueEnabled) SpotifyGreenBright else Color(0xFF888888),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Spotify Queue Track Item Row with Right-Hand 3-Bar Drag Handle (`≡`).
 * - Hold and drag up or down to reorder.
 * - Tap drag handle or item to reveal direct [▲ Move Up] [▼ Move Down] [✕ Remove] buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpotifyQueueTrackRow(
    item: QueueItem,
    index: Int,
    totalCount: Int,
    isDragging: Boolean = false,
    draggingOffsetY: Float = 0f,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> },
    isEditing: Boolean = false,
    onToggleEdit: () -> Unit,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val density = LocalDensity.current
    val deleteThresholdPx = with(density) { 72.dp.toPx() }
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedSwipeOffsetX by animateFloatAsState(targetValue = swipeOffsetX)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = draggingOffsetY
                scaleX = if (isDragging) 1.04f else 1f
                scaleY = if (isDragging) 1.04f else 1f
                shadowElevation = if (isDragging) 28f else 0f
            }
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Red swipe-left-to-delete background
        if (animatedSwipeOffsetX < 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFFE91E63)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete from Queue",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedSwipeOffsetX.roundToInt(), 0) }
                .background(if (isDragging) Color(0xFF282828) else if (isEditing) Color(0xFF222222) else Color(0xFF141414))
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {},
                        onDragEnd = {
                            if (swipeOffsetX < -deleteThresholdPx) {
                                onRemove()
                            }
                            swipeOffsetX = 0f
                        },
                        onDragCancel = {
                            swipeOffsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = swipeOffsetX + dragAmount
                            if (newOffset <= 0f) {
                                change.consume()
                                swipeOffsetX = newOffset.coerceAtLeast(-deleteThresholdPx * 1.5f)
                            }
                        }
                    )
                }
                .combinedClickable(
                    onClick = onPlay,
                    onLongClick = onToggleEdit
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Cover Art (Square 46.dp)
            TrackCoverArt(
                albumArtPath = item.albumArtPath,
                title = item.title,
                artist = item.artist,
                size = 46.dp,
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Artist Subtitle (Clean Spotify-Style without green badges)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isEditing) {
                // Three Quick Action Items: Up, Down, Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 1. Move Up
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (index > 0) Color.White else Color(0xFF555555),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 2. Move Down
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (index < totalCount - 1) Color.White else Color(0xFF555555),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3. Remove
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
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
                // Authentic 3-Bar Drag Handle (`≡`)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .pointerInput(item.id) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onDrag = { change, dragAmount -> onDrag(change, dragAmount) }
                            )
                        }
                        .clip(CircleShape)
                        .clickable { onToggleEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    SpotifyDragHandleIcon(color = if (isDragging) Color.White else Color(0xFF888888))
                }
            }
        }
    }
}

/**
 * 3 Horizontal Line Drag Handle Icon (`≡`).
 * Pixel-perfect match for Spotify's queue drag handle.
 */
@Composable
fun SpotifyDragHandleIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF888888)
) {
    Column(
        modifier = modifier.size(22.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}
