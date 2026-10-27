package com.audiovault.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.audiovault.app.ui.theme.SpotifyGreenBright
import kotlin.math.roundToInt

/**
 * Authentic Spotify Add-to-Queue Icon (`+≡`).
 * Pixel-perfect recreation of Spotify's iconic queue indicator:
 * Plus sign on the left with 3 horizontal playlist lines on the right.
 */
@Composable
fun SpotifyAddToQueueIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Black
) {
    Canvas(modifier = modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.6.dp.toPx()

        // 1. Plus symbol (+) on the left
        val plusCenterX = w * 0.25f
        val plusCenterY = h * 0.50f
        val plusArm = w * 0.15f

        // Horizontal line of '+'
        drawLine(
            color = tint,
            start = Offset(plusCenterX - plusArm, plusCenterY),
            end = Offset(plusCenterX + plusArm, plusCenterY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Vertical line of '+'
        drawLine(
            color = tint,
            start = Offset(plusCenterX, plusCenterY - plusArm),
            end = Offset(plusCenterX, plusCenterY + plusArm),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 2. Three horizontal lines (≡) on the right
        val lineStartX = w * 0.48f
        val lineEndX = w * 0.92f

        // Top line
        drawLine(
            color = tint,
            start = Offset(lineStartX, h * 0.32f),
            end = Offset(lineEndX, h * 0.32f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Middle line
        drawLine(
            color = tint,
            start = Offset(lineStartX, h * 0.52f),
            end = Offset(lineEndX, h * 0.52f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Bottom line
        drawLine(
            color = tint,
            start = Offset(lineStartX, h * 0.72f),
            end = Offset(lineEndX, h * 0.72f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Authentic Spotify-Style Swipe Right to Add to Queue Container.
 * Reveals a vibrant Spotify green indicator (`#1ED760`) with centered `+≡` icon
 * when dragged to the right, exactly matching the Spotify UI.
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
    val haptic = LocalHapticFeedback.current
    val triggerThresholdPx = with(density) { 68.dp.toPx() }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var hasHapticTriggered by remember { mutableStateOf(false) }

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
        // Vibrant Spotify green background revealed when swiping right
        if (animatedOffsetX > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(SpotifyGreenBright),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier.padding(start = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SpotifyAddToQueueIcon(
                        modifier = Modifier.size(26.dp),
                        tint = Color.Black
                    )
                }
            }
        }

        // Swipable child content with solid dark background so green is only revealed on the left
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .background(Color(0xFF121212))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasHapticTriggered = false
                        },
                        onDragEnd = {
                            if (dragOffsetX > triggerThresholdPx) {
                                onSwipeToQueue()
                            }
                            dragOffsetX = 0f
                            hasHapticTriggered = false
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                            hasHapticTriggered = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = dragOffsetX + dragAmount
                            if (newOffset >= 0f) {
                                change.consume()
                                dragOffsetX = newOffset.coerceAtMost(triggerThresholdPx * 1.5f)
                                if (dragOffsetX > triggerThresholdPx && !hasHapticTriggered) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hasHapticTriggered = true
                                }
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
