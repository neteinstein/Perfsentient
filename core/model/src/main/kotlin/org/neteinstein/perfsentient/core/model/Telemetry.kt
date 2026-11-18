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
    val startsOnBoot: Boolean,
    val cacheBytes: Long,
    val firstInstallTime: Long,
) {
    /** True when the app did not come from a known store (Play / OEM store). */
    val isSideloaded: Boolean
        get() = !isSystemApp && (installerPackage == null || installerPackage !in TRUSTED_INSTALLERS)

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

/** Data transferred by an app in the analysed window, split by foreground/background state. */
data class NetworkUsage(
    val packageName: String,
    val foregroundBytes: Long,
    val backgroundBytes: Long,
)

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
)

/** Environment facts needed to interpret usage events. */
data class DeviceContext(
    val selfPackage: String,
    val launcherPackages: Set<String>,
    val dialerPackages: Set<String>,
    val systemUiPackages: Set<String>,
)
