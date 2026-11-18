package org.neteinstein.perfsentient.feature.parental.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.neteinstein.perfsentient.core.domain.usecase.ObserveParentalScanUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SetParentalAppReviewedUseCase
import org.neteinstein.perfsentient.core.model.ParentalScan
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.ParentalWarning
import org.neteinstein.perfsentient.core.model.Severity

data class ParentalUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = true,
    val childAge: Int = ParentalSettings.DEFAULT_CHILD_AGE,
    /** Concerning apps the parent has not reviewed yet, worst first. */
    val warnings: List<ParentalWarning> = emptyList(),
    /** Concerning apps the parent already reviewed: still listed, so nothing is hidden. */
    val reviewed: List<ParentalWarning> = emptyList(),
) {
    val highRiskCount: Int get() = warnings.count { it.severity == Severity.HIGH }
}

class ParentalViewModel(
    private val observeParentalScan: ObserveParentalScanUseCase,
    private val setParentalAppReviewed: SetParentalAppReviewedUseCase,
) : ViewModel() {
    private val rescans = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ParentalUiState> = rescans
        .flatMapLatest { observeParentalScan() }
        .map { scan -> scan?.toUiState() ?: ParentalUiState(isLoading = false, isEnabled = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ParentalUiState())

    /** Re-reads the installed apps, e.g. when coming back after installing or uninstalling one. */
    fun onRescan() {
        rescans.update { it + 1 }
    }

    /** Moves an app to the Reviewed section (or back to the warnings). */
    fun onReviewedChanged(packageName: String, reviewed: Boolean) {
        viewModelScope.launch { setParentalAppReviewed(packageName, reviewed) }
    }

    private fun ParentalScan.toUiState(): ParentalUiState {
        val (reviewed, open) = warnings.partition { it.isReviewed }
        return ParentalUiState(isLoading = false, isEnabled = true, childAge = childAge, warnings = open, reviewed = reviewed)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
