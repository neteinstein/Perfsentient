package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.evidence
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * The full scam chain, which neither app sees alone: SMS Toolbox flags a text with a link
 * ("your parcel is held, install our tracking app") and an app from outside a store is installed
 * within [DetectionThresholds.scamTextToInstallWindowMs]. Always HIGH. [Finding.value] is the
 * text's SMS Toolbox conversation id (0 when unknown), so the user can open it.
 */
class ScamChainDetector(private val thresholds: DetectionThresholds) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        if (input.scamMessages.isEmpty()) return emptyList()
        return input.candidates
            .filter { it.isSideloaded && it.firstInstallTime > 0 && !input.isVerifiedPartner(it.packageName) }
            .mapNotNull { app ->
                val text = input.scamTextBefore(app.firstInstallTime, thresholds.scamTextToInstallWindowMs) ?: return@mapNotNull null
                app.finding(
                    BehaviorType.SCAM_LINK_INSTALL,
                    Severity.HIGH,
                    text.threadId ?: 0L,
                    listOf(input.evidence(text.receivedAtMs, input.partner.packageName)),
                )
            }
    }
}
