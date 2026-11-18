package com.neteinstein.perfsentient.core.domain.analysis.detector

import com.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import com.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import com.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import com.neteinstein.perfsentient.core.domain.analysis.finding
import com.neteinstein.perfsentient.core.model.BehaviorType
import com.neteinstein.perfsentient.core.model.Finding
import com.neteinstein.perfsentient.core.model.Severity

/**
 * Push-notification spam (often ads disguised as alerts). Messaging apps the user actively
 * uses are tolerated unless the volume is extreme.
 */
class NotificationSpamDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.notifications.mapNotNull { (pkg, count) ->
        val app = input.candidate(pkg) ?: return@mapNotNull null
        val rarelyUsed = input.foregroundMs(pkg) < thresholds.rarelyUsedForegroundMs
        val severity = when {
            rarelyUsed && count >= thresholds.notificationHighCount -> Severity.HIGH
            rarelyUsed && count >= thresholds.notificationMediumCount -> Severity.MEDIUM
            count >= thresholds.notificationAbsoluteCount -> Severity.MEDIUM
            else -> return@mapNotNull null
        }
        app.finding(BehaviorType.NOTIFICATION_SPAM, severity, count.toLong())
    }
}
