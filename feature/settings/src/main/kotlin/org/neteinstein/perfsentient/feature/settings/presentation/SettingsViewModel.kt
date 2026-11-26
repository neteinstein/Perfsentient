package org.neteinstein.perfsentient.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
import org.neteinstein.perfsentient.core.model.PermissionState
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.SmsPartnerStatus
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.model.UserSettings

data class SettingsUiState(
    val permissions: List<PermissionState> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val monitoringState: MonitoringState = MonitoringState.WAITING_FOR_PERMISSIONS,
    val trustedApps: List<TrustedApp> = emptyList(),
    /** SMS Toolbox, for the "Scam text warnings" card. Read again on every resume. */
    val partner: SmsPartnerStatus = SmsPartnerStatus.NOT_INSTALLED,
) {
    /** Permissions monitoring waits for; optional extras (e.g. system-log access) are left out. */
    val requiredPermissions: List<PermissionState> get() = permissions.filter { it.isRequired }

    val grantedCount: Int get() = requiredPermissions.count { it.isGranted }
}

class SettingsViewModel(
    observePermissions: ObservePermissionsUseCase,
    observeSettings: ObserveSettingsUseCase,
    observeMonitoringState: ObserveMonitoringStateUseCase,
    observeTrustedApps: ObserveTrustedAppsUseCase,
    private val syncMonitoring: SyncMonitoringUseCase,
    private val updateThemeMode: UpdateThemeModeUseCase,
    private val updateDynamicColor: UpdateDynamicColorUseCase,
    private val updateReportTime: UpdateReportTimeUseCase,
    private val untrustApp: UntrustAppUseCase,
    private val updateParentalSettings: UpdateParentalSettingsUseCase,
    private val updateSecurityAlerts: UpdateSecurityAlertsUseCase,
    private val getSmsPartnerStatus: GetSmsPartnerStatusUseCase,
) : ViewModel() {

    private val partner = MutableStateFlow(SmsPartnerStatus.NOT_INSTALLED)

    val uiState: StateFlow<SettingsUiState> = combine(
        observePermissions(),
        observeSettings(),
        observeMonitoringState(),
        observeTrustedApps(),
        partner,
    ) { permissions, settings, monitoring, trusted, partner -> SettingsUiState(permissions, settings, monitoring, trusted, partner) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    /**
     * Re-check grants (the user may have toggled something in system settings) and start monitoring.
     * Also re-reads SMS Toolbox, which the user may just have installed or connected.
     */
    fun onPermissionsMaybeChanged() {
        viewModelScope.launch { syncMonitoring() }
        viewModelScope.launch { partner.value = getSmsPartnerStatus() }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { updateThemeMode(mode) }
    }

    fun onDynamicColorToggled(enabled: Boolean) {
        viewModelScope.launch { updateDynamicColor(enabled) }
    }

    fun onReportTimeSelected(hour: Int, minute: Int) {
        viewModelScope.launch { updateReportTime(ReportTime(hour, minute)) }
    }

    /** Removes the app from the allowlist: its findings show up in reports again. */
    fun onUntrustApp(packageName: String) {
        viewModelScope.launch { untrustApp(packageName) }
    }

    /** Turns parental mode on or off; [childAge] is the age installed apps are checked against. */
    fun onParentalModeChanged(enabled: Boolean, childAge: Int) {
        viewModelScope.launch { updateParentalSettings(enabled, childAge) }
    }

    /** Live security alerts: a notification as soon as a risky app is installed. */
    fun onSecurityAlertsToggled(enabled: Boolean) {
        viewModelScope.launch { updateSecurityAlerts(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
