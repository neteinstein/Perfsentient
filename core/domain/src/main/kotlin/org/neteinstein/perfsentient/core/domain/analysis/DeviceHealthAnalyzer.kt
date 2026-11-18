package org.neteinstein.perfsentient.core.domain.analysis

import kotlin.math.roundToInt
import kotlin.math.roundToLong
import org.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.DeviceMetrics
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.VitalSample

/** Computes device-wide metrics from periodic vital samples and flags device-level issues. */
class DeviceHealthAnalyzer(private val thresholds: DetectionThresholds) {

    fun metrics(input: AnalysisInput): DeviceMetrics {
        val samples = input.vitals.sortedBy { it.timestamp }
        val drain = drainStats(samples) { _, _ -> true }
        val screenOffDrain = drainStats(samples) { a, b -> !a.isScreenOn && !b.isScreenOn }
        val memoryUsed = samples
            .filter { it.totalMemoryBytes > 0 }
            .map { 100.0 * (it.totalMemoryBytes - it.availableMemoryBytes) / it.totalMemoryBytes }

        return DeviceMetrics(
            sampleCount = samples.size,
            drainPerHour = drain.ratePerHour,
            screenOffDrainPerHour = if (screenOffDrain.hours >= thresholds.minScreenOffHoursForDrain) screenOffDrain.ratePerHour else 0f,
            maxTemperatureC = samples.maxOfOrNull { it.batteryTemperatureC } ?: 0f,
            worstThermalStatus = samples.maxOfOrNull { it.thermalStatus } ?: 0,
            averageMemoryUsedPercent = if (memoryUsed.isEmpty()) 0 else memoryUsed.average().roundToInt(),
            lowMemorySamples = samples.count { it.isLowMemory },
            totalBackgroundBytes = input.network.values.sumOf { it.backgroundBytes },
            totalForegroundTimeMs = input.usage.values.sumOf { it.foregroundTimeMs },
        )
    }

    fun issues(metrics: DeviceMetrics): List<DeviceIssue> = buildList {
        severityFor(metrics.screenOffDrainPerHour, thresholds.screenOffDrainMedium, thresholds.screenOffDrainHigh)?.let {
            add(DeviceIssue(DeviceIssueType.SCREEN_OFF_DRAIN, it, (metrics.screenOffDrainPerHour * 10).roundToLong()))
        }
        severityFor(metrics.maxTemperatureC, thresholds.temperatureMediumC, thresholds.temperatureHighC)?.let {
            add(DeviceIssue(DeviceIssueType.HIGH_TEMPERATURE, it, (metrics.maxTemperatureC * 10).roundToLong()))
        }
        severityFor(metrics.worstThermalStatus, thresholds.thermalMedium, thresholds.thermalHigh)?.let {
            add(DeviceIssue(DeviceIssueType.THERMAL_THROTTLING, it, metrics.worstThermalStatus.toLong()))
        }
        if (metrics.sampleCount > 0 && metrics.lowMemorySamples.toFloat() / metrics.sampleCount >= thresholds.lowMemoryRatio) {
            add(DeviceIssue(DeviceIssueType.MEMORY_PRESSURE, Severity.MEDIUM, metrics.lowMemorySamples.toLong()))
        }
    }

    private data class DrainStats(val percent: Int, val hours: Float) {
        val ratePerHour: Float get() = if (hours <= 0f) 0f else percent / hours
    }

    private inline fun drainStats(samples: List<VitalSample>, include: (VitalSample, VitalSample) -> Boolean): DrainStats {
        var percent = 0
        var millis = 0L
        samples.zipWithNext().forEach { (a, b) ->
            val gap = b.timestamp - a.timestamp
            val discharging = !a.isCharging && !b.isCharging && b.batteryPercent <= a.batteryPercent
            if (gap in 1..thresholds.maxSampleGapMs && discharging && include(a, b)) {
                percent += a.batteryPercent - b.batteryPercent
                millis += gap
            }
        }
        return DrainStats(percent, millis.toFloat() / MILLIS_IN_HOUR)
    }
}
