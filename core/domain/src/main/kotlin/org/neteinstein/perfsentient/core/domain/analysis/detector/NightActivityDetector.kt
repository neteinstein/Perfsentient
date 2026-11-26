package org.neteinstein.perfsentient.core.domain.analysis.detector

import java.time.Instant
import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.analysis.severityFor
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Evidence
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.UsageEventType

/**
 * Apps that start services or even launch activities while the screen is off during the
 * night drain the battery and are a common trait of ad-fraud apps (hidden ad rendering).
 */
class NightActivityDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        val counts = mutableMapOf<String, Int>()
        val times = mutableMapOf<String, MutableList<Evidence>>()
        var screenOn = true
        for (event in input.events.sortedBy { it.timestamp }) {
            when (event.type) {
                UsageEventType.SCREEN_INTERACTIVE -> screenOn = true

                UsageEventType.SCREEN_NON_INTERACTIVE -> screenOn = false

                UsageEventType.ACTIVITY_RESUMED, UsageEventType.FOREGROUND_SERVICE_START -> {
                    if (!screenOn && isNight(event.timestamp, input)) {
                        // An activity launched with the screen off is worse than a service.
                        val weight = if (event.type == UsageEventType.ACTIVITY_RESUMED) 2 else 1
                        counts.merge(event.packageName, weight, Int::plus)
                        times.getOrPut(event.packageName) { mutableListOf() } += Evidence(event.timestamp)
                    }
                }

                else -> Unit
            }
        }
        val excluded = input.context.dialerPackages
        return counts.mapNotNull { (pkg, count) ->
            if (pkg in excluded) return@mapNotNull null
            val app = input.candidate(pkg) ?: return@mapNotNull null
            val severity = severityFor(count, thresholds.nightMediumCount, thresholds.nightHighCount)
                ?: return@mapNotNull null
            app.finding(BehaviorType.NIGHT_ACTIVITY, severity, count.toLong(), times[pkg].orEmpty())
        }
    }

    private fun isNight(timestamp: Long, input: AnalysisInput): Boolean {
        val hour = Instant.ofEpochMilli(timestamp).atZone(input.zone).hour
        return hour >= thresholds.nightStartHour && hour < thresholds.nightEndHour
    }
}
