package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * Powerful capabilities that malware abuses: accessibility services (screen reading / auto
 * clicking), device admin (prevents uninstall) and battery-optimisation exemption combined
 * with heavy background work. Risk is escalated for sideloaded apps.
 */
class CapabilityRiskDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates.flatMap { app ->
        buildList {
            if (app.hasEnabledAccessibilityService) {
                val severity = if (app.isSideloaded) Severity.HIGH else Severity.LOW
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
}
