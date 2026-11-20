package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.analysis.severityFor
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding

/**
 * Excessive background traffic. Mobile data uses the 50 MB / 200 MB per day limits; unmetered
 * Wi-Fi traffic (e.g. a social app prefetching videos while charging at home) costs the user far
 * less, so it is only reported above much larger limits.
 */
class BackgroundDataDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.network.values.mapNotNull { usage ->
        val app = input.candidate(usage.packageName) ?: return@mapNotNull null
        val mobile = severityFor(usage.backgroundMobileBytes, thresholds.backgroundDataMediumBytes, thresholds.backgroundDataHighBytes)
        val wifi = severityFor(usage.backgroundWifiBytes, thresholds.backgroundWifiDataMediumBytes, thresholds.backgroundWifiDataHighBytes)
        val severity = listOfNotNull(mobile, wifi).maxOrNull() ?: return@mapNotNull null
        app.finding(BehaviorType.BACKGROUND_DATA, severity, usage.backgroundBytes)
    }
}
