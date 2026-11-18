package com.neteinstein.perfsentience.core.domain.analysis.detector

import com.neteinstein.perfsentience.core.domain.analysis.AnalysisInput
import com.neteinstein.perfsentience.core.domain.analysis.BehaviorDetector
import com.neteinstein.perfsentience.core.domain.analysis.DetectionThresholds
import com.neteinstein.perfsentience.core.domain.analysis.finding
import com.neteinstein.perfsentience.core.domain.analysis.severityFor
import com.neteinstein.perfsentience.core.model.BehaviorType
import com.neteinstein.perfsentience.core.model.Finding

/** Android vitals considers > 50 MB/day of background traffic as excessive. */
class BackgroundDataDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.network.values.mapNotNull { usage ->
        val app = input.candidate(usage.packageName) ?: return@mapNotNull null
        val severity = severityFor(usage.backgroundBytes, thresholds.backgroundDataMediumBytes, thresholds.backgroundDataHighBytes)
            ?: return@mapNotNull null
        app.finding(BehaviorType.BACKGROUND_DATA, severity, usage.backgroundBytes)
    }
}
