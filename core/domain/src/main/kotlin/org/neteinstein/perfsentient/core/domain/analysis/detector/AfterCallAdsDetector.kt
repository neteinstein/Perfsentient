package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.ForegroundSessions
import org.neteinstein.perfsentient.core.domain.analysis.evidence
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.analysis.unsolicitedLaunchSeverity
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding

/**
 * "After-call" ads: adware listens for the call state going back to idle and immediately
 * launches a full-screen activity (usually using the overlay permission). We see that as a
 * short session of a third-party app directly following the dialer / in-call UI. "Directly"
 * matters: a session that starts long after the call screen closed (e.g. after the screen went
 * off) is ordinary use, not an after-call ad.
 *
 * Going back to the app that was on screen when the call came in is not counted, and a single
 * occurrence is not enough: users routinely open an app right after hanging up.
 */
class AfterCallAdsDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        val ctx = input.context
        if (ctx.dialerPackages.isEmpty()) return emptyList()
        val sessions = ForegroundSessions.build(input.events, ctx.systemUiPackages, thresholds.unlockPopupWindowMs)
        val excluded = ctx.launcherPackages + ctx.dialerPackages + ctx.selfPackage

        val afterCalls = sessions.indices
            .filter { index ->
                val previous = sessions.getOrNull(index - 1) ?: return@filter false
                val current = sessions[index]
                // The call interrupted this app and the user simply went back to it.
                val returnedToInterruptedApp = sessions.getOrNull(index - 2)?.packageName == current.packageName
                previous.packageName in ctx.dialerPackages &&
                    current.startMs - previous.endMs <= thresholds.afterCallMaxGapMs &&
                    current.packageName !in excluded &&
                    !returnedToInterruptedApp &&
                    current.durationMs <= thresholds.afterCallMaxDurationMs
            }
            .groupBy({ sessions[it].packageName }, { input.evidence(sessions[it].startMs, sessions[it - 1].packageName) })

        return afterCalls.mapNotNull { (pkg, evidence) ->
            val app = input.candidate(pkg) ?: return@mapNotNull null
            val count = evidence.size
            val severity = app.unsolicitedLaunchSeverity(count, thresholds.afterCallMediumCount, thresholds.afterCallHighCount)
                ?: return@mapNotNull null
            app.finding(BehaviorType.AFTER_CALL_ADS, severity, count.toLong(), evidence)
        }
    }
}
