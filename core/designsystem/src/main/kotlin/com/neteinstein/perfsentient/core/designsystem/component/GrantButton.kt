package com.neteinstein.perfsentient.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neteinstein.perfsentient.core.designsystem.theme.PerfsentienceTheme

/** "Grant" button that morphs into an animated green check once [granted] flips to true. */
@Composable
fun GrantButton(
    granted: Boolean,
    grantLabel: String,
    grantedDescription: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = granted,
        transitionSpec = {
            (scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn()) togetherWith
                (scaleOut(tween(150)) + fadeOut(tween(150)))
        },
        contentAlignment = Alignment.Center,
        modifier = modifier,
        label = "grant",
    ) { isGranted ->
        if (isGranted) {
            AnimatedCheck(
                color = PerfsentienceTheme.status.good,
                modifier = Modifier.semantics { contentDescription = grantedDescription },
            )
        } else {
            FilledTonalButton(onClick = onGrant) { Text(grantLabel) }
        }
    }
}

@Composable
fun AnimatedCheck(color: Color, modifier: Modifier = Modifier) {
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(durationMillis = 450, delayMillis = 120)) }
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.55f)
                lineTo(size.width * 0.40f, size.height * 0.80f)
                lineTo(size.width * 0.90f, size.height * 0.22f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * draw.value, partial, true)
            drawPath(partial, Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
