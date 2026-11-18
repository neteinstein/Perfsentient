package org.neteinstein.perfsentient.feature.parental.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.neteinstein.perfsentient.core.designsystem.component.SectionHeader
import org.neteinstein.perfsentient.core.designsystem.component.staggeredEntrance
import org.neteinstein.perfsentient.feature.parental.R
import org.neteinstein.perfsentient.feature.parental.presentation.component.GuidanceCard
import org.neteinstein.perfsentient.feature.parental.presentation.component.ParentalDisabledCard
import org.neteinstein.perfsentient.feature.parental.presentation.component.ParentalSummaryCard
import org.neteinstein.perfsentient.feature.parental.presentation.component.ParentalWarningCard
import org.neteinstein.perfsentient.feature.parental.presentation.component.ParentalWarningSheet

@Composable
fun ParentalRoute(onOpenSettings: () -> Unit, modifier: Modifier = Modifier, viewModel: ParentalViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Apps may have been installed or removed while we were in the background.
    LifecycleResumeEffect(Unit) {
        viewModel.onRescan()
        onPauseOrDispose { }
    }
    ParentalScreen(
        state = state,
        onReviewedChanged = viewModel::onReviewedChanged,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
internal fun ParentalScreen(
    state: ParentalUiState,
    onReviewedChanged: (packageName: String, reviewed: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val context = LocalContext.current
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = insets.calculateStartPadding(layoutDirection) + 20.dp,
            end = insets.calculateEndPadding(layoutDirection) + 20.dp,
            top = insets.calculateTopPadding() + 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            SectionHeader(title = stringResource(R.string.parental_title), subtitle = stringResource(R.string.parental_subtitle))
        }
        if (!state.isEnabled) {
            item(key = "disabled") { ParentalDisabledCard(onOpenSettings = onOpenSettings) }
            return@LazyColumn
        }
        item(key = "summary") {
            ParentalSummaryCard(state = state, onChangeAge = onOpenSettings, modifier = Modifier.staggeredEntrance(0))
        }
        if (state.warnings.isNotEmpty()) {
            item(key = "warnings_header") {
                SectionHeader(
                    title = stringResource(R.string.parental_section_warnings),
                    subtitle = stringResource(R.string.parental_section_warnings_desc),
                )
            }
            itemsIndexed(state.warnings, key = { _, it -> it.packageName }) { index, warning ->
                ParentalWarningCard(
                    warning = warning,
                    onClick = { selectedPackage = warning.packageName },
                    modifier = Modifier.animateItem().staggeredEntrance(index + 1),
                )
            }
        }
        if (state.reviewed.isNotEmpty()) {
            item(key = "reviewed_header") {
                SectionHeader(
                    title = stringResource(R.string.parental_section_reviewed),
                    subtitle = stringResource(R.string.parental_section_reviewed_desc),
                )
            }
            itemsIndexed(state.reviewed, key = { _, it -> it.packageName }) { _, warning ->
                ParentalWarningCard(warning = warning, onClick = {
                    selectedPackage = warning.packageName
                }, modifier = Modifier.animateItem())
            }
        }
        item(key = "signs") {
            GuidanceCard(
                Icons.Rounded.Visibility,
                stringResource(R.string.parental_signs_title),
                stringResource(R.string.parental_signs_text),
            )
        }
        item(key = "help") {
            GuidanceCard(
                Icons.Rounded.SupportAgent,
                stringResource(R.string.parental_help_title),
                stringResource(R.string.parental_help_text),
            )
        }
        item(key = "about") {
            GuidanceCard(Icons.Rounded.Info, stringResource(R.string.parental_about_title), stringResource(R.string.parental_about_text))
        }
    }

    // Looked up by package so the sheet follows live state (e.g. right after marking it reviewed).
    (state.warnings + state.reviewed).firstOrNull { it.packageName == selectedPackage }?.let { selected ->
        ParentalWarningSheet(
            warning = selected,
            onOpenAppInfo = { context.openAppInfo(selected.packageName) },
            onReviewedChanged = { reviewed ->
                onReviewedChanged(selected.packageName, reviewed)
                selectedPackage = null
            },
            onDismiss = { selectedPackage = null },
        )
    }
}

/** System "App info" page, where the parent can uninstall the app or change its permissions. */
private fun Context.openAppInfo(packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        runCatching { startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)) }
    }
}
