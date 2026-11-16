package com.neteinstein.persentient.feature.report

import app.cash.turbine.test
import com.neteinstein.persentient.core.domain.analysis.BehaviorAnalyzer
import com.neteinstein.persentient.core.domain.analysis.DetectionThresholds
import com.neteinstein.persentient.core.domain.analysis.DeviceHealthAnalyzer
import com.neteinstein.persentient.core.domain.analysis.HealthScoreCalculator
import com.neteinstein.persentient.core.domain.usecase.GenerateDailyReportUseCase
import com.neteinstein.persentient.core.domain.usecase.MonitoringState
import com.neteinstein.persentient.core.domain.usecase.ObserveLatestReportUseCase
import com.neteinstein.persentient.core.domain.usecase.ObserveMonitoringStateUseCase
import com.neteinstein.persentient.core.domain.usecase.ObserveReportHistoryUseCase
import com.neteinstein.persentient.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.persentient.core.model.UserSettings
import com.neteinstein.persentient.core.testing.FakeAppInventoryRepository
import com.neteinstein.persentient.core.testing.FakeNotificationStatsRepository
import com.neteinstein.persentient.core.testing.FakePermissionRepository
import com.neteinstein.persentient.core.testing.FakeReportRepository
import com.neteinstein.persentient.core.testing.FakeSettingsRepository
import com.neteinstein.persentient.core.testing.FakeTimeProvider
import com.neteinstein.persentient.core.testing.FakeUsageRepository
import com.neteinstein.persentient.core.testing.FakeVitalsRepository
import com.neteinstein.persentient.core.testing.MainDispatcherRule
import com.neteinstein.persentient.core.testing.TestData
import com.neteinstein.persentient.core.testing.TestDispatcherProvider
import com.neteinstein.persentient.feature.report.presentation.ReportEvent
import com.neteinstein.persentient.feature.report.presentation.ReportViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class ReportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reports = FakeReportRepository()
    private val permissions = FakePermissionRepository()
    private val settings = FakeSettingsRepository()
    private val usage = FakeUsageRepository()
    private val thresholds = DetectionThresholds()

    private fun viewModel() = ReportViewModel(
        observeLatestReport = ObserveLatestReportUseCase(reports),
        observeReportHistory = ObserveReportHistoryUseCase(reports),
        observeMonitoringState = ObserveMonitoringStateUseCase(permissions, settings),
        observeSettings = ObserveSettingsUseCase(settings),
        generateDailyReport = GenerateDailyReportUseCase(
            usage,
            FakeAppInventoryRepository(apps = listOf(TestData.app("calc"))),
            FakeVitalsRepository(),
            FakeNotificationStatsRepository(),
            reports,
            BehaviorAnalyzer(thresholds),
            DeviceHealthAnalyzer(thresholds),
            HealthScoreCalculator(),
            FakeTimeProvider(),
            TestDispatcherProvider(mainDispatcherRule.dispatcher),
        ),
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
    fun `failures are surfaced as an event`() = runTest {
        usage.failWith = IllegalStateException("boom")
        val vm = viewModel()
        vm.events.test {
            vm.onAnalyseNow()
            assertEquals(ReportEvent.GenerationFailed, awaitItem())
        }
    }
}
