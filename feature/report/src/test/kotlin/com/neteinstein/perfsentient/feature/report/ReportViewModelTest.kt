package com.neteinstein.perfsentient.feature.report

import app.cash.turbine.test
import com.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import com.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import com.neteinstein.perfsentient.core.domain.analysis.DeviceHealthAnalyzer
import com.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import com.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import com.neteinstein.perfsentient.core.domain.usecase.GenerateDailyReportUseCase
import com.neteinstein.perfsentient.core.domain.usecase.MonitoringState
import com.neteinstein.perfsentient.core.domain.usecase.ObserveLatestReportUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveReportHistoryUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.TrustAppUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import com.neteinstein.perfsentient.core.model.BehaviorType
import com.neteinstein.perfsentient.core.model.TrustedApp
import com.neteinstein.perfsentient.core.model.UserSettings
import com.neteinstein.perfsentient.core.testing.FakeAppInventoryRepository
import com.neteinstein.perfsentient.core.testing.FakeNotificationStatsRepository
import com.neteinstein.perfsentient.core.testing.FakePermissionRepository
import com.neteinstein.perfsentient.core.testing.FakeReportRepository
import com.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import com.neteinstein.perfsentient.core.testing.FakeTimeProvider
import com.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import com.neteinstein.perfsentient.core.testing.FakeUsageRepository
import com.neteinstein.perfsentient.core.testing.FakeVitalsRepository
import com.neteinstein.perfsentient.core.testing.MainDispatcherRule
import com.neteinstein.perfsentient.core.testing.TestData
import com.neteinstein.perfsentient.core.testing.TestDispatcherProvider
import com.neteinstein.perfsentient.feature.report.presentation.ReportEvent
import com.neteinstein.perfsentient.feature.report.presentation.ReportViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
        generateDailyReport = GenerateDailyReportUseCase(
            usage,
            FakeAppInventoryRepository(apps = listOf(TestData.app("calc"))),
            FakeVitalsRepository(),
            FakeNotificationStatsRepository(),
            reports,
            trusted,
            BehaviorAnalyzer(thresholds),
            DeviceHealthAnalyzer(thresholds),
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
    fun `failures are surfaced as an event`() = runTest {
        usage.failWith = IllegalStateException("boom")
        val vm = viewModel()
        vm.events.test {
            vm.onAnalyseNow()
            assertEquals(ReportEvent.GenerationFailed, awaitItem())
        }
    }
}
