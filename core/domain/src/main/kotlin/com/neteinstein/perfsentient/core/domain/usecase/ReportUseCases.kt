package com.neteinstein.perfsentient.core.domain.usecase

import com.neteinstein.perfsentient.core.common.DispatcherProvider
import com.neteinstein.perfsentient.core.common.TimeProvider
import com.neteinstein.perfsentient.core.domain.analysis.AnalysisInput
import com.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import com.neteinstein.perfsentient.core.domain.analysis.DeviceHealthAnalyzer
import com.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import com.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import com.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import com.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import com.neteinstein.perfsentient.core.domain.repository.ReportRepository
import com.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import com.neteinstein.perfsentient.core.domain.repository.UsageRepository
import com.neteinstein.perfsentient.core.domain.repository.VitalsRepository
import com.neteinstein.perfsentient.core.model.DailyReport
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Collects today's telemetry, runs every detector, scores the day and persists the report.
 * Used both by the end-of-day worker and by the "analyse now" button.
 *
 * The stored report keeps every finding; the returned one leaves out trusted apps (see [TrustedAppsFilter]).
 */
class GenerateDailyReportUseCase(
    private val usageRepository: UsageRepository,
    private val inventoryRepository: AppInventoryRepository,
    private val vitalsRepository: VitalsRepository,
    private val notificationStatsRepository: NotificationStatsRepository,
    private val reportRepository: ReportRepository,
    private val trustedAppsRepository: TrustedAppsRepository,
    private val behaviorAnalyzer: BehaviorAnalyzer,
    private val deviceHealthAnalyzer: DeviceHealthAnalyzer,
    private val scoreCalculator: HealthScoreCalculator,
    private val trustedAppsFilter: TrustedAppsFilter,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(): DailyReport = withContext(dispatchers.default) {
        val start = timeProvider.startOfToday().toEpochMilli()
        val end = timeProvider.now().toEpochMilli()
        val today = timeProvider.today()

        val input = coroutineScope {
            val apps = async { inventoryRepository.getInstalledApps() }
            val context = async { inventoryRepository.getDeviceContext() }
            val usage = async { usageRepository.getAppUsage(start, end) }
            val events = async { usageRepository.getEvents(start, end) }
            val network = async { usageRepository.getNetworkUsage(start, end) }
            val notifications = async { notificationStatsRepository.getCounts(today) }
            val vitals = async { vitalsRepository.getSamples(start, end) }
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
            )
        }

        val findings = behaviorAnalyzer.analyze(input)
        val metrics = deviceHealthAnalyzer.metrics(input)
        val issues = deviceHealthAnalyzer.issues(metrics)
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
        )
        reportRepository.save(report)
        reportRepository.deleteOlderThan(today.minusDays(REPORT_RETENTION_DAYS))
        trustedAppsFilter.apply(report, trustedAppsRepository.trustedPackages().first())
    }

    private companion object {
        const val REPORT_RETENTION_DAYS = 30L
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
