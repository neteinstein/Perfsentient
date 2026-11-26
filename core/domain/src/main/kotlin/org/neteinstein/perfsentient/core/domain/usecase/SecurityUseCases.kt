package org.neteinstein.perfsentient.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.neteinstein.perfsentient.core.common.DispatcherProvider
import org.neteinstein.perfsentient.core.common.TimeProvider
import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import org.neteinstein.perfsentient.core.domain.repository.SecurityWatchRepository
import org.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import org.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import org.neteinstein.perfsentient.core.domain.repository.UsageRepository
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.SecurityWatchState
import org.neteinstein.perfsentient.core.model.Severity

/**
 * The live part of the scam checks. Runs with every vitals sample (about every 15 minutes) and
 * returns the findings worth a notification right away:
 *
 * 1. Apps installed or updated since the last run, plus apps that newly hold accessibility,
 *    device-admin or notification access, or became the default SMS app.
 * 2. Those apps go through the same security detectors as the daily report, with the last hour of
 *    usage events so a remote-access install can be matched to a call.
 * 3. A finding alerts when it is High (or a Medium remote-access app), the app is not trusted, and
 *    that app + behaviour was not alerted before.
 *
 * The first run only records a baseline, so apps that were already there raise no alerts. With
 * alerts turned off the baseline still moves on, so turning them back on does not replay the past.
 */
class CheckSecurityChangesUseCase(
    private val inventoryRepository: AppInventoryRepository,
    private val usageRepository: UsageRepository,
    private val watchRepository: SecurityWatchRepository,
    private val trustedAppsRepository: TrustedAppsRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
    thresholds: DetectionThresholds,
    catalog: ScamSignalCatalog,
) {
    private val analyzer = BehaviorAnalyzer.security(thresholds, catalog)

    suspend operator fun invoke(): List<Finding> = withContext(dispatchers.default) {
        val previous = watchRepository.state()
        val changes = inventoryRepository.getChangedPackages(previous?.cursor)
        val holders = inventoryRepository.getCapabilityHolders()
        if (previous == null) {
            watchRepository.save(SecurityWatchState(changes.cursor, holders, alerted = emptyList()))
            return@withContext emptyList()
        }

        val suspects = changes.packages + holders.newlyGrantedSince(previous.holders)
        val findings = if (suspects.isEmpty()) emptyList() else analyze(suspects)
        val trusted = trustedAppsRepository.trustedApps.first().map { it.packageName }.toSet()
        val alerts = findings
            .filter { it.isAlertWorthy() && it.packageName !in trusted && it.key() !in previous.alerted }
            .distinctBy { it.key() }
        val enabled = settingsRepository.settings.first().securityAlertsEnabled

        watchRepository.save(
            SecurityWatchState(
                cursor = changes.cursor,
                holders = holders,
                alerted = (previous.alerted + alerts.map { it.key() }).takeLast(MAX_REMEMBERED_ALERTS),
            ),
        )
        if (enabled) alerts else emptyList()
    }

    private suspend fun analyze(packages: Set<String>): List<Finding> {
        val end = timeProvider.now().toEpochMilli()
        val start = end - CALL_LOOKBACK_MS
        val input = AnalysisInput(
            windowStartMs = start,
            windowEndMs = end,
            zone = timeProvider.zone(),
            apps = inventoryRepository.getProfiles(packages).associateBy { it.packageName },
            usage = emptyMap(),
            events = usageRepository.getEvents(start, end),
            network = emptyMap(),
            notifications = emptyMap(),
            vitals = emptyList(),
            context = inventoryRepository.getDeviceContext(),
        )
        return analyzer.analyze(input)
    }

    private fun Finding.isAlertWorthy(): Boolean =
        severity == Severity.HIGH || (type == BehaviorType.REMOTE_ACCESS && severity >= Severity.MEDIUM)

    private fun Finding.key(): String = "$packageName:${type.name}"

    private companion object {
        /** Enough usage history to see the call a remote-access app was installed after. */
        const val CALL_LOOKBACK_MS = 60 * 60 * 1_000L
        const val MAX_REMEMBERED_ALERTS = 500
    }
}

class UpdateSecurityAlertsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSecurityAlertsEnabled(enabled)
}
