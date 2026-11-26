package org.neteinstein.perfsentient.core.model

/** Static facts about an installed application. */
data class AppProfile(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val hasLauncherIcon: Boolean,
    val installerPackage: String?,
    val canDrawOverlays: Boolean,
    val hasEnabledAccessibilityService: Boolean,
    val isDeviceAdmin: Boolean,
    val ignoresBatteryOptimizations: Boolean,
    /**
     * Requests RECEIVE_BOOT_COMPLETED. Not used as a misbehaviour signal on purpose: WorkManager
     * merges this permission into almost every app, so it says nothing about the app itself.
     */
    val startsOnBoot: Boolean,
    val cacheBytes: Long,
    val firstInstallTime: Long,
    /** The app that asked for the install (e.g. the browser or chat app an APK was opened from). */
    val initiatingPackage: String? = null,
    val packageSource: PackageSource = PackageSource.UNKNOWN,
    val lastUpdateTime: Long = 0L,
    /** Sensitive runtime permissions the user has granted. */
    val grantedPermissions: Set<SensitivePermission> = emptySet(),
    /** Has notification access: can read every notification, including one-time codes. */
    val hasNotificationAccess: Boolean = false,
    /** Is the default SMS app: receives and can read every text message. */
    val isDefaultSmsApp: Boolean = false,
    /** Is allowed to "install unknown apps". */
    val canInstallApps: Boolean = false,
    /**
     * True when the app declares a launcher activity but has disabled it, i.e. it actively hid
     * its icon. Apps that never had an icon (plugins, keyboards, companion services) are false.
     */
    val launcherIconDisabled: Boolean = false,
) {
    /** True when the app did not come from a known store (Play / OEM store). */
    val isSideloaded: Boolean
        get() = !isSystemApp && (installerPackage == null || installerPackage !in TRUSTED_INSTALLERS)

    /** Can read text messages or notifications, which is where one-time codes arrive. */
    val canReadMessages: Boolean
        get() = hasNotificationAccess ||
            isDefaultSmsApp ||
            SensitivePermission.READ_SMS in grantedPermissions ||
            SensitivePermission.RECEIVE_SMS in grantedPermissions

    /** Holds any access that fraud apps abuse: screen control, overlays, messages, codes or calls. */
    val hasSensitiveCapability: Boolean
        get() = hasEnabledAccessibilityService ||
            hasNotificationAccess ||
            isDeviceAdmin ||
            canDrawOverlays ||
            isDefaultSmsApp ||
            grantedPermissions.any { it.isFraudRelevant }

    companion object {
        val TRUSTED_INSTALLERS = setOf(
            "com.android.vending",
            "com.google.android.feedback",
            "com.sec.android.app.samsungapps",
            "com.huawei.appmarket",
            "com.xiaomi.market",
            "com.amazon.venezia",
            "com.oppo.market",
            "com.heytap.market",
            "com.vivo.appstore",
            "org.fdroid.fdroid",
        )
    }
}

/** Aggregated usage of an app for the analysed window. */
data class AppUsage(
    val packageName: String,
    val foregroundTimeMs: Long,
    val foregroundServiceTimeMs: Long,
    val lastTimeUsed: Long,
)

/**
 * Data transferred by an app in the analysed window, split by foreground/background state.
 * [backgroundBytes] counts every network; [backgroundMobileBytes] is the mobile-data part of it.
 */
data class NetworkUsage(
    val packageName: String,
    val foregroundBytes: Long,
    /** Background traffic over every network (Wi-Fi + mobile). */
    val backgroundBytes: Long,
    /** The part of [backgroundBytes] sent over the (usually metered) mobile network. */
    val backgroundMobileBytes: Long = 0,
) {
    /** Background traffic over unmetered networks such as Wi-Fi. */
    val backgroundWifiBytes: Long get() = (backgroundBytes - backgroundMobileBytes).coerceAtLeast(0)
}

/** Subset of android.app.usage.UsageEvents we care about. */
enum class UsageEventType {
    ACTIVITY_RESUMED,
    ACTIVITY_PAUSED,
    FOREGROUND_SERVICE_START,
    FOREGROUND_SERVICE_STOP,
    SCREEN_INTERACTIVE,
    SCREEN_NON_INTERACTIVE,
    KEYGUARD_SHOWN,
    KEYGUARD_HIDDEN,
}

data class UsageEventRecord(
    val packageName: String,
    val type: UsageEventType,
    val timestamp: Long,
)

/** A periodic sample of device-wide vitals, recorded by the background sampler. */
data class VitalSample(
    val timestamp: Long,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTemperatureC: Float,
    val isScreenOn: Boolean,
    val thermalStatus: Int,
    val availableMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val isLowMemory: Boolean,
    /**
     * Remaining battery charge from BATTERY_PROPERTY_CHARGE_COUNTER, finer than [batteryPercent].
     * Null when the device does not report it. Only ratios are used, so the unit (µAh on most
     * devices, mAh on a few) does not matter.
     */
    val chargeCounterUah: Long? = null,
)

/** Environment facts needed to interpret usage events. */
data class DeviceContext(
    val selfPackage: String,
    val launcherPackages: Set<String>,
    val dialerPackages: Set<String>,
    val systemUiPackages: Set<String>,
)

/** Kinds of app failure the system records in its error log (DropBox). */
enum class AppIssueType {
    /** "App isn't responding": the main thread was blocked for too long. */
    ANR,

    /** The app process crashed (Java or native). */
    CRASH,
}

/** One ANR or crash of [packageName], read from the system error log. */
data class AppIssueRecord(
    val packageName: String,
    val type: AppIssueType,
    val timestamp: Long,
)
