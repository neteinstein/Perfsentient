package com.neteinstein.perfsentient.feature.report.presentation

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.StringRes
import com.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import com.neteinstein.perfsentient.core.common.MILLIS_IN_MINUTE
import com.neteinstein.perfsentient.core.model.BehaviorType
import com.neteinstein.perfsentient.core.model.DeviceIssue
import com.neteinstein.perfsentient.core.model.DeviceIssueType
import com.neteinstein.perfsentient.core.model.Finding
import com.neteinstein.perfsentient.feature.report.R

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
}

@get:StringRes
internal val DeviceIssueType.title: Int
    get() = when (this) {
        DeviceIssueType.SCREEN_OFF_DRAIN -> R.string.report_issue_screen_off_drain_title
        DeviceIssueType.HIGH_TEMPERATURE -> R.string.report_issue_temperature_title
        DeviceIssueType.THERMAL_THROTTLING -> R.string.report_issue_thermal_title
        DeviceIssueType.MEMORY_PRESSURE -> R.string.report_issue_memory_title
    }

internal fun Context.describe(issue: DeviceIssue): String = when (issue.type) {
    DeviceIssueType.SCREEN_OFF_DRAIN -> getString(R.string.report_issue_screen_off_drain, issue.value / TENTHS)
    DeviceIssueType.HIGH_TEMPERATURE -> getString(R.string.report_issue_temperature, issue.value / TENTHS)
    DeviceIssueType.THERMAL_THROTTLING -> getString(R.string.report_issue_thermal)
    DeviceIssueType.MEMORY_PRESSURE -> quantity(R.plurals.report_issue_memory, issue.value.toInt())
}

private fun Context.quantity(id: Int, count: Int): String = resources.getQuantityString(id, count, count)

private const val TENTHS = 10f
