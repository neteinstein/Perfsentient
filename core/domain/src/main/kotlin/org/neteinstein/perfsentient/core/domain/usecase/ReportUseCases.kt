package org.neteinstein.perfsentient.core.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.neteinstein.perfsentient.core.common.DispatcherProvider
import org.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import org.neteinstein.perfsentient.core.common.TimeProvider
import org.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import org.neteinstein.perfsentient.core.domain.analysis.BatteryAttributor
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.DeviceHealthAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.DeviceSecurityAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import org.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import org.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import org.neteinstein.perfsentient.core.domain.repository.DeviceSecurityRepository
import org.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import org.neteinstein.perfsentient.core.domain.repository.ReportRepository
import org.neteinstein.perfsentient.core.domain.repository.ScamMessageRepository
import org.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import org.neteinstein.perfsentient.core.domain.repository.UsageRepository
import org.neteinstein.perfsentient.core.domain.repository.VitalsRepository
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.ScamMessageSummary

/**
 * Collects the last 24 hours of telemetry and the device's security settings, runs every detector,
 * scores the day, estimates each
 * app's battery use and persists the report under today's date. Used both by the end-of-day worker and by the "analyse now" button.
 *
 * A rolling window (instead of "since midnight") means consecutive daily reports cover the whole
 * day with no gap: the evening after the report time is part of the next report.
 *
 * The stored report keeps every finding; the returned one leaves out trusted apps (see [TrustedAppsFilter]).
 */
class GenerateDailyReportUseCase(
    private val usageRepository: UsageRepository,
    private val inventoryRepository: AppInventoryRepository,
    private val vitalsRepository: VitalsRepository,
    private val notificationStatsRepository: NotificationStatsRepository,
    private val deviceSecurityRepository: DeviceSecurityRepository,
    private val scamMessageRepository: ScamMessageRepository,
    private val reportRepository: ReportRepository,
    private val trustedAppsRepository: TrustedAppsRepository,
    private val behaviorAnalyzer: BehaviorAnalyzer,
    private val deviceHealthAnalyzer: DeviceHealthAnalyzer,
    private val batteryAttributor: BatteryAttributor,
    private val deviceSecurityAnalyzer: DeviceSecurityAnalyzer,
    private val thresholds: DetectionThresholds,
    private val scoreCalculator: HealthScoreCalculator,
    private val trustedAppsFilter: TrustedAppsFilter,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(): DailyReport = withContext(dispatchers.default) {
        val end = timeProvider.now().toEpochMilli()
        val start = end - ANALYSIS_WINDOW_MS
        val today = timeProvider.today()

        val input = coroutineScope {
            val apps = async { inventoryRepository.getInstalledApps() }
            val context = async { inventoryRepository.getDeviceContext() }
            val usage = async { usageRepository.getAppUsage(start, end) }
            val events = async { usageRepository.getEvents(start, end) }
            val network = async { usageRepository.getNetworkUsage(start, end) }
            val notifications = async { notificationStatsRepository.getCounts(start, end) }
            val vitals = async { vitalsRepository.getSamples(start, end) }
            val security = async { deviceSecurityRepository.posture() }
            val partner = async { scamMessageRepository.status() }
            // From a little earlier, so an install early in the window can be matched to the text before it.
            val scamTexts = async { scamMessageRepository.getSignals(start - thresholds.scamTextToInstallWindowMs, end) }
            AnalysisInput(
                windowStartMs = start,
                windowEndMs = end,
                zone = timeProvider.zone(),
                apps = apps.await().associateBy { it.packageName },
                usage = usage.await().associateBy { it.packageName },
                events = events.await(),
                network = network.await().associateBy { it.packageName },
                notifications = notifications.await(),
                vitals = vitals.await(),
                context = context.await(),
                security = security.await(),
                scamMessages = scamTexts.await(),
                partner = partner.await(),
            )
        }

        val findings = behaviorAnalyzer.analyze(input)
        val metrics = deviceHealthAnalyzer.metrics(input)
        val issues = deviceHealthAnalyzer.issues(metrics) + deviceSecurityAnalyzer.issues(input.security, today)
        val score = scoreCalculator.score(findings, issues)

        val report = DailyReport(
            date = today,
            generatedAt = end,
            score = score,
            verdict = scoreCalculator.verdict(score),
            metrics = metrics,
            deviceIssues = issues,
            findings = findings,
            appsAnalysed = input.apps.size,
            battery = batteryAttributor.attribute(input),
            scamMessages = ScamMessageSummary.of(input.scamMessages.filter { it.receivedAtMs in start..end }),
        )
        reportRepository.save(report)
        reportRepository.deleteOlderThan(today.minusDays(REPORT_RETENTION_DAYS))
        trustedAppsFilter.apply(report, trustedAppsRepository.trustedPackages().first())
    }

    private companion object {
        const val REPORT_RETENTION_DAYS = 30L
        const val ANALYSIS_WINDOW_MS = 24 * MILLIS_IN_HOUR
    }
}

/** The most recent report, without the findings of trusted apps. */
class ObserveLatestReportUseCase(
    private val reportRepository: ReportRepository,
    private val trustedAppsRepository: TrustedAppsRepository,
    private val trustedAppsFilter: TrustedAppsFilter,
) {
    operator fun invoke(): Flow<DailyReport?> = combine(
        reportRepository.observeLatest(),
        trustedAppsRepository.trustedPackages(),
    ) { report, trusted -> report?.let { trustedAppsFilter.apply(it, trusted) } }
}

/** The last [days] reports, newest first, re-scored without trusted apps. */
class ObserveReportHistoryUseCase(
    private val reportRepository: ReportRepository,
    private val trustedAppsRepository: TrustedAppsRepository,
    private val trustedAppsFilter: TrustedAppsFilter,
) {
    operator fun invoke(days: Int = 7): Flow<List<DailyReport>> = combine(
        reportRepository.observeHistory(days),
        trustedAppsRepository.trustedPackages(),
    ) { reports, trusted -> reports.map { trustedAppsFilter.apply(it, trusted) } }
}
