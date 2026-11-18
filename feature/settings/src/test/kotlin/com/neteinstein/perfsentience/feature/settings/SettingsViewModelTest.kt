package com.neteinstein.perfsentience.feature.settings

import app.cash.turbine.test
import com.neteinstein.perfsentience.core.domain.usecase.MonitoringState
import com.neteinstein.perfsentience.core.domain.usecase.ObserveMonitoringStateUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObservePermissionsUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObserveTrustedAppsUseCase
import com.neteinstein.perfsentience.core.domain.usecase.SyncMonitoringUseCase
import com.neteinstein.perfsentience.core.domain.usecase.UntrustAppUseCase
import com.neteinstein.perfsentience.core.domain.usecase.UpdateDynamicColorUseCase
import com.neteinstein.perfsentience.core.domain.usecase.UpdateReportTimeUseCase
import com.neteinstein.perfsentience.core.domain.usecase.UpdateThemeModeUseCase
import com.neteinstein.perfsentience.core.model.PermissionType
import com.neteinstein.perfsentience.core.model.ReportTime
import com.neteinstein.perfsentience.core.model.ThemeMode
import com.neteinstein.perfsentience.core.model.TrustedApp
import com.neteinstein.perfsentience.core.testing.FakeMonitoringScheduler
import com.neteinstein.perfsentience.core.testing.FakePermissionRepository
import com.neteinstein.perfsentience.core.testing.FakeSettingsRepository
import com.neteinstein.perfsentience.core.testing.FakeTrustedAppsRepository
import com.neteinstein.perfsentience.core.testing.MainDispatcherRule
import com.neteinstein.perfsentience.feature.settings.presentation.SettingsViewModel
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
