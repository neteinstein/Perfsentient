package com.neteinstein.perfsentience

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neteinstein.perfsentience.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.perfsentience.core.domain.usecase.SyncMonitoringUseCase
import com.neteinstein.perfsentience.core.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab { REPORT, APPS, SETTINGS }

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val settings: UserSettings) : MainUiState
}

class MainViewModel(
    private val savedStateHandle: SavedStateHandle,
    observeSettings: ObserveSettingsUseCase,
    private val syncMonitoring: SyncMonitoringUseCase,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = observeSettings()
        .map<UserSettings, MainUiState> { MainUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MainUiState.Loading)

    val selectedTab: StateFlow<AppTab> = savedStateHandle.getStateFlow(KEY_TAB, AppTab.REPORT)

    fun selectTab(tab: AppTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    /** Keeps scheduling in sync with the permission state every time the app comes to front. */
    fun onAppResumed() {
        viewModelScope.launch { syncMonitoring() }
    }

    private companion object {
        const val KEY_TAB = "selected_tab"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
