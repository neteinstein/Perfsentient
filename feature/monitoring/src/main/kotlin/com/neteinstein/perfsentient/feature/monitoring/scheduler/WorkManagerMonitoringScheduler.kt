package com.neteinstein.perfsentient.feature.monitoring.scheduler

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neteinstein.perfsentient.core.common.TimeProvider
import com.neteinstein.perfsentient.core.domain.repository.MonitoringScheduler
import com.neteinstein.perfsentient.core.model.ReportTime
import com.neteinstein.perfsentient.feature.monitoring.worker.DailyReportWorker
import com.neteinstein.perfsentient.feature.monitoring.worker.VitalsSamplerWorker
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * WorkManager-backed scheduling. No foreground service is needed: usage events and network
 * stats are retained by the OS, so we only sample cheap device vitals every 15 minutes
 * (the WorkManager minimum) and analyse everything once per day.
 */
class WorkManagerMonitoringScheduler(
    private val workManager: WorkManager,
    private val timeProvider: TimeProvider,
) : MonitoringScheduler {

    override fun ensureScheduled(reportTime: ReportTime) {
        enqueueSampler()
        enqueueReport(reportTime, ExistingWorkPolicy.KEEP)
    }

    override fun reschedule(reportTime: ReportTime) {
        enqueueSampler()
        enqueueReport(reportTime, ExistingWorkPolicy.REPLACE)
    }

    /** Called by [DailyReportWorker] once it finished, to chain tomorrow's run. */
    fun scheduleNextReport(reportTime: ReportTime) = enqueueReport(reportTime, ExistingWorkPolicy.APPEND_OR_REPLACE)

    override fun cancel() {
        workManager.cancelUniqueWork(SAMPLER_WORK)
        workManager.cancelUniqueWork(REPORT_WORK)
    }

    private fun enqueueSampler() {
        val request = PeriodicWorkRequestBuilder<VitalsSamplerWorker>(SAMPLE_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .build()
        workManager.enqueueUniquePeriodicWork(SAMPLER_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun enqueueReport(reportTime: ReportTime, policy: ExistingWorkPolicy) {
        val delay: Duration = ReportTimeCalculator.delayUntilNext(timeProvider.nowZoned(), reportTime)
        val request = OneTimeWorkRequestBuilder<DailyReportWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(REPORT_WORK, policy, request)
    }

    companion object {
        const val SAMPLER_WORK = "perfsentience.vitals-sampler"
        const val REPORT_WORK = "perfsentience.daily-report"
        private const val SAMPLE_INTERVAL_MINUTES = 15L
        private const val BACKOFF_MINUTES = 5L
    }
}
