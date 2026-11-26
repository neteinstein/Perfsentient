package org.neteinstein.perfsentient.core.domain.analysis

import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Evidence
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/** One heuristic that inspects [AnalysisInput] and reports misbehaving apps. */
fun interface BehaviorDetector {
    fun detect(input: AnalysisInput): List<Finding>
}

internal fun AppProfile.finding(type: BehaviorType, severity: Severity, value: Long, evidence: List<Evidence> = emptyList()) = Finding(
    packageName = packageName,
    appLabel = label,
    type = type,
    severity = severity,
    value = value,
    evidence = evidence.sortedByDescending { it.atMs }.take(Finding.MAX_EVIDENCE),
)

/** Evidence pointing at [pkg], labelled from the installed apps when it is one. */
internal fun AnalysisInput.evidence(atMs: Long, pkg: String? = null) =
    Evidence(atMs, pkg, pkg?.let { apps[it]?.label ?: it })

/** Maps a measured value onto a severity using two ascending thresholds. */
internal fun <T : Comparable<T>> severityFor(value: T, medium: T, high: T): Severity? = when {
    value >= high -> Severity.HIGH
    value >= medium -> Severity.MEDIUM
    else -> null
}
