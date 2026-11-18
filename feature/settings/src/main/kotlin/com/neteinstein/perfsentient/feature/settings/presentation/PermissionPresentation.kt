package com.neteinstein.perfsentient.feature.settings.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.ui.graphics.vector.ImageVector
import com.neteinstein.perfsentient.core.model.PermissionType
import com.neteinstein.perfsentient.feature.settings.R

internal data class PermissionPresentation(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val rationale: Int,
)

internal val PermissionType.presentation: PermissionPresentation
    get() = when (this) {
        PermissionType.USAGE_ACCESS -> PermissionPresentation(
            Icons.Rounded.Insights,
            R.string.permission_usage_title,
            R.string.permission_usage_desc,
            R.string.permission_usage_why,
        )

        PermissionType.POST_NOTIFICATIONS -> PermissionPresentation(
            Icons.Rounded.NotificationsActive,
            R.string.permission_notifications_title,
            R.string.permission_notifications_desc,
            R.string.permission_notifications_why,
        )

        PermissionType.NOTIFICATION_ACCESS -> PermissionPresentation(
            Icons.Rounded.MarkEmailUnread,
            R.string.permission_notification_access_title,
            R.string.permission_notification_access_desc,
            R.string.permission_notification_access_why,
        )

        PermissionType.APP_INVENTORY -> PermissionPresentation(
            Icons.Rounded.Apps,
            R.string.permission_inventory_title,
            R.string.permission_inventory_desc,
            R.string.permission_inventory_why,
        )
    }

/** System-settings deep links for special accesses that cannot be requested with a dialog. */
internal object PermissionIntents {
    fun usageAccess(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun usageAccessFallback(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun notificationAccess(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun appNotificationSettings(context: Context): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}
