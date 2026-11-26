package org.neteinstein.perfsentient.core.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.detector.CanInstallAppsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.DataTheftRiskDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.ImpersonationDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.RemoteAccessDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.RiskyInstallSourceDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.ScamChainDetector
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Evidence
import org.neteinstein.perfsentient.core.model.ScamMessageKind
import org.neteinstein.perfsentient.core.model.ScamMessageSignal
import org.neteinstein.perfsentient.core.model.SensitivePermission
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.SmsPartnerStatus
import org.neteinstein.perfsentient.core.testing.TestData.app

class ScamChainTest {
    private val thresholds = DetectionThresholds()
    private val catalog = ScamSignalCatalog()
    private val partnerPkg = "com.neteinstein.smsredirect"
    private val verified =
        SmsPartnerStatus(partnerPkg, verified = true, connected = true, isDefaultSmsApp = true, checkedToday = 3, suspiciousToday = 1)
    private val textAt = NOW - 3 * HOUR
    private val scamText =
        ScamMessageSignal(textAt, setOf(ScamMessageKind.DELIVERY_OR_FEE, ScamMessageKind.SHORTENED_LINK), hasLink = true, threadId = 42)

    private fun input(vararg apps: AppProfile, texts: List<ScamMessageSignal> = listOf(scamText), partner: SmsPartnerStatus = verified) =
        input(apps = apps.toList()).copy(windowEndMs = NOW, scamMessages = texts, partner = partner)

    @Test
    fun `a sideloaded app installed soon after a scam text with a link is high`() {
        val findings = ScamChainDetector(thresholds).detect(
            input(
                app("parcel.tracker", installer = null, installedAt = textAt + 6 * MIN),
                app("store.app", installedAt = textAt + 6 * MIN),
                app("late.app", installer = null, installedAt = textAt + 3 * HOUR),
                app("early.app", installer = null, installedAt = textAt - MIN),
                app(partnerPkg, installer = null, installedAt = textAt + MIN),
            ),
        )

        val finding = findings.single()
        assertEquals("parcel.tracker", finding.packageName)
        assertEquals(BehaviorType.SCAM_LINK_INSTALL, finding.type)
        assertEquals(Severity.HIGH, finding.severity)
        assertEquals(42L, finding.value)
        assertEquals(listOf(Evidence(textAt, partnerPkg, "smsredirect")), finding.evidence)
    }

    @Test
    fun `texts without a link or no texts at all raise nothing`() {
        val noLink = scamText.copy(hasLink = false)
        val app = app("parcel.tracker", installer = null, installedAt = textAt + MIN)
        assertTrue(ScamChainDetector(thresholds).detect(input(app, texts = listOf(noLink))).isEmpty())
        assertTrue(ScamChainDetector(thresholds).detect(input(app, texts = emptyList())).isEmpty())
    }

    @Test
    fun `remote-access and risky installs right after a scam text become high`() {
        val anydesk = app("com.anydesk.anydeskandroid", installedAt = textAt + 20 * MIN)
        val download = app("tracker", installer = null, initiator = "com.android.chrome", installedAt = textAt + 5 * MIN, overlay = true)

        assertEquals(Severity.HIGH, RemoteAccessDetector(thresholds, catalog).detect(input(anydesk)).single().severity)
        assertEquals(
            Severity.MEDIUM,
            RemoteAccessDetector(thresholds, catalog).detect(input(anydesk, texts = emptyList())).single().severity,
        )
        assertEquals(Severity.HIGH, RiskyInstallSourceDetector(catalog, thresholds).detect(input(download)).single().severity)
        assertEquals(
            Severity.MEDIUM,
            RiskyInstallSourceDetector(catalog, thresholds).detect(input(download, texts = emptyList())).single().severity,
        )
    }

    @Test
    fun `genuine SMS Toolbox is not flagged for reading texts or installing its updates`() {
        val toolbox =
            app(
                partnerPkg,
                label = "SMS Toolbox",
                installer = null,
                defaultSms = true,
                canInstallApps = true,
                permissions = setOf(SensitivePermission.READ_SMS),
            )

        assertTrue(DataTheftRiskDetector().detect(input(toolbox)).isEmpty())
        assertTrue(CanInstallAppsDetector(catalog).detect(input(toolbox)).isEmpty())
        assertTrue(ImpersonationDetector(catalog).detect(input(toolbox)).isEmpty())
    }

    @Test
    fun `a copy of SMS Toolbox signed by someone else is a high impersonation`() {
        val fake =
            app(partnerPkg, label = "SMS Toolbox", installer = null, defaultSms = true, permissions = setOf(SensitivePermission.READ_SMS))
        val unverified = verified.copy(verified = false)

        assertEquals(Severity.HIGH, ImpersonationDetector(catalog).detect(input(fake, partner = unverified)).single().severity)
        assertEquals(Severity.HIGH, DataTheftRiskDetector().detect(input(fake, partner = unverified)).single().severity)
        val renamed = app("com.fake.sms", label = "SMS Toolbox", installer = null)
        assertEquals(BehaviorType.IMPERSONATION, ImpersonationDetector(catalog).detect(input(renamed)).single().type)
    }

    private companion object {
        const val NOW = 10 * 24 * HOUR
    }
}
