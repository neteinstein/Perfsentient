package com.neteinstein.perfsentient.feature.apps

import app.cash.turbine.test
import com.neteinstein.perfsentient.core.domain.usecase.ObserveFlaggedAppsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.TrustAppUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import com.neteinstein.perfsentient.core.testing.FakeReportRepository
import com.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import com.neteinstein.perfsentient.core.testing.MainDispatcherRule
import com.neteinstein.perfsentient.core.testing.TestData
import com.neteinstein.perfsentient.feature.apps.presentation.AppsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val reports = FakeReportRepository()
    private val trusted = FakeTrustedAppsRepository()

    private fun viewModel() = AppsViewModel(
        ObserveFlaggedAppsUseCase(reports, trusted),
        TrustAppUseCase(trusted),
        UntrustAppUseCase(trusted),
    )

    @Test
    fun `lists flagged apps and flips them when trusted then un-trusted`() = runTest {
        reports.save(TestData.report(findings = listOf(TestData.finding("adware"))))
        val viewModel = viewModel()
        viewModel.uiState.test {
            var loaded = awaitItem()
            while (loaded.isLoading) loaded = awaitItem() // the initial state may or may not be conflated away
            assertFalse(loaded.apps.single().isTrusted)

            viewModel.onTrustApp("adware", "Adware")
            assertTrue(awaitItem().apps.single().isTrusted)

            viewModel.onUntrustApp("adware")
            val after = awaitItem()
            assertEquals(listOf("adware"), after.apps.map { it.packageName })
            assertFalse(after.apps.single().isTrusted)
        }
    }
}
