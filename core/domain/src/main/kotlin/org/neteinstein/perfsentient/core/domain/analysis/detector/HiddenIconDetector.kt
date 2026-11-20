package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * Adware frequently hides its launcher icon (e.g. by disabling its launcher activity-alias) so
 * the user cannot find and uninstall it. Many legitimate store apps never had an icon at all
 * (watch/companion plugins, keyboards, wallpapers, TTS engines), so a missing icon alone is not
 * enough: the app must have actively disabled its icon, come from outside a trusted store, or
 * hold a risky capability.
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
            val active = activeSignals > 0
            when {
                riskyCapabilities > 0 -> app.finding(BehaviorType.HIDDEN_ICON, Severity.HIGH, activeSignals.toLong())
                active && app.isSideloaded -> app.finding(BehaviorType.HIDDEN_ICON, Severity.HIGH, activeSignals.toLong())
                active && app.launcherIconDisabled -> app.finding(BehaviorType.HIDDEN_ICON, Severity.MEDIUM, activeSignals.toLong())
                else -> null
            }
        }
}
