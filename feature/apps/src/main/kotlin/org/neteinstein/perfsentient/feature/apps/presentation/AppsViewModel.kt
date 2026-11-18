package org.neteinstein.perfsentient.feature.apps.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.neteinstein.perfsentient.core.domain.usecase.ObserveFlaggedAppsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.TrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import org.neteinstein.perfsentient.core.model.FlaggedApp

data class AppsUiState(
    val isLoading: Boolean = true,
    val apps: List<FlaggedApp> = emptyList(),
)

class AppsViewModel(
    observeFlaggedApps: ObserveFlaggedAppsUseCase,
    private val trustApp: TrustAppUseCase,
    private val untrustApp: UntrustAppUseCase,
) : ViewModel() {

    val uiState: StateFlow<AppsUiState> = observeFlaggedApps()
        .map { AppsUiState(isLoading = false, apps = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppsUiState())

    /** Adds the app to the allowlist: it is left out of reports and the score. */
    fun onTrustApp(packageName: String, label: String) {
        viewModelScope.launch { trustApp(packageName, label) }
    }

    /** Removes the app from the allowlist: its findings show up in reports again. */
    fun onUntrustApp(packageName: String) {
        viewModelScope.launch { untrustApp(packageName) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
