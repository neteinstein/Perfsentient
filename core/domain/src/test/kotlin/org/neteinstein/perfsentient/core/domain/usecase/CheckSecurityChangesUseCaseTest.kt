package org.neteinstein.perfsentient.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.CapabilityHolders
import org.neteinstein.perfsentient.core.model.PackageChangeCursor
import org.neteinstein.perfsentient.core.model.PackageChanges
import org.neteinstein.perfsentient.core.model.SecurityWatchState
import org.neteinstein.perfsentient.core.model.SensitivePermission
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.testing.FakeAppInventoryRepository
import org.neteinstein.perfsentient.core.testing.FakeSecurityWatchRepository
import org.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import org.neteinstein.perfsentient.core.testing.FakeTimeProvider
import org.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import org.neteinstein.perfsentient.core.testing.FakeUsageRepository
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.core.testing.TestDispatcherProvider

class CheckSecurityChangesUseCaseTest {
    private val time = FakeTimeProvider()
    private val now = time.now().toEpochMilli()
    private val inventory = FakeAppInventoryRepository()
    private val usage = FakeUsageRepository()
    private val watch = FakeSecurityWatchRepository()
    private val trusted = FakeTrustedAppsRepository()
    private val settings = FakeSettingsRepository(UserSettings(monitoringEnabled = true))

    private val useCase = CheckSecurityChangesUseCase(
        inventoryRepository = inventory,
        usageRepository = usage,
        watchRepository = watch,
        trustedAppsRepository = trusted,
        settingsRepository = settings,
        timeProvider = time,
        dispatchers = TestDispatcherProvider(),
        thresholds = DetectionThresholds(),
        catalog = ScamSignalCatalog(),
    )

    private val trojan = TestData.app(
        "bad.sms",
        installer = null,
        initiator = "com.whatsapp",
        permissions = setOf(SensitivePermission.READ_SMS),
    )

    private fun changes(sequence: Int, vararg packages: String, boot: Int = 1) = PackageChanges(
        PackageChangeCursor(boot, sequence),
        packages.toSet(),
    )

    private fun baseline(sequence: Int = 5, holders: CapabilityHolders = CapabilityHolders.NONE, alerted: List<String> = emptyList()) {
        watch.stored = SecurityWatchState(PackageChangeCursor(1, sequence), holders, alerted)
    }

    @Test
    fun `the first run only records a baseline`() = runTest {
        inventory.apps = listOf(trojan)
        inventory.changes = changes(7, "bad.sms")

        assertTrue(useCase().isEmpty())

        assertEquals(PackageChangeCursor(1, 7), watch.stored?.cursor)
        assertTrue("existing apps are not analysed", inventory.profileLookups.isEmpty())
    }

    @Test
    fun `a risky new install alerts once`() = runTest {
        baseline()
        inventory.apps = listOf(trojan, TestData.app("store.game"))
        inventory.changes = changes(8, "bad.sms", "store.game")

        val alerts = useCase()

        assertEquals(PackageChangeCursor(1, 5), inventory.lastChangesSince)
        assertEquals(setOf("bad.sms"), alerts.map { it.packageName }.toSet())
        assertTrue(alerts.any { it.type == BehaviorType.RISKY_INSTALL_SOURCE })
        assertEquals(PackageChangeCursor(1, 8), watch.stored?.cursor)
        assertTrue(watch.stored!!.alerted.contains("bad.sms:RISKY_INSTALL_SOURCE"))

        inventory.changes = changes(9, "bad.sms") // e.g. an update of the same app
        assertTrue("already alerted", useCase().isEmpty())
    }

    @Test
    fun `an app that newly gets notification access is checked even without an install`() = runTest {
        baseline()
        val spy = TestData.app("spy", installer = null, notificationAccess = true)
        inventory.apps = listOf(spy)
        inventory.holders = CapabilityHolders(emptySet(), emptySet(), setOf("spy"), defaultSms = null)
        inventory.changes = changes(5)

        val alerts = useCase()

        assertEquals(listOf(BehaviorType.DATA_THEFT_RISK), alerts.map { it.type })
        assertEquals(setOf("spy"), inventory.profileLookups.single())
    }

    @Test
    fun `a remote-access app installed after a call alerts at medium`() = runTest {
        baseline()
        val anydesk = TestData.app("com.anydesk.anydeskandroid", installedAt = now - 10 * MIN)
        inventory.apps = listOf(anydesk)
        inventory.changes = changes(6, anydesk.packageName)

        val alerts = useCase()

        assertEquals(BehaviorType.REMOTE_ACCESS, alerts.single().type)
        assertEquals(now - 60 * MIN to now, usage.lastEventsWindow)
    }

    @Test
    fun `trusted apps and low findings do not alert`() = runTest {
        baseline()
        trusted.trust(TrustedApp("bad.sms", "Bad"))
        inventory.apps = listOf(trojan, TestData.app("store.browser", canInstallApps = true))
        inventory.changes = changes(6, "bad.sms", "store.browser")

        assertTrue(useCase().isEmpty())
    }

    @Test
    fun `with alerts off the baseline still moves on`() = runTest {
        baseline()
        settings.state.value = settings.state.value.copy(securityAlertsEnabled = false)
        inventory.apps = listOf(trojan)
        inventory.changes = changes(6, "bad.sms")

        assertTrue(useCase().isEmpty())
        assertEquals(PackageChangeCursor(1, 6), watch.stored?.cursor)

        settings.state.value = settings.state.value.copy(securityAlertsEnabled = true)
        inventory.changes = changes(6)
        assertTrue("the past is not replayed", useCase().isEmpty())
    }

    @Test
    fun `nothing changed means nothing is analysed`() = runTest {
        baseline(holders = CapabilityHolders(setOf("a11y"), emptySet(), emptySet(), "sms"))
        inventory.holders = CapabilityHolders(setOf("a11y"), emptySet(), emptySet(), "sms")
        inventory.changes = changes(5)

        assertTrue(useCase().isEmpty())
        assertTrue(inventory.profileLookups.isEmpty())
    }

    @Test
    fun `holders report what is new`() {
        val before = CapabilityHolders(setOf("a"), setOf("admin"), setOf("n"), "sms.old")
        val after = CapabilityHolders(setOf("a", "b"), setOf("admin", "admin2"), setOf("n", "n2"), "sms.new")
        assertEquals(setOf("b", "admin2", "n2", "sms.new"), after.newlyGrantedSince(before))
        assertTrue(before.newlyGrantedSince(before).isEmpty())
        assertTrue(before.copy(defaultSms = null).newlyGrantedSince(before).isEmpty())
    }

    @Test
    fun `the toggle use case updates the setting`() = runTest {
        UpdateSecurityAlertsUseCase(settings)(false)
        assertEquals(false, settings.state.value.securityAlertsEnabled)
    }

    private companion object {
        const val MIN = 60_000L
    }
}
