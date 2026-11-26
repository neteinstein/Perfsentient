package org.neteinstein.perfsentient.feature.monitoring.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.neteinstein.perfsentient.core.domain.usecase.GenerateDailyReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.RecordVitalSampleUseCase
import org.neteinstein.perfsentient.feature.monitoring.notification.ReportNotifier
import org.neteinstein.perfsentient.feature.monitoring.scheduler.WorkManagerMonitoringScheduler

/** Records a device-vitals sample (battery, temperature, memory, screen state). */
class VitalsSamplerWorker(
    context: Context,
    params: WorkerParameters,
    private val recordVitalSample: RecordVitalSampleUseCase,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        recordVitalSample()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }
}

/** Builds the end-of-day report, notifies the user and chains tomorrow's run. */
class DailyReportWorker(
    context: Context,
    params: WorkerParameters,
    private val generateDailyReport: GenerateDailyReportUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val notifier: ReportNotifier,
    private val scheduler: WorkManagerMonitoringScheduler,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = observeSettings().first()
        if (!settings.monitoringEnabled) return Result.success()
        return try {
            val report = generateDailyReport()
            notifier.notify(report)
            scheduler.scheduleNextReport(settings.reportTime)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                // Give up on today's report but keep the chain alive: the next run is appended to
                // this work, and WorkManager cancels dependents of a *failed* work. Ending in
                // success lets tomorrow's report run.
                scheduler.scheduleNextReport(settings.reportTime)
                Result.success()
            }
        }
    }
}

private const val MAX_RETRIES = 3
