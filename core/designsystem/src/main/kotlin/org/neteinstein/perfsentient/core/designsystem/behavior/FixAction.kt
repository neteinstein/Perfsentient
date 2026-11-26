package org.neteinstein.perfsentient.core.designsystem.behavior

import androidx.annotation.StringRes
import org.neteinstein.perfsentient.core.designsystem.R
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.ReadableData

/** The system screen that fixes a finding or device issue. See [fixIntent] for where each one goes. */
enum class FixAction(@StringRes val label: Int) {
    /** App info: uninstall, force stop, permissions, battery, storage. */
    APP_INFO(R.string.fix_app_info),
    OVERLAY(R.string.fix_overlay),
    ACCESSIBILITY(R.string.fix_accessibility),
    NOTIFICATION_ACCESS(R.string.fix_notification_access),
    INSTALL_APPS(R.string.fix_install_apps),
    APP_NOTIFICATIONS(R.string.fix_app_notifications),
    BACKGROUND_DATA(R.string.fix_background_data),
    BATTERY_OPTIMIZATION(R.string.fix_battery_optimization),
    DEVICE_ADMIN(R.string.fix_device_admin),
    SCREEN_LOCK(R.string.fix_screen_lock),
    DEVELOPER_OPTIONS(R.string.fix_developer_options),
    SYSTEM_UPDATE(R.string.fix_system_update),
    BATTERY_USAGE(R.string.fix_battery_usage),
}

/** Where to fix this finding; App info when nothing more specific helps. */
val Finding.fixAction: FixAction
    get() = when (type) {
        BehaviorType.OVERLAY_ABUSE -> FixAction.OVERLAY

        BehaviorType.ACCESSIBILITY_RISK -> FixAction.ACCESSIBILITY

        BehaviorType.DEVICE_ADMIN -> FixAction.DEVICE_ADMIN

        BehaviorType.BACKGROUND_DATA -> FixAction.BACKGROUND_DATA

        BehaviorType.NOTIFICATION_SPAM -> FixAction.APP_NOTIFICATIONS

        BehaviorType.UNRESTRICTED_BATTERY -> FixAction.BATTERY_OPTIMIZATION

        BehaviorType.CAN_INSTALL_APPS -> FixAction.INSTALL_APPS

        // Notification access is revoked in its own screen; SMS permissions live in App info.
        BehaviorType.DATA_THEFT_RISK ->
            if (value and ReadableData.NOTIFICATIONS != 0L) FixAction.NOTIFICATION_ACCESS else FixAction.APP_INFO

        BehaviorType.OUT_OF_CONTEXT_ADS,
        BehaviorType.AFTER_CALL_ADS,
        BehaviorType.HIDDEN_ICON,
        BehaviorType.PERSISTENT_BACKGROUND,
        BehaviorType.NIGHT_ACTIVITY,
        BehaviorType.STORAGE_BLOAT,
        BehaviorType.RISKY_INSTALL_SOURCE,
        BehaviorType.REMOTE_ACCESS,
        BehaviorType.IMPERSONATION,
        -> FixAction.APP_INFO
    }

/** Where to fix this device issue, or null when there is no setting to change (heat, memory). */
val DeviceIssueType.fixAction: FixAction?
    get() = when (this) {
        DeviceIssueType.SCREEN_OFF_DRAIN -> FixAction.BATTERY_USAGE
        DeviceIssueType.NO_SCREEN_LOCK -> FixAction.SCREEN_LOCK
        DeviceIssueType.OUTDATED_SECURITY_PATCH -> FixAction.SYSTEM_UPDATE
        DeviceIssueType.USB_DEBUGGING -> FixAction.DEVELOPER_OPTIONS
        DeviceIssueType.HIGH_TEMPERATURE, DeviceIssueType.THERMAL_THROTTLING, DeviceIssueType.MEMORY_PRESSURE -> null
    }
