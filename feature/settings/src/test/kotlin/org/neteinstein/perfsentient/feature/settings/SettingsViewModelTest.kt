package org.neteinstein.perfsentient.feature.settings

import app.cash.turbine.test
import org.neteinstein.perfsentient.core.domain.usecase.MonitoringState
import org.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObservePermissionsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveTrustedAppsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateDynamicColorUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateReportTimeUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateThemeModeUseCase
import org.neteinstein.perfsentient.core.model.PermissionType
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.testing.FakeMonitoringScheduler
import org.neteinstein.perfsentient.core.testing.FakePermissionRepository
import org.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import org.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import org.neteinstein.perfsentient.core.testing.MainDispatcherRule
import org.neteinstein.perfsentient.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val permissions = FakePermissionRepository()
    private val settings = FakeSettingsRepository()
    private val scheduler = FakeMonitoringScheduler()
    private val trusted = FakeTrustedAppsRepository(
        listOf(TrustedApp("com.zeta.vpn", "Zeta VPN"), TrustedApp("com.alpha.chat", "alpha chat")),
    )

    // Lazy: must be created after MainDispatcherRule installed the test Main dispatcher.
    private val viewModel by lazy {
        SettingsViewModel(
            observePermissions = ObservePermissionsUseCase(permissions),
            observeSettings = ObserveSettingsUseCase(settings),
            observeMonitoringState = ObserveMonitoringStateUseCase(permissions, settings),
            observeTrustedApps = ObserveTrustedAppsUseCase(trusted),
            syncMonitoring = SyncMonitoringUseCase(permissions, settings, scheduler),
            updateThemeMode = UpdateThemeModeUseCase(settings),
            updateDynamicColor = UpdateDynamicColorUseCase(settings),
            updateReportTime = UpdateReportTimeUseCase(settings, scheduler),
            untrustApp = UntrustAppUseCase(trusted),
        )
    }

    @Test
    fun `granting permissions one by one starts monitoring only at the end`() = runTest {
        viewModel.uiState.test {
            permissions.systemGrants[PermissionType.USAGE_ACCESS] = true
            viewModel.onPermissionsMaybeChanged()
            assertEquals(MonitoringState.WAITING_FOR_PERMISSIONS, expectMostRecentItem().monitoringState)
            assertTrue(scheduler.ensured.isEmpty())

            PermissionType.entries.forEach { permissions.systemGrants[it] = true }
            viewModel.onPermissionsMaybeChanged()
            val state = expectMostRecentItem()
            assertEquals(MonitoringState.ACTIVE, state.monitoringState)
            assertEquals(PermissionType.entries.size, state.grantedCount)
            assertEquals(1, scheduler.ensured.size)
        }
    }

    @Test
    fun `appearance and report time changes are persisted`() = runTest {
        viewModel.uiState.test {
            viewModel.onThemeModeSelected(ThemeMode.LIGHT)
            viewModel.onDynamicColorToggled(true)
            viewModel.onReportTimeSelected(23, 5)
            val state = expectMostRecentItem()
            assertEquals(ThemeMode.LIGHT, state.settings.themeMode)
            assertTrue(state.settings.useDynamicColor)
            assertEquals(ReportTime(23, 5), state.settings.reportTime)
        }
    }

    @Test
    fun `trusted apps are listed by label and can be removed`() = runTest {
        viewModel.uiState.test {
            assertEquals(listOf("alpha chat", "Zeta VPN"), expectMostRecentItem().trustedApps.map { it.label })

            viewModel.onUntrustApp("com.zeta.vpn")
            assertEquals(listOf("com.alpha.chat"), expectMostRecentItem().trustedApps.map { it.packageName })
        }
    }
}
