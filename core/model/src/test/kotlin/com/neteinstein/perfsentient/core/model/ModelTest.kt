package com.neteinstein.perfsentient.core.model

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
}
