package org.neteinstein.perfsentient.core.designsystem.behavior

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import org.neteinstein.perfsentient.core.model.SmsPartnerLinks

/**
 * The system settings screen for [action], scoped to [packageName] where Android allows it.
 * Some OEMs do not implement every screen, so [openFix] falls back to App info (or the app list).
 */
fun fixIntent(action: FixAction, packageName: String?): Intent {
    val packageUri = packageName?.let { Uri.fromParts("package", it, null) }
    return when (action) {
        FixAction.APP_INFO -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        FixAction.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
        FixAction.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        FixAction.NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        FixAction.INSTALL_APPS -> Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri)
        FixAction.APP_NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        FixAction.BACKGROUND_DATA -> Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS, packageUri)
        FixAction.BATTERY_OPTIMIZATION -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        FixAction.DEVICE_ADMIN -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        FixAction.SCREEN_LOCK -> Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
        FixAction.DEVELOPER_OPTIONS -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        FixAction.SYSTEM_UPDATE -> Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
        FixAction.BATTERY_USAGE -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** Opens a conversation in SMS Toolbox (the scam text behind a finding). */
fun Context.openPartnerMessage(threadId: Long) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        (SmsPartnerLinks.CONVERSATION_PREFIX + threadId).toUri(),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

/** Opens SMS Toolbox's consent dialog, where the user can turn on sharing with Perfsentient. */
fun Context.openPartnerConnect() {
    val intent = Intent(Intent.ACTION_VIEW, SmsPartnerLinks.CONNECT.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

/** SMS Toolbox's store page; its GitHub releases when no store app handles it. Opening a page needs no INTERNET permission. */
fun Context.openPartnerStore() {
    val store = Intent(Intent.ACTION_VIEW, SmsPartnerLinks.PLAY_STORE.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(store)
    } catch (_: ActivityNotFoundException) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, SmsPartnerLinks.RELEASES.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/** Opens the screen for [action]; if the device lacks it, App info for [packageName], else the app list / Settings. */
fun Context.openFix(action: FixAction, packageName: String? = null) {
    val fallback = when {
        action != FixAction.APP_INFO && packageName != null -> fixIntent(FixAction.APP_INFO, packageName)
        packageName != null -> Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        action == FixAction.SCREEN_LOCK -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        else -> Intent(Settings.ACTION_SETTINGS)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(fixIntent(action, packageName))
    } catch (_: ActivityNotFoundException) {
        runCatching { startActivity(fallback) }
    } catch (_: SecurityException) {
        runCatching { startActivity(fallback) }
    }
}
