package com.neteinstein.perfsentience.feature.settings.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neteinstein.perfsentience.core.designsystem.behavior.presentation
import com.neteinstein.perfsentience.core.designsystem.component.AppIcon
import com.neteinstein.perfsentience.core.designsystem.component.AuroraBackground
import com.neteinstein.perfsentience.core.designsystem.component.GrantButton
import com.neteinstein.perfsentience.core.designsystem.component.PulsingDot
import com.neteinstein.perfsentience.core.designsystem.component.StatusChip
import com.neteinstein.perfsentience.core.designsystem.component.SurfaceCard
import com.neteinstein.perfsentience.core.designsystem.theme.PerfsentienceTheme
import com.neteinstein.perfsentience.core.domain.usecase.MonitoringState
import com.neteinstein.perfsentience.core.model.BehaviorType
import com.neteinstein.perfsentience.core.model.GrantMode
import com.neteinstein.perfsentience.core.model.PermissionState
import com.neteinstein.perfsentience.core.model.ReportTime
import com.neteinstein.perfsentience.core.model.ThemeMode
import com.neteinstein.perfsentience.core.model.TrustedApp
import com.neteinstein.perfsentience.core.model.UserSettings
import com.neteinstein.perfsentience.feature.settings.R
import com.neteinstein.perfsentience.feature.settings.presentation.SettingsUiState
import com.neteinstein.perfsentience.feature.settings.presentation.presentation
import java.util.Locale

@Composable
internal fun MonitoringStatusCard(state: SettingsUiState, modifier: Modifier = Modifier) {
    val active = state.monitoringState == MonitoringState.ACTIVE
    val accent = if (active) PerfsentienceTheme.status.good else PerfsentienceTheme.status.fair
    val total = state.permissions.size.coerceAtLeast(1)
    val progress by animateFloatAsState(state.grantedCount / total.toFloat(), tween(700), label = "grantProgress")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        AuroraBackground(accent = accent) {
            Column(Modifier.padding(24.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(color = accent, active = active)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(if (active) R.string.settings_monitoring_active else R.string.settings_monitoring_waiting),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Text(
                    text = if (active) {
                        stringResource(R.string.settings_monitoring_active_desc, state.settings.reportTime.format())
                    } else {
                        pluralStringResource(
                            R.plurals.settings_monitoring_waiting_desc,
                            state.permissions.size,
                            state.grantedCount,
                            state.permissions.size,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
internal fun PermissionCard(permission: PermissionState, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    val ui = permission.type.presentation
    var expanded by rememberSaveable(permission.type) { mutableStateOf(false) }
    val good = PerfsentienceTheme.status.good
    val accent = if (permission.isGranted) good else MaterialTheme.colorScheme.primary
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = 18) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.08f))),
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ui.icon, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(ui.title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                val chip = if (permission.grantMode ==
                    GrantMode.INSTALL_TIME
                ) {
                    R.string.settings_permission_automatic
                } else {
                    R.string.settings_permission_required
                }
                StatusChip(text = stringResource(chip), color = if (permission.isGranted) good else MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.width(8.dp))
            GrantButton(
                granted = permission.isGranted,
                grantLabel = stringResource(R.string.settings_permission_grant),
                grantedDescription = stringResource(R.string.settings_permission_granted),
                onGrant = onGrant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(ui.description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings_permission_why),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.rotate(chevron))
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text(
                stringResource(ui.rationale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportTimeCard(time: ReportTime, onTimeSelected: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    SurfaceCard(modifier = modifier.fillMaxWidth(), onClick = { showDialog = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_report_time), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_report_time_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(time.format(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (showDialog) {
        val pickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_time_dialog_title)) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(pickerState.hour, pickerState.minute)
                    showDialog = false
                }) { Text(stringResource(R.string.settings_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.settings_cancel)) } },
        )
    }
}

@Composable
internal fun AppearanceCard(
    settings: UserSettings,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        val labels = mapOf(
            ThemeMode.SYSTEM to R.string.settings_theme_system,
            ThemeMode.LIGHT to R.string.settings_theme_light,
            ThemeMode.DARK to R.string.settings_theme_dark,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = settings.themeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                ) { Text(stringResource(labels.getValue(mode))) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_dynamic_color), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_dynamic_color_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = settings.useDynamicColor, onCheckedChange = onDynamicColorToggled)
        }
    }
}

@Composable
internal fun BehaviorInfoCard(behavior: BehaviorType, modifier: Modifier = Modifier) {
    val ui = behavior.presentation
    var expanded by rememberSaveable(behavior) { mutableStateOf(false) }
    SurfaceCard(modifier = modifier.fillMaxWidth(), onClick = { expanded = !expanded }, contentPadding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(ui.icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(ui.title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "behaviorChevron")
            Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.rotate(chevron), tint = Color.Gray)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text(
                stringResource(ui.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
internal fun TrustedAppCard(app: TrustedApp, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.packageName, size = 40)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.settings_trusted_remove, app.label),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun TrustedAppsEmptyCard(modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(R.string.settings_trusted_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun ReportTime.format(): String = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
