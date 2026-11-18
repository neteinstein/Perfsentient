package org.neteinstein.perfsentient.core.domain.usecase

import app.cash.turbine.test
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import org.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.testing.FakeReportRepository
import org.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import org.neteinstein.perfsentient.core.testing.TestData

class TrustedAppsUseCasesTest {
    private val trusted = FakeTrustedAppsRepository()
    private val reports = FakeReportRepository()
    private val filter = TrustedAppsFilter(HealthScoreCalculator())

    @Test
    fun `trusting and untrusting updates the allowlist sorted by label`() = runTest {
        val observe = ObserveTrustedAppsUseCase(trusted)
        val trust = TrustAppUseCase(trusted)
        observe().test {
            assertEquals(emptyList<TrustedApp>(), awaitItem())

            trust("com.zeta", "Zeta")
            trust("com.alpha", "alpha")
            trust("com.beta", "Alpha") // same label, ordered by package name
            assertEquals(listOf("com.alpha", "com.beta", "com.zeta"), expectMostRecentItem().map { it.packageName })

            trust("com.zeta", "Zeta 2") // re-trusting refreshes the label instead of duplicating
            UntrustAppUseCase(trusted)("com.alpha")
            assertEquals(listOf(TrustedApp("com.beta", "Alpha"), TrustedApp("com.zeta", "Zeta 2")), expectMostRecentItem())
        }
    }

    @Test
    fun `latest report hides trusted apps and follows allowlist changes`() = runTest {
        val observe = ObserveLatestReportUseCase(reports, trusted, filter)
        assertNull(observe().first())

        reports.save(TestData.report(findings = listOf(TestData.finding("adware"), TestData.finding("chat"))))
        observe().test {
            assertEquals(listOf("adware", "chat"), awaitItem()!!.offenders.map { it.packageName }.sorted())

            trusted.trust(TrustedApp("chat", "Chat"))
            val filtered = awaitItem()!!
            assertEquals(listOf("adware"), filtered.offenders.map { it.packageName })
            assertEquals(1, filtered.hiddenTrustedApps)

            trusted.untrust("chat")
            assertEquals(0, awaitItem()!!.hiddenTrustedApps)
        }
    }

    @Test
    fun `history is re-scored without trusted apps`() = runTest {
        val noisy = TestData.finding("chat", BehaviorType.NOTIFICATION_SPAM)
        reports.save(TestData.report(date = LocalDate.of(2026, 11, 15), score = 93, findings = listOf(noisy)))
        reports.save(TestData.report(date = LocalDate.of(2026, 11, 16), score = 100))
        trusted.trust(TrustedApp("chat", "Chat"))

        val history = ObserveReportHistoryUseCase(reports, trusted, filter)(days = 7).first()

        assertEquals(listOf(100, 100), history.map { it.score })
        assertEquals(listOf(0, 1), history.map { it.hiddenTrustedApps })
    }
}
