package org.neteinstein.perfsentient.feature.parental.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.neteinstein.perfsentient.core.designsystem.behavior.label
import org.neteinstein.perfsentient.core.designsystem.component.AppIcon
import org.neteinstein.perfsentient.core.designsystem.component.AuroraBackground
import org.neteinstein.perfsentient.core.designsystem.component.StatusChip
import org.neteinstein.perfsentient.core.designsystem.component.SurfaceCard
import org.neteinstein.perfsentient.core.designsystem.theme.PerfsentienceTheme
import org.neteinstein.perfsentient.core.model.ParentalWarning
import org.neteinstein.perfsentient.feature.parental.R
import org.neteinstein.perfsentient.feature.parental.presentation.ParentalUiState
import org.neteinstein.perfsentient.feature.parental.presentation.presentation

/** Headline of the tab: how many apps need a look for the child's age. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ParentalSummaryCard(state: ParentalUiState, onChangeAge: () -> Unit, modifier: Modifier = Modifier) {
    val status = PerfsentienceTheme.status
    val count = state.warnings.size
    val accent = when {
        state.highRiskCount > 0 -> status.poor
        count > 0 -> status.fair
        else -> status.good
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        AuroraBackground(accent = accent) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(if (count > 0) Icons.Rounded.GppMaybe else Icons.Rounded.VerifiedUser, accent)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = if (count == 0) {
                            stringResource(R.string.parental_summary_clean)
                        } else {
                            pluralStringResource(R.plurals.parental_summary_count, count, count)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Text(
                    stringResource(if (count == 0) R.string.parental_summary_clean_desc else R.string.parental_summary_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        stringResource(R.string.parental_checked_for_age, state.childAge),
                        MaterialTheme.colorScheme.primary,
                        icon = Icons.Rounded.Cake,
                    )
                    if (state.highRiskCount > 0) {
                        StatusChip(
                            pluralStringResource(R.plurals.parental_high_risk_count, state.highRiskCount, state.highRiskCount),
                            status.poor,
                            icon = Icons.Rounded.Warning,
                        )
                    }
                    if (state.reviewed.isNotEmpty()) {
                        StatusChip(
                            pluralStringResource(R.plurals.parental_reviewed_count, state.reviewed.size, state.reviewed.size),
                            MaterialTheme.colorScheme.secondary,
                            icon = Icons.Rounded.TaskAlt,
                        )
                    }
                }
                TextButton(onClick = onChangeAge) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.parental_change_age))
                }
            }
        }
    }
}

/**
 * One concerning app. Unreviewed apps keep the severity colours; reviewed ones get the secondary
 * (teal) tint and chip, like trusted apps in the Apps tab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ParentalWarningCard(warning: ParentalWarning, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = PerfsentienceTheme.status
    val scheme = MaterialTheme.colorScheme
    val category = warning.app.category.presentation
    val colors = CardDefaults.cardColors(
        containerColor = if (warning.isReviewed) scheme.secondaryContainer.copy(alpha = 0.45f) else scheme.surfaceContainerLow,
    )
    val border = BorderStroke(
        1.dp,
        if (warning.isReviewed) scheme.secondary.copy(alpha = 0.6f) else status.forSeverity(warning.severity).copy(alpha = 0.45f),
    )
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors, border = border) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(warning.packageName)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(warning.appLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(category.title),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (warning.isReviewed) {
                    StatusChip(stringResource(R.string.parental_reviewed_chip), scheme.secondary, icon = Icons.Rounded.TaskAlt)
                } else {
                    StatusChip(stringResource(warning.severity.label), status.forSeverity(warning.severity))
                }
            }
            Spacer(Modifier.height(10.dp))
            val concerns = warning.app.concerns.sorted()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val chipColor = if (warning.isReviewed) scheme.secondary else status.forSeverity(warning.severity)
                concerns.take(MAX_CARD_CONCERNS).forEach { concern ->
                    val ui = concern.presentation
                    StatusChip(stringResource(ui.title), chipColor, icon = ui.icon)
                }
                val more = concerns.size - MAX_CARD_CONCERNS
                if (more > 0) StatusChip(pluralStringResource(R.plurals.parental_more_concerns, more, more), scheme.outline)
            }
            Spacer(Modifier.height(8.dp))
            AgeLine(warning)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgeLine(warning: ParentalWarning, modifier: Modifier = Modifier) {
    // FlowRow: the chip moves to its own line instead of squeezing the ages onto two lines.
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildList {
                add(stringResource(R.string.parental_suggested_age, warning.app.recommendedAge))
                if (warning.app.minimumAge > 0) add(stringResource(R.string.parental_minimum_age, warning.app.minimumAge))
            }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (warning.isBelowMinimumAge) {
            StatusChip(stringResource(R.string.parental_below_minimum_chip), PerfsentienceTheme.status.poor)
        }
    }
}

/** Why the app is flagged, each concern explained, what to do, and the review / app-info actions. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ParentalWarningSheet(
    warning: ParentalWarning,
    onOpenAppInfo: () -> Unit,
    onReviewedChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val status = PerfsentienceTheme.status
    val scheme = MaterialTheme.colorScheme
    val category = warning.app.category.presentation
    val accent = status.forSeverity(warning.severity)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(warning.packageName, size = 56)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(warning.appLabel, style = MaterialTheme.typography.headlineSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(category.icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(category.title), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(stringResource(warning.severity.label), accent, icon = Icons.Rounded.Warning)
                StatusChip(
                    stringResource(R.string.parental_suggested_age, warning.app.recommendedAge),
                    scheme.primary,
                    icon = Icons.Rounded.FamilyRestroom,
                )
                if (warning.app.minimumAge > 0) {
                    StatusChip(stringResource(R.string.parental_minimum_age, warning.app.minimumAge), scheme.tertiary)
                }
            }
            if (warning.isBelowMinimumAge) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.parental_sheet_below_minimum, warning.app.minimumAge),
                    style = MaterialTheme.typography.bodyMedium,
                    color = status.poor,
                )
            }

            SheetHeading(stringResource(R.string.parental_sheet_why))
            Text(stringResource(category.description), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            warning.app.concerns.sorted().forEach { concern ->
                val ui = concern.presentation
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Icon(ui.icon, contentDescription = null, tint = accent, modifier = Modifier.padding(top = 2.dp).size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(ui.title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(ui.description), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            }

            SheetHeading(stringResource(R.string.parental_sheet_what_to_do))
            Text(stringResource(category.guidance), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.parental_sheet_open_app_info))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.parental_sheet_open_app_info_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (warning.isReviewed) {
                OutlinedButton(onClick = { onReviewedChanged(false) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.parental_sheet_unreview))
                }
            } else {
                Button(onClick = { onReviewedChanged(true) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.parental_sheet_mark_reviewed))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.parental_sheet_mark_reviewed_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SheetHeading(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
}

/** Static advice for parents (warning signs, what to do, how the check works). */
@Composable
internal fun GuidanceCard(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = 18) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon, MaterialTheme.colorScheme.tertiary, size = 40)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ParentalDisabledCard(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Rounded.FamilyRestroom, MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.parental_disabled), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.parental_disabled_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpenSettings) { Text(stringResource(R.string.parental_open_settings)) }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, accent: Color, size: Int = 48) {
    Box(
        Modifier
            .size(size.dp)
            .background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.08f))),
                RoundedCornerShape((size / 3).dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent)
    }
}

private const val MAX_CARD_CONCERNS = 3
