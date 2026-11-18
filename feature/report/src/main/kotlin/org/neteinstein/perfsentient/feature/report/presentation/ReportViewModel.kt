package org.neteinstein.perfsentient.feature.report.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.neteinstein.perfsentient.core.domain.usecase.GenerateDailyReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.MonitoringState
import org.neteinstein.perfsentient.core.domain.usecase.ObserveLatestReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveReportHistoryUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.TrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.ReportTime

data class ReportUiState(
    val isLoading: Boolean = true,
    val monitoringState: MonitoringState = MonitoringState.WAITING_FOR_PERMISSIONS,
    val report: DailyReport? = null,
    val history: List<DailyReport> = emptyList(),
    val isGenerating: Boolean = false,
    val reportTime: ReportTime = ReportTime.DEFAULT,
)

sealed interface ReportEvent {
    data object GenerationFailed : ReportEvent

    /** Shown as a snackbar with an "Undo" action. */
    data class AppTrusted(val packageName: String, val label: String) : ReportEvent
}

class ReportViewModel(
    observeLatestReport: ObserveLatestReportUseCase,
    observeReportHistory: ObserveReportHistoryUseCase,
    observeMonitoringState: ObserveMonitoringStateUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val generateDailyReport: GenerateDailyReportUseCase,
    private val trustApp: TrustAppUseCase,
    private val untrustApp: UntrustAppUseCase,
) : ViewModel() {

    private val generating = MutableStateFlow(false)
    private val eventChannel = Channel<ReportEvent>(Channel.BUFFERED)
    val events: Flow<ReportEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<ReportUiState> = combine(
        observeLatestReport(),
        observeReportHistory(HISTORY_DAYS),
        observeMonitoringState(),
        observeSettings(),
        generating,
    ) { report, history, monitoring, settings, isGenerating ->
        ReportUiState(
            isLoading = false,
            monitoringState = monitoring,
            report = report,
            history = history.sortedBy { it.date },
            isGenerating = isGenerating,
            reportTime = settings.reportTime,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ReportUiState())

    fun onAnalyseNow() {
        if (generating.value) return
        viewModelScope.launch {
            generating.value = true
            try {
                generateDailyReport()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                eventChannel.send(ReportEvent.GenerationFailed)
            } finally {
                generating.value = false
            }
        }
    }

    /** Adds the app to the allowlist: it disappears from the report and the score is recomputed. */
    fun onTrustApp(packageName: String, label: String) {
        viewModelScope.launch {
            trustApp(packageName, label)
            eventChannel.send(ReportEvent.AppTrusted(packageName, label))
        }
    }

    fun onUndoTrust(packageName: String) {
        viewModelScope.launch { untrustApp(packageName) }
    }

    private companion object {
        const val HISTORY_DAYS = 7
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
