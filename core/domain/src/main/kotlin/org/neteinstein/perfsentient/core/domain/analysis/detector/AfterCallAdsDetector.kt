package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.ForegroundSessions
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.analysis.severityFor
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding

/**
 * "After-call" ads: adware listens for the call state going back to idle and immediately
 * launches a full-screen activity (usually using the overlay permission). We see that as a
 * short session of a third-party app directly following the dialer / in-call UI. "Directly"
 * matters: a session that starts long after the call screen closed (e.g. after the screen went
 * off) is ordinary use, not an after-call ad.
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
                    current.startMs - previous.endMs <= thresholds.afterCallMaxGapMs &&
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
