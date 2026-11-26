package org.neteinstein.perfsentient.core.domain.analysis

import org.neteinstein.perfsentient.core.domain.analysis.detector.AfterCallAdsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.AppStabilityDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.BackgroundDataDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.CanInstallAppsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.CapabilityRiskDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.DataTheftRiskDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.HiddenIconDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.ImpersonationDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.NightActivityDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.NotificationSpamDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.OverlayAbuseDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.PersistentBackgroundDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.PopupAdsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.RemoteAccessDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.RiskyInstallSourceDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.StorageBloatDetector
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.Finding

/** Runs every registered [BehaviorDetector] and de-duplicates the result. */
class BehaviorAnalyzer internal constructor(private val detectors: List<BehaviorDetector>) {

    constructor(
        thresholds: DetectionThresholds,
        catalog: ScamSignalCatalog = ScamSignalCatalog(),
    ) : this(defaultDetectors(thresholds, catalog))

    fun analyze(input: AnalysisInput): List<Finding> = detectors
        .flatMap { it.detect(input) }
        .distinctBy { it.packageName to it.type }
        .sortedWith(compareByDescending<Finding> { it.severity }.thenBy { it.appLabel.lowercase() })

    companion object {
        /** Only the detectors that judge an app by what it is and can do, for the live security check. */
        fun security(
            thresholds: DetectionThresholds,
            catalog: ScamSignalCatalog = ScamSignalCatalog(),
        ): BehaviorAnalyzer = BehaviorAnalyzer(
            listOf(
                RiskyInstallSourceDetector(catalog),
                DataTheftRiskDetector(),
                RemoteAccessDetector(thresholds, catalog),
                ImpersonationDetector(catalog),
                CanInstallAppsDetector(catalog),
                CapabilityRiskDetector(thresholds),
                HiddenIconDetector(thresholds),
            ),
        )

        fun defaultDetectors(
            thresholds: DetectionThresholds,
            catalog: ScamSignalCatalog = ScamSignalCatalog(),
        ): List<BehaviorDetector> = listOf(
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
            RiskyInstallSourceDetector(catalog),
            DataTheftRiskDetector(),
            RemoteAccessDetector(thresholds, catalog),
            ImpersonationDetector(catalog),
            CanInstallAppsDetector(catalog),
            AppStabilityDetector(thresholds),
        )
    }
}
