package com.audiovault.app.ui.tv

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * Utility to detect whether the current Android device is an Android TV / Google TV / Set-top box.
 */
fun isTvDevice(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
    if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
    return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
}

/**
 * Modifier extension that adds TV Remote D-Pad focusability, spring scale animation,
 * glowing border, elevation, and remote click support.
 */
@Composable
fun Modifier.tvRemoteFocusable(
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(10.dp),
    focusScale: Float = 1.03f,
    focusBorderWidth: Dp = 1.5.dp,
    focusBorderColor: Color = Color.White,
    unfocusedBorderColor: Color = Color.Transparent,
    elevationOnFocus: Dp = 0.dp,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null
): Modifier {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tv_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusBorderColor else unfocusedBorderColor,
        label = "tv_border_color"
    )

    val currentElevation by animateFloatAsState(
        targetValue = if (isFocused) elevationOnFocus.value else 0f,
        label = "tv_elevation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    return this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChanged?.invoke(state.isFocused)
        }
        .scale(scale)
        .then(if (currentElevation > 0f) Modifier.shadow(currentElevation.dp, shape = shape) else Modifier)
        .border(width = if (isFocused) focusBorderWidth else 1.dp, color = borderColor, shape = shape)
        .focusable(interactionSource = interactionSource)
        .onKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown &&
                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
            ) {
                onClick()
                true
            } else {
                false
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
