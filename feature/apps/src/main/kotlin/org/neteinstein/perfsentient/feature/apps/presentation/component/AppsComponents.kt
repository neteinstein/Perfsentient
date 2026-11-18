package org.neteinstein.perfsentient.feature.apps.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.neteinstein.perfsentient.core.designsystem.behavior.label
import org.neteinstein.perfsentient.core.designsystem.behavior.presentation
import org.neteinstein.perfsentient.core.designsystem.component.AppIcon
import org.neteinstein.perfsentient.core.designsystem.component.StatusChip
import org.neteinstein.perfsentient.core.designsystem.component.SurfaceCard
import org.neteinstein.perfsentient.core.designsystem.theme.PerfsentienceTheme
import org.neteinstein.perfsentient.core.model.FlaggedApp
import org.neteinstein.perfsentient.feature.apps.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private fun LocalDate.display(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/**
 * One flagged app. Trusted apps get the secondary (teal) tint and chip, apps that are still
 * flagged keep the severity colours, so the two are told apart at a glance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppRow(app: FlaggedApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = PerfsentienceTheme.status
    val scheme = MaterialTheme.colorScheme
    val colors = CardDefaults.cardColors(
        containerColor = if (app.isTrusted) scheme.secondaryContainer.copy(alpha = 0.45f) else scheme.surfaceContainerLow,
    )
    val border = BorderStroke(
        1.dp,
        if (app.isTrusted) scheme.secondary.copy(alpha = 0.6f) else scheme.outlineVariant.copy(alpha = 0.5f),
    )
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors, border = border) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.appLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (app.isTrusted) {
                    StatusChip(stringResource(R.string.apps_trusted_chip), scheme.secondary, icon = Icons.Rounded.VerifiedUser)
                } else {
                    app.worstSeverity?.let { StatusChip(stringResource(it.label), status.forSeverity(it)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (app.findings.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    app.findings.forEach { finding ->
                        val ui = finding.type.presentation
                        val color = if (app.isTrusted) scheme.secondary else status.forSeverity(finding.severity)
                        StatusChip(stringResource(ui.title), color, icon = ui.icon)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(app.flaggedSummary(), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FlaggedApp.flaggedSummary(): String {
    val last = lastFlagged ?: return stringResource(R.string.apps_never_flagged)
    return stringResource(
        R.string.apps_sheet_flagged_summary,
        stringResource(R.string.apps_last_flagged, last.display()),
        pluralStringResource(R.plurals.apps_days_flagged, daysFlagged, daysFlagged),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDetailsSheet(app: FlaggedApp, onTrust: () -> Unit, onUntrust: () -> Unit, onDismiss: () -> Unit) {
    val status = PerfsentienceTheme.status
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName, size = 56)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.appLabel, style = MaterialTheme.typography.headlineSmall)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (app.findings.isEmpty()) {
                Text(
                    stringResource(R.string.apps_sheet_no_findings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                app.findings.forEach { finding ->
                    val ui = finding.type.presentation
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            ui.icon,
                            contentDescription = null,
                            tint = status.forSeverity(finding.severity),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(ui.title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        StatusChip(stringResource(finding.severity.label), status.forSeverity(finding.severity))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(app.flaggedSummary(), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            if (app.isTrusted) {
                OutlinedButton(onClick = onUntrust, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.apps_sheet_untrust))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.apps_sheet_untrust_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                Button(onClick = onTrust, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.apps_sheet_trust))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.apps_sheet_trust_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun AppsEmptyCard(modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(R.string.apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
