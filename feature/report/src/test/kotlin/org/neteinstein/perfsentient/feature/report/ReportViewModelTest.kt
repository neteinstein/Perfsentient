package org.neteinstein.perfsentient.feature.report

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.BatteryAttributor
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.DeviceHealthAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import org.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import org.neteinstein.perfsentient.core.domain.usecase.GenerateDailyReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.MonitoringState
import org.neteinstein.perfsentient.core.domain.usecase.ObserveBatteryHistoryUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveLatestReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveReportHistoryUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.TrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import org.neteinstein.perfsentient.core.model.BatterySort
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.testing.FakeAppInventoryRepository
import org.neteinstein.perfsentient.core.testing.FakeNotificationStatsRepository
import org.neteinstein.perfsentient.core.testing.FakePermissionRepository
import org.neteinstein.perfsentient.core.testing.FakeReportRepository
import org.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import org.neteinstein.perfsentient.core.testing.FakeTimeProvider
import org.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import org.neteinstein.perfsentient.core.testing.FakeUsageRepository
import org.neteinstein.perfsentient.core.testing.FakeVitalsRepository
import org.neteinstein.perfsentient.core.testing.MainDispatcherRule
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.core.testing.TestDispatcherProvider
import org.neteinstein.perfsentient.feature.report.presentation.ReportEvent
import org.neteinstein.perfsentient.feature.report.presentation.ReportViewModel

class ReportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reports = FakeReportRepository()
    private val permissions = FakePermissionRepository()
    private val settings = FakeSettingsRepository()
    private val usage = FakeUsageRepository()
    private val trusted = FakeTrustedAppsRepository()
    private val thresholds = DetectionThresholds()
    private val filter = TrustedAppsFilter(HealthScoreCalculator())

    private fun viewModel() = ReportViewModel(
        observeLatestReport = ObserveLatestReportUseCase(reports, trusted, filter),
        observeReportHistory = ObserveReportHistoryUseCase(reports, trusted, filter),
        observeMonitoringState = ObserveMonitoringStateUseCase(permissions, settings),
        observeSettings = ObserveSettingsUseCase(settings),
        observeBatteryHistory = ObserveBatteryHistoryUseCase(reports, FakeTimeProvider(), thresholds),
        generateDailyReport = GenerateDailyReportUseCase(
            usage,
            FakeAppInventoryRepository(apps = listOf(TestData.app("calc"))),
            FakeVitalsRepository(),
            FakeNotificationStatsRepository(),
            reports,
            trusted,
            BehaviorAnalyzer(thresholds),
            DeviceHealthAnalyzer(thresholds),
            BatteryAttributor(thresholds),
            HealthScoreCalculator(),
            filter,
            FakeTimeProvider(),
            TestDispatcherProvider(mainDispatcherRule.dispatcher),
        ),
        trustApp = TrustAppUseCase(trusted),
        untrustApp = UntrustAppUseCase(trusted),
    )

    @Test
    fun `waits for permissions when monitoring is off`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(MonitoringState.WAITING_FOR_PERMISSIONS, state.monitoringState)
        }
    }

    @Test
    fun `analyse now generates and exposes a report with sorted history`() = runTest {
        permissions.grantAll()
        settings.state.value = UserSettings(monitoringEnabled = true)
        reports.save(TestData.report(date = java.time.LocalDate.of(2026, 11, 14), score = 40))
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            vm.onAnalyseNow()
            val state = expectMostRecentItem()
            assertEquals(MonitoringState.ACTIVE, state.monitoringState)
            assertNotNull(state.report)
            assertEquals(100, state.report!!.score)
            assertEquals(listOf(40, 100), state.history.map { it.score })
            assertFalse(state.isGenerating)
        }
    }

    @Test
    fun `trusting an app hides it, re-scores the report and can be undone`() = runTest {
        val flagged = TestData.finding("adware", BehaviorType.OVERLAY_ABUSE)
        reports.save(TestData.report(score = 93, findings = listOf(flagged)))
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(listOf("adware"), expectMostRecentItem().report!!.offenders.map { it.packageName })

            vm.events.test {
                vm.onTrustApp("adware", "Adware")
                assertEquals(ReportEvent.AppTrusted("adware", "Adware"), awaitItem())
            }
            assertEquals(listOf(TrustedApp("adware", "Adware")), trusted.state.value)
            val hidden = expectMostRecentItem().report!!
            assertTrue(hidden.offenders.isEmpty())
            assertEquals(1, hidden.hiddenTrustedApps)
            assertEquals(100, hidden.score)

            vm.onUndoTrust("adware")
            val restored = expectMostRecentItem().report!!
            assertEquals(listOf("adware"), restored.offenders.map { it.packageName })
            assertEquals(0, restored.hiddenTrustedApps)
            assertEquals(93, restored.score)
        }
    }

    @Test
    fun `battery history is exposed and re-sorted on request`() = runTest {
        reports.save(
            TestData.report(
                battery = TestData.battery(
                    TestData.batteryUsage("heavy", drain = 20f, foregroundMs = 10 * HOUR),
                    TestData.batteryUsage("intense", drain = 5f, foregroundMs = HOUR / 2),
                ),
            ),
        )
        val vm = viewModel()
        vm.uiState.test {
            val initial = expectMostRecentItem()
            assertEquals(BatterySort.MOST_BATTERY, initial.batterySort)
            assertEquals(listOf("heavy", "intense"), initial.battery.apps.map { it.packageName })

            vm.onBatterySortChange(BatterySort.PER_HOUR_OPEN)
            val sorted = expectMostRecentItem()
            assertEquals(BatterySort.PER_HOUR_OPEN, sorted.batterySort)
            assertEquals(listOf("intense", "heavy"), sorted.battery.apps.map { it.packageName })
        }
    }

    @Test
    fun `failures are surfaced as an event`() = runTest {
        usage.failWith = IllegalStateException("boom")
        val vm = viewModel()
        vm.events.test {
            vm.onAnalyseNow()
            assertEquals(ReportEvent.GenerationFailed, awaitItem())
        }
    }
}

private const val HOUR = 3_600_000L
