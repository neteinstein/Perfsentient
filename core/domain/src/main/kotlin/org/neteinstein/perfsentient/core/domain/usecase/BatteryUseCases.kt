package org.neteinstein.perfsentient.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import org.neteinstein.perfsentient.core.common.TimeProvider
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.repository.ReportRepository
import org.neteinstein.perfsentient.core.model.AppBatteryHistory
import org.neteinstein.perfsentient.core.model.AppBatteryUsage
import org.neteinstein.perfsentient.core.model.BatteryHistory
import org.neteinstein.perfsentient.core.model.DailyBatteryPoint

/**
 * Estimated battery use vs time open for each app over the last [days] days (today included),
 * summed from the battery breakdown of each daily report. Days without a report count as zero.
 * Trusted apps are kept: this is information, not a finding.
 */
class ObserveBatteryHistoryUseCase(
    private val reportRepository: ReportRepository,
    private val timeProvider: TimeProvider,
    private val thresholds: DetectionThresholds,
) {
    operator fun invoke(days: Int = DEFAULT_DAYS): Flow<BatteryHistory> = reportRepository.observeHistory(days).map { reports ->
        val today = timeProvider.today()
        val window = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val byDay = reports.filter { it.date in window }.associateBy { it.date }
        val breakdowns = window.mapNotNull { byDay[it]?.battery }
        if (breakdowns.all { it.apps.isEmpty() }) return@map BatteryHistory.EMPTY

        val usageByApp: Map<String, List<Pair<LocalDate, AppBatteryUsage>>> = window
            .flatMap { day -> byDay[day]?.battery?.apps.orEmpty().map { day to it } }
            .groupBy { it.second.packageName }

        val apps = usageByApp.map { (pkg, entries) ->
            val perDay = entries.associate { it.first to it.second }
            val drain = entries.sumOf { it.second.estimatedDrainPct.toDouble() }.toFloat()
            val background = entries.sumOf { it.second.backgroundDrainPct.toDouble() }.toFloat()
            val foreground = entries.sumOf { it.second.foregroundMs }
            AppBatteryHistory(
                packageName = pkg,
                appLabel = entries.last().second.appLabel,
                drainPct = drain,
                backgroundDrainPct = background,
                foregroundMs = foreground,
                backgroundMs = entries.sumOf { it.second.backgroundMs },
                drainPerHourOpen = if (foreground >= thresholds.batteryMinOpenMsForRate) {
                    (drain - background).coerceAtLeast(0f) * MILLIS_IN_HOUR / foreground
                } else {
                    null
                },
                mostlyBackground = drain > 0f && background / drain >= thresholds.batteryMostlyBackgroundRatio,
                daily = window.map { day ->
                    val usage = perDay[day]
                    DailyBatteryPoint(day, usage?.estimatedDrainPct ?: 0f, usage?.foregroundMs ?: 0L)
                },
            )
        }.sortedWith(compareByDescending<AppBatteryHistory> { it.drainPct }.thenByDescending { it.foregroundMs })

        BatteryHistory(
            days = window,
            apps = apps,
            screenAndSystemPct = breakdowns.sumOf { it.screenAndSystemPct.toDouble() }.toFloat(),
            standbyPct = breakdowns.sumOf { it.standbyPct.toDouble() }.toFloat(),
            measuredDrainPct = breakdowns.sumOf { it.measuredDrainPct.toDouble() }.toFloat(),
            measuredHours = breakdowns.sumOf { it.measuredHours.toDouble() }.toFloat(),
        )
    }

    private companion object {
        const val DEFAULT_DAYS = 7
    }
}
