package org.neteinstein.perfsentient.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.DeviceHealthAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import org.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.model.Verdict
import org.neteinstein.perfsentient.core.testing.FakeAppInventoryRepository
import org.neteinstein.perfsentient.core.testing.FakeNotificationStatsRepository
import org.neteinstein.perfsentient.core.testing.FakeReportRepository
import org.neteinstein.perfsentient.core.testing.FakeTimeProvider
import org.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import org.neteinstein.perfsentient.core.testing.FakeUsageRepository
import org.neteinstein.perfsentient.core.testing.FakeVitalsRepository
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.core.testing.TestDispatcherProvider

class GenerateDailyReportUseCaseTest {
    private val time = FakeTimeProvider()
    private val usage = FakeUsageRepository()
    private val inventory = FakeAppInventoryRepository()
    private val vitals = FakeVitalsRepository()
    private val notifications = FakeNotificationStatsRepository()
    private val reports = FakeReportRepository()
    private val trusted = FakeTrustedAppsRepository()
    private val thresholds = DetectionThresholds()
    private val calculator = HealthScoreCalculator()

    private val useCase = GenerateDailyReportUseCase(
        usageRepository = usage,
        inventoryRepository = inventory,
        vitalsRepository = vitals,
        notificationStatsRepository = notifications,
        reportRepository = reports,
        trustedAppsRepository = trusted,
        behaviorAnalyzer = BehaviorAnalyzer(thresholds),
        deviceHealthAnalyzer = DeviceHealthAnalyzer(thresholds),
        scoreCalculator = calculator,
        trustedAppsFilter = TrustedAppsFilter(calculator),
        timeProvider = time,
        dispatchers = TestDispatcherProvider(),
    )

    @Test
    fun `clean device produces a perfect report and persists it`() = runTest {
        inventory.apps = listOf(TestData.app("calculator"))

        val report = useCase()

        assertEquals(100, report.score)
        assertEquals(Verdict.GOOD, report.verdict)
        assertTrue(report.findings.isEmpty())
        assertEquals(time.today(), report.date)
        assertEquals(report, reports.observeLatest().first())
        assertEquals(time.today().minusDays(30), reports.deletedBefore)
    }

    @Test
    fun `misbehaving apps lower the score and show up as offenders`() = runTest {
        inventory.apps = listOf(TestData.app("adware", overlay = true, launcher = false, installer = null), TestData.app("spammer"))
        usage.network = listOf(TestData.network("adware", background = 300L * 1024 * 1024))
        usage.appIssues = List(3) { TestData.anr("spammer", it.toLong()) }
        notifications.increment("spammer", time.today())
        repeat(150) { notifications.increment("spammer", time.today()) }

        val report = useCase()

        assertTrue(report.score < 80)
        assertEquals(listOf("adware", "spammer"), report.offenders.map { it.packageName })
        assertTrue(report.findings.any { it.type == BehaviorType.NOTIFICATION_SPAM })
        assertTrue(report.findings.any { it.type == BehaviorType.BACKGROUND_DATA })
        assertTrue("ANRs reach the detectors", report.findings.any { it.type == BehaviorType.APP_NOT_RESPONDING })
    }

    @Test
    fun `trusted apps are left out of the returned report but kept in storage`() = runTest {
        inventory.apps = listOf(TestData.app("adware", overlay = true, launcher = false, installer = null), TestData.app("spammer"))
        repeat(150) { notifications.increment("spammer", time.today()) }
        trusted.trust(TrustedApp("spammer", "Spammer"))

        val report = useCase()

        assertEquals(listOf("adware"), report.offenders.map { it.packageName })
        assertEquals(1, report.hiddenTrustedApps)
        val stored = reports.observeLatest().first()!!
        assertTrue("raw findings are persisted", stored.findings.any { it.packageName == "spammer" })
        assertTrue(report.score > stored.score)
    }
}
