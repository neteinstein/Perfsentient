package org.neteinstein.perfsentient.core.domain.analysis

import org.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import org.neteinstein.perfsentient.core.model.AppBatteryUsage
import org.neteinstein.perfsentient.core.model.BatteryBreakdown
import org.neteinstein.perfsentient.core.model.VitalSample

/**
 * Estimates how much battery each app used.
 *
 * Android keeps per-app battery stats to itself, so we split what we *can* measure: every drop
 * between two consecutive samples taken on battery is shared between what was happening in that
 * interval, in proportion to these weights (see [DetectionThresholds]):
 *
 * - each app's time open on screen × [DetectionThresholds.batteryForegroundWeight];
 * - each app's foreground-service time while not on screen × [DetectionThresholds.batteryServiceWeight];
 * - screen-on time with no app open (home screen, lock screen) × the foreground weight → "screen & system";
 * - screen-off time × [DetectionThresholds.batteryIdleWeight] → "standby".
 *
 * Intervals while charging, with the battery going up, or longer than
 * [DetectionThresholds.maxSampleGapMs] (the phone was off or the sampler was delayed) are skipped.
 */
class BatteryAttributor(private val thresholds: DetectionThresholds) {

    fun attribute(input: AnalysisInput): BatteryBreakdown {
        val samples = input.vitals.sortedBy { it.timestamp }
        if (samples.size < 2) return BatteryBreakdown.EMPTY

        val systemPackages = input.context.launcherPackages + input.context.systemUiPackages
        val foreground = ForegroundSessions
            .build(input.events, input.context.systemUiPackages, thresholds.unlockPopupWindowMs, openUntilMs = input.windowEndMs)
            .groupBy({ it.packageName }, { TimeRange(it.startMs, it.endMs) })
        val serviceOnly = ForegroundServiceIntervals.build(input.events, input.windowStartMs, input.windowEndMs)
            .filterKeys { it !in systemPackages }
            .mapValues { (pkg, ranges) -> ranges.minus(foreground[pkg].orEmpty()) }
        val screenOff = ScreenOffIntervals.build(input.events, input.windowStartMs, input.windowEndMs, samples.first().isScreenOn)

        val totals = Totals()
        samples.zipWithNext().forEach { (a, b) ->
            val gap = b.timestamp - a.timestamp
            if (gap !in 1..thresholds.maxSampleGapMs || a.isCharging || b.isCharging) return@forEach
            totals.measuredMs += gap
            val drop = dropPct(a, b)
            if (drop > 0.0) share(drop, a.timestamp, b.timestamp, foreground, serviceOnly, screenOff, systemPackages, totals)
        }

        val apps = (foreground.keys + serviceOnly.keys)
            .filter { it !in systemPackages }
            .map { pkg ->
                AppBatteryUsage(
                    packageName = pkg,
                    appLabel = input.apps[pkg]?.label ?: pkg,
                    estimatedDrainPct = totals.app[pkg]?.toFloat() ?: 0f,
                    backgroundDrainPct = totals.background[pkg]?.toFloat() ?: 0f,
                    foregroundMs = foreground[pkg].orEmpty().overlapMs(input.windowStartMs, input.windowEndMs),
                    backgroundMs = serviceOnly[pkg].orEmpty().overlapMs(input.windowStartMs, input.windowEndMs),
                )
            }
            .filter { it.estimatedDrainPct > 0f || it.foregroundMs > 0L }
            .sortedWith(compareByDescending<AppBatteryUsage> { it.estimatedDrainPct }.thenByDescending { it.foregroundMs })
            .take(thresholds.batteryTopApps)

        return BatteryBreakdown(
            apps = apps,
            screenAndSystemPct = totals.screenAndSystem.toFloat(),
            standbyPct = totals.standby.toFloat(),
            measuredDrainPct = totals.measured.toFloat(),
            measuredHours = totals.measuredMs.toFloat() / MILLIS_IN_HOUR,
        )
    }

    private fun share(
        drop: Double,
        start: Long,
        end: Long,
        foreground: Map<String, List<TimeRange>>,
        serviceOnly: Map<String, List<TimeRange>>,
        screenOff: List<TimeRange>,
        systemPackages: Set<String>,
        totals: Totals,
    ) {
        val appWeights = HashMap<String, Double>()
        val backgroundWeights = HashMap<String, Double>()
        var sessionMs = 0L
        var systemWeight = 0.0
        foreground.forEach { (pkg, ranges) ->
            val ms = ranges.overlapMs(start, end)
            if (ms <= 0L) return@forEach
            sessionMs += ms
            val weight = ms * thresholds.batteryForegroundWeight.toDouble()
            if (pkg in systemPackages) systemWeight += weight else appWeights.merge(pkg, weight, Double::plus)
        }
        serviceOnly.forEach { (pkg, ranges) ->
            val ms = ranges.overlapMs(start, end)
            if (ms <= 0L) return@forEach
            val weight = ms * thresholds.batteryServiceWeight.toDouble()
            appWeights.merge(pkg, weight, Double::plus)
            backgroundWeights[pkg] = weight
        }
        val offMs = screenOff.overlapMs(start, end)
        val onMs = (end - start) - offMs
        systemWeight += (onMs - sessionMs).coerceAtLeast(0L) * thresholds.batteryForegroundWeight.toDouble()
        val idleWeight = offMs * thresholds.batteryIdleWeight.toDouble()

        val total = appWeights.values.sum() + systemWeight + idleWeight
        totals.measured += drop
        if (total <= 0.0) {
            totals.standby += drop
            return
        }
        appWeights.forEach { (pkg, weight) -> totals.app.merge(pkg, drop * weight / total, Double::plus) }
        backgroundWeights.forEach { (pkg, weight) -> totals.background.merge(pkg, drop * weight / total, Double::plus) }
        totals.screenAndSystem += drop * systemWeight / total
        totals.standby += drop * idleWeight / total
    }

    /**
     * The drop between two samples in % of a full battery. Uses the charge counter when both
     * samples have it (finer than whole percents); otherwise the percentage difference.
     */
    private fun dropPct(a: VitalSample, b: VitalSample): Double {
        val before = a.chargeCounterUah
        val after = b.chargeCounterUah
        if (before != null && after != null && before > 0 && a.batteryPercent > 0) {
            // Full charge ≈ counter / level, so the drop in % is (before − after) × level / before.
            return ((before - after).toDouble() * a.batteryPercent / before).coerceAtLeast(0.0)
        }
        return (a.batteryPercent - b.batteryPercent).coerceAtLeast(0).toDouble()
    }

    private class Totals {
        val app = HashMap<String, Double>()
        val background = HashMap<String, Double>()
        var screenAndSystem = 0.0
        var standby = 0.0
        var measured = 0.0
        var measuredMs = 0L
    }
}
