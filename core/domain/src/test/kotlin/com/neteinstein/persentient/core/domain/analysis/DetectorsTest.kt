package com.neteinstein.persentient.core.domain.analysis

import com.neteinstein.persentient.core.domain.analysis.detector.AfterCallAdsDetector
import com.neteinstein.persentient.core.domain.analysis.detector.BackgroundDataDetector
import com.neteinstein.persentient.core.domain.analysis.detector.CapabilityRiskDetector
import com.neteinstein.persentient.core.domain.analysis.detector.HiddenIconDetector
import com.neteinstein.persentient.core.domain.analysis.detector.NightActivityDetector
import com.neteinstein.persentient.core.domain.analysis.detector.NotificationSpamDetector
import com.neteinstein.persentient.core.domain.analysis.detector.OverlayAbuseDetector
import com.neteinstein.persentient.core.domain.analysis.detector.PersistentBackgroundDetector
import com.neteinstein.persentient.core.domain.analysis.detector.PopupAdsDetector
import com.neteinstein.persentient.core.domain.analysis.detector.StorageBloatDetector
import com.neteinstein.persentient.core.model.BehaviorType
import com.neteinstein.persentient.core.model.Severity
import com.neteinstein.persentient.core.model.UsageEventType
import com.neteinstein.persentient.core.testing.TestData.DIALER
import com.neteinstein.persentient.core.testing.TestData.LAUNCHER
import com.neteinstein.persentient.core.testing.TestData.SELF
import com.neteinstein.persentient.core.testing.TestData.app
import com.neteinstein.persentient.core.testing.TestData.event
import com.neteinstein.persentient.core.testing.TestData.network
import com.neteinstein.persentient.core.testing.TestData.resumed
import com.neteinstein.persentient.core.testing.TestData.usage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorsTest {
    private val thresholds = DetectionThresholds()

    // region Pop-up ads
    private fun popupEvents(times: Int, adDurationMs: Long = 5 * SEC): List<com.neteinstein.persentient.core.model.UsageEventRecord> {
        var t = 0L
        return buildList {
            repeat(times) {
                add(resumed("chat", t))
                t += MIN
                add(resumed("adware", t))
                t += adDurationMs
                add(resumed("chat", t))
                t += MIN
                add(resumed(LAUNCHER, t))
                t += MIN
            }
        }
    }

    @Test
    fun `popup detector flags A-X-A interruptions above threshold`() {
        val result = PopupAdsDetector(thresholds).detect(input(apps = listOf(app("adware"), app("chat")), events = popupEvents(3)))
        assertEquals(1, result.size)
        assertEquals(BehaviorType.OUT_OF_CONTEXT_ADS, result[0].type)
        assertEquals(Severity.MEDIUM, result[0].severity)
        assertEquals(3L, result[0].value)
    }

    @Test
    fun `popup detector escalates to high and ignores long sessions`() {
        val high = PopupAdsDetector(thresholds).detect(input(apps = listOf(app("adware"), app("chat")), events = popupEvents(8)))
        assertEquals(Severity.HIGH, high.single().severity)

        val long = PopupAdsDetector(thresholds).detect(
            input(
                apps = listOf(app("adware"), app("chat")),
                events = popupEvents(
                    8,
                    adDurationMs =
                        5 * MIN,
                ),
            ),
        )
        assertTrue(long.isEmpty())
    }

    @Test
    fun `popup detector ignores system apps and below-threshold counts`() {
        assertTrue(
            PopupAdsDetector(thresholds).detect(input(apps = listOf(app("adware", system = true)), events = popupEvents(5))).isEmpty(),
        )
        assertTrue(PopupAdsDetector(thresholds).detect(input(apps = listOf(app("adware")), events = popupEvents(2))).isEmpty())
    }

    @Test
    fun `popup detector counts pop-ups right after unlock`() {
        val events = (0 until 3).flatMap { i ->
            val base = i * HOUR
            listOf(event("android", UsageEventType.KEYGUARD_HIDDEN, base), resumed("adware", base + SEC), resumed(LAUNCHER, base + 6 * SEC))
        }
        val result = PopupAdsDetector(thresholds).detect(input(apps = listOf(app("adware")), events = events))
        assertEquals(3L, result.single().value)
    }
    // endregion

    @Test
    fun `after-call detector flags short sessions right after the dialer`() {
        val events = listOf(resumed(DIALER, 0), resumed("caller-ads", 2 * MIN), resumed(LAUNCHER, 2 * MIN + 10 * SEC))
        val result = AfterCallAdsDetector(thresholds).detect(input(apps = listOf(app("caller-ads")), events = events))
        assertEquals(BehaviorType.AFTER_CALL_ADS, result.single().type)
        assertEquals(Severity.MEDIUM, result.single().severity)
    }

    @Test
    fun `after-call detector ignores going home first`() {
        val events = listOf(resumed(DIALER, 0), resumed(LAUNCHER, MIN), resumed("whatsapp", MIN + SEC), resumed(LAUNCHER, MIN + 10 * SEC))
        assertTrue(AfterCallAdsDetector(thresholds).detect(input(apps = listOf(app("whatsapp")), events = events)).isEmpty())
    }

    @Test
    fun `overlay detector flags hidden overlay apps as high`() {
        val result = OverlayAbuseDetector(thresholds).detect(input(apps = listOf(app("ghost", overlay = true, launcher = false))))
        assertEquals(Severity.HIGH, result.single().severity)
    }

    @Test
    fun `overlay detector flags rarely used busy overlay apps`() {
        val apps =
            listOf(app("cleaner", overlay = true), app("sideloaded", overlay = true, installer = null), app("chathead", overlay = true))
        val result = OverlayAbuseDetector(thresholds).detect(
            input(
                apps = apps,
                usage = listOf(usage("cleaner", serviceMs = HOUR), usage("chathead", foregroundMs = 2 * HOUR, serviceMs = 5 * HOUR)),
                network = listOf(network("sideloaded", background = 30 * MB)),
            ),
        ).associateBy { it.packageName }
        assertEquals(Severity.MEDIUM, result.getValue("cleaner").severity)
        assertEquals(Severity.HIGH, result.getValue("sideloaded").severity)
        assertTrue("actively used app is fine", "chathead" !in result)
    }

    @Test
    fun `hidden icon detector needs activity or risky capabilities`() {
        val apps = listOf(
            app("idle-plugin", launcher = false),
            app("active-hidden", launcher = false),
            app("risky-hidden", launcher = false, accessibility = true),
            app("sideloaded-hidden", launcher = false, installer = null),
        )
        val result = HiddenIconDetector(thresholds).detect(
            input(
                apps = apps,
                usage = listOf(usage("active-hidden", serviceMs = MIN)),
                network = listOf(network("sideloaded-hidden", background = 5 * MB)),
            ),
        ).associateBy { it.packageName }
        assertTrue("idle-plugin" !in result)
        assertEquals(Severity.MEDIUM, result.getValue("active-hidden").severity)
        assertEquals(Severity.HIGH, result.getValue("risky-hidden").severity)
        assertEquals(Severity.HIGH, result.getValue("sideloaded-hidden").severity)
    }

    @Test
    fun `persistent background detector uses vitals wake lock thresholds`() {
        val result = PersistentBackgroundDetector(thresholds).detect(
            input(
                apps = listOf(app("tracker"), app("heavy"), app("music"), app("fine")),
                usage = listOf(
                    usage("tracker", serviceMs = 3 * HOUR),
                    usage("heavy", serviceMs = 9 * HOUR),
                    usage("music", foregroundMs = HOUR, serviceMs = 9 * HOUR),
                    usage("fine", serviceMs = HOUR),
                ),
            ),
        ).associateBy { it.packageName }
        assertEquals(setOf("tracker", "heavy"), result.keys)
        assertEquals(Severity.MEDIUM, result.getValue("tracker").severity)
        assertEquals(Severity.HIGH, result.getValue("heavy").severity)
    }

    @Test
    fun `night detector counts activity launches with screen off at night`() {
        val events = listOf(
            event("android", UsageEventType.SCREEN_NON_INTERACTIVE, HOUR),
            resumed("owl", 2 * HOUR), // weight 2
            event("owl", UsageEventType.FOREGROUND_SERVICE_START, 3 * HOUR), // weight 1
            event("owl", UsageEventType.FOREGROUND_SERVICE_START, 4 * HOUR), // weight 1
            event("android", UsageEventType.SCREEN_INTERACTIVE, 7 * HOUR),
            resumed("day", 8 * HOUR),
        )
        val result = NightActivityDetector(thresholds).detect(input(apps = listOf(app("owl"), app("day")), events = events))
        assertEquals("owl", result.single().packageName)
        assertEquals(4L, result.single().value)
    }

    @Test
    fun `background data detector applies 50MB and 200MB thresholds`() {
        val result = BackgroundDataDetector(thresholds).detect(
            input(
                apps = listOf(app("a"), app("b"), app("c"), app(SELF)),
                network = listOf(
                    network("a", background = 60 * MB),
                    network("b", background = 250 * MB),
                    network("c", background = 10 * MB),
                    network(SELF, background = 999 * MB),
                ),
            ),
        ).associateBy { it.packageName }
        assertEquals(setOf("a", "b"), result.keys)
        assertEquals(Severity.HIGH, result.getValue("b").severity)
    }

    @Test
    fun `notification spam tolerates used apps unless extreme`() {
        val result = NotificationSpamDetector(thresholds).detect(
            input(
                apps = listOf(app("promo"), app("loud"), app("messenger"), app("busy-messenger")),
                usage = listOf(usage("messenger", foregroundMs = HOUR), usage("busy-messenger", foregroundMs = HOUR)),
                notifications = mapOf("promo" to 45, "loud" to 120, "messenger" to 90, "busy-messenger" to 200),
            ),
        ).associateBy { it.packageName }
        assertEquals(Severity.MEDIUM, result.getValue("promo").severity)
        assertEquals(Severity.HIGH, result.getValue("loud").severity)
        assertTrue("messenger" !in result)
        assertEquals(Severity.MEDIUM, result.getValue("busy-messenger").severity)
    }

    @Test
    fun `capability detector escalates sideloaded apps`() {
        val result = CapabilityRiskDetector(thresholds).detect(
            input(
                apps = listOf(
                    app("password-manager", accessibility = true),
                    app("rogue", accessibility = true, admin = true, installer = null),
                    app("exempt", batteryExempt = true),
                ),
                usage = listOf(usage("exempt", serviceMs = 2 * HOUR)),
            ),
        )
        val byKey = result.associateBy { it.packageName to it.type }
        assertEquals(Severity.LOW, byKey.getValue("password-manager" to BehaviorType.ACCESSIBILITY_RISK).severity)
        assertEquals(Severity.HIGH, byKey.getValue("rogue" to BehaviorType.ACCESSIBILITY_RISK).severity)
        assertEquals(Severity.HIGH, byKey.getValue("rogue" to BehaviorType.DEVICE_ADMIN).severity)
        assertEquals(Severity.MEDIUM, byKey.getValue("exempt" to BehaviorType.UNRESTRICTED_BATTERY).severity)
    }

    @Test
    fun `storage detector flags large caches`() {
        val gb = 1024 * MB
        val result = StorageBloatDetector(thresholds).detect(
            input(apps = listOf(app("small", cacheBytes = 100 * MB), app("big", cacheBytes = 2 * gb), app("huge", cacheBytes = 4 * gb))),
        ).associateBy { it.packageName }
        assertEquals(setOf("big", "huge"), result.keys)
        assertEquals(Severity.LOW, result.getValue("big").severity)
        assertEquals(Severity.MEDIUM, result.getValue("huge").severity)
    }
}
