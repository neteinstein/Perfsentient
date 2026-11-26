package org.neteinstein.perfsentient.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
import org.neteinstein.perfsentient.core.domain.usecase.UpdateThemeModeUseCase
import org.neteinstein.perfsentient.core.model.PermissionState
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.model.UserSettings

data class SettingsUiState(
    val permissions: List<PermissionState> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val monitoringState: MonitoringState = MonitoringState.WAITING_FOR_PERMISSIONS,
    val trustedApps: List<TrustedApp> = emptyList(),
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
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        observePermissions(),
        observeSettings(),
        observeMonitoringState(),
        observeTrustedApps(),
    ) { permissions, settings, monitoring, trusted -> SettingsUiState(permissions, settings, monitoring, trusted) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    /** Re-check grants (the user may have toggled something in system settings) and start monitoring. */
    fun onPermissionsMaybeChanged() {
        viewModelScope.launch { syncMonitoring() }
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

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
