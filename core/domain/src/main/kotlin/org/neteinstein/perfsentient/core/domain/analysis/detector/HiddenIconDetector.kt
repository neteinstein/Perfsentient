package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * Adware frequently hides its launcher icon (e.g. by swapping an activity-alias) so the user
 * cannot find and uninstall it. A user-installed app without a launcher entry that is active
 * in the background is a strong red flag.
 */
class HiddenIconDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates
        .filter { !it.hasLauncherIcon }
        .mapNotNull { app ->
            val pkg = app.packageName
            val activeSignals = listOf(
                input.foregroundServiceMs(pkg) > 0,
                input.backgroundBytes(pkg) >= thresholds.hiddenActivityBytes,
                (input.notifications[pkg] ?: 0) > 0,
                input.events.any { it.packageName == pkg },
            ).count { it }
            val riskyCapabilities = listOf(
                app.canDrawOverlays,
                app.hasEnabledAccessibilityService,
                app.isDeviceAdmin,
            ).count { it }
            when {
                activeSignals == 0 && riskyCapabilities == 0 -> null
                app.isSideloaded || riskyCapabilities > 0 -> app.finding(BehaviorType.HIDDEN_ICON, Severity.HIGH, activeSignals.toLong())
                else -> app.finding(BehaviorType.HIDDEN_ICON, Severity.MEDIUM, activeSignals.toLong())
            }
        }
}
