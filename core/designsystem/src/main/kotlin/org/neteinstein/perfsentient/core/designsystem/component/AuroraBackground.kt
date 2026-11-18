package org.neteinstein.perfsentient.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/** Slowly drifting colour blobs – gives headers a living "aurora" look. */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Box(
        modifier = modifier.drawBehind {
            val w = size.width
            val h = size.height
            val r = maxOf(w, h) * 0.55f
            fun blob(color: Color, cx: Float, cy: Float) = drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.32f), Color.Transparent), center = Offset(cx, cy), radius = r),
                radius = r,
                center = Offset(cx, cy),
            )
            blob(accent, w * (0.25f + 0.12f * cos(phase)), h * (0.30f + 0.10f * sin(phase)))
            blob(secondary, w * (0.80f + 0.10f * sin(phase * 2)), h * (0.20f + 0.12f * cos(phase)))
            blob(tertiary, w * (0.55f + 0.15f * cos(phase + 1.5f)), h * (0.85f + 0.08f * sin(phase * 2)))
        },
        content = content,
    )
}
