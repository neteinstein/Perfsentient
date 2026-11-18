package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.analysis.severityFor
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding

/**
 * Long-running foreground services keep the CPU awake (Android vitals flags 2 h/day of
 * background wake locks as excessive). Flag apps that run services for hours while the user
 * hardly opens them. Media/navigation apps that the user actively uses are not flagged.
 */
class PersistentBackgroundDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates.mapNotNull { app ->
        val pkg = app.packageName
        if (input.foregroundMs(pkg) >= thresholds.rarelyUsedForegroundMs) return@mapNotNull null
        val serviceMs = input.foregroundServiceMs(pkg)
        val severity = severityFor(serviceMs, thresholds.persistentBackgroundMediumMs, thresholds.persistentBackgroundHighMs)
            ?: return@mapNotNull null
        app.finding(BehaviorType.PERSISTENT_BACKGROUND, severity, serviceMs)
    }
}
