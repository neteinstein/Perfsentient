package com.neteinstein.perfsentience.core.domain.usecase

import com.neteinstein.perfsentience.core.domain.repository.ReportRepository
import com.neteinstein.perfsentience.core.domain.repository.TrustedAppsRepository
import com.neteinstein.perfsentience.core.model.DailyReport
import com.neteinstein.perfsentience.core.model.FlaggedApp
import com.neteinstein.perfsentience.core.model.Severity
import com.neteinstein.perfsentience.core.model.TrustedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Every app flagged in the retained report history, plus every trusted app, so the user can
 * trust or un-trust any of them. Reads the stored (unfiltered) reports, so trusted apps keep
 * their findings. Untrusted apps come first, worst severity first; trusted apps follow by name.
 */
class ObserveFlaggedAppsUseCase(
    private val reportRepository: ReportRepository,
    private val trustedAppsRepository: TrustedAppsRepository,
) {
    operator fun invoke(days: Int = HISTORY_DAYS): Flow<List<FlaggedApp>> = combine(
        reportRepository.observeHistory(days),
        trustedAppsRepository.trustedApps,
    ) { reports, trusted -> merge(reports, trusted) }

    private fun merge(reports: List<DailyReport>, trusted: List<TrustedApp>): List<FlaggedApp> {
        val trustedByPackage = trusted.associateBy { it.packageName }
        val newestFirst = reports.sortedByDescending { it.date }
        val flagged = newestFirst
            .flatMap { report -> report.findings.map { report.date to it } }
            .groupBy { (_, finding) -> finding.packageName }
            .map { (pkg, entries) ->
                FlaggedApp(
                    packageName = pkg,
                    appLabel = entries.first().second.appLabel,
                    // newest first, so distinctBy keeps the latest finding of each behaviour
                    findings = entries.map { it.second }.distinctBy { it.type }.sortedByDescending { it.severity },
                    lastFlagged = entries.first().first,
                    daysFlagged = entries.map { it.first }.distinct().size,
                    isTrusted = pkg in trustedByPackage,
                )
            }
        val flaggedPackages = flagged.mapTo(HashSet()) { it.packageName }
        val trustedOnly = trusted.filterNot { it.packageName in flaggedPackages }
            .map { FlaggedApp(it.packageName, it.label, emptyList(), null, 0, isTrusted = true) }
        return (flagged + trustedOnly).sortedWith(
            compareBy<FlaggedApp> { it.isTrusted }
                .thenByDescending { it.worstSeverity ?: Severity.LOW }
                .thenBy { it.appLabel.lowercase() }
                .thenBy { it.packageName },
        )
    }

    private companion object {
        const val HISTORY_DAYS = 30
    }
}
