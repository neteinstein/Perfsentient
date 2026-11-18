package com.neteinstein.perfsentience.feature.settings.presentation

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.perfsentience.core.designsystem.component.SectionHeader
import com.neteinstein.perfsentience.core.designsystem.component.staggeredEntrance
import com.neteinstein.perfsentience.core.model.BehaviorType
import com.neteinstein.perfsentience.core.model.PermissionType
import com.neteinstein.perfsentience.feature.settings.R
import com.neteinstein.perfsentience.feature.settings.presentation.component.AppearanceCard
import com.neteinstein.perfsentience.feature.settings.presentation.component.BehaviorInfoCard
import com.neteinstein.perfsentience.feature.settings.presentation.component.MonitoringStatusCard
import com.neteinstein.perfsentience.feature.settings.presentation.component.PermissionCard
import com.neteinstein.perfsentience.feature.settings.presentation.component.ReportTimeCard
import com.neteinstein.perfsentience.feature.settings.presentation.component.TrustedAppCard
import com.neteinstein.perfsentience.feature.settings.presentation.component.TrustedAppsEmptyCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(modifier: Modifier = Modifier, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onPermissionsMaybeChanged()
        onPauseOrDispose { }
    }
    SettingsScreen(
        state = state,
        onPermissionsMaybeChanged = viewModel::onPermissionsMaybeChanged,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onDynamicColorToggled = viewModel::onDynamicColorToggled,
        onReportTimeSelected = viewModel::onReportTimeSelected,
        onUntrustApp = viewModel::onUntrustApp,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onPermissionsMaybeChanged: () -> Unit,
    onThemeModeSelected: (com.neteinstein.perfsentience.core.model.ThemeMode) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    onReportTimeSelected: (Int, Int) -> Unit,
    onUntrustApp: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var notificationsBlocked by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // When the system stops showing the dialog the user must enable it from settings.
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsBlocked = activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false
        }
        onPermissionsMaybeChanged()
    }

    val onGrant: (PermissionType) -> Unit = { type ->
        when (type) {
            PermissionType.USAGE_ACCESS ->
                context.launch(PermissionIntents.usageAccess(context), fallback = PermissionIntents.usageAccessFallback())

            PermissionType.POST_NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsBlocked) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.launch(PermissionIntents.appNotificationSettings(context))
                }

            PermissionType.NOTIFICATION_ACCESS -> context.launch(PermissionIntents.notificationAccess())

            PermissionType.APP_INVENTORY -> Unit
        }
    }

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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "title") {
            SectionHeader(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        }
        item(key = "status") {
            MonitoringStatusCard(state, Modifier.staggeredEntrance(0))
        }
        item(key = "permissions_header") {
            SectionHeader(
                title = stringResource(R.string.settings_section_permissions),
                subtitle = stringResource(R.string.settings_section_permissions_desc),
            )
        }
        itemsIndexed(state.permissions, key = { _, it -> it.type.name }) { index, permission ->
            PermissionCard(
                permission = permission,
                onGrant = { onGrant(permission.type) },
                modifier = Modifier.animateItem().staggeredEntrance(index + 1),
            )
        }
        item(key = "report_header") { SectionHeader(stringResource(R.string.settings_section_report)) }
        item(key = "report_time") {
            ReportTimeCard(time = state.settings.reportTime, onTimeSelected = onReportTimeSelected)
        }
        item(key = "trusted_header") {
            SectionHeader(
                title = stringResource(R.string.settings_section_trusted),
                subtitle = stringResource(R.string.settings_section_trusted_desc),
            )
        }
        if (state.trustedApps.isEmpty()) {
            item(key = "trusted_empty") { TrustedAppsEmptyCard() }
        } else {
            items(state.trustedApps, key = { "trusted_${it.packageName}" }) { app ->
                TrustedAppCard(app = app, onRemove = { onUntrustApp(app.packageName) }, modifier = Modifier.animateItem())
            }
        }
        item(key = "appearance_header") { SectionHeader(stringResource(R.string.settings_section_appearance)) }
        item(key = "appearance") {
            AppearanceCard(
                settings = state.settings,
                onThemeModeSelected = onThemeModeSelected,
                onDynamicColorToggled = onDynamicColorToggled,
            )
        }
        item(key = "detection_header") {
            SectionHeader(
                title = stringResource(R.string.settings_section_detection),
                subtitle = stringResource(R.string.settings_section_detection_desc),
            )
        }
        itemsIndexed(BehaviorType.entries, key = { _, it -> "behavior_${it.name}" }) { _, behavior ->
            BehaviorInfoCard(behavior)
        }
        item(key = "footer") {
            Text(
                text = "Perfsentient • on-device • no internet permission",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Context.launch(intent: Intent, fallback: Intent? = null) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        fallback?.let { runCatching { startActivity(it) } }
    } catch (_: SecurityException) {
        fallback?.let { runCatching { startActivity(it) } }
    }
}
