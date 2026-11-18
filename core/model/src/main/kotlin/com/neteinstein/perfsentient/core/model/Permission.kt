package com.neteinstein.perfsentient.core.model

/** Every permission / special access the app may ask for. */
enum class PermissionType {
    /** Special access: android.permission.PACKAGE_USAGE_STATS. */
    USAGE_ACCESS,

    /** Runtime permission: android.permission.POST_NOTIFICATIONS (API 33+). */
    POST_NOTIFICATIONS,

    /** Special access: bind NotificationListenerService (only counts notifications per app). */
    NOTIFICATION_ACCESS,

    /** Install-time: android.permission.QUERY_ALL_PACKAGES (app inventory). */
    APP_INVENTORY,
}

/** How the permission is granted – drives the UI affordance. */
enum class GrantMode {
    RUNTIME_DIALOG,
    SYSTEM_SETTINGS,
    INSTALL_TIME,
}

data class PermissionState(
    val type: PermissionType,
    val isGranted: Boolean,
    val isRequired: Boolean,
    val grantMode: GrantMode,
)

val List<PermissionState>.allRequiredGranted: Boolean
    get() = isNotEmpty() && filter { it.isRequired }.all { it.isGranted }
