package org.neteinstein.perfsentient.core.domain.analysis

import org.neteinstein.perfsentient.core.domain.analysis.detector.AfterCallAdsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.AppStabilityDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.BackgroundDataDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.CapabilityRiskDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.HiddenIconDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.NightActivityDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.NotificationSpamDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.OverlayAbuseDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.PersistentBackgroundDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.PopupAdsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.StorageBloatDetector
import org.neteinstein.perfsentient.core.model.Finding

/** Runs every registered [BehaviorDetector] and de-duplicates the result. */
class BehaviorAnalyzer internal constructor(private val detectors: List<BehaviorDetector>) {

    constructor(thresholds: DetectionThresholds) : this(defaultDetectors(thresholds))

    fun analyze(input: AnalysisInput): List<Finding> = detectors
        .flatMap { it.detect(input) }
        .distinctBy { it.packageName to it.type }
        .sortedWith(compareByDescending<Finding> { it.severity }.thenBy { it.appLabel.lowercase() })

    companion object {
        fun defaultDetectors(thresholds: DetectionThresholds): List<BehaviorDetector> = listOf(
            PopupAdsDetector(thresholds),
            AfterCallAdsDetector(thresholds),
            OverlayAbuseDetector(thresholds),
            HiddenIconDetector(thresholds),
            PersistentBackgroundDetector(thresholds),
            NightActivityDetector(thresholds),
            BackgroundDataDetector(thresholds),
            NotificationSpamDetector(thresholds),
            CapabilityRiskDetector(thresholds),
            StorageBloatDetector(thresholds),
            AppStabilityDetector(thresholds),
        )
    }
}
