package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.ForegroundSessions
import org.neteinstein.perfsentient.core.domain.analysis.evidence
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * "Your account is at risk, install this app so I can help": bank-impersonation scams talk the
 * victim into installing a remote-access app (AnyDesk, TeamViewer QuickSupport…) and then watch
 * the screen while one-time codes arrive. Android 16 blocks sideloading during calls for the
 * same reason.
 *
 * A remote-access app installed in the last [DetectionThresholds.freshInstallMs] is MEDIUM; HIGH
 * when it was installed during a phone call or within [DetectionThresholds.remoteAccessAfterCallMs]
 * after one, within [DetectionThresholds.scamTextToInstallWindowMs] after a scam text flagged by SMS
 * Toolbox, or when it has accessibility turned on. [Finding.value] is the install time (epoch ms).
 */
class RemoteAccessDetector(
    private val thresholds: DetectionThresholds,
    private val catalog: ScamSignalCatalog,
) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        val fresh = input.candidates.filter { app ->
            catalog.isRemoteAccessApp(app.packageName) &&
                app.firstInstallTime > 0 &&
                input.windowEndMs - app.firstInstallTime in 0..thresholds.freshInstallMs
        }
        if (fresh.isEmpty()) return emptyList()
        val calls = ForegroundSessions
            .build(input.events, input.context.systemUiPackages, thresholds.unlockPopupWindowMs)
            .filter { it.packageName in input.context.dialerPackages }
        return fresh.map { app ->
            val installedAt = app.firstInstallTime
            val call = calls.firstOrNull { installedAt in it.startMs..(it.endMs + thresholds.remoteAccessAfterCallMs) }
            val afterScamText = input.scamTextBefore(installedAt, thresholds.scamTextToInstallWindowMs) != null
            val severity = if (call != null || afterScamText || app.hasEnabledAccessibilityService) Severity.HIGH else Severity.MEDIUM
            // The related app is the dialer when the install followed a call.
            app.finding(BehaviorType.REMOTE_ACCESS, severity, installedAt, listOf(input.evidence(installedAt, call?.packageName)))
        }
    }
}
