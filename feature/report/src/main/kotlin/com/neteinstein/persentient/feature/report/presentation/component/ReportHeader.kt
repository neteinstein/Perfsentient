package com.neteinstein.persentient.feature.report.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neteinstein.persentient.core.designsystem.behavior.label
import com.neteinstein.persentient.core.designsystem.component.AuroraBackground
import com.neteinstein.persentient.core.designsystem.component.HealthGauge
import com.neteinstein.persentient.core.designsystem.component.StatusChip
import com.neteinstein.persentient.core.designsystem.theme.PersentientTheme
import com.neteinstein.persentient.core.model.DailyReport
import com.neteinstein.persentient.feature.report.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun ReportHeader(
    report: DailyReport?,
    isGenerating: Boolean,
    canAnalyse: Boolean,
    onAnalyseNow: () -> Unit,
    topInset: Dp,
    horizontalPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val status = PersentientTheme.status
    val color = report?.let { status.forVerdict(it.verdict) } ?: MaterialTheme.colorScheme.primary
    AuroraBackground(modifier = modifier.fillMaxWidth(), accent = color) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontalPadding)
                .padding(top = topInset + 20.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(report?.date ?: java.time.LocalDate.now()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.report_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))
            HealthGauge(
                score = report?.score ?: 0,
                color = color,
                label = stringResource(R.string.report_health_score),
            )
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = report,
                transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 }) },
                label = "verdict",
            ) { current ->
                if (current != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusChip(text = stringResource(current.verdict.label), color = status.forVerdict(current.verdict))
                        Spacer(Modifier.height(8.dp))
                        val offenders = current.offenders.size
                        Text(
                            text = if (offenders == 0) {
                                stringResource(R.string.report_summary_clean)
                            } else {
                                pluralStringResource(R.plurals.report_summary_offenders, offenders, offenders)
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                            .format(Instant.ofEpochMilli(current.generatedAt).atZone(ZoneId.systemDefault()))
                        Text(
                            pluralStringResource(R.plurals.report_generated_at, current.appsAnalysed, time, current.appsAnalysed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (canAnalyse) {
                Spacer(Modifier.height(16.dp))
                AnalyseButton(isGenerating = isGenerating, onClick = onAnalyseNow)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun AnalyseButton(isGenerating: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spinAngle",
    )
    Button(onClick = onClick, enabled = !isGenerating, modifier = modifier) {
        Icon(
            Icons.Rounded.Autorenew,
            contentDescription = null,
            modifier = Modifier.size(18.dp).rotate(if (isGenerating) spin else 0f),
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(if (isGenerating) R.string.report_analysing else R.string.report_analyse_now))
    }
}
