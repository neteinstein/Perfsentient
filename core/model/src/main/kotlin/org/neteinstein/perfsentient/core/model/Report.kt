package org.neteinstein.perfsentient.core.model

import java.time.LocalDate

enum class Severity(val weight: Int) {
    LOW(weight = 2),
    MEDIUM(weight = 7),
    HIGH(weight = 15),
}

/** Misbehaviours we can detect, each backed by a detector in core:domain. */
enum class BehaviorType {
    /** Brief full-screen activities shown on top of other apps (disruptive / out-of-context ads). */
    OUT_OF_CONTEXT_ADS,

    /** Activities launched right after a phone call ends ("after-call" ads). */
    AFTER_CALL_ADS,

    /** Can draw over other apps and shows signs of using it while the user is elsewhere. */
    OVERLAY_ABUSE,

    /** Has no launcher icon yet is active in the background. */
    HIDDEN_ICON,

    /** Foreground service running for hours while the user barely opens the app. */
    PERSISTENT_BACKGROUND,

    /** Wakes up / launches screens while the screen is off at night. */
    NIGHT_ACTIVITY,

    /** Transfers lots of data while in background. */
    BACKGROUND_DATA,

    /** Posts an unusual number of notifications. */
    NOTIFICATION_SPAM,

    /** Holds an enabled accessibility service (can read & control the screen). */
    ACCESSIBILITY_RISK,

    /** Registered as device administrator (hard to uninstall). */
    DEVICE_ADMIN,

    /** Repeatedly froze and triggered "App isn't responding" (ANR). Needs the optional system-log access. */
    APP_NOT_RESPONDING,

    /** Repeatedly crashed. Needs the optional system-log access. */
    APP_CRASHES,

    /** Exempt from battery optimisations while working heavily in background. */
    UNRESTRICTED_BATTERY,

    /** Very large cache footprint. */
    STORAGE_BLOAT,

    /** Not from a store: installed from a link in a chat or email, or a downloaded file, with risky access. */
    RISKY_INSTALL_SOURCE,

    /** Can read text messages or notifications, where one-time codes arrive. */
    DATA_THEFT_RISK,

    /** A remote-access app installed recently: scammers ask victims to install one during a call. */
    REMOTE_ACCESS,

    /** Not from a store, yet named like a well-known app or a system component. */
    IMPERSONATION,

    /** Allowed to install other apps. */
    CAN_INSTALL_APPS,
}

/** Where a sideloaded app came from, stored as [Finding.value] of [BehaviorType.RISKY_INSTALL_SOURCE]. */
enum class InstallChannel {
    MESSAGING,
    BROWSER,
    FILE,
}

/** What an app can read, stored as a bit mask in [Finding.value] of [BehaviorType.DATA_THEFT_RISK]. */
object ReadableData {
    const val NOTIFICATIONS = 1L
    const val TEXT_MESSAGES = 2L
}

/**
 * A misbehaviour detected for one app.
 * [value] meaning depends on [type] (ms, bytes or count) – see presentation mapping.
 */
data class Finding(
    val packageName: String,
    val appLabel: String,
    val type: BehaviorType,
    val severity: Severity,
    val value: Long,
)

enum class DeviceIssueType {
    SCREEN_OFF_DRAIN,
    HIGH_TEMPERATURE,
    THERMAL_THROTTLING,
    MEMORY_PRESSURE,

    /** No PIN, pattern or password: anyone holding the phone can use it. */
    NO_SCREEN_LOCK,

    /** The security patch is months old. */
    OUTDATED_SECURITY_PATCH,

    /** USB debugging is on. */
    USB_DEBUGGING,
}

data class DeviceIssue(
    val type: DeviceIssueType,
    val severity: Severity,
    /**
     * Scaled value: drain in 1/10 %/h, temperature in 1/10 °C, thermal status, low-memory samples,
     * days since the security patch.
     */
    val value: Long,
)

data class DeviceMetrics(
    val sampleCount: Int,
    /** Average battery drain while discharging, in percent per hour. */
    val drainPerHour: Float,
    /** Average battery drain while the screen was off, in percent per hour. */
    val screenOffDrainPerHour: Float,
    val maxTemperatureC: Float,
    val worstThermalStatus: Int,
    val averageMemoryUsedPercent: Int,
    val lowMemorySamples: Int,
    val totalBackgroundBytes: Long,
    val totalForegroundTimeMs: Long,
) {
    companion object {
        val EMPTY = DeviceMetrics(0, 0f, 0f, 0f, 0, 0, 0, 0L, 0L)
    }
}

enum class Verdict { GOOD, FAIR, POOR }

/** Groups findings per app so the UI can render one card per offender. */
data class AppAssessment(
    val packageName: String,
    val appLabel: String,
    val findings: List<Finding>,
) {
    val worstSeverity: Severity get() = findings.maxOf { it.severity }
    val penalty: Int get() = findings.sumOf { it.severity.weight }
}

data class DailyReport(
    val date: LocalDate,
    val generatedAt: Long,
    val score: Int,
    val verdict: Verdict,
    val metrics: DeviceMetrics,
    val deviceIssues: List<DeviceIssue>,
    val findings: List<Finding>,
    val appsAnalysed: Int,
    /** Apps whose findings were left out because the user trusts them. Computed on read, never stored. */
    val hiddenTrustedApps: Int = 0,
    /** Estimated battery use per app. Not a finding: trusted apps stay in it. */
    val battery: BatteryBreakdown = BatteryBreakdown.EMPTY,
) {
    val offenders: List<AppAssessment>
        get() = findings
            .groupBy { it.packageName }
            .map { (pkg, list) -> AppAssessment(pkg, list.first().appLabel, list.sortedByDescending { it.severity }) }
            .sortedWith(compareByDescending<AppAssessment> { it.worstSeverity }.thenByDescending { it.penalty })
}

/**
 * An app that was flagged in the retained report history (or is on the allowlist), with its
 * [isTrusted] state. Used by the Apps tab. [findings] holds the latest finding per behaviour
 * type; it is empty for a trusted app that is no longer in any retained report.
 */
data class FlaggedApp(
    val packageName: String,
    val appLabel: String,
    val findings: List<Finding>,
    val lastFlagged: LocalDate?,
    val daysFlagged: Int,
    val isTrusted: Boolean,
) {
    val worstSeverity: Severity? get() = findings.maxOfOrNull { it.severity }
}
