package com.neteinstein.perfsentient.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.perfsentient.core.domain.usecase.MonitoringState
import com.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObservePermissionsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveTrustedAppsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UpdateDynamicColorUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UpdateReportTimeUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UpdateThemeModeUseCase
import com.neteinstein.perfsentient.core.model.PermissionState
import com.neteinstein.perfsentient.core.model.ReportTime
import com.neteinstein.perfsentient.core.model.ThemeMode
import com.neteinstein.perfsentient.core.model.TrustedApp
import com.neteinstein.perfsentient.core.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val permissions: List<PermissionState> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val monitoringState: MonitoringState = MonitoringState.WAITING_FOR_PERMISSIONS,
    val trustedApps: List<TrustedApp> = emptyList(),
) {
    val grantedCount: Int get() = permissions.count { it.isGranted }
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

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
