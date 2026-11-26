package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.ReadableData
import org.neteinstein.perfsentient.core.model.SensitivePermission
import org.neteinstein.perfsentient.core.model.Severity

/**
 * "Can read your codes and messages": one-time codes arrive by text and in notifications, so
 * banking malware asks for SMS or notification access.
 *
 * - accessibility + "display over other apps" on top of that (the banking-trojan set): HIGH;
 * - not from a store: HIGH;
 * - a store app that can read texts without being the default SMS app: MEDIUM;
 * - a store app with notification access only: LOW.
 *
 * A store-installed default SMS app, and SMS Toolbox signed by its developer, are not flagged for
 * doing their job. [Finding.value] is a
 * [ReadableData] bit mask.
 */
class DataTheftRiskDetector : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates
        .filter { it.canReadMessages && !input.isVerifiedPartner(it.packageName) }
        .mapNotNull { app ->
            val readsTexts = app.readsTexts()
            val severity = when {
                app.hasEnabledAccessibilityService && app.canDrawOverlays -> Severity.HIGH
                app.isSideloaded -> Severity.HIGH
                readsTexts && !app.isDefaultSmsApp -> Severity.MEDIUM
                app.hasNotificationAccess -> Severity.LOW
                else -> return@mapNotNull null
            }
            var readable = 0L
            if (app.hasNotificationAccess) readable = readable or ReadableData.NOTIFICATIONS
            if (readsTexts || app.isDefaultSmsApp) readable = readable or ReadableData.TEXT_MESSAGES
            app.finding(BehaviorType.DATA_THEFT_RISK, severity, readable)
        }

    private fun AppProfile.readsTexts(): Boolean =
        SensitivePermission.READ_SMS in grantedPermissions || SensitivePermission.RECEIVE_SMS in grantedPermissions
}
