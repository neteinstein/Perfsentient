package org.neteinstein.perfsentient.feature.monitoring

import android.app.Application
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.perfsentient.core.domain.usecase.CheckSecurityChangesUseCase
import org.neteinstein.perfsentient.core.domain.usecase.RecordVitalSampleUseCase
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.feature.monitoring.notification.SecurityAlertNotifier
import org.neteinstein.perfsentient.feature.monitoring.worker.VitalsSamplerWorker
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class VitalsSamplerWorkerTest {
    private val record = mockk<RecordVitalSampleUseCase>(relaxed = true)
    private val check = mockk<CheckSecurityChangesUseCase>()
    private val notifier = mockk<SecurityAlertNotifier>(relaxed = true)

    private fun worker(runAttemptCount: Int = 0): VitalsSamplerWorker = TestListenableWorkerBuilder<VitalsSamplerWorker>(
        RuntimeEnvironment.getApplication(),
    )
        .setRunAttemptCount(runAttemptCount)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    VitalsSamplerWorker(appContext, workerParameters, record, check, notifier)
            },
        )
        .build()

    @Test
    fun `records vitals and notifies about risky new apps`() = runTest {
        val alerts = listOf(TestData.finding("bad", BehaviorType.REMOTE_ACCESS, Severity.HIGH))
        coEvery { check() } returns alerts

        assertEquals(ListenableWorker.Result.success(), worker().doWork())

        coVerify { record() }
        verify { notifier.notify(alerts) }
    }

    @Test
    fun `a failing security check never fails the sample`() = runTest {
        coEvery { check() } throws IllegalStateException("boom")

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        verify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun `a failing sample is retried and the security check still runs`() = runTest {
        coEvery { record() } throws IllegalStateException("db locked")
        coEvery { check() } returns emptyList()

        assertEquals(ListenableWorker.Result.retry(), worker(runAttemptCount = 0).doWork())
        assertEquals(ListenableWorker.Result.failure(), worker(runAttemptCount = 3).doWork())
        coVerify(exactly = 2) { check() }
    }
}
