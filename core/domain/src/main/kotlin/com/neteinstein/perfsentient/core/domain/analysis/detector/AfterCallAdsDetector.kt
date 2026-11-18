package com.neteinstein.perfsentient.core.domain.analysis.detector

import com.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import com.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import com.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import com.neteinstein.perfsentient.core.domain.analysis.ForegroundSessions
import com.neteinstein.perfsentient.core.domain.analysis.finding
import com.neteinstein.perfsentient.core.domain.analysis.severityFor
import com.neteinstein.perfsentient.core.model.BehaviorType
import com.neteinstein.perfsentient.core.model.Finding

/**
 * "After-call" ads: adware listens for the call state going back to idle and immediately
 * launches a full-screen activity (usually using the overlay permission). We see that as a
 * short session of a third-party app directly following the dialer / in-call UI.
 */
class AfterCallAdsDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        val ctx = input.context
        if (ctx.dialerPackages.isEmpty()) return emptyList()
        val sessions = ForegroundSessions.build(input.events, ctx.systemUiPackages, thresholds.unlockPopupWindowMs)
        val excluded = ctx.launcherPackages + ctx.dialerPackages + ctx.selfPackage

        val counts = sessions.zipWithNext()
            .filter { (previous, current) ->
                previous.packageName in ctx.dialerPackages &&
                    current.packageName !in excluded &&
                    current.durationMs <= thresholds.afterCallMaxDurationMs
            }
            .groupingBy { it.second.packageName }
            .eachCount()

        return counts.mapNotNull { (pkg, count) ->
            val app = input.candidate(pkg) ?: return@mapNotNull null
            val severity = severityFor(count, thresholds.afterCallMediumCount, thresholds.afterCallHighCount)
                ?: return@mapNotNull null
            app.finding(BehaviorType.AFTER_CALL_ADS, severity, count.toLong())
        }
    }
}
