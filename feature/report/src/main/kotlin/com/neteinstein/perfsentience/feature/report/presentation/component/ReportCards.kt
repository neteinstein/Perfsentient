package com.neteinstein.perfsentience.feature.report.presentation.component

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neteinstein.perfsentience.core.designsystem.behavior.label
import com.neteinstein.perfsentience.core.designsystem.behavior.presentation
import com.neteinstein.perfsentience.core.designsystem.component.AnimatedCheck
import com.neteinstein.perfsentience.core.designsystem.component.AppIcon
import com.neteinstein.perfsentience.core.designsystem.component.MetricTile
import com.neteinstein.perfsentience.core.designsystem.component.ScoreBar
import com.neteinstein.perfsentience.core.designsystem.component.ScoreBars
import com.neteinstein.perfsentience.core.designsystem.component.StatusChip
import com.neteinstein.perfsentience.core.designsystem.component.SurfaceCard
import com.neteinstein.perfsentience.core.designsystem.theme.PerfsentienceTheme
import com.neteinstein.perfsentience.core.model.AppAssessment
import com.neteinstein.perfsentience.core.model.DailyReport
import com.neteinstein.perfsentience.core.model.DeviceIssue
import com.neteinstein.perfsentience.core.model.ReportTime
import com.neteinstein.perfsentience.feature.report.R
import com.neteinstein.perfsentience.feature.report.presentation.describe
import com.neteinstein.perfsentience.feature.report.presentation.describeValue
import com.neteinstein.perfsentience.feature.report.presentation.formatBytes
import com.neteinstein.perfsentience.feature.report.presentation.formatDuration
import com.neteinstein.perfsentience.feature.report.presentation.title
import java.time.format.TextStyle

@Composable
internal fun SetupCard(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier.fillMaxWidth()) {
        BouncingIcon(Icons.Rounded.Shield)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.report_setup_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.report_setup_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.report_setup_action))
        }
    }
}

@Composable
internal fun EmptyReportCard(reportTime: ReportTime, isGenerating: Boolean, onAnalyseNow: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier.fillMaxWidth()) {
        BouncingIcon(Icons.Rounded.Timer)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.report_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(
                R.string.report_empty_desc,
                String.format(LocalConfiguration.current.locales[0], "%02d:%02d", reportTime.hour, reportTime.minute),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        AnalyseButton(isGenerating = isGenerating, onClick = onAnalyseNow)
    }
}

@Composable
private fun BouncingIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow)) }
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.35f), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))),
                RoundedCornerShape(18.dp),
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp)) }
}

@Composable
internal fun AllClearCard(modifier: Modifier = Modifier) {
    SurfaceCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedCheck(color = PerfsentienceTheme.status.good)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.report_all_clear_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.report_all_clear_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun DeviceIssueCard(issue: DeviceIssue, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val color = PerfsentienceTheme.status.forSeverity(issue.severity)
    SurfaceCard(modifier.fillMaxWidth(), contentPadding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(issue.type.title), style = MaterialTheme.typography.titleSmall)
                Text(
                    context.describe(issue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(stringResource(issue.severity.label), color)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppOffenderCard(assessment: AppAssessment, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = PerfsentienceTheme.status
    val worst = status.forSeverity(assessment.worstSeverity)
    SurfaceCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(assessment.packageName)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(assessment.appLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    assessment.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(stringResource(assessment.worstSeverity.label), worst)
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            assessment.findings.forEach { finding ->
                val ui = finding.type.presentation
                StatusChip(stringResource(ui.title), status.forSeverity(finding.severity), icon = ui.icon)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OffenderDetailsSheet(assessment: AppAssessment, onTrust: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val status = PerfsentienceTheme.status
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(assessment.packageName, size = 56)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(assessment.appLabel, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        assessment.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            assessment.findings.forEachIndexed { index, finding ->
                val ui = finding.type.presentation
                val color = status.forSeverity(finding.severity)
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(40.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(ui.icon, contentDescription = null, tint = color) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(ui.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusChip(stringResource(finding.severity.label), color)
                        }
                        Text(context.describeValue(finding), style = MaterialTheme.typography.labelLarge, color = color)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(ui.description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < assessment.findings.lastIndex) HorizontalDivider(Modifier.padding(vertical = 14.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.report_sheet_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", assessment.packageName, null),
                    )
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.report_sheet_open_app_info)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onTrust, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.report_sheet_trust_app))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.report_sheet_trust_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "N trusted apps hidden" – a reminder that the allowlist is filtering this report, linking to Settings. */
@Composable
internal fun TrustedAppsHiddenNote(count: Int, onManage: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onManage, modifier = modifier) {
        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(pluralStringResource(R.plurals.report_trusted_hidden, count, count))
    }
}

@Composable
internal fun VitalsGrid(report: DailyReport, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val metrics = report.metrics
    val scheme = MaterialTheme.colorScheme
    val status = PerfsentienceTheme.status
    val noData = stringResource(R.string.report_metric_not_enough_data)
    val tiles = listOf(
        Triple(
            Icons.Rounded.BatteryStd,
            if (metrics.screenOffDrainPerHour >
                0f
            ) {
                stringResource(R.string.report_metric_per_hour, metrics.screenOffDrainPerHour)
            } else {
                noData
            },
            R.string.report_metric_screen_off_drain,
        ) to scheme.secondary,
        Triple(
            Icons.Rounded.BatteryChargingFull,
            if (metrics.drainPerHour > 0f) stringResource(R.string.report_metric_per_hour, metrics.drainPerHour) else noData,
            R.string.report_metric_drain,
        ) to scheme.primary,
        Triple(
            Icons.Rounded.DeviceThermostat,
            if (metrics.maxTemperatureC > 0f) stringResource(R.string.report_metric_celsius, metrics.maxTemperatureC) else noData,
            R.string.report_metric_temperature,
        ) to status.fair,
        Triple(
            Icons.Rounded.Memory,
            if (metrics.sampleCount > 0) stringResource(R.string.report_metric_percent, metrics.averageMemoryUsedPercent) else noData,
            R.string.report_metric_memory,
        ) to scheme.tertiary,
        Triple(Icons.Rounded.CloudSync, context.formatBytes(metrics.totalBackgroundBytes), R.string.report_metric_background_data) to
            scheme.primary,
        Triple(Icons.Rounded.Verified, context.formatDuration(metrics.totalForegroundTimeMs), R.string.report_metric_screen_time) to
            status.good,
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (data, accent) ->
                    val (icon, value, label) = data
                    MetricTile(icon = icon, value = value, label = stringResource(label), accent = accent, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun HistoryCard(history: List<DailyReport>, modifier: Modifier = Modifier) {
    val status = PerfsentienceTheme.status
    val locale = LocalConfiguration.current.locales[0]
    SurfaceCard(modifier.fillMaxWidth()) {
        ScoreBars(
            bars = history.map {
                ScoreBar(
                    label = it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    score = it.score,
                    color = status.forVerdict(it.verdict),
                )
            },
        )
    }
}
