package org.neteinstein.perfsentient.feature.monitoring

import android.app.Application
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.perfsentient.core.domain.usecase.GenerateDailyReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.feature.monitoring.notification.ReportNotifier
import org.neteinstein.perfsentient.feature.monitoring.scheduler.WorkManagerMonitoringScheduler
import org.neteinstein.perfsentient.feature.monitoring.worker.DailyReportWorker
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class DailyReportWorkerTest {
    private val generate = mockk<GenerateDailyReportUseCase>()
    private val observeSettings = mockk<ObserveSettingsUseCase>()
    private val notifier = mockk<ReportNotifier>(relaxed = true)
    private val scheduler = mockk<WorkManagerMonitoringScheduler>(relaxed = true)
    private val settings = UserSettings(monitoringEnabled = true)

    init {
        every { observeSettings() } returns flowOf(settings)
    }

    private fun worker(runAttemptCount: Int): DailyReportWorker = TestListenableWorkerBuilder<DailyReportWorker>(
        RuntimeEnvironment.getApplication(),
    )
        .setRunAttemptCount(runAttemptCount)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    DailyReportWorker(appContext, workerParameters, generate, observeSettings, notifier, scheduler)
            },
        )
        .build()

    @Test
    fun `a successful run notifies and chains the next report`() = runTest {
        val report = TestData.report()
        coEvery { generate() } returns report

        assertEquals(ListenableWorker.Result.success(), worker(runAttemptCount = 0).doWork())

        verify { notifier.notify(report) }
        verify { scheduler.scheduleNextReport(settings.reportTime) }
    }

    @Test
    fun `a failing run retries before giving up`() = runTest {
        coEvery { generate() } throws IllegalStateException("boom")

        assertEquals(ListenableWorker.Result.retry(), worker(runAttemptCount = 0).doWork())
        verify(exactly = 0) { scheduler.scheduleNextReport(any()) }
    }

    @Test
    fun `giving up still succeeds so the chained next report is not cancelled`() = runTest {
        coEvery { generate() } throws IllegalStateException("boom")

        assertEquals(ListenableWorker.Result.success(), worker(runAttemptCount = 3).doWork())
        verify { scheduler.scheduleNextReport(settings.reportTime) }
    }
}
