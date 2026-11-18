package com.neteinstein.perfsentience.core.model

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

    /** Exempt from battery optimisations while working heavily in background. */
    UNRESTRICTED_BATTERY,

    /** Very large cache footprint. */
    STORAGE_BLOAT,
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
}

data class DeviceIssue(
    val type: DeviceIssueType,
    val severity: Severity,
    /** Scaled value: drain in 1/10 %/h, temperature in 1/10 °C, thermal status, low-memory samples. */
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
) {
    val offenders: List<AppAssessment>
        get() = findings
            .groupBy { it.packageName }
            .map { (pkg, list) -> AppAssessment(pkg, list.first().appLabel, list.sortedByDescending { it.severity }) }
            .sortedWith(compareByDescending<AppAssessment> { it.worstSeverity }.thenByDescending { it.penalty })
}
