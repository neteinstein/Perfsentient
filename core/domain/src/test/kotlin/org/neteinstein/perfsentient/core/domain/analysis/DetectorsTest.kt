package org.neteinstein.perfsentient.core.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.detector.AfterCallAdsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.AppStabilityDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.BackgroundDataDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.CapabilityRiskDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.HiddenIconDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.NightActivityDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.NotificationSpamDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.OverlayAbuseDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.PersistentBackgroundDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.PopupAdsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.StorageBloatDetector
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.UsageEventType
import org.neteinstein.perfsentient.core.testing.TestData.DIALER
import org.neteinstein.perfsentient.core.testing.TestData.LAUNCHER
import org.neteinstein.perfsentient.core.testing.TestData.SELF
import org.neteinstein.perfsentient.core.testing.TestData.anr
import org.neteinstein.perfsentient.core.testing.TestData.app
import org.neteinstein.perfsentient.core.testing.TestData.crash
import org.neteinstein.perfsentient.core.testing.TestData.event
import org.neteinstein.perfsentient.core.testing.TestData.network
import org.neteinstein.perfsentient.core.testing.TestData.resumed
import org.neteinstein.perfsentient.core.testing.TestData.usage

class DetectorsTest {
    private val thresholds = DetectionThresholds()

    private fun popup(input: AnalysisInput) = PopupAdsDetector(thresholds).detect(input)

    private fun afterCall(input: AnalysisInput) = AfterCallAdsDetector(thresholds).detect(input)

    // region Pop-up ads
    private fun popupEvents(times: Int, adDurationMs: Long = 5 * SEC): List<org.neteinstein.perfsentient.core.model.UsageEventRecord> {
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
        val result = popup(input(apps = listOf(app("adware", overlay = true), app("chat")), events = popupEvents(3)))
        assertEquals(1, result.size)
        assertEquals(BehaviorType.OUT_OF_CONTEXT_ADS, result[0].type)
        assertEquals(Severity.MEDIUM, result[0].severity)
        assertEquals(3L, result[0].value)
    }

