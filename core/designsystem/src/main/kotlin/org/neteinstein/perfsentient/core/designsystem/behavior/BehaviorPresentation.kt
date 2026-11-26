package org.neteinstein.perfsentient.core.designsystem.behavior

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Sd
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import org.neteinstein.perfsentient.core.designsystem.R
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.Verdict

/** UI metadata (icon + copy) for each [BehaviorType]. Shared by Report and Settings. */
data class BehaviorPresentation(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val description: Int,
)

val BehaviorType.presentation: BehaviorPresentation
    get() = when (this) {
        BehaviorType.OUT_OF_CONTEXT_ADS -> BehaviorPresentation(
            Icons.AutoMirrored.Rounded.OpenInNew,
            R.string.behavior_out_of_context_ads_title,
            R.string.behavior_out_of_context_ads_desc,
        )

        BehaviorType.AFTER_CALL_ADS -> BehaviorPresentation(
            Icons.Rounded.CallEnd,
            R.string.behavior_after_call_ads_title,
            R.string.behavior_after_call_ads_desc,
        )

        BehaviorType.OVERLAY_ABUSE -> BehaviorPresentation(
            Icons.Rounded.Layers,
            R.string.behavior_overlay_abuse_title,
            R.string.behavior_overlay_abuse_desc,
        )

        BehaviorType.HIDDEN_ICON -> BehaviorPresentation(
            Icons.Rounded.VisibilityOff,
            R.string.behavior_hidden_icon_title,
            R.string.behavior_hidden_icon_desc,
        )

        BehaviorType.PERSISTENT_BACKGROUND -> BehaviorPresentation(
            Icons.Rounded.Sync,
            R.string.behavior_persistent_background_title,
            R.string.behavior_persistent_background_desc,
        )

        BehaviorType.NIGHT_ACTIVITY -> BehaviorPresentation(
            Icons.Rounded.Bedtime,
            R.string.behavior_night_activity_title,
            R.string.behavior_night_activity_desc,
        )

        BehaviorType.BACKGROUND_DATA -> BehaviorPresentation(
            Icons.Rounded.CloudSync,
            R.string.behavior_background_data_title,
            R.string.behavior_background_data_desc,
        )

        BehaviorType.NOTIFICATION_SPAM -> BehaviorPresentation(
            Icons.Rounded.NotificationsActive,
            R.string.behavior_notification_spam_title,
            R.string.behavior_notification_spam_desc,
        )

        BehaviorType.ACCESSIBILITY_RISK -> BehaviorPresentation(
            Icons.Rounded.Accessibility,
            R.string.behavior_accessibility_risk_title,
            R.string.behavior_accessibility_risk_desc,
        )

        BehaviorType.DEVICE_ADMIN -> BehaviorPresentation(
            Icons.Rounded.AdminPanelSettings,
            R.string.behavior_device_admin_title,
            R.string.behavior_device_admin_desc,
        )

        BehaviorType.UNRESTRICTED_BATTERY -> BehaviorPresentation(
            Icons.Rounded.BatteryAlert,
            R.string.behavior_unrestricted_battery_title,
            R.string.behavior_unrestricted_battery_desc,
        )

        BehaviorType.STORAGE_BLOAT -> BehaviorPresentation(
            Icons.Rounded.Sd,
            R.string.behavior_storage_bloat_title,
            R.string.behavior_storage_bloat_desc,
        )

        BehaviorType.RISKY_INSTALL_SOURCE -> BehaviorPresentation(
            Icons.Rounded.InstallMobile,
            R.string.behavior_risky_install_source_title,
            R.string.behavior_risky_install_source_desc,
        )

        BehaviorType.DATA_THEFT_RISK -> BehaviorPresentation(
            Icons.Rounded.Password,
            R.string.behavior_data_theft_risk_title,
            R.string.behavior_data_theft_risk_desc,
        )

        BehaviorType.REMOTE_ACCESS -> BehaviorPresentation(
            Icons.AutoMirrored.Rounded.ScreenShare,
            R.string.behavior_remote_access_title,
            R.string.behavior_remote_access_desc,
        )

        BehaviorType.IMPERSONATION -> BehaviorPresentation(
            Icons.Rounded.TheaterComedy,
            R.string.behavior_impersonation_title,
            R.string.behavior_impersonation_desc,
        )

        BehaviorType.CAN_INSTALL_APPS -> BehaviorPresentation(
            Icons.Rounded.Download,
            R.string.behavior_can_install_apps_title,
            R.string.behavior_can_install_apps_desc,
        )

        BehaviorType.APP_NOT_RESPONDING -> BehaviorPresentation(
            Icons.Rounded.HourglassBottom,
            R.string.behavior_app_not_responding_title,
            R.string.behavior_app_not_responding_desc,
        )

        BehaviorType.APP_CRASHES -> BehaviorPresentation(
            Icons.Rounded.BugReport,
            R.string.behavior_app_crashes_title,
            R.string.behavior_app_crashes_desc,
        )
    }

@get:StringRes
val Severity.label: Int
    get() = when (this) {
        Severity.LOW -> R.string.severity_low
        Severity.MEDIUM -> R.string.severity_medium
        Severity.HIGH -> R.string.severity_high
    }

@get:StringRes
val Verdict.label: Int
    get() = when (this) {
        Verdict.GOOD -> R.string.verdict_good
        Verdict.FAIR -> R.string.verdict_fair
        Verdict.POOR -> R.string.verdict_poor
    }
