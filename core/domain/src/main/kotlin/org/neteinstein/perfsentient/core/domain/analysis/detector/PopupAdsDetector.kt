package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.ForegroundSessions
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.analysis.unsolicitedLaunchSeverity
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding

/**
 * Out-of-context (disruptive) ads: an app briefly takes over the screen while the user is
 * inside *another* app and then the user immediately goes back (A -> X -> A), or an app pops
 * up right after the device is unlocked. Both are explicitly banned by Google Play's
 * Disruptive Ads policy and are the signature of adware families such as IconAds.
 *
 * Resuming the app that was open when the device was locked is not a pop-up (see
 * [ForegroundSessions]), and apps that cannot start activities from the background need many
 * more occurrences before they are reported (see [unsolicitedLaunchSeverity]).
 */
class PopupAdsDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        val ctx = input.context
        val sessions = ForegroundSessions.build(
            events = input.events,
            ignoredPackages = ctx.systemUiPackages,
            unlockWindowMs = thresholds.unlockPopupWindowMs,
        )
        val counts = mutableMapOf<String, Int>()
        val excluded = ctx.launcherPackages + ctx.dialerPackages + ctx.selfPackage

        sessions.forEachIndexed { index, current ->
            if (current.packageName in excluded) return@forEachIndexed
            if (current.durationMs > thresholds.popupMaxDurationMs) return@forEachIndexed
            val previous = sessions.getOrNull(index - 1)
            val next = sessions.getOrNull(index + 1)
            val interruptedAnotherApp = previous != null &&
                next != null &&
                previous.packageName == next.packageName &&
                previous.packageName !in ctx.launcherPackages &&
                previous.endMs == current.startMs
            if (interruptedAnotherApp || current.startedRightAfterUnlock) {
                counts.merge(current.packageName, 1, Int::plus)
            }
        }

        return counts.mapNotNull { (pkg, count) ->
            val app = input.candidate(pkg) ?: return@mapNotNull null
            val severity = app.unsolicitedLaunchSeverity(count, thresholds.popupMediumCount, thresholds.popupHighCount)
                ?: return@mapNotNull null
            app.finding(BehaviorType.OUT_OF_CONTEXT_ADS, severity, count.toLong())
        }
    }
}
