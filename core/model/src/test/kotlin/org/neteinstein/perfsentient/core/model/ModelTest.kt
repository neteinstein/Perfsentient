package org.neteinstein.perfsentient.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {
    private fun app(installer: String?, system: Boolean = false) =
        AppProfile("p", "P", system, true, installer, false, false, false, false, false, 0, 0)

    @Test
    fun `sideloaded means user app from an unknown installer`() {
        assertFalse(app("com.android.vending").isSideloaded)
        assertTrue(app(null).isSideloaded)
        assertTrue(app("com.some.apk.installer").isSideloaded)
        assertFalse(app(null, system = true).isSideloaded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `report time rejects invalid hours`() {
        ReportTime(24, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `report time rejects invalid minutes`() {
        ReportTime(10, 60)
    }

    @Test
    fun `all required granted ignores optional permissions`() {
        val list = listOf(
            PermissionState(PermissionType.USAGE_ACCESS, true, true, GrantMode.SYSTEM_SETTINGS),
            PermissionState(PermissionType.NOTIFICATION_ACCESS, false, false, GrantMode.SYSTEM_SETTINGS),
        )
        assertTrue(list.allRequiredGranted)
        assertFalse(emptyList<PermissionState>().allRequiredGranted)
        assertFalse(listOf(list[0].copy(isGranted = false)).allRequiredGranted)
    }

    @Test
    fun `offenders are grouped per app and ordered by worst severity then penalty`() {
        fun f(pkg: String, type: BehaviorType, severity: Severity) = Finding(pkg, pkg, type, severity, 0)
        val report = DailyReport(
            date = LocalDate.of(2026, 1, 1),
            generatedAt = 0,
            score = 50,
            verdict = Verdict.POOR,
            metrics = DeviceMetrics.EMPTY,
            deviceIssues = emptyList(),
            findings = listOf(
                f("low", BehaviorType.STORAGE_BLOAT, Severity.LOW),
                f("medium", BehaviorType.BACKGROUND_DATA, Severity.MEDIUM),
                f("high", BehaviorType.HIDDEN_ICON, Severity.MEDIUM),
                f("high", BehaviorType.OVERLAY_ABUSE, Severity.HIGH),
            ),
            appsAnalysed = 3,
        )
        val offenders = report.offenders
        assertEquals(listOf("high", "medium", "low"), offenders.map { it.packageName })
        assertEquals(Severity.HIGH, offenders.first().worstSeverity)
        assertEquals(22, offenders.first().penalty)
        assertEquals(BehaviorType.OVERLAY_ABUSE, offenders.first().findings.first().type)
    }

    @Test
    fun `parental settings default to off for a ten year old`() {
        assertEquals(ParentalSettings(enabled = false, childAge = 10), UserSettings().parental)
        assertEquals(17, ParentalSettings(true, 17).childAge)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parental settings reject adults`() {
        ParentalSettings(enabled = true, childAge = 18)
    }

    @Test
    fun `a warning knows when the child is below the platform minimum age`() {
        val app = ConcerningApp(
            "chat",
            setOf("com.chat"),
            ParentalRiskCategory.PRIVATE_MESSAGING,
            minimumAge = 13,
            recommendedAge = 16,
            baseSeverity = Severity.MEDIUM,
            concerns = setOf(ParentalConcern.STRANGER_CONTACT),
        )
        assertTrue(ParentalWarning("com.chat", "Chat", app, Severity.HIGH, childAge = 12, isReviewed = false).isBelowMinimumAge)
        assertFalse(ParentalWarning("com.chat", "Chat", app, Severity.MEDIUM, childAge = 13, isReviewed = false).isBelowMinimumAge)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a concerning app needs a package`() {
        ConcerningApp("x", emptySet(), ParentalRiskCategory.DATING, 18, 18, Severity.HIGH, setOf(ParentalConcern.ADULT_CONTENT))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a concerning app needs a concern`() {
        ConcerningApp("x", setOf("com.x"), ParentalRiskCategory.DATING, 18, 18, Severity.HIGH, emptySet())
    }

    @Test
    fun `sms partner state follows install, signature, opt-in and SMS role`() {
        val connected =
            SmsPartnerStatus("p", verified = true, connected = true, isDefaultSmsApp = true, checkedToday = 1, suspiciousToday = 0)
        assertEquals(SmsPartnerState.CONNECTED, connected.state)
        assertEquals(SmsPartnerState.CONNECTED_NOT_DEFAULT, connected.copy(isDefaultSmsApp = false).state)
        assertEquals(SmsPartnerState.NOT_CONNECTED, connected.copy(connected = false).state)
        assertEquals(SmsPartnerState.UNVERIFIED, connected.copy(verified = false).state)
        assertEquals(SmsPartnerState.NOT_INSTALLED, SmsPartnerStatus.NOT_INSTALLED.state)
    }

    @Test
    fun `scam summary counts texts, links and kinds`() {
        assertEquals(null, ScamMessageSummary.of(emptyList()))
        val summary = ScamMessageSummary.of(
            listOf(
                ScamMessageSignal(1, setOf(ScamMessageKind.URGENCY, ScamMessageKind.SHORTENED_LINK), hasLink = true, threadId = 1),
                ScamMessageSignal(2, setOf(ScamMessageKind.URGENCY), hasLink = false, threadId = null),
            ),
        )
        assertEquals(ScamMessageSummary(2, 1, mapOf(ScamMessageKind.URGENCY to 2, ScamMessageKind.SHORTENED_LINK to 1)), summary)
    }
}
