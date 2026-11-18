package com.neteinstein.perfsentient.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val START_ANGLE = 135f
private const val SWEEP = 270f

/**
 * Animated circular health gauge: the arc sweeps in with a gradient, the number counts up and
 * a soft halo "breathes" behind it.
 */
@Composable
fun HealthGauge(
    score: Int,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    strokeWidth: Dp = 18.dp,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        progress.animateTo(score / 100f, tween(durationMillis = 1_400, easing = FastOutSlowInEasing))
    }
    val displayed by animateIntAsState(score, tween(1_400, easing = FastOutSlowInEasing), label = "score")
    val animatedColor by animateColorAsState(color, tween(800), label = "gaugeColor")

    val infinite = rememberInfiniteTransition(label = "halo")
    val halo by infinite.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing), RepeatMode.Reverse),
        label = "haloAlpha",
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "shimmer",
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$label $score" }
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(animatedColor.copy(alpha = halo), Color.Transparent),
                        center = center,
                        radius = this.size.minDimension / 1.6f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(trackColor, START_ANGLE, SWEEP, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            rotate(shimmer) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(animatedColor.copy(alpha = 0.55f), animatedColor, Color.White.copy(alpha = 0.9f), animatedColor),
                    ),
                    startAngle = START_ANGLE - shimmer,
                    sweepAngle = SWEEP * progress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayed.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = animatedColor,
            )
        }
    }
}
