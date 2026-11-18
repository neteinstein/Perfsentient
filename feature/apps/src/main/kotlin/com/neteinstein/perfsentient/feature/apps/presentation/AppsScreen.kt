package com.neteinstein.perfsentient.feature.apps.presentation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.perfsentient.core.designsystem.component.SectionHeader
import com.neteinstein.perfsentient.feature.apps.R
import com.neteinstein.perfsentient.feature.apps.presentation.component.AppDetailsSheet
import com.neteinstein.perfsentient.feature.apps.presentation.component.AppRow
import com.neteinstein.perfsentient.feature.apps.presentation.component.AppsEmptyCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppsRoute(modifier: Modifier = Modifier, viewModel: AppsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppsScreen(
        state = state,
        onTrustApp = viewModel::onTrustApp,
        onUntrustApp = viewModel::onUntrustApp,
        modifier = modifier,
    )
}

@Composable
internal fun AppsScreen(
    state: AppsUiState,
    onTrustApp: (packageName: String, label: String) -> Unit,
    onUntrustApp: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues()
    val contentPadding = PaddingValues(
        start = insets.calculateStartPadding(layoutDirection) + 20.dp,
        end = insets.calculateEndPadding(layoutDirection) + 20.dp,
        top = insets.calculateTopPadding() + 16.dp,
        bottom = 32.dp,
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            SectionHeader(title = stringResource(R.string.apps_title), subtitle = stringResource(R.string.apps_subtitle))
        }
        if (state.apps.isEmpty()) {
            item(key = "empty") { AppsEmptyCard() }
        } else {
            items(state.apps, key = { it.packageName }) { app ->
                AppRow(app = app, onClick = { selectedPackage = app.packageName }, modifier = Modifier.animateItem())
            }
        }
    }

    // Looked up by package so the sheet follows live state (e.g. the colour flips right after trusting).
    state.apps.firstOrNull { it.packageName == selectedPackage }?.let { selected ->
        AppDetailsSheet(
            app = selected,
            onTrust = {
                onTrustApp(selected.packageName, selected.appLabel)
                selectedPackage = null
            },
            onUntrust = {
                onUntrustApp(selected.packageName)
                selectedPackage = null
            },
            onDismiss = { selectedPackage = null },
        )
    }
}
