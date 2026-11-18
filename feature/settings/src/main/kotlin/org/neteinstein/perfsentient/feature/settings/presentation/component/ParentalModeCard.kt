package org.neteinstein.perfsentient.feature.settings.presentation.component

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.neteinstein.perfsentient.core.designsystem.component.SurfaceCard
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.feature.settings.R

/**
 * Parental mode switch. Turning it on first asks for the child's age; once on, the age can be changed.
 */
@Composable
internal fun ParentalModeCard(
    parental: ParentalSettings,
    onParentalModeChanged: (enabled: Boolean, childAge: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAgeDialog by rememberSaveable { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.tertiary

    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = 18) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .toggleable(
                    value = parental.enabled,
                    role = Role.Switch,
                    onValueChange = { checked ->
                        if (checked) showAgeDialog = true else onParentalModeChanged(false, parental.childAge)
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.08f))),
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.FamilyRestroom, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_parental_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_parental_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            // The whole row is the toggle target (48dp+), so the switch itself is display only.
            Switch(checked = parental.enabled, onCheckedChange = null)
        }
        AnimatedVisibility(parental.enabled, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { showAgeDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        stringResource(R.string.settings_parental_child_age),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        pluralStringResource(R.plurals.settings_parental_age_years, parental.childAge, parental.childAge),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_parental_change),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (showAgeDialog) {
        ChildAgeDialog(
            initialAge = parental.childAge,
            confirmLabel = stringResource(if (parental.enabled) R.string.settings_parental_save else R.string.settings_parental_turn_on),
            onConfirm = { age ->
                onParentalModeChanged(true, age)
                showAgeDialog = false
            },
            onDismiss = { showAgeDialog = false },
        )
    }
}

/** Asks for the child's age (3–17) with a slider plus −/+ buttons for precise, accessible input. */
@Composable
internal fun ChildAgeDialog(initialAge: Int, confirmLabel: String, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val range = ParentalSettings.CHILD_AGE_RANGE
    var age by rememberSaveable { mutableIntStateOf(initialAge.coerceIn(range)) }
    val sliderDescription = stringResource(R.string.settings_parental_child_age)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.FamilyRestroom, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_parental_age_title), textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.settings_parental_age_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { age-- }, enabled = age > range.first) {
                        Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.settings_parental_age_younger))
                    }
                    Text(
                        pluralStringResource(R.plurals.settings_parental_age_years, age, age),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(onClick = { age++ }, enabled = age < range.last) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.settings_parental_age_older))
                    }
                }
                Slider(
                    value = age.toFloat(),
                    onValueChange = { age = it.roundToInt().coerceIn(range) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = range.last - range.first - 1,
                    modifier = Modifier.semantics { contentDescription = sliderDescription },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(age) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}
