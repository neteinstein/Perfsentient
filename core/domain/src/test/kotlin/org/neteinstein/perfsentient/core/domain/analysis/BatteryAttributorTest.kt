package org.neteinstein.perfsentient.core.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.model.BatteryBreakdown
import org.neteinstein.perfsentient.core.model.UsageEventType
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.core.testing.TestData.event
import org.neteinstein.perfsentient.core.testing.TestData.resumed
import org.neteinstein.perfsentient.core.testing.TestData.vital

class BatteryAttributorTest {
    private val attributor = BatteryAttributor(DetectionThresholds())

    private fun window(endMs: Long, build: () -> AnalysisInput) = build().copy(windowStartMs = 0, windowEndMs = endMs)

    @Test
    fun `splits a drop between the apps open in proportion to their time on screen`() {
        val result = attributor.attribute(
            window(HOUR) {
                input(
                    apps = listOf(TestData.app("game", label = "Game"), TestData.app("chat")),
                    events = listOf(
                        resumed("game", 0),
                        resumed("chat", 45 * MIN),
                        event("chat", UsageEventType.SCREEN_NON_INTERACTIVE, HOUR),
                    ),
                    vitals = listOf(vital(0, battery = 100), vital(HOUR, battery = 90)),
                )
            },
        )

        assertEquals(listOf("game", "chat"), result.apps.map { it.packageName })
        assertEquals("Game", result.apps[0].appLabel)
        assertEquals(7.5f, result.apps[0].estimatedDrainPct, DELTA)
        assertEquals(2.5f, result.apps[1].estimatedDrainPct, DELTA)
        assertEquals(45 * MIN, result.apps[0].foregroundMs)
        assertEquals(0f, result.apps[0].backgroundDrainPct, DELTA)
        assertEquals(10f, result.measuredDrainPct, DELTA)
        assertEquals(1f, result.measuredHours, DELTA)
        assertEquals(0f, result.standbyPct, DELTA)
    }

    @Test
    fun `charging intervals, rising levels and long gaps are skipped`() {
        val result = attributor.attribute(
            window(5 * HOUR) {
                input(
                    events = listOf(resumed("game", 0)),
                    vitals = listOf(
                        vital(0, battery = 80, charging = true),
                        vital(30 * MIN, battery = 85),
                        vital(HOUR, battery = 84),
                        vital(4 * HOUR, battery = 60), // 3 h gap: the phone was off or the sampler was late
                    ),
                )
            },
        )

        assertEquals(1f, result.measuredDrainPct, DELTA)
        assertEquals(0.5f, result.measuredHours, DELTA)
        assertEquals(1f, result.apps.single().estimatedDrainPct, DELTA)
    }

    @Test
    fun `a background service with the screen off outweighs standby`() {
        val result = attributor.attribute(
            window(HOUR) {
                input(
                    apps = listOf(TestData.app("music")),
                    events = listOf(event("music", UsageEventType.FOREGROUND_SERVICE_START, 0)),
                    vitals = listOf(vital(0, battery = 50, screenOn = false), vital(HOUR, battery = 47, screenOn = false)),
                )
            },
        )

        val music = result.apps.single()
        assertEquals(2f, music.estimatedDrainPct, DELTA)
        assertEquals(2f, music.backgroundDrainPct, DELTA)
        assertEquals(HOUR, music.backgroundMs)
        assertEquals(0L, music.foregroundMs)
        assertEquals(1f, result.standbyPct, DELTA)
    }

    @Test
    fun `service time while the app is on screen counts as foreground only`() {
        val result = attributor.attribute(
            window(HOUR) {
                input(
                    events = listOf(
                        resumed("maps", 0),
                        event("maps", UsageEventType.FOREGROUND_SERVICE_START, 0),
                        event("maps", UsageEventType.FOREGROUND_SERVICE_STOP, HOUR),
                        event("maps", UsageEventType.SCREEN_NON_INTERACTIVE, HOUR),
                    ),
                    vitals = listOf(vital(0, battery = 60), vital(HOUR, battery = 55)),
                )
            },
        )

        val maps = result.apps.single()
        assertEquals(5f, maps.estimatedDrainPct, DELTA)
        assertEquals(0f, maps.backgroundDrainPct, DELTA)
        assertEquals(0L, maps.backgroundMs)
    }

