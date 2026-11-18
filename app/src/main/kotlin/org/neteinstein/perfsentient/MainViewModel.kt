package org.neteinstein.perfsentient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
import org.neteinstein.perfsentient.core.model.UserSettings

/** Navigation tabs, in display order. PARENTAL sits in the middle and only exists while parental mode is on. */
enum class AppTab { REPORT, APPS, PARENTAL, SETTINGS }

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val settings: UserSettings) : MainUiState {
        /** Tabs to show: the Parental help tab is added while parental mode is on. */
        val tabs: List<AppTab>
            get() = AppTab.entries.filter { it != AppTab.PARENTAL || settings.parental.enabled }

        /** [selected] if it is shown, otherwise Settings (e.g. right after parental mode was turned off there). */
        fun visibleTab(selected: AppTab): AppTab = if (selected in tabs) selected else AppTab.SETTINGS
    }
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
