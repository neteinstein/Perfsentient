package com.neteinstein.perfsentient.core.data.source

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.UserHandle
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityManager
import com.neteinstein.perfsentient.core.model.AppProfile
import com.neteinstein.perfsentient.core.model.DeviceContext

/** Builds an [AppProfile] for every installed app using only public, Play-compliant APIs. */
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

        return installedPackages().mapNotNull { info ->
            val app = info.applicationInfo ?: return@mapNotNull null
            val pkg = info.packageName
            val isSystem = app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            AppProfile(
                packageName = pkg,
                label = runCatching { app.loadLabel(pm).toString() }.getOrDefault(pkg),
                isSystemApp = isSystem,
                hasLauncherIcon = pkg in launcherPackages,
                installerPackage = installerOf(pkg),
                canDrawOverlays = !isSystem && canDrawOverlays(app, info),
                hasEnabledAccessibilityService = pkg in accessibilityPackages,
                isDeviceAdmin = pkg in adminPackages,
                ignoresBatteryOptimizations = !isSystem && runCatching { power.isIgnoringBatteryOptimizations(pkg) }.getOrDefault(false),
                startsOnBoot = info.requestedPermissions?.contains(Manifest.permission.RECEIVE_BOOT_COMPLETED) == true,
                cacheBytes = if (isSystem) 0L else cacheBytes(app, user),
                firstInstallTime = info.firstInstallTime,
            )
        }
    }

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

    private fun packagesFor(intent: Intent): Set<String> {
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        return resolved.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    private fun installerOf(pkg: String): String? = runCatching {
        pm.getInstallSourceInfo(pkg).installingPackageName
    }.getOrNull()

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
