package org.neteinstein.perfsentient.feature.monitoring

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.testing.FakeTimeProvider
import org.neteinstein.perfsentient.feature.monitoring.scheduler.WorkManagerMonitoringScheduler

class WorkManagerMonitoringSchedulerTest {
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val time = FakeTimeProvider() // 21:30 UTC
    private val scheduler = WorkManagerMonitoringScheduler(workManager, time)

    @Test
    fun `ensure keeps existing work and delays the report until the chosen time`() {
        val request = slot<OneTimeWorkRequest>()
        scheduler.ensureScheduled(ReportTime(22, 30))

        verify {
            workManager.enqueueUniquePeriodicWork(WorkManagerMonitoringScheduler.SAMPLER_WORK, ExistingPeriodicWorkPolicy.KEEP, any())
        }
        verify { workManager.enqueueUniqueWork(WorkManagerMonitoringScheduler.REPORT_WORK, ExistingWorkPolicy.KEEP, capture(request)) }
        assertEquals(TimeUnit.HOURS.toMillis(1), request.captured.workSpec.initialDelay)
    }

    @Test
    fun `reschedule replaces, chaining appends and cancel removes everything`() {
        scheduler.reschedule(ReportTime(8, 0))
        scheduler.scheduleNextReport(ReportTime(8, 0))
        scheduler.cancel()

        verify {
            workManager.enqueueUniqueWork(WorkManagerMonitoringScheduler.REPORT_WORK, ExistingWorkPolicy.REPLACE, any<OneTimeWorkRequest>())
        }
        verify {
            workManager.enqueueUniqueWork(
                WorkManagerMonitoringScheduler.REPORT_WORK,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
        verify { workManager.cancelUniqueWork(WorkManagerMonitoringScheduler.SAMPLER_WORK) }
        verify { workManager.cancelUniqueWork(WorkManagerMonitoringScheduler.REPORT_WORK) }
    }
}
