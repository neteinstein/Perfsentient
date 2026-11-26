package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * Powerful capabilities that malware abuses: accessibility services (screen reading / auto
 * clicking), device admin (prevents uninstall) and battery-optimisation exemption combined
 * with heavy background work. Risk is escalated for sideloaded apps, and for store apps that
 * had accessibility turned on within [DetectionThresholds.freshInstallMs] of being installed.
 */
class CapabilityRiskDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates.flatMap { app ->
        buildList {
            if (app.hasEnabledAccessibilityService) {
                val severity = when {
                    app.isSideloaded -> Severity.HIGH

                    // Play "dropper" apps get accessibility turned on within days of install.
                    app.isFreshInstall(input) -> Severity.MEDIUM

                    else -> Severity.LOW
                }
                add(app.finding(BehaviorType.ACCESSIBILITY_RISK, severity, 1))
            }
            if (app.isDeviceAdmin) {
                val severity = if (app.isSideloaded) Severity.HIGH else Severity.LOW
                add(app.finding(BehaviorType.DEVICE_ADMIN, severity, 1))
            }
            val serviceMs = input.foregroundServiceMs(app.packageName)
            if (app.ignoresBatteryOptimizations &&
                serviceMs >= thresholds.unrestrictedBatteryServiceMs &&
                input.foregroundMs(app.packageName) < thresholds.rarelyUsedForegroundMs
            ) {
                add(app.finding(BehaviorType.UNRESTRICTED_BATTERY, Severity.MEDIUM, serviceMs))
            }
        }
    }

    private fun AppProfile.isFreshInstall(input: AnalysisInput): Boolean =
        firstInstallTime > 0 && input.windowEndMs - firstInstallTime in 0..thresholds.freshInstallMs
}
