package com.neteinstein.persentient.core.domain.analysis

import com.neteinstein.persentient.core.model.DeviceIssue
import com.neteinstein.persentient.core.model.Finding
import com.neteinstein.persentient.core.model.Verdict

/**
 * 100 = pristine. Each finding subtracts its severity weight, capped per app so that one
 * terrible app cannot zero the score alone; device-level issues subtract their weight too.
 */
class HealthScoreCalculator(
    private val maxPenaltyPerApp: Int = 30,
    private val goodThreshold: Int = 80,
    private val fairThreshold: Int = 55,
) {
    fun score(findings: List<Finding>, deviceIssues: List<DeviceIssue>): Int {
        val appPenalty = findings
            .groupBy { it.packageName }
            .values
            .sumOf { perApp -> perApp.sumOf { it.severity.weight }.coerceAtMost(maxPenaltyPerApp) }
        val devicePenalty = deviceIssues.sumOf { it.severity.weight }
        return (100 - appPenalty - devicePenalty).coerceIn(0, 100)
    }

    fun verdict(score: Int): Verdict = when {
        score >= goodThreshold -> Verdict.GOOD
        score >= fairThreshold -> Verdict.FAIR
        else -> Verdict.POOR
    }
}
