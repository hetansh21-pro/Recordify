package com.audiovault.app.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.audiovault.app.R
import kotlinx.coroutines.delay

/**
 * High-polish Recordify branded loading / splash screen.
 * Displays the circular audio wave logo and clean white "Recordify" wordmark
 * on a deep matte black background (#0A0A0A) matching the app theme.
 */
@Composable
fun RecordifySplashScreen(
    isTv: Boolean = false,
    durationMs: Long = 1100L,
    onSplashFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        delay(durationMs)
        onSplashFinished()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "SplashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.92f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "SplashScale"
    )

    val iconSize: Dp = if (isTv) 130.dp else 100.dp
    val wordmarkWidth: Dp = if (isTv) 250.dp else 190.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
        ) {
            // 1. Circular Audio Wave Logo
            Image(
                painter = painterResource(id = R.drawable.ic_recordify_logo_circle),
                contentDescription = "Recordify Logo",
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(if (isTv) 20.dp else 16.dp))

            // 2. Clean Recordify Wordmark (White font with green dot in 'o')
            Image(
                painter = painterResource(id = R.drawable.ic_recordify_wordmark_dark),
                contentDescription = "Recordify",
                modifier = Modifier.width(wordmarkWidth),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
