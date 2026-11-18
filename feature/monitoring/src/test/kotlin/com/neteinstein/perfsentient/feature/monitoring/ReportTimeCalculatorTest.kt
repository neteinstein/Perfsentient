package com.neteinstein.perfsentient.feature.monitoring

import com.neteinstein.perfsentient.core.model.ReportTime
import com.neteinstein.perfsentient.feature.monitoring.scheduler.ReportTimeCalculator
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportTimeCalculatorTest {
    private fun at(hour: Int, minute: Int) = ZonedDateTime.of(2026, 11, 16, hour, minute, 0, 0, ZoneOffset.UTC)

    @Test
    fun `schedules later today when the time is still ahead`() {
        assertEquals(Duration.ofHours(2), ReportTimeCalculator.delayUntilNext(at(19, 30), ReportTime(21, 30)))
    }

    @Test
    fun `schedules tomorrow when the time has passed`() {
        assertEquals(Duration.ofHours(23), ReportTimeCalculator.delayUntilNext(at(22, 30), ReportTime(21, 30)))
    }

    @Test
    fun `never schedules for right now to avoid double reports`() {
        assertEquals(Duration.ofHours(24), ReportTimeCalculator.delayUntilNext(at(21, 30), ReportTime(21, 30)))
    }
}
