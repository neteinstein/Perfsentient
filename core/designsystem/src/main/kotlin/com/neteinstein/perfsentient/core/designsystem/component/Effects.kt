package com.neteinstein.perfsentient.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fades and slides an item in, delayed by its [index], to create a staggered list entrance.
 */
fun Modifier.staggeredEntrance(index: Int, baseDelayMs: Long = 60L): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(48f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(MAX_STAGGER) * baseDelayMs)
        launch { alpha.animateTo(1f, tween(durationMillis = 420)) }
        offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
    }
    graphicsLayer {
        this.alpha = alpha.value
        translationY = offset.value * density
    }
}

private const val MAX_STAGGER = 12
