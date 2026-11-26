package org.neteinstein.perfsentient.core.domain.analysis

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.DeviceSecurityPosture
import org.neteinstein.perfsentient.core.model.Severity

/**
 * Device settings that make scams and theft easier: no screen lock (MEDIUM), an old security patch
 * (LOW after [DetectionThresholds.patchLowDays], MEDIUM after [DetectionThresholds.patchMediumDays])
 * and USB debugging left on (LOW).
 */
class DeviceSecurityAnalyzer(private val thresholds: DetectionThresholds) {
    fun issues(posture: DeviceSecurityPosture, today: LocalDate): List<DeviceIssue> = buildList {
        if (!posture.hasSecureLock) add(DeviceIssue(DeviceIssueType.NO_SCREEN_LOCK, Severity.MEDIUM, 1))
        posture.securityPatch?.let { patch ->
            val days = ChronoUnit.DAYS.between(patch, today)
            severityFor(days, thresholds.patchLowDays, thresholds.patchMediumDays)?.let { severity ->
                // severityFor maps the two thresholds to MEDIUM/HIGH; an old patch is one level lower.
                val level = if (severity == Severity.HIGH) Severity.MEDIUM else Severity.LOW
                add(DeviceIssue(DeviceIssueType.OUTDATED_SECURITY_PATCH, level, days))
            }
        }
        if (posture.adbEnabled) add(DeviceIssue(DeviceIssueType.USB_DEBUGGING, Severity.LOW, 1))
    }
}
