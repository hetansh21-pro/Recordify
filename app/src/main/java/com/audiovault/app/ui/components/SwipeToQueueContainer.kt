package com.audiovault.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.ui.theme.SpotifyGreenBright
import kotlin.math.roundToInt

/**
 * Authentic Spotify-Style Swipe Right to Add to Queue Container.
 * Reveals a vibrant Spotify green indicator when dragged to the right.
 */
@Composable
fun SwipeToQueueContainer(
    onSwipeToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val density = LocalDensity.current
    val triggerThresholdPx = with(density) { 68.dp.toPx() }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "SwipeToQueueOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Green background revealed when swiping right
        if (animatedOffsetX > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(SpotifyGreenBright),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Add to Queue",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add to queue",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Swipable child content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {},
                        onDragEnd = {
                            if (dragOffsetX > triggerThresholdPx) {
                                onSwipeToQueue()
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = dragOffsetX + dragAmount
                            if (newOffset >= 0f) {
                                change.consume()
                                dragOffsetX = newOffset.coerceAtMost(triggerThresholdPx * 1.4f)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
