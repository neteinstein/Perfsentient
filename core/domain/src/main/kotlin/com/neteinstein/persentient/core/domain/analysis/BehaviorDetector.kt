package com.neteinstein.persentient.core.domain.analysis

import com.neteinstein.persentient.core.model.AppProfile
import com.neteinstein.persentient.core.model.BehaviorType
import com.neteinstein.persentient.core.model.Finding
import com.neteinstein.persentient.core.model.Severity

/** One heuristic that inspects [AnalysisInput] and reports misbehaving apps. */
fun interface BehaviorDetector {
    fun detect(input: AnalysisInput): List<Finding>
}

internal fun AppProfile.finding(type: BehaviorType, severity: Severity, value: Long) = Finding(
    packageName = packageName,
    appLabel = label,
    type = type,
    severity = severity,
    value = value,
)

/** Maps a measured value onto a severity using two ascending thresholds. */
internal fun <T : Comparable<T>> severityFor(value: T, medium: T, high: T): Severity? = when {
    value >= high -> Severity.HIGH
    value >= medium -> Severity.MEDIUM
    else -> null
}
