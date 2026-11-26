package org.neteinstein.perfsentient.core.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.detector.CanInstallAppsDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.CapabilityRiskDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.DataTheftRiskDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.HiddenIconDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.ImpersonationDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.RemoteAccessDetector
import org.neteinstein.perfsentient.core.domain.analysis.detector.RiskyInstallSourceDetector
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.InstallChannel
import org.neteinstein.perfsentient.core.model.PackageSource
import org.neteinstein.perfsentient.core.model.ReadableData
import org.neteinstein.perfsentient.core.model.SensitivePermission.READ_SMS
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.testing.TestData.DIALER
import org.neteinstein.perfsentient.core.testing.TestData.app
import org.neteinstein.perfsentient.core.testing.TestData.resumed

class ScamDetectorsTest {
    private val thresholds = DetectionThresholds()
    private val catalog = ScamSignalCatalog()

    private fun BehaviorDetector.severities(vararg apps: AppProfile, now: Long = NOW, build: (AnalysisInput) -> AnalysisInput = { it }) =
        detect(build(input(apps = apps.toList()).copy(windowEndMs = now))).associate { it.packageName to it.severity }

    // region Risky install source
    @Test
    fun `apps installed from a chat link are flagged, higher with sensitive access`() {
        val detector = RiskyInstallSourceDetector(catalog)
        val findings = detector.detect(
            input(
                apps = listOf(
                    app("link", installer = null, initiator = "com.whatsapp"),
                    app("link.sms", installer = null, initiator = "org.telegram.messenger", permissions = setOf(READ_SMS)),
                ),
            ),
        ).associateBy { it.packageName }

        assertEquals(Severity.MEDIUM, findings.getValue("link").severity)
        assertEquals(InstallChannel.MESSAGING.ordinal.toLong(), findings.getValue("link").value)
        assertEquals(Severity.HIGH, findings.getValue("link.sms").severity)
    }

    @Test
    fun `downloads are flagged only with sensitive access`() {
        val detector = RiskyInstallSourceDetector(catalog)
        val findings = detector.detect(
            input(
                apps = listOf(
                    app("plain.download", installer = null, initiator = "com.android.chrome"),
                    app("bad.download", installer = null, initiator = "com.android.chrome", notificationAccess = true),
                    app("web.source", installer = null, source = PackageSource.DOWNLOADED_FILE, accessibility = true),
                    app("copied", installer = null, source = PackageSource.LOCAL_FILE, overlay = true),
                ),
            ),
        ).associateBy { it.packageName }

        assertEquals(setOf("bad.download", "web.source", "copied"), findings.keys)
        assertTrue(findings.values.all { it.severity == Severity.MEDIUM })
        assertEquals(InstallChannel.BROWSER.ordinal.toLong(), findings.getValue("bad.download").value)
        assertEquals(InstallChannel.FILE.ordinal.toLong(), findings.getValue("copied").value)
    }

    @Test
    fun `store installs and unknown sources are not flagged`() {
        val severities = RiskyInstallSourceDetector(catalog).severities(
            app("store", initiator = "com.whatsapp", permissions = setOf(READ_SMS)),
            app("unknown", installer = null, accessibility = true),
        )
        assertTrue(severities.isEmpty())
    }
    // endregion

    // region Data theft
    @Test
    fun `apps that can read codes are rated by where they came from`() {
        val findings = DataTheftRiskDetector().detect(
            input(
                apps = listOf(
                    app("sideloaded.reader", installer = null, permissions = setOf(READ_SMS)),
                    app("store.reader", permissions = setOf(READ_SMS)),
                    app("watch.companion", notificationAccess = true),
                    app("sms.app", defaultSms = true, permissions = setOf(READ_SMS)),
                    app("plain"),
                ),
            ),
        ).associateBy { it.packageName }

        assertEquals(Severity.HIGH, findings.getValue("sideloaded.reader").severity)
        assertEquals(ReadableData.TEXT_MESSAGES, findings.getValue("sideloaded.reader").value)
        assertEquals(Severity.MEDIUM, findings.getValue("store.reader").severity)
        assertEquals(Severity.LOW, findings.getValue("watch.companion").severity)
        assertEquals(ReadableData.NOTIFICATIONS, findings.getValue("watch.companion").value)
        assertEquals(
            "the default SMS app from a store is doing its job",
            setOf("sideloaded.reader", "store.reader", "watch.companion"),
            findings.keys,
        )
    }

    @Test
    fun `the banking trojan combination is high even from a store`() {
        val finding = DataTheftRiskDetector().detect(
            input(
                apps = listOf(
                    app("trojan", accessibility = true, overlay = true, notificationAccess = true, permissions = setOf(READ_SMS)),
                ),
            ),
        ).single()
        assertEquals(Severity.HIGH, finding.severity)
        assertEquals(ReadableData.NOTIFICATIONS or ReadableData.TEXT_MESSAGES, finding.value)
    }
    // endregion

