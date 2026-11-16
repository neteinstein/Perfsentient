package com.neteinstein.persentient.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ScoreBar(val label: String, val score: Int, val color: Color)

/** Animated vertical bars, e.g. the last 7 daily scores. */
@Composable
fun ScoreBars(bars: List<ScoreBar>, modifier: Modifier = Modifier, height: Int = 120) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { index, bar ->
            val grow = remember(bar.label) { Animatable(0f) }
            LaunchedEffect(bar.score) {
                grow.animateTo(bar.score / 100f, tween(900, delayMillis = index * 70, easing = FastOutSlowInEasing))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(bar.score.toString(), style = MaterialTheme.typography.labelSmall, color = bar.color)
                Canvas(Modifier.width(22.dp).height(height.dp)) {
                    val radius = CornerRadius(size.width / 2, size.width / 2)
                    drawRoundRect(track, cornerRadius = radius)
                    val h = size.height * grow.value
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(bar.color, bar.color.copy(alpha = 0.45f))),
                        topLeft = Offset(0f, size.height - h),
                        size = Size(size.width, h),
                        cornerRadius = radius,
                    )
                }
                Text(bar.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
