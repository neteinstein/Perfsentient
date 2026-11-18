package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/** Huge caches slow down storage-bound operations and backups. */
class StorageBloatDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates.mapNotNull { app ->
        val severity = when {
            app.cacheBytes >= thresholds.storageMediumBytes -> Severity.MEDIUM
            app.cacheBytes >= thresholds.storageLowBytes -> Severity.LOW
            else -> return@mapNotNull null
        }
        app.finding(BehaviorType.STORAGE_BLOAT, severity, app.cacheBytes)
    }
}
