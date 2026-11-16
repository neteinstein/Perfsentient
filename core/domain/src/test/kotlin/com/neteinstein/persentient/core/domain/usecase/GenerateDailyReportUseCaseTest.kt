package com.neteinstein.persentient.core.domain.usecase

import com.neteinstein.persentient.core.domain.analysis.BehaviorAnalyzer
import com.neteinstein.persentient.core.domain.analysis.DetectionThresholds
import com.neteinstein.persentient.core.domain.analysis.DeviceHealthAnalyzer
import com.neteinstein.persentient.core.domain.analysis.HealthScoreCalculator
import com.neteinstein.persentient.core.model.BehaviorType
import com.neteinstein.persentient.core.model.Verdict
import com.neteinstein.persentient.core.testing.FakeAppInventoryRepository
import com.neteinstein.persentient.core.testing.FakeNotificationStatsRepository
import com.neteinstein.persentient.core.testing.FakeReportRepository
import com.neteinstein.persentient.core.testing.FakeTimeProvider
import com.neteinstein.persentient.core.testing.FakeUsageRepository
import com.neteinstein.persentient.core.testing.FakeVitalsRepository
import com.neteinstein.persentient.core.testing.TestData
import com.neteinstein.persentient.core.testing.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateDailyReportUseCaseTest {
    private val time = FakeTimeProvider()
    private val usage = FakeUsageRepository()
    private val inventory = FakeAppInventoryRepository()
    private val vitals = FakeVitalsRepository()
    private val notifications = FakeNotificationStatsRepository()
    private val reports = FakeReportRepository()
    private val thresholds = DetectionThresholds()

    private val useCase = GenerateDailyReportUseCase(
        usageRepository = usage,
        inventoryRepository = inventory,
        vitalsRepository = vitals,
        notificationStatsRepository = notifications,
        reportRepository = reports,
        behaviorAnalyzer = BehaviorAnalyzer(thresholds),
        deviceHealthAnalyzer = DeviceHealthAnalyzer(thresholds),
        scoreCalculator = HealthScoreCalculator(),
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
        notifications.increment("spammer", time.today())
        repeat(150) { notifications.increment("spammer", time.today()) }

        val report = useCase()

        assertTrue(report.score < 80)
        assertEquals(listOf("adware", "spammer"), report.offenders.map { it.packageName })
        assertTrue(report.findings.any { it.type == BehaviorType.NOTIFICATION_SPAM })
        assertTrue(report.findings.any { it.type == BehaviorType.BACKGROUND_DATA })
    }
}
