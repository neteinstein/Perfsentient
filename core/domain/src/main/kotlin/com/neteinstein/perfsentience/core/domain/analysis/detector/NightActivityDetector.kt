package com.neteinstein.perfsentience.core.domain.analysis.detector

import com.neteinstein.perfsentience.core.domain.analysis.AnalysisInput
import com.neteinstein.perfsentience.core.domain.analysis.BehaviorDetector
import com.neteinstein.perfsentience.core.domain.analysis.DetectionThresholds
import com.neteinstein.perfsentience.core.domain.analysis.finding
import com.neteinstein.perfsentience.core.domain.analysis.severityFor
import com.neteinstein.perfsentience.core.model.BehaviorType
import com.neteinstein.perfsentience.core.model.Finding
import com.neteinstein.perfsentience.core.model.UsageEventType
import java.time.Instant

/**
 * Apps that start services or even launch activities while the screen is off during the
 * night drain the battery and are a common trait of ad-fraud apps (hidden ad rendering).
 */
class NightActivityDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        val counts = mutableMapOf<String, Int>()
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
            app.finding(BehaviorType.NIGHT_ACTIVITY, severity, count.toLong())
        }
    }

    private fun isNight(timestamp: Long, input: AnalysisInput): Boolean {
        val hour = Instant.ofEpochMilli(timestamp).atZone(input.zone).hour
        return hour >= thresholds.nightStartHour && hour < thresholds.nightEndHour
    }
}
