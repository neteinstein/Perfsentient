package org.neteinstein.perfsentient.feature.report.presentation

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.annotation.StringRes
import org.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import org.neteinstein.perfsentient.core.common.MILLIS_IN_MINUTE
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.InstallChannel
import org.neteinstein.perfsentient.core.model.ReadableData
import org.neteinstein.perfsentient.core.model.ScamMessageKind
import org.neteinstein.perfsentient.feature.report.R

internal fun Context.formatDuration(ms: Long): String {
    val hours = (ms / MILLIS_IN_HOUR).toInt()
    val minutes = ((ms % MILLIS_IN_HOUR) / MILLIS_IN_MINUTE).toInt()
    return if (hours > 0) getString(R.string.report_duration_hm, hours, minutes) else getString(R.string.report_duration_m, minutes)
}

internal fun Context.formatBytes(bytes: Long): String = Formatter.formatShortFileSize(this, bytes)

/** Human readable evidence for a finding; [Finding.value] semantics depend on the behaviour. */
internal fun Context.describeValue(finding: Finding): String = when (finding.type) {
    BehaviorType.OUT_OF_CONTEXT_ADS -> quantity(R.plurals.report_value_popups, finding.value.toInt())

    BehaviorType.AFTER_CALL_ADS -> quantity(R.plurals.report_value_after_call, finding.value.toInt())

    BehaviorType.OVERLAY_ABUSE,
    BehaviorType.PERSISTENT_BACKGROUND,
    BehaviorType.UNRESTRICTED_BATTERY,
    -> getString(R.string.report_value_service_time, formatDuration(finding.value))

    BehaviorType.HIDDEN_ICON -> quantity(R.plurals.report_value_signals, finding.value.toInt())

    BehaviorType.NIGHT_ACTIVITY -> quantity(R.plurals.report_value_night, finding.value.toInt())

    BehaviorType.BACKGROUND_DATA -> getString(R.string.report_value_bytes_background, formatBytes(finding.value))

    BehaviorType.NOTIFICATION_SPAM -> quantity(R.plurals.report_value_notifications, finding.value.toInt())

    BehaviorType.STORAGE_BLOAT -> getString(R.string.report_value_cache, formatBytes(finding.value))

    BehaviorType.ACCESSIBILITY_RISK, BehaviorType.DEVICE_ADMIN -> getString(R.string.report_value_capability)

    BehaviorType.RISKY_INSTALL_SOURCE -> when (InstallChannel.entries.getOrNull(finding.value.toInt())) {
        InstallChannel.MESSAGING -> getString(R.string.report_value_install_messaging)
        InstallChannel.FILE -> getString(R.string.report_value_install_file)
        InstallChannel.BROWSER, null -> getString(R.string.report_value_install_browser)
    }

    BehaviorType.DATA_THEFT_RISK -> {
        val notifications = finding.value and ReadableData.NOTIFICATIONS != 0L
        val texts = finding.value and ReadableData.TEXT_MESSAGES != 0L
        getString(
            when {
                notifications && texts -> R.string.report_value_reads_both
                texts -> R.string.report_value_reads_texts
                else -> R.string.report_value_reads_notifications
            },
        )
    }

    BehaviorType.REMOTE_ACCESS -> getString(R.string.report_value_installed_at, formatDateTime(finding.value))

    BehaviorType.IMPERSONATION -> getString(R.string.report_value_impersonation)

    BehaviorType.CAN_INSTALL_APPS -> getString(R.string.report_value_can_install)

    BehaviorType.SCAM_LINK_INSTALL -> getString(R.string.report_value_scam_link_install)
}

@get:StringRes
internal val ScamMessageKind.label: Int
    get() = when (this) {
        ScamMessageKind.SHORTENED_LINK -> R.string.report_scam_kind_shortened_link
        ScamMessageKind.IP_ADDRESS_LINK -> R.string.report_scam_kind_ip_link
        ScamMessageKind.DECEPTIVE_LINK -> R.string.report_scam_kind_deceptive_link
        ScamMessageKind.RISKY_DOMAIN_LINK -> R.string.report_scam_kind_risky_domain
        ScamMessageKind.URGENCY -> R.string.report_scam_kind_urgency
        ScamMessageKind.ACCOUNT_OR_CREDENTIALS -> R.string.report_scam_kind_credentials
        ScamMessageKind.MONEY_OR_PRIZE -> R.string.report_scam_kind_money
        ScamMessageKind.DELIVERY_OR_FEE -> R.string.report_scam_kind_delivery
    }

internal fun Context.formatDateTime(epochMs: Long): String =
    DateUtils.formatDateTime(this, epochMs, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH)

@get:StringRes
internal val DeviceIssueType.title: Int
    get() = when (this) {
        DeviceIssueType.SCREEN_OFF_DRAIN -> R.string.report_issue_screen_off_drain_title
        DeviceIssueType.HIGH_TEMPERATURE -> R.string.report_issue_temperature_title
        DeviceIssueType.THERMAL_THROTTLING -> R.string.report_issue_thermal_title
        DeviceIssueType.MEMORY_PRESSURE -> R.string.report_issue_memory_title
        DeviceIssueType.NO_SCREEN_LOCK -> R.string.report_issue_no_screen_lock_title
        DeviceIssueType.OUTDATED_SECURITY_PATCH -> R.string.report_issue_security_patch_title
        DeviceIssueType.USB_DEBUGGING -> R.string.report_issue_usb_debugging_title
    }

internal fun Context.describe(issue: DeviceIssue): String = when (issue.type) {
    DeviceIssueType.SCREEN_OFF_DRAIN -> getString(R.string.report_issue_screen_off_drain, issue.value / TENTHS)
    DeviceIssueType.HIGH_TEMPERATURE -> getString(R.string.report_issue_temperature, issue.value / TENTHS)
    DeviceIssueType.THERMAL_THROTTLING -> getString(R.string.report_issue_thermal)
    DeviceIssueType.MEMORY_PRESSURE -> quantity(R.plurals.report_issue_memory, issue.value.toInt())
    DeviceIssueType.NO_SCREEN_LOCK -> getString(R.string.report_issue_no_screen_lock)
    DeviceIssueType.OUTDATED_SECURITY_PATCH -> quantity(R.plurals.report_issue_security_patch, issue.value.toInt())
    DeviceIssueType.USB_DEBUGGING -> getString(R.string.report_issue_usb_debugging)
}

private fun Context.quantity(id: Int, count: Int): String = resources.getQuantityString(id, count, count)

private const val TENTHS = 10f
