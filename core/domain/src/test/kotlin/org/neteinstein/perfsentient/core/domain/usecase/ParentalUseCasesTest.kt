package org.neteinstein.perfsentient.core.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.parental.ParentalAppCatalog
import org.neteinstein.perfsentient.core.domain.parental.ParentalRiskAssessor
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.testing.FakeAppInventoryRepository
import org.neteinstein.perfsentient.core.testing.FakeParentalReviewRepository
import org.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.core.testing.TestDispatcherProvider

class ParentalUseCasesTest {
    private val settings = FakeSettingsRepository()
    private val reviews = FakeParentalReviewRepository()
    private val inventory = FakeAppInventoryRepository(
        apps = listOf(
            TestData.app("com.roblox.client", label = "Roblox"),
            TestData.app("com.tinder", label = "Tinder"),
            TestData.app("com.example.notes", label = "Notes"),
        ),
    )
    private val catalog = ParentalAppCatalog()
    private val observe = ObserveParentalScanUseCase(
        settings,
        reviews,
        inventory,
        catalog,
        ParentalRiskAssessor(catalog),
        TestDispatcherProvider(),
    )

    @Test
    fun `parental settings are persisted`() = runTest {
        UpdateParentalSettingsUseCase(settings)(enabled = true, childAge = 9)
        assertEquals(ParentalSettings(enabled = true, childAge = 9), settings.settings.first().parental)

        UpdateParentalSettingsUseCase(settings)(enabled = false, childAge = 9)
        assertEquals(false, settings.settings.first().parental.enabled)
    }

    @Test
    fun `nothing is scanned while parental mode is off`() = runTest {
        assertNull(observe().first())
        assertEquals(0, inventory.labelLookups)
    }

    @Test
    fun `scan follows the age and reviews without re-reading the installed apps`() = runTest {
        settings.state.value = UserSettings(parental = ParentalSettings(enabled = true, childAge = 10))
        observe().test {
            val first = awaitItem()!!
            assertEquals(10, first.childAge)
            assertEquals(listOf("Tinder", "Roblox"), first.warnings.map { it.appLabel })
            assertEquals(Severity.MEDIUM, first.warnings.last().severity)

            SetParentalAppReviewedUseCase(reviews)("com.tinder", reviewed = true)
            val reviewed = awaitItem()!!
            assertEquals(listOf("Roblox", "Tinder"), reviewed.warnings.map { it.appLabel })
            assertTrue(reviewed.warnings.last().isReviewed)
            assertEquals(1, inventory.labelLookups)

            // Roblox is fine from 13, Tinder never is.
            settings.setParentalSettings(ParentalSettings(enabled = true, childAge = 14))
            assertEquals(listOf("Tinder"), awaitItem()!!.warnings.map { it.appLabel })

            settings.setParentalSettings(ParentalSettings(enabled = false, childAge = 14))
            assertNull(awaitItem())

            SetParentalAppReviewedUseCase(reviews)("com.tinder", reviewed = false)
            expectNoEvents()
        }
        assertEquals(emptySet<String>(), reviews.state.value)
    }
}