    @Test
    fun `popup detector escalates to high and ignores long sessions`() {
        val high = popup(input(apps = listOf(app("adware", overlay = true), app("chat")), events = popupEvents(8)))
        assertEquals(Severity.HIGH, high.single().severity)

        val long = popup(
            input(
                apps = listOf(app("adware", overlay = true), app("chat")),
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
            popup(input(apps = listOf(app("adware", system = true)), events = popupEvents(5))).isEmpty(),
        )
        assertTrue(popup(input(apps = listOf(app("adware", overlay = true)), events = popupEvents(2))).isEmpty())
    }

    @Test
    fun `popup detector needs many more occurrences for apps that cannot launch from the background`() {
        val apps = listOf(app("adware"), app("chat"))
        assertTrue(popup(input(apps = apps, events = popupEvents(7))).isEmpty())
        assertEquals(Severity.MEDIUM, popup(input(apps = apps, events = popupEvents(20))).single().severity)
    }

    @Test
    fun `popup detector ignores the app that was open when the phone was locked`() {
        val events = (0 until 5).flatMap { i ->
            val base = i * HOUR
            listOf(
                resumed("loyalty-card", base),
                event("android", UsageEventType.SCREEN_NON_INTERACTIVE, base + 10 * SEC),
                event("android", UsageEventType.KEYGUARD_SHOWN, base + 10 * SEC),
                event("android", UsageEventType.KEYGUARD_HIDDEN, base + MIN),
                resumed("loyalty-card", base + MIN + SEC),
                event("android", UsageEventType.SCREEN_NON_INTERACTIVE, base + MIN + 10 * SEC),
            )
        }
        assertTrue(popup(input(apps = listOf(app("loyalty-card", overlay = true)), events = events)).isEmpty())
    }

    @Test
    fun `popup detector counts pop-ups right after unlock`() {
        val events = (0 until 3).flatMap { i ->
            val base = i * HOUR
            listOf(event("android", UsageEventType.KEYGUARD_HIDDEN, base), resumed("adware", base + SEC), resumed(LAUNCHER, base + 6 * SEC))
        }
        val result = popup(input(apps = listOf(app("adware", overlay = true)), events = events))
        assertEquals(3L, result.single().value)
    }
    // endregion

    private fun afterCallEvents(calls: Int, app: String = "caller-ads", before: String = LAUNCHER) = (0 until calls).flatMap { i ->
        val base = i * HOUR
        listOf(
            resumed(before, base),
            resumed(DIALER, base + MIN),
            resumed(app, base + 3 * MIN),
            resumed(LAUNCHER, base + 3 * MIN + 10 * SEC),
        )
    }

    @Test
    fun `after-call detector flags repeated short sessions right after the dialer`() {
        val apps = listOf(app("caller-ads", overlay = true))
        val result = afterCall(input(apps = apps, events = afterCallEvents(2)))
        assertEquals(BehaviorType.AFTER_CALL_ADS, result.single().type)
        assertEquals(Severity.MEDIUM, result.single().severity)
        assertEquals(Severity.HIGH, afterCall(input(apps = apps, events = afterCallEvents(4))).single().severity)
    }

    @Test
    fun `after-call detector ignores a single call and apps that cannot launch from the background`() {
        assertTrue(afterCall(input(apps = listOf(app("caller-ads", overlay = true)), events = afterCallEvents(1))).isEmpty())
        val noOverlay = listOf(app("caller-ads"))
        assertTrue(afterCall(input(apps = noOverlay, events = afterCallEvents(3))).isEmpty())
        assertEquals(Severity.MEDIUM, afterCall(input(apps = noOverlay, events = afterCallEvents(4))).single().severity)
    }

    @Test
    fun `after-call detector ignores going back to the app the call interrupted`() {
        val events = afterCallEvents(5, app = "slack", before = "slack")
        assertTrue(afterCall(input(apps = listOf(app("slack", overlay = true)), events = events)).isEmpty())
    }

    @Test
    fun `after-call detector ignores going home first`() {
        val events = listOf(resumed(DIALER, 0), resumed(LAUNCHER, MIN), resumed("whatsapp", MIN + SEC), resumed(LAUNCHER, MIN + 10 * SEC))
        assertTrue(afterCall(input(apps = listOf(app("whatsapp")), events = events)).isEmpty())
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
            app("active-plugin", launcher = false),
            app("active-hidden", launcher = false, iconDisabled = true),
            app("idle-hidden", launcher = false, iconDisabled = true),
            app("risky-hidden", launcher = false, accessibility = true),
            app("sideloaded-hidden", launcher = false, installer = null),
        )
        val result = HiddenIconDetector(thresholds).detect(
            input(
                apps = apps,
                usage = listOf(usage("active-hidden", serviceMs = MIN), usage("active-plugin", serviceMs = HOUR)),
                network = listOf(network("sideloaded-hidden", background = 5 * MB)),
            ),
        ).associateBy { it.packageName }
        assertTrue("idle-plugin" !in result)
        assertTrue("store apps that never had an icon are fine", "active-plugin" !in result)
        assertTrue("idle-hidden" !in result)
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
    fun `background data detector tolerates much more unmetered wi-fi traffic`() {
        val result = BackgroundDataDetector(thresholds).detect(
            input(
                apps = listOf(app("prefetcher"), app("uploader"), app("hog")),
                network = listOf(
                    network("prefetcher", background = 10 * MB, backgroundWifi = 300 * MB),
                    network("uploader", backgroundWifi = 600 * MB),
                    network("hog", backgroundWifi = 3 * 1024 * MB),
                ),
            ),
        ).associateBy { it.packageName }
        assertEquals(setOf("uploader", "hog"), result.keys)
        assertEquals(Severity.MEDIUM, result.getValue("uploader").severity)
        assertEquals(Severity.HIGH, result.getValue("hog").severity)
        val mixed = BackgroundDataDetector(thresholds).detect(
            input(apps = listOf(app("x")), network = listOf(network("x", background = 60 * MB, backgroundWifi = 550 * MB))),
        )
        assertEquals("reports total background bytes", 610 * MB, mixed.single().value)
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

    @Test
    fun `stability detector flags repeated ANRs and crashes and escalates`() {
        val issues = List(2) { anr("frozen", it.toLong()) } +
            List(5) { anr("very-frozen", it.toLong()) } +
            List(3) { crash("crashy", it.toLong()) } +
            List(10) { crash("very-crashy", it.toLong()) }
        val result = AppStabilityDetector(thresholds).detect(
            input(apps = listOf(app("frozen"), app("very-frozen"), app("crashy"), app("very-crashy")), appIssues = issues),
        ).associateBy { it.packageName to it.type }
        assertEquals(Severity.MEDIUM, result.getValue("frozen" to BehaviorType.APP_NOT_RESPONDING).severity)
        assertEquals(2L, result.getValue("frozen" to BehaviorType.APP_NOT_RESPONDING).value)
        assertEquals(Severity.HIGH, result.getValue("very-frozen" to BehaviorType.APP_NOT_RESPONDING).severity)
        assertEquals(Severity.MEDIUM, result.getValue("crashy" to BehaviorType.APP_CRASHES).severity)
        assertEquals(Severity.HIGH, result.getValue("very-crashy" to BehaviorType.APP_CRASHES).severity)
    }

    @Test
    fun `stability detector ignores occasional failures, system apps and unknown packages`() {
        val issues = listOf(anr("once"), crash("once"), crash("once")) +
            List(5) { anr("system-app") } +
            List(5) { anr("uninstalled") }
        val result = AppStabilityDetector(thresholds).detect(
            input(apps = listOf(app("once"), app("system-app", system = true)), appIssues = issues),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `stability detector reports nothing without system-log access`() {
        assertTrue(AppStabilityDetector(thresholds).detect(input(apps = listOf(app("a")))).isEmpty())
    }
}
