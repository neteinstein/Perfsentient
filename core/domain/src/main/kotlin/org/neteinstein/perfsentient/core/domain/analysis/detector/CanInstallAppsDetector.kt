package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity

/**
 * "Install unknown apps" lets an app drop more apps on the phone: droppers use it to fetch the
 * real malware after install. Flagged for apps that did not come from a store (MEDIUM) and for
 * store-installed chat apps and browsers (LOW), the usual route for a scam APK. App stores
 * themselves (F-Droid, Aurora…) and SMS Toolbox signed by its developer (it installs its own
 * GitHub updates) are left alone.
 */
class CanInstallAppsDetector(private val catalog: ScamSignalCatalog) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates
        .filter { it.canInstallApps && it.packageName !in AppProfile.TRUSTED_INSTALLERS && !input.isVerifiedPartner(it.packageName) }
        .mapNotNull { app ->
            val severity = when {
                app.isSideloaded -> Severity.MEDIUM
                catalog.isMessagingApp(app.packageName) || catalog.isBrowserOrFiles(app.packageName) -> Severity.LOW
                else -> return@mapNotNull null
            }
            app.finding(BehaviorType.CAN_INSTALL_APPS, severity, 1)
        }
}
