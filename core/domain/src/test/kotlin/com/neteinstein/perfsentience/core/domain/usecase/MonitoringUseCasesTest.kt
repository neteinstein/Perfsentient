package com.neteinstein.perfsentience.core.domain.usecase

import app.cash.turbine.test
import com.neteinstein.perfsentience.core.model.ReportTime
import com.neteinstein.perfsentience.core.model.ThemeMode
import com.neteinstein.perfsentience.core.model.UserSettings
import com.neteinstein.perfsentience.core.testing.FakeMonitoringScheduler
import com.neteinstein.perfsentience.core.testing.FakeNotificationStatsRepository
import com.neteinstein.perfsentience.core.testing.FakePermissionRepository
import com.neteinstein.perfsentience.core.testing.FakeSettingsRepository
import com.neteinstein.perfsentience.core.testing.FakeTimeProvider
import com.neteinstein.perfsentience.core.testing.FakeVitalsRepository
import com.neteinstein.perfsentience.core.testing.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringUseCasesTest {
    private val permissions = FakePermissionRepository()
    private val settings = FakeSettingsRepository()
    private val scheduler = FakeMonitoringScheduler()
    private val sync = SyncMonitoringUseCase(permissions, settings, scheduler)

    @Test
    fun `monitoring does not start until every required permission is granted`() = runTest {
        sync()
        assertTrue(scheduler.ensured.isEmpty())
        assertFalse(settings.state.value.monitoringEnabled)
        assertEquals(1, permissions.refreshCount)
    }

    @Test
    fun `monitoring starts once all permissions are granted`() = runTest {
        permissions.grantAll()
        sync()
        assertEquals(listOf(ReportTime.DEFAULT), scheduler.ensured)
        assertTrue(settings.state.value.monitoringEnabled)
    }

    @Test
    fun `revoking a permission stops monitoring`() = runTest {
        settings.state.value = UserSettings(monitoringEnabled = true)
        sync()
        assertEquals(1, scheduler.cancelCount)
        assertFalse(settings.state.value.monitoringEnabled)
    }

    @Test
    fun `monitoring state reflects permissions and settings`() = runTest {
        ObserveMonitoringStateUseCase(permissions, settings)().test {
            assertEquals(MonitoringState.WAITING_FOR_PERMISSIONS, awaitItem())
            permissions.grantAll()
            settings.setMonitoringEnabled(true)
            assertEquals(MonitoringState.ACTIVE, expectMostRecentItem())
        }
    }

    @Test
    fun `recording a vital sample saves it and prunes old data`() = runTest {
        val time = FakeTimeProvider()
        val vitals = FakeVitalsRepository(next = TestData.vital(at = time.now().toEpochMilli()))
        val notifications = FakeNotificationStatsRepository()
        RecordVitalSampleUseCase(vitals, notifications, time)()
        assertEquals(1, vitals.samples.size)
        assertEquals(time.now().toEpochMilli() - 7 * 24 * 3_600_000L, vitals.deletedBefore)
        assertEquals(time.today().minusDays(7), notifications.deletedBefore)
    }

    @Test
    fun `recording a notification increments today's counter`() = runTest {
        val time = FakeTimeProvider()
        val notifications = FakeNotificationStatsRepository()
        val record = RecordNotificationUseCase(notifications, time)
        record("spam")
        record("spam")
        assertEquals(mapOf("spam" to 2), notifications.getCounts(time.today()))
    }

    @Test
    fun `report time update reschedules only while monitoring`() = runTest {
        val update = UpdateReportTimeUseCase(settings, scheduler)
        update(ReportTime(20, 0))
        assertTrue(scheduler.rescheduled.isEmpty())

        settings.setMonitoringEnabled(true)
        update(ReportTime(22, 15))
        assertEquals(listOf(ReportTime(22, 15)), scheduler.rescheduled)
        assertEquals(ReportTime(22, 15), settings.settings.first().reportTime)
    }

    @Test
    fun `theme and dynamic colour updates are persisted`() = runTest {
        UpdateThemeModeUseCase(settings)(ThemeMode.DARK)
        UpdateDynamicColorUseCase(settings)(true)
        val current = ObserveSettingsUseCase(settings)().first()
        assertEquals(ThemeMode.DARK, current.themeMode)
        assertTrue(current.useDynamicColor)
        assertEquals(permissions.permissions.value, ObservePermissionsUseCase(permissions)().value)
    }
}
