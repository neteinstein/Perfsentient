package org.neteinstein.perfsentient.core.testing

import java.time.LocalDate
import org.neteinstein.perfsentient.core.model.AppIssueRecord
import org.neteinstein.perfsentient.core.model.AppIssueType
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
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
        iconDisabled: Boolean = false,
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
        launcherIconDisabled = iconDisabled,
    )

    fun usage(pkg: String, foregroundMs: Long = 0, serviceMs: Long = 0) = AppUsage(pkg, foregroundMs, serviceMs, 0)

    /** [background] is mobile traffic by default; [backgroundWifi] is added on top of it. */
    fun network(pkg: String, foreground: Long = 0, background: Long = 0, backgroundWifi: Long = 0) =
        NetworkUsage(pkg, foreground, background + backgroundWifi, backgroundMobileBytes = background)

    fun anr(pkg: String, at: Long = 0) = AppIssueRecord(pkg, AppIssueType.ANR, at)

    fun crash(pkg: String, at: Long = 0) = AppIssueRecord(pkg, AppIssueType.CRASH, at)

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
    ) = VitalSample(at, battery, charging, temperature, screenOn, thermal, 2_000, 8_000, lowMemory)

    fun finding(pkg: String, type: BehaviorType = BehaviorType.BACKGROUND_DATA, severity: Severity = Severity.MEDIUM) =
        Finding(pkg, pkg, type, severity, 1)

    fun report(
        date: LocalDate = LocalDate.of(2026, 11, 16),
        score: Int = 90,
        verdict: Verdict = Verdict.GOOD,
        findings: List<Finding> = emptyList(),
    ) = DailyReport(date, 0, score, verdict, DeviceMetrics.EMPTY, emptyList(), findings, appsAnalysed = 10)
}
