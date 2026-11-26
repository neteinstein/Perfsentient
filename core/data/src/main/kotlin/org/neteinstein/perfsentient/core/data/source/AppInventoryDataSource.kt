package org.neteinstein.perfsentient.core.data.source

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.UserHandle
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.DeviceContext
import org.neteinstein.perfsentient.core.model.PackageSource

/**
 * Builds an [AppProfile] for every installed app using only public, Play-compliant APIs: install
 * source, granted permissions and special accesses come from PackageManager, AppOps and the
 * notification-listener and default-SMS settings, all readable without extra permissions.
 */
class AppInventoryDataSource(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val appOps = context.getSystemService(AppOpsManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)
    private val storageStats = context.getSystemService(StorageStatsManager::class.java)
    private val devicePolicy = context.getSystemService(DevicePolicyManager::class.java)
    private val accessibility = context.getSystemService(AccessibilityManager::class.java)

    fun installedApps(): List<AppProfile> {
        val launcherPackages = packagesFor(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
        val accessibilityPackages = accessibility
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
            ?.toSet()
            .orEmpty()
        val adminPackages = runCatching { devicePolicy?.activeAdmins?.map { it.packageName }?.toSet() }
            .getOrNull()
            .orEmpty()
        val user: UserHandle = Process.myUserHandle()
        val notificationListeners = runCatching { NotificationManagerCompat.getEnabledListenerPackages(context) }.getOrDefault(emptySet())
        val defaultSms = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()

        return installedPackages().mapNotNull { info ->
            val app = info.applicationInfo ?: return@mapNotNull null
            val pkg = info.packageName
            val isSystem = app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val source = installSourceOf(pkg)
            AppProfile(
                packageName = pkg,
                label = runCatching { app.loadLabel(pm).toString() }.getOrDefault(pkg),
                isSystemApp = isSystem,
                hasLauncherIcon = pkg in launcherPackages,
                installerPackage = source?.installingPackageName,
                canDrawOverlays = !isSystem && canDrawOverlays(app, info),
                hasEnabledAccessibilityService = pkg in accessibilityPackages,
                isDeviceAdmin = pkg in adminPackages,
                ignoresBatteryOptimizations = !isSystem && runCatching { power.isIgnoringBatteryOptimizations(pkg) }.getOrDefault(false),
                startsOnBoot = info.requestedPermissions?.contains(Manifest.permission.RECEIVE_BOOT_COMPLETED) == true,
                cacheBytes = if (isSystem) 0L else cacheBytes(app, user),
                firstInstallTime = info.firstInstallTime,
                initiatingPackage = source?.initiatingPackageName,
                packageSource = source.packageSource(),
                lastUpdateTime = info.lastUpdateTime,
                grantedPermissions = grantedSensitivePermissions(info.requestedPermissions, info.requestedPermissionsFlags),
                hasNotificationAccess = pkg in notificationListeners,
                isDefaultSmsApp = pkg == defaultSms,
                canInstallApps = !isSystem && canInstallApps(app, info),
            )
        }
    }

    /** Labels of the installed apps among [packageNames]; one cheap lookup per package, no stats. */
    fun installedAppLabels(packageNames: Set<String>): Map<String, String> = packageNames.mapNotNull { pkg ->
        val app = runCatching { applicationInfo(pkg) }.getOrNull() ?: return@mapNotNull null
        pkg to runCatching { app.loadLabel(pm).toString() }.getOrDefault(pkg)
    }.toMap()

    fun deviceContext(): DeviceContext {
        val telecom = context.getSystemService(TelecomManager::class.java)
        val dialers = buildSet {
            telecom?.defaultDialerPackage?.let(::add)
            telecom?.systemDialerPackage?.let(::add)
            addAll(KNOWN_CALL_UI_PACKAGES)
        }
        return DeviceContext(
            selfPackage = context.packageName,
            launcherPackages = packagesFor(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)),
            dialerPackages = dialers,
            systemUiPackages = SYSTEM_UI_PACKAGES,
        )
    }

    private fun installedPackages(): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS.toLong()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags.toInt())
        }
    }

    /** Throws [PackageManager.NameNotFoundException] when [pkg] is not installed. */
    private fun applicationInfo(pkg: String): ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getApplicationInfo(pkg, 0)
    }

    private fun packagesFor(intent: Intent): Set<String> {
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        return resolved.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    private fun installSourceOf(pkg: String): InstallSourceInfo? = runCatching { pm.getInstallSourceInfo(pkg) }.getOrNull()

    private fun InstallSourceInfo?.packageSource(): PackageSource = when {
        this == null -> PackageSource.UNKNOWN
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> packageSourceOf(packageSource)
        else -> PackageSource.UNKNOWN
    }

    /** "Install unknown apps" is an app op; only an explicit allow counts (the default is "ask / denied"). */
    private fun canInstallApps(app: ApplicationInfo, info: PackageInfo): Boolean {
        val requested = info.requestedPermissions?.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES) == true
        if (!requested) return false
        val mode = runCatching {
            @Suppress("DEPRECATION") // Only public API able to query another app's op mode.
            appOps.unsafeCheckOpNoThrow(OPSTR_REQUEST_INSTALL_PACKAGES, app.uid, app.packageName)
        }.getOrDefault(AppOpsManager.MODE_DEFAULT)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun canDrawOverlays(app: ApplicationInfo, info: PackageInfo): Boolean {
        val requested = info.requestedPermissions?.contains(Manifest.permission.SYSTEM_ALERT_WINDOW) == true
        if (!requested) return false
        val mode = runCatching {
            @Suppress("DEPRECATION") // Only public API able to query another app's op mode.
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, app.uid, app.packageName)
        }.getOrDefault(AppOpsManager.MODE_DEFAULT)
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true

            AppOpsManager.MODE_DEFAULT ->
                pm.checkPermission(Manifest.permission.SYSTEM_ALERT_WINDOW, app.packageName) == PackageManager.PERMISSION_GRANTED

            else -> false
        }
    }

    private fun cacheBytes(app: ApplicationInfo, user: UserHandle): Long = runCatching {
        storageStats.queryStatsForPackage(app.storageUuid, app.packageName, user).cacheBytes
    }.getOrDefault(0L)

    private companion object {
        /** AppOpsManager.OPSTR_REQUEST_INSTALL_PACKAGES is hidden from the SDK; the op name is stable since Android 8. */
        const val OPSTR_REQUEST_INSTALL_PACKAGES = "android:request_install_packages"

        val KNOWN_CALL_UI_PACKAGES = setOf(
            "com.android.server.telecom",
            "com.android.incallui",
            "com.google.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.dialer",
        )
        val SYSTEM_UI_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.intentresolver",
            "com.google.android.inputmethod.latin",
            "com.android.settings.intelligence",
        )
    }
}