    // region Remote access
    @Test
    fun `a remote-access app installed in the last 72 hours is flagged`() {
        val detector = RemoteAccessDetector(thresholds, catalog)
        val findings = detector.detect(
            input(
                apps = listOf(
                    app("com.anydesk.anydeskandroid", installedAt = NOW - HOUR),
                    app("com.teamviewer.quicksupport.market", installedAt = NOW - 100 * HOUR),
                    app("com.example.notes", installedAt = NOW - HOUR),
                ),
            ).copy(windowEndMs = NOW),
        )

        val finding = findings.single()
        assertEquals("com.anydesk.anydeskandroid", finding.packageName)
        assertEquals(Severity.MEDIUM, finding.severity)
        assertEquals(NOW - HOUR, finding.value)
    }

    @Test
    fun `installing it during or right after a call, or with accessibility, is high`() {
        val callStart = NOW - 2 * HOUR
        val callEnd = callStart + 10 * MIN
        val findings = RemoteAccessDetector(thresholds, catalog).detect(
            input(
                apps = listOf(
                    app("com.anydesk.anydeskandroid", installedAt = callEnd + 20 * MIN),
                    app("com.teamviewer.quicksupport.market", installedAt = callStart + 5 * MIN),
                    app("com.sand.airdroid", installedAt = callEnd + 90 * MIN),
                    app("com.carriez.flutter_hbb", installedAt = NOW - 30 * HOUR, accessibility = true),
                ),
                events = listOf(resumed(DIALER, callStart), resumed("chat", callEnd)),
            ).copy(windowEndMs = NOW),
        ).associate { it.packageName to it.severity }

        assertEquals(Severity.HIGH, findings["com.anydesk.anydeskandroid"])
        assertEquals(Severity.HIGH, findings["com.teamviewer.quicksupport.market"])
        assertEquals(Severity.MEDIUM, findings["com.sand.airdroid"])
        assertEquals(Severity.HIGH, findings["com.carriez.flutter_hbb"])
    }
    // endregion

    // region Impersonation
    @Test
    fun `sideloaded apps named like system parts or brands are flagged`() {
        val severities = ImpersonationDetector(catalog).severities(
            app("fake.update", label = "System Update", installer = null),
            app("fake.gms", label = "Gооgle Play Services", installer = null, launcher = false),
            app("fake.wa", label = "GBWhatsApp", installer = null),
            app("fake.bank", label = "Santander Seguro", installer = null, permissions = setOf(READ_SMS)),
        )
        assertEquals(
            mapOf(
                "fake.update" to Severity.MEDIUM,
                "fake.gms" to Severity.HIGH,
                "fake.wa" to Severity.MEDIUM,
                "fake.bank" to Severity.HIGH,
            ),
            severities,
        )
    }

    @Test
    fun `genuine packages, store apps and everyday words are not flagged`() {
        val severities = ImpersonationDetector(catalog).severities(
            app("com.whatsapp", label = "WhatsApp", installer = null),
            app("store.paypal", label = "PayPal Business Tools"),
            app("booster", label = "Signal Booster", installer = null),
            app("backup", label = "Settings Backup", installer = null),
            app("purchase", label = "Purchase Tracker", installer = null),
        )
        assertTrue(severities.toString(), severities.isEmpty())
    }
    // endregion

    // region Can install apps
    @Test
    fun `install permission is flagged for sideloaded apps and store browsers`() {
        val severities = CanInstallAppsDetector(catalog).severities(
            app("dropper", installer = null, canInstallApps = true),
            app("com.brave.browser", canInstallApps = true),
            app("store.updater", canInstallApps = true),
            app("org.fdroid.fdroid", installer = null, canInstallApps = true),
            app("no.permission", installer = null),
        )
        assertEquals(mapOf("dropper" to Severity.MEDIUM, "com.brave.browser" to Severity.LOW), severities)
    }
    // endregion

    // region Changes to existing detectors
    @Test
    fun `accessibility turned on right after install from a store is medium`() {
        val findings = CapabilityRiskDetector(thresholds).detect(
            input(
                apps = listOf(
                    app("new.pdf", accessibility = true, installedAt = NOW - 2 * HOUR),
                    app("old.helper", accessibility = true, installedAt = NOW - 30 * 24 * HOUR),
                ),
            ).copy(windowEndMs = NOW),
        ).associate { it.packageName to it.severity }

        assertEquals(Severity.MEDIUM, findings["new.pdf"])
        assertEquals(Severity.LOW, findings["old.helper"])
    }

    @Test
    fun `a hidden app with notification access is high`() {
        val finding = HiddenIconDetector(
            thresholds,
        ).detect(input(apps = listOf(app("spy", launcher = false, notificationAccess = true)))).single()
        assertEquals(BehaviorType.HIDDEN_ICON, finding.type)
        assertEquals(Severity.HIGH, finding.severity)
    }
    // endregion

    @Test
    fun `the analyzer runs the new detectors`() {
        val findings = BehaviorAnalyzer(thresholds).analyze(
            input(apps = listOf(app("fake.update", label = "System Update", installer = null, initiator = "com.whatsapp"))),
        )
        assertEquals(setOf(BehaviorType.IMPERSONATION, BehaviorType.RISKY_INSTALL_SOURCE), findings.map { it.type }.toSet())
    }

    private companion object {
        const val NOW = 10 * 24 * HOUR
    }
}
