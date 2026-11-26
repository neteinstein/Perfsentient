package org.neteinstein.perfsentient.core.domain.usecase

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.model.BatteryHistory
import org.neteinstein.perfsentient.core.model.BatterySort
import org.neteinstein.perfsentient.core.testing.FakeReportRepository
import org.neteinstein.perfsentient.core.testing.FakeTimeProvider
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.core.testing.TestData.batteryUsage

class ObserveBatteryHistoryUseCaseTest {
    private val reports = FakeReportRepository()
    private val useCase = ObserveBatteryHistoryUseCase(reports, FakeTimeProvider(), DetectionThresholds())
    private val today = LocalDate.of(2026, 11, 16)

    @Test
    fun `sums each app over the week and fills missing days with zero`() = runTest {
        reports.save(
            TestData.report(
                date = today.minusDays(2),
                battery = TestData.battery(batteryUsage("game", drain = 10f, foregroundMs = HOUR), standby = 2f),
            ),
        )
        reports.save(
            TestData.report(
                date = today,
                battery = TestData.battery(
                    batteryUsage("game", drain = 6f, foregroundMs = HOUR),
                    batteryUsage("music", drain = 4f, background = 3f, foregroundMs = MIN, backgroundMs = 5 * HOUR),
                    screenAndSystem = 1f,
                ),
            ),
        )
        reports.save(TestData.report(date = today.minusDays(9), battery = TestData.battery(batteryUsage("old", drain = 50f))))

        val history = useCase().first()

        assertEquals((6L downTo 0).map { today.minusDays(it) }, history.days)
        assertEquals(listOf("game", "music"), history.apps.map { it.packageName })
        val game = history.apps[0]
        assertEquals(16f, game.drainPct, DELTA)
        assertEquals(2 * HOUR, game.foregroundMs)
        assertEquals(8f, game.drainPerHourOpen!!, DELTA)
        assertFalse(game.mostlyBackground)
        assertEquals(listOf(0f, 0f, 0f, 0f, 10f, 0f, 6f), game.daily.map { it.drainPct })
        assertEquals(today, game.daily.last().date)

        val music = history.apps[1]
        assertNull("open for 1 minute: no rate", music.drainPerHourOpen)
        assertTrue(music.mostlyBackground)
        assertEquals(5 * HOUR, music.backgroundMs)

        assertEquals(1f, history.screenAndSystemPct, DELTA)
        assertEquals(2f, history.standbyPct, DELTA)
        assertEquals(23f, history.measuredDrainPct, DELTA)
        assertEquals(2f, history.measuredHours, DELTA)
    }

    @Test
    fun `sorting by battery per hour open puts barely opened apps last`() = runTest {
        reports.save(
            TestData.report(
                battery = TestData.battery(
                    batteryUsage("heavy", drain = 20f, foregroundMs = 10 * HOUR),
                    batteryUsage("hidden", drain = 15f, background = 15f),
                    batteryUsage("intense", drain = 5f, foregroundMs = 30 * MIN),
                ),
            ),
        )

        val history = useCase().first()

        assertEquals(listOf("heavy", "hidden", "intense"), history.sortedBy(BatterySort.MOST_BATTERY).apps.map { it.packageName })
        assertEquals(listOf("intense", "heavy", "hidden"), history.sortedBy(BatterySort.PER_HOUR_OPEN).apps.map { it.packageName })
    }

    @Test
    fun `no battery data gives an empty history`() = runTest {
        assertEquals(BatteryHistory.EMPTY, useCase().first())
        reports.save(TestData.report())
        assertTrue(useCase().first().isEmpty)
    }

    private companion object {
        const val DELTA = 0.001f
        const val MIN = 60_000L
        const val HOUR = 60 * MIN
    }
}
