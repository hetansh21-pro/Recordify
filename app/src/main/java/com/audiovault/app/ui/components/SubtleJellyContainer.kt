package com.audiovault.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 60/120 FPS hardware-accelerated spring jelly overscroll container.
 * - Activates ONLY on intentional user pull-down at the top boundary (never on flings).
 * - 0 lag, 0 recompositions, purely draw-phase GPU transformation.
 * - Non-blocking asynchronous spring snap-back.
 */
@Composable
fun SubtleJellyContainer(
    isAtTop: () -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Modifier) -> Unit
) {
    val scope = rememberCoroutineScope()
    val rawDrag = remember { mutableFloatStateOf(0f) }
    var releaseJob: Job? = remember { null }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If container is stretched and user drags upwards, retract stretch first
                if (rawDrag.floatValue > 0f && available.y < 0f && source == NestedScrollSource.UserInput) {
                    releaseJob?.cancel()
                    val consumed = available.y.coerceAtLeast(-rawDrag.floatValue)
                    rawDrag.floatValue = (rawDrag.floatValue + consumed).coerceAtLeast(0f)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // ONLY activate on direct vertical user drag at top boundary where Y > X, NEVER on flings or horizontal swipes
                if (available.y > 0f && kotlin.math.abs(available.y) > kotlin.math.abs(available.x) && isAtTop() && source == NestedScrollSource.UserInput) {
                    releaseJob?.cancel()
                    val current = rawDrag.floatValue
                    val maxDrag = 360f
                    val resistance = (1f - (current / maxDrag).coerceIn(0f, 0.75f)) * 0.45f
                    val delta = available.y * resistance
                    rawDrag.floatValue = (current + delta).coerceAtMost(maxDrag)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (rawDrag.floatValue > 0f) {
                    releaseJob?.cancel()
                    releaseJob = scope.launch {
                        val anim = Animatable(rawDrag.floatValue)
                        anim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) {
                            rawDrag.floatValue = value
                        }
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (rawDrag.floatValue > 0f) {
                    releaseJob?.cancel()
                    releaseJob = scope.launch {
                        val anim = Animatable(rawDrag.floatValue)
                        anim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) {
                            rawDrag.floatValue = value
                        }
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val contentModifier = Modifier.graphicsLayer {
        val drag = rawDrag.floatValue
        val translation = drag * 0.42f
        val stretchY = (drag / 1600f).coerceIn(0f, 0.075f)
        val squeezeX = (drag / 4800f).coerceIn(0f, 0.022f)

        translationY = translation
        scaleY = 1f + stretchY
        scaleX = 1f - squeezeX
        transformOrigin = TransformOrigin(0.5f, 0f)
    }

    Box(
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) {
        content(contentModifier)
    }
}
