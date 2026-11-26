package org.neteinstein.perfsentient.screenshots

import java.time.LocalDate
import java.time.ZoneOffset
import org.neteinstein.perfsentient.AppTab
import org.neteinstein.perfsentient.core.model.AppBatteryUsage
import org.neteinstein.perfsentient.core.model.BatteryBreakdown
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.ConcerningApp
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.DeviceMetrics
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.ParentalConcern
import org.neteinstein.perfsentient.core.model.ParentalRiskCategory
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.Verdict
import org.neteinstein.perfsentient.core.testing.TestData

/** Fictional data for the store screenshots. Every app name and number here is made up. */
internal object SampleData {
    val today: LocalDate = LocalDate.of(2026, 11, 16)

    /** The tabs of the published store screenshots and review video (parental mode off). */
    val coreTabs = listOf(AppTab.REPORT, AppTab.APPS, AppTab.SETTINGS)

    const val CHILD_AGE = 11

    private fun concerning(
        pkg: String,
        category: ParentalRiskCategory,
        minimumAge: Int,
        recommendedAge: Int,
        vararg concerns: ParentalConcern,
    ) = ConcerningApp(pkg.substringAfterLast('.'), setOf(pkg), category, minimumAge, recommendedAge, Severity.MEDIUM, concerns.toSet())

    /** A fictional parental catalogue, so screenshots never show real app names. */
    val parentalCatalog = listOf(
        concerning(
            "com.example.blockparty",
            ParentalRiskCategory.GAMES_WITH_CHAT,
            0,
            13,
            ParentalConcern.GROOMING_REPORTS,
            ParentalConcern.STRANGER_CONTACT,
            ParentalConcern.SPENDING,
        ),
        concerning(
            "com.example.pixelchat",
            ParentalRiskCategory.PRIVATE_MESSAGING,
            13,
            16,
            ParentalConcern.SEXTORTION_REPORTS,
            ParentalConcern.STRANGER_CONTACT,
            ParentalConcern.DISAPPEARING_MESSAGES,
            ParentalConcern.LOCATION_SHARING,
        ),
        concerning(
            "com.example.swipemates",
            ParentalRiskCategory.FRIEND_FINDING,
            13,
            17,
            ParentalConcern.STRANGER_CONTACT,
            ParentalConcern.LOCATION_SHARING,
        ),
        concerning(
            "com.example.calcvault",
            ParentalRiskCategory.HIDDEN_VAULTS,
            0,
            18,
            ParentalConcern.HIDES_CONTENT,
            ParentalConcern.BYPASSES_CONTROLS,
        ),
    )

    val installedApps = listOf(
        TestData.app("com.example.blockparty", label = "Block Party"),
        TestData.app("com.example.pixelchat", label = "Pixel Chat"),
        TestData.app("com.example.swipemates", label = "SwipeMates"),
        TestData.app("com.example.calcvault", label = "Calc+ Vault"),
        TestData.app("com.example.notes", label = "Notes"),
    )

    val reviewedParentalApps = setOf("com.example.calcvault")

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

    private const val HOUR = 3_600_000L

    private fun usage(pkg: String, label: String, drain: Float, background: Float, openMinutes: Long, serviceHours: Long = 0) =
        AppBatteryUsage(pkg, label, drain, background, openMinutes * 60_000, serviceHours * HOUR)

    /** One day of estimated battery use; [scale] varies it a little from day to day. */
    private fun battery(scale: Float) = BatteryBreakdown(
        apps = listOf(
            usage("com.example.game", "Galaxy Racer", 14f * scale, 0.4f * scale, openMinutes = 95),
            usage("com.example.video", "StreamBox", 9f * scale, 0.2f * scale, openMinutes = 120),
            usage("com.example.flashlight", "Flashlight Pro", 6.5f * scale, 6.2f * scale, openMinutes = 1, serviceHours = 9),
            usage("com.example.chat", "ChatNow", 4f * scale, 0.8f * scale, openMinutes = 70),
            usage("com.example.musicplayer", "Beat Player", 3.2f * scale, 3f * scale, openMinutes = 4, serviceHours = 3),
            usage("com.example.maps", "Pathfinder Maps", 2.5f * scale, 0.5f * scale, openMinutes = 25),
            usage("com.example.notes", "Notes", 0.6f * scale, 0f, openMinutes = 15),
        ),
        screenAndSystemPct = 7f * scale,
        standbyPct = 5f,
        measuredDrainPct = 52f * scale,
        measuredHours = 14f,
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
            battery = battery(scale = 0.8f + (daysAgo % 3) * 0.15f),
        )
    }
}
