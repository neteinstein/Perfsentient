package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * "Display over other apps" (SYSTEM_ALERT_WINDOW) is the most abused permission by adware.
 * Android does not expose *when* another app draws an overlay, so we flag apps holding the
 * permission that the user barely opens while they keep running services or using data in
 * the background – i.e. they have both the means and signs of activity behind the user's back.
 */
class OverlayAbuseDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates
        .filter { it.canDrawOverlays }
        .mapNotNull { app ->
            val pkg = app.packageName
            val rarelyUsed = input.foregroundMs(pkg) < thresholds.rarelyUsedForegroundMs
            val busyInBackground = input.foregroundServiceMs(pkg) >= thresholds.overlaySuspiciousServiceMs ||
                input.backgroundBytes(pkg) >= thresholds.overlaySuspiciousBytes
            when {
                !app.hasLauncherIcon -> app.finding(BehaviorType.OVERLAY_ABUSE, Severity.HIGH, input.foregroundServiceMs(pkg))

                rarelyUsed && busyInBackground -> {
                    val severity = if (app.isSideloaded) Severity.HIGH else Severity.MEDIUM
                    app.finding(BehaviorType.OVERLAY_ABUSE, severity, input.foregroundServiceMs(pkg))
                }

                else -> null
            }
        }
}