    @Test
    fun `the charge counter measures drops smaller than one percent`() {
        val events = listOf(resumed("reader", 0))
        val precise = attributor.attribute(
            window(HOUR) {
                input(
                    events = events,
                    vitals = listOf(
                        vital(0, battery = 50, chargeCounter = 2_000_000),
                        vital(HOUR, battery = 50, chargeCounter = 1_990_000),
                    ),
                )
            },
        )
        val coarse = attributor.attribute(
            window(HOUR) {
                input(events = events, vitals = listOf(vital(0, battery = 50, chargeCounter = 2_000_000), vital(HOUR, battery = 50)))
            },
        )

        // 10 000 of 2 000 000 at 50 % → 0.25 % of a full battery.
        assertEquals(0.25f, precise.apps.single().estimatedDrainPct, DELTA)
        assertEquals(0f, coarse.measuredDrainPct, DELTA)
        assertEquals(0f, coarse.apps.single().estimatedDrainPct, DELTA)
    }

    @Test
    fun `home screen and idle time go to the screen-and-system and standby buckets`() {
        val result = attributor.attribute(
            window(HOUR) {
                input(
                    events = listOf(
                        resumed(TestData.LAUNCHER, 0),
                        resumed("app", 30 * MIN),
                        event("android", UsageEventType.SCREEN_NON_INTERACTIVE, 45 * MIN),
                    ),
                    vitals = listOf(vital(0, battery = 100), vital(HOUR, battery = 94)),
                )
            },
        )

        // Weights: launcher 30 min, app 15 min, screen off 15 min × 0.1.
        val total = 30f + 15f + 1.5f
        assertEquals(listOf("app"), result.apps.map { it.packageName })
        assertEquals(6f * 15f / total, result.apps.single().estimatedDrainPct, DELTA)
        assertEquals(6f * 30f / total, result.screenAndSystemPct, DELTA)
        assertEquals(6f * 1.5f / total, result.standbyPct, DELTA)
        assertEquals(
            6f,
            result.apps.sumOf {
                it.estimatedDrainPct.toDouble()
            }.toFloat() + result.screenAndSystemPct + result.standbyPct,
            DELTA,
        )
    }

    @Test
    fun `screen on with no app open is screen and system`() {
        val result = attributor.attribute(
            window(HOUR) { input(vitals = listOf(vital(0, battery = 70), vital(HOUR, battery = 68))) },
        )

        assertTrue(result.apps.isEmpty())
        assertEquals(2f, result.screenAndSystemPct, DELTA)
    }

    @Test
    fun `apps used only while charging are listed with no battery`() {
        val result = attributor.attribute(
            window(HOUR) {
                input(events = listOf(resumed("video", 0)), vitals = listOf(vital(0, charging = true), vital(HOUR, charging = true)))
            },
        )

        assertEquals(0f, result.apps.single().estimatedDrainPct, DELTA)
        assertEquals(HOUR, result.apps.single().foregroundMs)
        assertEquals(0f, result.measuredHours, DELTA)
    }

    @Test
    fun `uninstalled apps keep their package name and the list is capped`() {
        val attributor = BatteryAttributor(DetectionThresholds(batteryTopApps = 2))
        val result = attributor.attribute(
            window(HOUR) {
                input(
                    events = listOf(resumed("a.gone", 0), resumed("b", 30 * MIN), resumed("c", 50 * MIN)),
                    vitals = listOf(vital(0, battery = 100), vital(HOUR, battery = 90)),
                )
            },
        )

        assertEquals(listOf("a.gone", "b"), result.apps.map { it.packageName })
        assertEquals("a.gone", result.apps.first().appLabel)
    }

    @Test
    fun `fewer than two samples give an empty breakdown`() {
        assertEquals(BatteryBreakdown.EMPTY, attributor.attribute(input(vitals = listOf(vital(0)))))
        assertFalse(attributor.attribute(input(vitals = listOf(vital(0), vital(MIN, battery = 79)))) == BatteryBreakdown.EMPTY)
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
