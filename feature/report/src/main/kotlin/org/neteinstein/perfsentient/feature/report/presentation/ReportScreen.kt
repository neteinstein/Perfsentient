package org.neteinstein.perfsentient.feature.report.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.neteinstein.perfsentient.core.designsystem.component.SectionHeader
import org.neteinstein.perfsentient.core.designsystem.component.staggeredEntrance
import org.neteinstein.perfsentient.core.domain.usecase.MonitoringState
import org.neteinstein.perfsentient.feature.report.R
import org.neteinstein.perfsentient.feature.report.presentation.component.AllClearCard
import org.neteinstein.perfsentient.feature.report.presentation.component.AppOffenderCard
import org.neteinstein.perfsentient.feature.report.presentation.component.DeviceIssueCard
import org.neteinstein.perfsentient.feature.report.presentation.component.EmptyReportCard
import org.neteinstein.perfsentient.feature.report.presentation.component.HistoryCard
import org.neteinstein.perfsentient.feature.report.presentation.component.OffenderDetailsSheet
import org.neteinstein.perfsentient.feature.report.presentation.component.ReportHeader
import org.neteinstein.perfsentient.feature.report.presentation.component.SetupCard
import org.neteinstein.perfsentient.feature.report.presentation.component.TrustedAppsHiddenNote
import org.neteinstein.perfsentient.feature.report.presentation.component.VitalsGrid
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReportRoute(onOpenSettings: () -> Unit, modifier: Modifier = Modifier, viewModel: ReportViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReportEvent.GenerationFailed -> snackbar.showSnackbar(resources.getString(R.string.report_generation_failed))

                is ReportEvent.AppTrusted -> {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.report_trusted_message, event.label),
                        actionLabel = resources.getString(R.string.report_trusted_undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoTrust(event.packageName)
                }
            }
        }
    }
    Box(modifier.fillMaxSize()) {
        ReportScreen(
            state = state,
            onAnalyseNow = viewModel::onAnalyseNow,
            onOpenSettings = onOpenSettings,
            onTrustApp = viewModel::onTrustApp,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
internal fun ReportScreen(
    state: ReportUiState,
    onAnalyseNow: () -> Unit,
    onOpenSettings: () -> Unit,
    onTrustApp: (packageName: String, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state.isLoading,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "reportLoading",
        modifier = modifier,
    ) { loading ->
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            ReportContent(state, onAnalyseNow, onOpenSettings, onTrustApp)
        }
    }
}

@Composable
private fun ReportContent(
    state: ReportUiState,
    onAnalyseNow: () -> Unit,
    onOpenSettings: () -> Unit,
    onTrustApp: (packageName: String, label: String) -> Unit,
) {
    val report = state.report
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues()
    val horizontal = PaddingValues(
        start = insets.calculateStartPadding(layoutDirection) + 20.dp,
        end = insets.calculateEndPadding(layoutDirection) + 20.dp,
    )
    val itemModifier = Modifier.padding(horizontal)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            ReportHeader(
                report = report,
                isGenerating = state.isGenerating,
                canAnalyse = state.monitoringState == MonitoringState.ACTIVE,
                onAnalyseNow = onAnalyseNow,
                topInset = insets.calculateTopPadding(),
                horizontalPadding = horizontal,
            )
        }
        if (state.monitoringState != MonitoringState.ACTIVE) {
            item(key = "setup") { SetupCard(onOpenSettings, itemModifier.staggeredEntrance(0)) }
        } else if (report == null) {
            item(key = "empty") { EmptyReportCard(state.reportTime, state.isGenerating, onAnalyseNow, itemModifier.staggeredEntrance(0)) }
        }
        if (report != null) {
            val offenders = report.offenders
            if (report.deviceIssues.isNotEmpty()) {
                item(key = "issues_header") { SectionHeader(stringResource(R.string.report_section_issues), itemModifier) }
                itemsIndexed(report.deviceIssues, key = { _, it -> "issue_${it.type}" }) { index, issue ->
                    DeviceIssueCard(issue, itemModifier.staggeredEntrance(index))
                }
            }
            item(key = "offenders_header") {
                SectionHeader(
                    title = stringResource(R.string.report_section_offenders),
                    subtitle = if (offenders.isEmpty()) {
                        stringResource(R.string.report_summary_clean)
                    } else {
                        pluralStringResource(R.plurals.report_summary_offenders, offenders.size, offenders.size)
                    },
                    modifier = itemModifier,
                )
            }
            if (offenders.isEmpty()) {
                item(key = "all_clear") { AllClearCard(itemModifier.staggeredEntrance(1)) }
            } else {
                itemsIndexed(offenders, key = { _, it -> "app_${it.packageName}" }) { index, assessment ->
                    AppOffenderCard(
                        assessment = assessment,
                        onClick = { selectedPackage = assessment.packageName },
                        modifier = itemModifier.animateItem().staggeredEntrance(index + 1),
                    )
                }
            }
            if (report.hiddenTrustedApps > 0) {
                item(key = "trusted_hidden") { TrustedAppsHiddenNote(report.hiddenTrustedApps, onOpenSettings, itemModifier) }
            }
            item(key = "vitals_header") { SectionHeader(stringResource(R.string.report_section_vitals), itemModifier) }
            item(key = "vitals") { VitalsGrid(report, itemModifier) }
            if (state.history.size > 1) {
                item(key = "history_header") { SectionHeader(stringResource(R.string.report_section_history), itemModifier) }
                item(key = "history") { HistoryCard(state.history, itemModifier) }
            }
        }
    }

    val selected = report?.offenders?.firstOrNull { it.packageName == selectedPackage }
    if (selected != null) {
        OffenderDetailsSheet(
            assessment = selected,
            onTrust = {
                selectedPackage = null
                onTrustApp(selected.packageName, selected.appLabel)
            },
            onDismiss = { selectedPackage = null },
        )
    }
}
