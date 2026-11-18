package com.neteinstein.perfsentient.core.domain.analysis

import com.neteinstein.perfsentient.core.model.DailyReport

/**
 * Removes the findings of apps the user trusts and re-scores the day.
 *
 * Reports are stored with every finding and filtered when read. That way trusting an app, or
 * changing your mind, updates today's report and the 7-day trend at once, without a new analysis.
 */
class TrustedAppsFilter(private val scoreCalculator: HealthScoreCalculator) {
    fun apply(report: DailyReport, trustedPackages: Set<String>): DailyReport {
        val (hidden, kept) = report.findings.partition { it.packageName in trustedPackages }
        if (hidden.isEmpty()) return report
        val score = scoreCalculator.score(kept, report.deviceIssues)
        return report.copy(
            score = score,
            verdict = scoreCalculator.verdict(score),
            findings = kept,
            hiddenTrustedApps = hidden.distinctBy { it.packageName }.size,
        )
    }
}
