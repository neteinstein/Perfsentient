package com.neteinstein.perfsentience.core.domain.usecase

import app.cash.turbine.test
import com.neteinstein.perfsentience.core.model.BehaviorType
import com.neteinstein.perfsentience.core.model.Severity
import com.neteinstein.perfsentience.core.model.TrustedApp
import com.neteinstein.perfsentience.core.testing.FakeReportRepository
import com.neteinstein.perfsentience.core.testing.FakeTrustedAppsRepository
import com.neteinstein.perfsentience.core.testing.TestData
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveFlaggedAppsUseCaseTest {
    private val reports = FakeReportRepository()
    private val trusted = FakeTrustedAppsRepository()
    private val observe = ObserveFlaggedAppsUseCase(reports, trusted)

    private val yesterday = LocalDate.of(2026, 11, 15)
    private val today = LocalDate.of(2026, 11, 16)

    @Test
    fun `empty when nothing was flagged and nothing is trusted`() = runTest {
        assertEquals(emptyList<Any>(), observe().first())
    }

    @Test
    fun `lists apps flagged in past reports with last date and days flagged`() = runTest {
        reports.save(TestData.report(yesterday, findings = listOf(TestData.finding("old.ads"), TestData.finding("both"))))
        reports.save(TestData.report(today, findings = listOf(TestData.finding("both"), TestData.finding("new.spam"))))

        val apps = observe().first().associateBy { it.packageName }

        assertEquals(setOf("old.ads", "both", "new.spam"), apps.keys)
        assertEquals(yesterday, apps.getValue("old.ads").lastFlagged)
        assertEquals(today, apps.getValue("both").lastFlagged)
        assertEquals(2, apps.getValue("both").daysFlagged)
        assertEquals(1, apps.getValue("new.spam").daysFlagged)
        assertTrue(apps.values.none { it.isTrusted })
    }

    @Test
    fun `keeps the latest finding per behaviour and sorts them by severity`() = runTest {
        reports.save(
            TestData.report(
                yesterday,
                findings = listOf(TestData.finding("app", BehaviorType.BACKGROUND_DATA, Severity.HIGH)),
            ),
        )
        reports.save(
            TestData.report(
                today,
                findings = listOf(
                    TestData.finding("app", BehaviorType.BACKGROUND_DATA, Severity.LOW),
                    TestData.finding("app", BehaviorType.STORAGE_BLOAT, Severity.MEDIUM),
                ),
            ),
        )

        val app = observe().first().single()

        assertEquals(listOf(BehaviorType.STORAGE_BLOAT, BehaviorType.BACKGROUND_DATA), app.findings.map { it.type })
        assertEquals(Severity.LOW, app.findings.last().severity) // today's finding wins over yesterday's
        assertEquals(Severity.MEDIUM, app.worstSeverity)
    }

    @Test
    fun `trusted apps keep their findings and are listed after untrusted ones`() = runTest {
        reports.save(
            TestData.report(
                today,
                findings = listOf(
                    TestData.finding("a.trusted", severity = Severity.HIGH),
                    TestData.finding("b.minor", severity = Severity.LOW),
                    TestData.finding("c.major", severity = Severity.HIGH),
                ),
            ),
        )
        trusted.trust(TrustedApp("a.trusted", "a.trusted"))

        val apps = observe().first()

        assertEquals(listOf("c.major", "b.minor", "a.trusted"), apps.map { it.packageName })
        assertEquals(listOf(false, false, true), apps.map { it.isTrusted })
        assertEquals(Severity.HIGH, apps.last().worstSeverity)
    }

    @Test
    fun `trusted app without findings in the retained history is still listed`() = runTest {
        trusted.trust(TrustedApp("gone.app", "Gone"))

        val app = observe().first().single()

        assertEquals("Gone", app.appLabel)
        assertTrue(app.isTrusted)
        assertTrue(app.findings.isEmpty())
        assertNull(app.worstSeverity)
        assertNull(app.lastFlagged)
        assertEquals(0, app.daysFlagged)
    }

    @Test
    fun `follows allowlist changes`() = runTest {
        reports.save(TestData.report(today, findings = listOf(TestData.finding("chat"))))
        observe().test {
            assertFalse(awaitItem().single().isTrusted)

            trusted.trust(TrustedApp("chat", "Chat"))
            assertTrue(awaitItem().single().isTrusted)

            trusted.untrust("chat")
            assertFalse(awaitItem().single().isTrusted)
        }
    }
}
