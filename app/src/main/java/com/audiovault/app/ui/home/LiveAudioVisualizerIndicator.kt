package com.audiovault.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.audiovault.app.R
import com.audiovault.app.core.service.MonitoringState
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright
import com.audiovault.app.ui.theme.StateRecordingRed

/**
 * Minimalist, ultra-clean soundwave and audio pulse visualizer.
 * 100% idle with 0 CPU / frame cost when monitoring is not active.
 */
@Composable
fun LiveAudioVisualizerIndicator(
    state: MonitoringState,
    modifier: Modifier = Modifier
) {
    val isRecording = state is MonitoringState.Recording
    val isMonitoring = state is MonitoringState.Monitoring || state is MonitoringState.WaitingForAudio

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording || isMonitoring) {
            val activeColor = if (isRecording) StateRecordingRed else SpotifyGreenBright
            val infiniteTransition = rememberInfiniteTransition(label = "VisualizerGlow")

            val glowPulse by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = if (isRecording) 1.12f else 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "GlowPulse"
            )

            // Soft aura glow when active
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = glowPulse
                        scaleY = glowPulse
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                activeColor.copy(alpha = if (isRecording) 0.28f else 0.15f),
                                activeColor.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Center Capsule with jumping bars
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SpotifyCardSurface),
                contentAlignment = Alignment.Center
            ) {
                CleanEqualizerBars(color = activeColor)
            }
        } else {
            // Idle: Crisp static circular Recordify logo with 0 CPU overhead
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_recordify_logo),
                    contentDescription = "Recordify",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun CleanEqualizerBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CleanBars")

    val s1 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s1"
    )
    val s2 by infiniteTransition.animateFloat(
        initialValue = 0.90f, targetValue = 0.40f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s2"
    )
    val s3 by infiniteTransition.animateFloat(
        initialValue = 0.50f, targetValue = 1.20f,
        animationSpec = infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s3"
    )
    val s4 by infiniteTransition.animateFloat(
        initialValue = 1.05f, targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s4"
    )
    val s5 by infiniteTransition.animateFloat(
        initialValue = 0.40f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s5"
    )

    Row(
        modifier = modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EqualizerBar(scale = s1, color = color)
        EqualizerBar(scale = s2, color = color)
        EqualizerBar(scale = s3, color = color)
        EqualizerBar(scale = s4, color = color)
        EqualizerBar(scale = s5, color = color)
    }
}

@Composable
private fun EqualizerBar(
    scale: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(3.5.dp)
            .height(24.dp)
            .graphicsLayer {
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}
