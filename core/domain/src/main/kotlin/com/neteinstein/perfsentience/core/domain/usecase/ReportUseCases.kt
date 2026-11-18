package com.neteinstein.perfsentience.core.domain.usecase

import com.neteinstein.perfsentience.core.common.DispatcherProvider
import com.neteinstein.perfsentience.core.common.TimeProvider
import com.neteinstein.perfsentience.core.domain.analysis.AnalysisInput
import com.neteinstein.perfsentience.core.domain.analysis.BehaviorAnalyzer
import com.neteinstein.perfsentience.core.domain.analysis.DeviceHealthAnalyzer
import com.neteinstein.perfsentience.core.domain.analysis.HealthScoreCalculator
import com.neteinstein.perfsentience.core.domain.repository.AppInventoryRepository
import com.neteinstein.perfsentience.core.domain.repository.NotificationStatsRepository
import com.neteinstein.perfsentience.core.domain.repository.ReportRepository
import com.neteinstein.perfsentience.core.domain.repository.UsageRepository
import com.neteinstein.perfsentience.core.domain.repository.VitalsRepository
import com.neteinstein.perfsentience.core.model.DailyReport
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Collects today's telemetry, runs every detector, scores the day and persists the report.
 * Used both by the end-of-day worker and by the "analyse now" button.
 */
class GenerateDailyReportUseCase(
    private val usageRepository: UsageRepository,
    private val inventoryRepository: AppInventoryRepository,
    private val vitalsRepository: VitalsRepository,
    private val notificationStatsRepository: NotificationStatsRepository,
    private val reportRepository: ReportRepository,
    private val behaviorAnalyzer: BehaviorAnalyzer,
    private val deviceHealthAnalyzer: DeviceHealthAnalyzer,
    private val scoreCalculator: HealthScoreCalculator,
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
        report
    }

    private companion object {
        const val REPORT_RETENTION_DAYS = 30L
    }
}

class ObserveLatestReportUseCase(private val repository: ReportRepository) {
    operator fun invoke(): Flow<DailyReport?> = repository.observeLatest()
}

class ObserveReportHistoryUseCase(private val repository: ReportRepository) {
    operator fun invoke(days: Int = 7): Flow<List<DailyReport>> = repository.observeHistory(days)
}
