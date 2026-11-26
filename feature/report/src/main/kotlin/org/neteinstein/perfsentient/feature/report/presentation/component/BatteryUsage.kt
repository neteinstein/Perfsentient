package org.neteinstein.perfsentient.feature.report.presentation.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.Locale
import org.neteinstein.perfsentient.core.designsystem.component.AppIcon
import org.neteinstein.perfsentient.core.designsystem.component.StatusChip
import org.neteinstein.perfsentient.core.designsystem.component.SurfaceCard
import org.neteinstein.perfsentient.core.designsystem.theme.PerfsentienceTheme
import org.neteinstein.perfsentient.core.model.AppBatteryHistory
import org.neteinstein.perfsentient.core.model.BatteryHistory
import org.neteinstein.perfsentient.core.model.BatterySort
import org.neteinstein.perfsentient.feature.report.R
import org.neteinstein.perfsentient.feature.report.presentation.formatDuration

/** Apps shown before "Show all". */
private const val COLLAPSED_APPS = 8

/** Sort toggle for the battery section. */
@Composable
internal fun BatterySortToggle(sort: BatterySort, onSortChange: (BatterySort) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        BatterySort.MOST_BATTERY to R.string.report_battery_sort_most,
        BatterySort.PER_HOUR_OPEN to R.string.report_battery_sort_per_hour,
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = sort == option,
                onClick = { onSortChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/** Battery vs time open for the last 7 days: one row per app, tap a row for its daily chart. */
@Composable
internal fun BatteryUsageCard(history: BatteryHistory, modifier: Modifier = Modifier) {
    if (history.isEmpty) {
        BatteryEmptyCard(modifier)
        return
    }
    var expandedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val maxDrain = history.apps.maxOf { it.drainPct }.coerceAtLeast(MIN_SCALE)
    val maxOpen = history.apps.maxOf { it.foregroundMs }.coerceAtLeast(1L)
    val visible = if (showAll) history.apps else history.apps.take(COLLAPSED_APPS)

    SurfaceCard(modifier.fillMaxWidth().animateContentSize(), contentPadding = 8) {
        visible.forEachIndexed { index, app ->
            BatteryAppRow(
                app = app,
                batteryFraction = app.drainPct / maxDrain,
                openFraction = app.foregroundMs.toFloat() / maxOpen,
                expanded = expandedPackage == app.packageName,
                onClick = { expandedPackage = if (expandedPackage == app.packageName) null else app.packageName },
            )
            if (index < visible.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 12.dp))
        }
        if (history.apps.size > COLLAPSED_APPS) {
            TextButton(onClick = { showAll = !showAll }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    if (showAll) {
                        stringResource(R.string.report_battery_show_less)
                    } else {
                        pluralStringResource(R.plurals.report_battery_show_all, history.apps.size, history.apps.size)
                    },
                )
            }
        }
    }
}

/** How the estimate is made, and what went to the home screen and standby. */
@Composable
internal fun BatteryFootnote(history: BatteryHistory, modifier: Modifier = Modifier) {
    if (history.isEmpty) return
    val context = LocalContext.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(
                R.string.report_battery_footnote,
                context.formatPercent(history.screenAndSystemPct),
                context.formatPercent(history.standbyPct),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BatteryEmptyCard(modifier: Modifier) {
    SurfaceCard(modifier.fillMaxWidth(), contentPadding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.BatteryStd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.report_battery_empty_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.report_battery_empty_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BatteryAppRow(
    app: AppBatteryHistory,
    batteryFraction: Float,
    openFraction: Float,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val batteryColor = MaterialTheme.colorScheme.primary
    val openColor = MaterialTheme.colorScheme.tertiary
    val battery = context.formatPercent(app.drainPct)
    val open = context.formatDuration(app.foregroundMs)
    val summary = buildString {
        append(stringResource(R.string.report_battery_usage, battery, open))
        app.drainPerHourOpen?.let { append(" · ").append(stringResource(R.string.report_battery_rate, context.formatPercent(it))) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.packageName, size = 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.appLabel, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        UsageBar(
            fraction = batteryFraction,
            color = batteryColor,
            icon = Icons.Rounded.BatteryAlert,
            description = stringResource(R.string.report_battery_bar_battery, battery),
        )
        Spacer(Modifier.height(6.dp))
        UsageBar(
            fraction = openFraction,
            color = openColor,
            icon = Icons.Rounded.Timer,
            description = stringResource(R.string.report_battery_bar_open, open),
        )
        if (app.mostlyBackground) {
            Spacer(Modifier.height(8.dp))
            StatusChip(
                stringResource(R.string.report_battery_mostly_background),
                PerfsentienceTheme.status.fair,
                icon = Icons.Rounded.NightsStay,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 12.dp)) {
                DailyBatteryChart(app, batteryColor, openColor)
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = { context.openAppInfo(app.packageName) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.report_sheet_open_app_info))
                }
            }
        }
    }
}

@Composable
private fun UsageBar(fraction: Float, color: Color, icon: ImageVector, description: String) {
    Row(Modifier.semantics(mergeDescendants = true) { contentDescription = description }, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}

/** Two thin bars per day: battery and time open, each scaled to the app's busiest day. */
@Composable
private fun DailyBatteryChart(app: AppBatteryHistory, batteryColor: Color, openColor: Color) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val maxDrain = app.daily.maxOf { it.drainPct }.coerceAtLeast(MIN_SCALE)
    val maxOpen = app.daily.maxOf { it.foregroundMs }.coerceAtLeast(1L)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            app.daily.forEach { point ->
                val day = point.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                val description = stringResource(
                    R.string.report_battery_day,
                    day,
                    context.formatPercent(point.drainPct),
                    context.formatDuration(point.foregroundMs),
                )
                Column(
                    Modifier.semantics(mergeDescendants = true) { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Canvas(Modifier.width(20.dp).height(64.dp)) {
                        val barWidth = size.width / 2 - 1.dp.toPx()
                        val radius = CornerRadius(barWidth / 2, barWidth / 2)
                        listOf(
                            0f to (point.drainPct / maxDrain) to batteryColor,
                            (size.width - barWidth) to (point.foregroundMs.toFloat() / maxOpen) to openColor,
                        ).forEach { (position, color) ->
                            val (x, fraction) = position
                            drawRoundRect(track, topLeft = Offset(x, 0f), size = Size(barWidth, size.height), cornerRadius = radius)
                            val h = size.height * fraction.coerceIn(0f, 1f)
                            drawRoundRect(color, topLeft = Offset(x, size.height - h), size = Size(barWidth, h), cornerRadius = radius)
                        }
                    }
                    Text(
                        point.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
            LegendDot(batteryColor, stringResource(R.string.report_battery_legend_battery))
            LegendDot(openColor, stringResource(R.string.report_battery_legend_open))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "18 %" (no decimals from 10 up, one decimal below), in the user's number format. */
internal fun Context.formatPercent(value: Float): String {
    val locale: Locale = resources.configuration.locales[0]
    val format = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = if (value >= DECIMALS_BELOW) 0 else 1
        minimumFractionDigits = 0
    }
    return getString(R.string.report_battery_percent, format.format(value))
}

internal fun Context.openAppInfo(packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

private const val MIN_SCALE = 0.1f
private const val DECIMALS_BELOW = 10f
