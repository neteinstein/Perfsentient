package org.neteinstein.perfsentient.screenshots

import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.DeviceMetrics
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.Verdict
import java.time.LocalDate
import java.time.ZoneOffset

/** Fictional data for the store screenshots. Every app name and number here is made up. */
internal object SampleData {
    val today: LocalDate = LocalDate.of(2026, 11, 16)

    private fun finding(pkg: String, label: String, type: BehaviorType, severity: Severity, value: Long) =
        Finding(pkg, label, type, severity, value)

    private val todayFindings = listOf(
        finding("com.example.quickclean", "QuickClean Booster", BehaviorType.OUT_OF_CONTEXT_ADS, Severity.HIGH, 9),
        finding("com.example.quickclean", "QuickClean Booster", BehaviorType.AFTER_CALL_ADS, Severity.HIGH, 4),
        finding("com.example.quickclean", "QuickClean Booster", BehaviorType.OVERLAY_ABUSE, Severity.MEDIUM, 1),
        finding("com.example.flashlight", "Flashlight Pro", BehaviorType.BACKGROUND_DATA, Severity.MEDIUM, 180L * 1024 * 1024),
        finding("com.example.flashlight", "Flashlight Pro", BehaviorType.NIGHT_ACTIVITY, Severity.MEDIUM, 6),
        finding("com.example.horoscope", "Daily Horoscope", BehaviorType.NOTIFICATION_SPAM, Severity.MEDIUM, 74),
        finding("com.example.wallpapers", "Wallpaper Magic", BehaviorType.STORAGE_BLOAT, Severity.LOW, 1_400L * 1024 * 1024),
    )

    private val trustedFindings = listOf(
        finding("com.example.musicplayer", "Beat Player", BehaviorType.PERSISTENT_BACKGROUND, Severity.MEDIUM, 6L * 3_600_000),
        finding("com.example.musicplayer", "Beat Player", BehaviorType.UNRESTRICTED_BATTERY, Severity.LOW, 1),
        finding("com.example.vpn", "SafeTunnel VPN", BehaviorType.ACCESSIBILITY_RISK, Severity.MEDIUM, 1),
    )

    private val pastOnly = listOf(
        finding("com.example.coupons", "Coupon Hunter", BehaviorType.HIDDEN_ICON, Severity.HIGH, 1),
        finding("com.example.coupons", "Coupon Hunter", BehaviorType.OUT_OF_CONTEXT_ADS, Severity.MEDIUM, 3),
    )

    val trustedPackages = setOf("com.example.musicplayer", "com.example.vpn")

    private val metrics = DeviceMetrics(
        sampleCount = 80,
        drainPerHour = 4.8f,
        screenOffDrainPerHour = 1.6f,
        maxTemperatureC = 39.5f,
        worstThermalStatus = 0,
        averageMemoryUsedPercent = 71,
        lowMemorySamples = 2,
        totalBackgroundBytes = 420L * 1024 * 1024,
        totalForegroundTimeMs = 4L * 3_600_000,
    )

    /** Seven days of reports, newest = [today], with a score that recovers as the user acts. */
    val reports: List<DailyReport> = listOf(
        Triple(6, 52, todayFindings + trustedFindings),
        Triple(5, 58, todayFindings.take(5) + trustedFindings),
        Triple(4, 49, todayFindings + trustedFindings + pastOnly),
        Triple(3, 61, todayFindings.take(6) + pastOnly),
        Triple(2, 66, todayFindings.take(4) + trustedFindings),
        Triple(1, 70, todayFindings.take(4)),
        Triple(0, 74, todayFindings + trustedFindings),
    ).map { (daysAgo, score, findings) ->
        DailyReport(
            date = today.minusDays(daysAgo.toLong()),
            generatedAt = today.minusDays(daysAgo.toLong()).atTime(21, 30).toInstant(ZoneOffset.UTC).toEpochMilli(),
            score = score,
            verdict = if (score >= 80) {
                Verdict.GOOD
            } else if (score >= 60) {
                Verdict.FAIR
            } else {
                Verdict.POOR
            },
            metrics = metrics,
            deviceIssues = if (daysAgo == 0) listOf(DeviceIssue(DeviceIssueType.SCREEN_OFF_DRAIN, Severity.MEDIUM, 16)) else emptyList(),
            findings = findings,
            appsAnalysed = 142,
        )
    }
}
