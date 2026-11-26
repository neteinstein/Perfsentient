package org.neteinstein.perfsentient.core.designsystem.behavior

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Date
import org.neteinstein.perfsentient.core.designsystem.R
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Evidence
import org.neteinstein.perfsentient.core.model.Finding

/**
 * "Last: 14:02 · over Chrome ×5", with a toggle listing every recorded time. Shows nothing for
 * findings without evidence (capability findings, reports saved before evidence existed).
 */
@Composable
fun EvidenceSummary(finding: Finding, modifier: Modifier = Modifier, expandable: Boolean = true) {
    val evidence = finding.evidence
    if (evidence.isEmpty()) return
    val context = LocalContext.current
    var expanded by rememberSaveable(finding.packageName, finding.type) { mutableStateOf(false) }
    val latest = evidence.first()
    val relation = relation(finding)
    val last = stringResource(R.string.evidence_last, context.formatEvidenceTime(latest.atMs))
    Column(modifier) {
        Text(
            if (relation == null) last else stringResource(R.string.evidence_joined, last, relation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expandable && evidence.size > 1) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(top = 0.dp)) {
                Text(
                    if (expanded) {
                        stringResource(R.string.evidence_hide_times)
                    } else {
                        pluralStringResource(R.plurals.evidence_show_times, evidence.size, evidence.size)
                    },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    evidence.forEach { item ->
                        val time = context.formatEvidenceTime(item.atMs)
                        Text(
                            item.relatedLabel?.let { stringResource(R.string.evidence_joined, time, it) } ?: time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun relation(finding: Finding): String? {
    val related = finding.evidence.mapNotNull(Evidence::relatedLabel)
    val top = related.groupingBy { it }.eachCount().maxByOrNull { it.value }
    return when (finding.type) {
        BehaviorType.OUT_OF_CONTEXT_ADS -> top?.let { (label, count) ->
            if (count > 1) stringResource(R.string.evidence_over_count, label, count) else stringResource(R.string.evidence_over, label)
        }

        BehaviorType.AFTER_CALL_ADS -> stringResource(R.string.evidence_after_call)

        BehaviorType.REMOTE_ACCESS -> if (related.isNotEmpty()) stringResource(R.string.evidence_after_call) else null

        BehaviorType.RISKY_INSTALL_SOURCE -> top?.let { stringResource(R.string.evidence_from, it.key) }

        BehaviorType.SCAM_LINK_INSTALL -> stringResource(R.string.evidence_scam_text)

        else -> null
    }
}

/** Time only for today, date and time otherwise, in the user's 12/24-hour format. */
fun Context.formatEvidenceTime(epochMs: Long): String = if (DateUtils.isToday(epochMs)) {
    DateFormat.getTimeFormat(this).format(Date(epochMs))
} else {
    DateUtils.formatDateTime(this, epochMs, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH)
}
