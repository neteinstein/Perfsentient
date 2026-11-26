package org.neteinstein.perfsentient.feature.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.usecase.GetSmsPartnerStatusUseCase
import org.neteinstein.perfsentient.core.domain.usecase.MonitoringState
import org.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObservePermissionsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveTrustedAppsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateDynamicColorUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateParentalSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateReportTimeUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateSecurityAlertsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateThemeModeUseCase
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.PermissionType
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.SmsPartnerState
import org.neteinstein.perfsentient.core.model.SmsPartnerStatus
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.testing.FakeMonitoringScheduler
import org.neteinstein.perfsentient.core.testing.FakePermissionRepository
import org.neteinstein.perfsentient.core.testing.FakeScamMessageRepository
import org.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import org.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import org.neteinstein.perfsentient.core.testing.MainDispatcherRule
import org.neteinstein.perfsentient.feature.settings.presentation.SettingsViewModel

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val permissions = FakePermissionRepository()
    private val settings = FakeSettingsRepository()
    private val scheduler = FakeMonitoringScheduler()
    private val partner = FakeScamMessageRepository()
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
            updateParentalSettings = UpdateParentalSettingsUseCase(settings),
            updateSecurityAlerts = UpdateSecurityAlertsUseCase(settings),
            getSmsPartnerStatus = GetSmsPartnerStatusUseCase(partner),
        )
    }

    @Test
    fun `sms toolbox status is read again on resume`() = runTest {
        viewModel.uiState.test {
            assertEquals(SmsPartnerState.NOT_INSTALLED, expectMostRecentItem().partner.state)
            partner.status = SmsPartnerStatus("com.neteinstein.smsredirect", true, true, true, 4, 1)
            viewModel.onPermissionsMaybeChanged()
            assertEquals(SmsPartnerState.CONNECTED, expectMostRecentItem().partner.state)
        }
    }

    @Test
    fun `security alerts are on by default and can be turned off`() = runTest {
        viewModel.uiState.test {
            assertTrue(expectMostRecentItem().settings.securityAlertsEnabled)
            viewModel.onSecurityAlertsToggled(false)
            assertEquals(false, expectMostRecentItem().settings.securityAlertsEnabled)
        }
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

    @Test
    fun `parental mode is turned on with an age, re-aged and turned off`() = runTest {
        viewModel.uiState.test {
            assertEquals(ParentalSettings(), expectMostRecentItem().settings.parental)

            viewModel.onParentalModeChanged(enabled = true, childAge = 11)
            assertEquals(ParentalSettings(enabled = true, childAge = 11), expectMostRecentItem().settings.parental)

            viewModel.onParentalModeChanged(enabled = true, childAge = 14)
            assertEquals(14, expectMostRecentItem().settings.parental.childAge)

            viewModel.onParentalModeChanged(enabled = false, childAge = 14)
            assertEquals(ParentalSettings(enabled = false, childAge = 14), expectMostRecentItem().settings.parental)
        }
    }
}
