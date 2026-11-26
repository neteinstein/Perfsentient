package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * Fake apps borrow trusted names: "System Update", "Google Play Services", "DHL", a bank, or a
 * modded "WhatsApp Plus". An app that did not come from a store and whose label matches such a
 * name (ignoring case, accents, spacing and look-alike characters), while its package is not the
 * real one, is MEDIUM; HIGH when it also hides its icon or holds a sensitive capability.
 */
class ImpersonationDetector(private val catalog: ScamSignalCatalog) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates
        .filter { it.isSideloaded }
        .mapNotNull { app ->
            val imitated = catalog.impersonatedBy(app.label) ?: return@mapNotNull null
            if (app.packageName in imitated.genuinePackages) return@mapNotNull null
            val severity = if (!app.hasLauncherIcon || app.hasSensitiveCapability) Severity.HIGH else Severity.MEDIUM
            app.finding(BehaviorType.IMPERSONATION, severity, 1)
        }
}
