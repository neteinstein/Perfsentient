package org.neteinstein.perfsentient.core.domain.analysis.detector

import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorDetector
import org.neteinstein.perfsentient.core.domain.analysis.evidence
import org.neteinstein.perfsentient.core.domain.analysis.finding
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.InstallChannel
import org.neteinstein.perfsentient.core.model.PackageSource
import org.neteinstein.perfsentient.core.model.Severity

/**
 * Fraud apps reach victims as an APK link in a text, chat or email, or a "download our app" page.
 * Google's Play Protect fraud pilot blocks exactly these installs when the app asks for SMS,
 * notification or accessibility access. We flag:
 *
 * - an app installed from a messaging or email app: MEDIUM, HIGH with a sensitive capability;
 * - an app downloaded through a browser or file manager that holds a sensitive capability: MEDIUM.
 *
 * [Finding.value] is the [InstallChannel] ordinal.
 */
class RiskyInstallSourceDetector(private val catalog: ScamSignalCatalog) : BehaviorDetector {
    override fun detect(input: AnalysisInput): List<Finding> = input.candidates
        .filter { it.isSideloaded }
        .mapNotNull { app ->
            val channel = channelOf(app) ?: return@mapNotNull null
            val risky = app.hasSensitiveCapability
            val severity = when {
                channel == InstallChannel.MESSAGING && risky -> Severity.HIGH
                channel == InstallChannel.MESSAGING -> Severity.MEDIUM
                risky -> Severity.MEDIUM
                else -> return@mapNotNull null
            }
            val initiator = app.initiatingPackage ?: app.installerPackage
            val evidence = if (app.firstInstallTime > 0) listOf(input.evidence(app.firstInstallTime, initiator)) else emptyList()
            app.finding(BehaviorType.RISKY_INSTALL_SOURCE, severity, channel.ordinal.toLong(), evidence)
        }

    private fun channelOf(app: AppProfile): InstallChannel? {
        val initiator = app.initiatingPackage ?: app.installerPackage
        return when {
            catalog.isMessagingApp(initiator) -> InstallChannel.MESSAGING

            catalog.isBrowserOrFiles(initiator) -> if (app.packageSource ==
                PackageSource.LOCAL_FILE
            ) {
                InstallChannel.FILE
            } else {
                InstallChannel.BROWSER
            }

            app.packageSource == PackageSource.DOWNLOADED_FILE -> InstallChannel.BROWSER

            app.packageSource == PackageSource.LOCAL_FILE -> InstallChannel.FILE

            else -> null
        }
    }
}
