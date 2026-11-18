package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.analysis.severityFor
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding

/** Android vitals considers > 50 MB/day of background traffic as excessive. */
class BackgroundDataDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.network.values.mapNotNull { usage ->
        val app = input.candidate(usage.packageName) ?: return@mapNotNull null
        val severity = severityFor(usage.backgroundBytes, thresholds.backgroundDataMediumBytes, thresholds.backgroundDataHighBytes)
            ?: return@mapNotNull null
        app.finding(BehaviorType.BACKGROUND_DATA, severity, usage.backgroundBytes)
    }
}
