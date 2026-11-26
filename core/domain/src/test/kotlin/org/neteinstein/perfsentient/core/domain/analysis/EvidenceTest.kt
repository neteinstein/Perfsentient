package org.neteinstein.perfsentient.core.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.detector.AfterCallAdsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.NightActivityDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.PopupAdsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.RemoteAccessDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.RiskyInstallSourceDetector
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.Evidence
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.SensitivePermission
import org.neteinstein.perfsentient.core.model.UsageEventType
import org.neteinstein.perfsentient.core.testing.TestData.DIALER
import org.neteinstein.perfsentient.core.testing.TestData.app
import org.neteinstein.perfsentient.core.testing.TestData.event
import org.neteinstein.perfsentient.core.testing.TestData.resumed

class EvidenceTest {
    private val thresholds = DetectionThresholds()

    @Test
    fun `pop-ups record when they happened and which app they interrupted, newest first`() {
        val events = buildList {
            var t = 0L
            repeat(12) {
                add(resumed("chat", t))
                t += MIN
                add(resumed("adware", t))
                t += 5 * SEC
            }
            add(resumed("chat", t))
        }
        val finding = PopupAdsDetector(
            thresholds,
        ).detect(input(apps = listOf(app("adware"), app("chat", label = "Chat")), events = events)).single()

        assertEquals(12L, finding.value)
        assertEquals(Finding.MAX_EVIDENCE, finding.evidence.size)
        assertEquals(Evidence(11 * (MIN + 5 * SEC) + MIN, "chat", "Chat"), finding.evidence.first())
        assertEquals(finding.evidence.sortedByDescending { it.atMs }, finding.evidence)
    }

    @Test
    fun `after-call ads point at the dialer`() {
        // Two calls: a single after-call screen is ordinary use.
        val events = listOf(0L, HOUR).flatMap { base ->
            listOf(resumed(DIALER, base), resumed("adware", base + MIN), resumed("chat", base + MIN + 10 * SEC))
        }
        val finding = AfterCallAdsDetector(thresholds).detect(input(apps = listOf(app("adware", overlay = true)), events = events)).single()

        assertEquals(listOf(Evidence(HOUR + MIN, DIALER, "dialer"), Evidence(MIN, DIALER, "dialer")), finding.evidence)
    }

    @Test
    fun `night activity lists each wake-up`() {
        val events = listOf(event("android", UsageEventType.SCREEN_NON_INTERACTIVE, 0)) +
            (1..3).map { event("owl", UsageEventType.FOREGROUND_SERVICE_START, it * HOUR) }
        val finding = NightActivityDetector(thresholds).detect(input(apps = listOf(app("owl")), events = events)).single()

        assertEquals(listOf(3 * HOUR, 2 * HOUR, HOUR), finding.evidence.map { it.atMs })
        assertNull(finding.evidence.first().relatedPackage)
    }

    @Test
    fun `install findings record the install time and where it came from`() {
        val now = 10 * 24 * HOUR
        val catalog = ScamSignalCatalog()
        val apps = listOf(
            app("rat", installedAt = 0),
            app("com.anydesk.anydeskandroid", installedAt = now - HOUR),
            app(
                "sms.stealer",
                installer = null,
                initiator = "com.whatsapp",
                installedAt = now - 2 * HOUR,
                permissions = setOf(SensitivePermission.READ_SMS),
            ),
            app("com.whatsapp", label = "WhatsApp"),
        )
        val callEvents = listOf(resumed(DIALER, now - HOUR - 5 * MIN), resumed("chat", now - HOUR - MIN))
        val input = input(apps = apps, events = callEvents).copy(windowEndMs = now)

        val remote = RemoteAccessDetector(thresholds, catalog).detect(input).single()
        assertEquals(listOf(Evidence(now - HOUR, DIALER, "dialer")), remote.evidence)

        val risky = RiskyInstallSourceDetector(catalog).detect(input).single { it.packageName == "sms.stealer" }
        assertEquals(listOf(Evidence(now - 2 * HOUR, "com.whatsapp", "WhatsApp")), risky.evidence)
    }
}
