package org.neteinstein.perfsentient.core.testing

import java.time.LocalDate
import org.neteinstein.perfsentient.core.model.AppBatteryUsage
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
import org.neteinstein.perfsentient.core.model.BatteryBreakdown
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceContext
import org.neteinstein.perfsentient.core.model.DeviceMetrics
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.NetworkUsage
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.UsageEventRecord
import org.neteinstein.perfsentient.core.model.UsageEventType
import org.neteinstein.perfsentient.core.model.Verdict
import org.neteinstein.perfsentient.core.model.VitalSample

/** Builders with sensible defaults so tests only spell out what matters. */
object TestData {
    const val SELF = "org.neteinstein.perfsentient"
    const val LAUNCHER = "com.android.launcher3"
    const val DIALER = "com.google.android.dialer"
    const val SYSTEM_UI = "com.android.systemui"

    fun deviceContext() = DeviceContext(
        selfPackage = SELF,
        launcherPackages = setOf(LAUNCHER),
        dialerPackages = setOf(DIALER),
        systemUiPackages = setOf(SYSTEM_UI),
    )

    fun app(
        pkg: String,
        label: String = pkg.substringAfterLast('.'),
        system: Boolean = false,
        launcher: Boolean = true,
        installer: String? = "com.android.vending",
        overlay: Boolean = false,
        accessibility: Boolean = false,
        admin: Boolean = false,
        batteryExempt: Boolean = false,
        cacheBytes: Long = 0,
    ) = AppProfile(
        packageName = pkg,
        label = label,
        isSystemApp = system,
        hasLauncherIcon = launcher,
        installerPackage = installer,
        canDrawOverlays = overlay,
        hasEnabledAccessibilityService = accessibility,
        isDeviceAdmin = admin,
        ignoresBatteryOptimizations = batteryExempt,
        startsOnBoot = false,
        cacheBytes = cacheBytes,
        firstInstallTime = 0,
    )

    fun usage(pkg: String, foregroundMs: Long = 0, serviceMs: Long = 0) = AppUsage(pkg, foregroundMs, serviceMs, 0)

    /** [mobileBackground] defaults to [background]: all background traffic on mobile data. */
    fun network(pkg: String, foreground: Long = 0, background: Long = 0, mobileBackground: Long = background) =
        NetworkUsage(pkg, foreground, background, mobileBackground)

    fun event(pkg: String, type: UsageEventType, at: Long) = UsageEventRecord(pkg, type, at)

    fun resumed(pkg: String, at: Long) = event(pkg, UsageEventType.ACTIVITY_RESUMED, at)

    fun vital(
        at: Long,
        battery: Int = 80,
        charging: Boolean = false,
        temperature: Float = 30f,
        screenOn: Boolean = true,
        thermal: Int = 0,
        lowMemory: Boolean = false,
        chargeCounter: Long? = null,
    ) = VitalSample(at, battery, charging, temperature, screenOn, thermal, 2_000, 8_000, lowMemory, chargeCounter)

    fun finding(pkg: String, type: BehaviorType = BehaviorType.BACKGROUND_DATA, severity: Severity = Severity.MEDIUM) =
        Finding(pkg, pkg, type, severity, 1)

    fun report(
        date: LocalDate = LocalDate.of(2026, 11, 16),
        score: Int = 90,
        verdict: Verdict = Verdict.GOOD,
        findings: List<Finding> = emptyList(),
        battery: BatteryBreakdown = BatteryBreakdown.EMPTY,
    ) = DailyReport(date, 0, score, verdict, DeviceMetrics.EMPTY, emptyList(), findings, appsAnalysed = 10, battery = battery)

    fun batteryUsage(pkg: String, drain: Float, background: Float = 0f, foregroundMs: Long = 0, backgroundMs: Long = 0) =
        AppBatteryUsage(pkg, pkg.substringAfterLast('.'), drain, background, foregroundMs, backgroundMs)

    fun battery(vararg apps: AppBatteryUsage, screenAndSystem: Float = 0f, standby: Float = 0f) = BatteryBreakdown(
        apps = apps.toList(),
        screenAndSystemPct = screenAndSystem,
        standbyPct = standby,
        measuredDrainPct = apps.sumOf { it.estimatedDrainPct.toDouble() }.toFloat() + screenAndSystem + standby,
        measuredHours = 1f,
    )
}
