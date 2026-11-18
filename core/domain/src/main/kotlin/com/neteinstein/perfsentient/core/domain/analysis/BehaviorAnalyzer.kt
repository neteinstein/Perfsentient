package com.neteinstein.perfsentient.core.domain.analysis

import com.neteinstein.perfsentient.core.domain.analysis.detector.AfterCallAdsDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.BackgroundDataDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.CapabilityRiskDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.HiddenIconDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.NightActivityDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.NotificationSpamDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.OverlayAbuseDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.PersistentBackgroundDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.PopupAdsDetector
import com.neteinstein.perfsentient.core.domain.analysis.detector.StorageBloatDetector
import com.neteinstein.perfsentient.core.model.Finding

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
        )
    }
}
