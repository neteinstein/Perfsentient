package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.analysis.severityFor
import org.neteinstein.perfsentient.core.model.AppIssueType
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding

/**
 * Apps that repeatedly freeze ("App isn't responding") or crash. A single failure happens to
 * every app now and then, so only repeated ones within the day are reported. Android vitals
 * treats ANRs and crashes as core quality metrics; the data comes from the system error log,
 * which is only readable once the optional system-log access was granted with adb.
 */
class AppStabilityDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.appIssues
        .groupingBy { it.packageName to it.type }
        .eachCount()
        .mapNotNull { (key, count) ->
            val (pkg, type) = key
            val app = input.candidate(pkg) ?: return@mapNotNull null
            when (type) {
                AppIssueType.ANR -> severityFor(count, thresholds.anrMediumCount, thresholds.anrHighCount)
                    ?.let { app.finding(BehaviorType.APP_NOT_RESPONDING, it, count.toLong()) }

                AppIssueType.CRASH -> severityFor(count, thresholds.crashMediumCount, thresholds.crashHighCount)
                    ?.let { app.finding(BehaviorType.APP_CRASHES, it, count.toLong()) }
            }
        }
}
