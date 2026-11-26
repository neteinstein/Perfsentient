package org.neteinstein.perfsentient.core.domain.analysis

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.DeviceSecurityPosture
import org.neteinstein.perfsentient.core.model.Severity

class DeviceSecurityAnalyzerTest {
    private val analyzer = DeviceSecurityAnalyzer(DetectionThresholds())
    private val today = LocalDate.of(2026, 11, 16)

    private fun patchedDaysAgo(days: Long) = DeviceSecurityPosture(
        hasSecureLock = true,
        securityPatch = today.minusDays(days),
        adbEnabled = false,
    )

    @Test
    fun `a locked, patched phone has no issues`() {
        assertTrue(analyzer.issues(patchedDaysAgo(30), today).isEmpty())
        assertTrue(analyzer.issues(DeviceSecurityPosture.UNKNOWN, today).isEmpty())
    }

    @Test
    fun `an old security patch is low after 90 days and medium after 180`() {
        assertTrue(analyzer.issues(patchedDaysAgo(89), today).isEmpty())
        assertEquals(
            listOf(DeviceIssue(DeviceIssueType.OUTDATED_SECURITY_PATCH, Severity.LOW, 90)),
            analyzer.issues(patchedDaysAgo(90), today),
        )
        assertEquals(
            listOf(DeviceIssue(DeviceIssueType.OUTDATED_SECURITY_PATCH, Severity.MEDIUM, 400)),
            analyzer.issues(patchedDaysAgo(400), today),
        )
    }

    @Test
    fun `no screen lock is medium and usb debugging is low`() {
        val issues = analyzer.issues(DeviceSecurityPosture(hasSecureLock = false, securityPatch = null, adbEnabled = true), today)
        assertEquals(
            listOf(
                DeviceIssue(DeviceIssueType.NO_SCREEN_LOCK, Severity.MEDIUM, 1),
                DeviceIssue(DeviceIssueType.USB_DEBUGGING, Severity.LOW, 1),
            ),
            issues,
        )
    }
}
