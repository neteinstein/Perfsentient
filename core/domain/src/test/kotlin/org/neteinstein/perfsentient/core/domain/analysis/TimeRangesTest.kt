package org.neteinstein.perfsentient.core.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.model.UsageEventType.FOREGROUND_SERVICE_START
import org.neteinstein.perfsentient.core.model.UsageEventType.FOREGROUND_SERVICE_STOP
import org.neteinstein.perfsentient.core.model.UsageEventType.SCREEN_INTERACTIVE
import org.neteinstein.perfsentient.core.model.UsageEventType.SCREEN_NON_INTERACTIVE
import org.neteinstein.perfsentient.core.testing.TestData.event

class TimeRangesTest {
    @Test
    fun `minus cuts holes and keeps what is left`() {
        val ranges = listOf(TimeRange(0, 100))
        assertEquals(
            listOf(TimeRange(0, 10), TimeRange(20, 50), TimeRange(60, 100)),
            ranges.minus(listOf(TimeRange(50, 60), TimeRange(10, 20))),
        )
        assertEquals(ranges, ranges.minus(emptyList()))
        assertTrue(ranges.minus(listOf(TimeRange(-5, 200))).isEmpty())
        assertEquals(listOf(TimeRange(0, 100)), ranges.minus(listOf(TimeRange(100, 200), TimeRange(-50, 0))))
    }

    @Test
    fun `overlap is clipped to the window`() {
        val ranges = listOf(TimeRange(0, 10), TimeRange(20, 30))
        assertEquals(10L, ranges.overlapMs(5, 25))
        assertEquals(0L, ranges.overlapMs(40, 50))
        assertEquals(0L, TimeRange(10, 5).durationMs)
    }

    @Test
    fun `nested services end when the last one stops`() {
        val ranges = ForegroundServiceIntervals.build(
            listOf(
                event("a", FOREGROUND_SERVICE_START, 0),
                event("a", FOREGROUND_SERVICE_START, 10),
                event("a", FOREGROUND_SERVICE_STOP, 20),
                event("a", FOREGROUND_SERVICE_STOP, 30),
                event("a", FOREGROUND_SERVICE_STOP, 35), // stray: ignored
            ),
            windowStartMs = 0,
            windowEndMs = 100,
        )
        assertEquals(mapOf("a" to listOf(TimeRange(0, 30))), ranges)
    }

    @Test
    fun `unclosed services run to the end and a stop before any start runs from the start`() {
        val ranges = ForegroundServiceIntervals.build(
            listOf(
                event("early", FOREGROUND_SERVICE_STOP, 5),
                event("early", FOREGROUND_SERVICE_START, 40),
                event("early", FOREGROUND_SERVICE_STOP, 50),
                event("late", FOREGROUND_SERVICE_START, 70),
                event("other", SCREEN_INTERACTIVE, 1),
            ),
            windowStartMs = 0,
            windowEndMs = 100,
        )
        assertEquals(listOf(TimeRange(0, 5), TimeRange(40, 50)), ranges["early"])
        assertEquals(listOf(TimeRange(70, 100)), ranges["late"])
        assertEquals(setOf("early", "late"), ranges.keys)
    }

    @Test
    fun `screen off ranges follow the screen events`() {
        val events = listOf(
            event("android", SCREEN_NON_INTERACTIVE, 10),
            event("android", SCREEN_NON_INTERACTIVE, 15),
            event("android", SCREEN_INTERACTIVE, 30),
            event("android", SCREEN_NON_INTERACTIVE, 80),
        )
        assertEquals(listOf(TimeRange(10, 30), TimeRange(80, 100)), ScreenOffIntervals.build(events, 0, 100, screenOnAtStart = false))
    }

    @Test
    fun `screen state before the first event is inferred from it`() {
        val turnedOn = listOf(event("android", SCREEN_INTERACTIVE, 20), event("android", SCREEN_INTERACTIVE, 25))
        assertEquals(listOf(TimeRange(0, 20)), ScreenOffIntervals.build(turnedOn, 0, 100, screenOnAtStart = true))
        assertEquals(listOf(TimeRange(0, 100)), ScreenOffIntervals.build(emptyList(), 0, 100, screenOnAtStart = false))
        assertTrue(ScreenOffIntervals.build(emptyList(), 0, 100, screenOnAtStart = true).isEmpty())
    }
}
