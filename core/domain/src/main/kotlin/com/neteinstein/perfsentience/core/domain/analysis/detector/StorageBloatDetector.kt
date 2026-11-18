package com.neteinstein.perfsentience.core.domain.analysis.detector

import com.neteinstein.perfsentience.core.domain.analysis.AnalysisInput
import com.neteinstein.perfsentience.core.domain.analysis.BehaviorDetector
import com.neteinstein.perfsentience.core.domain.analysis.DetectionThresholds
import com.neteinstein.perfsentience.core.domain.analysis.finding
import com.neteinstein.perfsentience.core.model.BehaviorType
import com.neteinstein.perfsentience.core.model.Finding
import com.neteinstein.perfsentience.core.model.Severity

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
