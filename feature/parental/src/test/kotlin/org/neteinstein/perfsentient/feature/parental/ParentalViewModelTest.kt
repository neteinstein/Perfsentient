package org.neteinstein.perfsentient.feature.parental

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.parental.ParentalAppCatalog
import org.neteinstein.perfsentient.core.domain.parental.ParentalRiskAssessor
import org.neteinstein.perfsentient.core.domain.usecase.ObserveParentalScanUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SetParentalAppReviewedUseCase
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.testing.FakeAppInventoryRepository
import org.neteinstein.perfsentient.core.testing.FakeParentalReviewRepository
import org.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import org.neteinstein.perfsentient.core.testing.MainDispatcherRule
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.core.testing.TestDispatcherProvider
import org.neteinstein.perfsentient.feature.parental.presentation.ParentalUiState
import org.neteinstein.perfsentient.feature.parental.presentation.ParentalViewModel

class ParentalViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val settings = FakeSettingsRepository(UserSettings(parental = ParentalSettings(enabled = true, childAge = 9)))
    private val reviews = FakeParentalReviewRepository()
    private val inventory = FakeAppInventoryRepository(
        apps = listOf(
            TestData.app("com.roblox.client", label = "Roblox"),
            TestData.app("com.discord", label = "Discord"),
            TestData.app("com.example.notes", label = "Notes"),
        ),
    )

    private fun viewModel(): ParentalViewModel {
        val catalog = ParentalAppCatalog()
        return ParentalViewModel(
            ObserveParentalScanUseCase(settings, reviews, inventory, catalog, ParentalRiskAssessor(catalog), TestDispatcherProvider()),
            SetParentalAppReviewedUseCase(reviews),
        )
    }

    @Test
    fun `lists concerning apps for the age and moves them to reviewed and back`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            val loaded = awaitLoaded()
            assertEquals(9, loaded.childAge)
            // Discord: 9 is below its minimum (13) and 7 years under 16 -> HIGH. Roblox: 4 under 13 -> MEDIUM.
            assertEquals(listOf("Discord", "Roblox"), loaded.warnings.map { it.appLabel })
            assertEquals(1, loaded.highRiskCount)
            assertTrue(loaded.reviewed.isEmpty())

            viewModel.onReviewedChanged("com.discord", reviewed = true)
            val reviewed = awaitItem()
            assertEquals(listOf("Roblox"), reviewed.warnings.map { it.appLabel })
            assertEquals(listOf("Discord"), reviewed.reviewed.map { it.appLabel })
            assertEquals(0, reviewed.highRiskCount)

            viewModel.onReviewedChanged("com.discord", reviewed = false)
            assertEquals(2, awaitItem().warnings.size)
        }
    }

    @Test
    fun `rescanning picks up newly installed apps`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(2, awaitLoaded().warnings.size)

            inventory.apps = inventory.apps + TestData.app("com.tinder", label = "Tinder")
            viewModel.onRescan()
            // Both High, so by name; Roblox (Medium) last.
            assertEquals(listOf("Discord", "Tinder", "Roblox"), awaitItem().warnings.map { it.appLabel })
        }
    }

    @Test
    fun `shows the disabled state when parental mode is turned off`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertTrue(awaitLoaded().isEnabled)

            settings.setParentalSettings(ParentalSettings(enabled = false, childAge = 9))
            val disabled = awaitItem()
            assertFalse(disabled.isEnabled)
            assertFalse(disabled.isLoading)
            assertTrue(disabled.warnings.isEmpty())
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<ParentalUiState>.awaitLoaded(): ParentalUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem() // the initial state may or may not be conflated away
        return state
    }
}
