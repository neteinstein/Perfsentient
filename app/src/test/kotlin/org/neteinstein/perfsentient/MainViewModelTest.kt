package org.neteinstein.perfsentient

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.testing.FakeMonitoringScheduler
import org.neteinstein.perfsentient.core.testing.FakePermissionRepository
import org.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import org.neteinstein.perfsentient.core.testing.MainDispatcherRule

class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(UserSettings(themeMode = ThemeMode.DARK))
    private val permissions = FakePermissionRepository(granted = true)
    private val scheduler = FakeMonitoringScheduler()

    // Lazy: must be created after MainDispatcherRule installed the test Main dispatcher.
    private val viewModel by lazy {
        MainViewModel(
            SavedStateHandle(),
            ObserveSettingsUseCase(settings),
            SyncMonitoringUseCase(permissions, settings, scheduler),
        )
    }

    @Test
    fun `exposes settings once loaded`() = runTest {
        viewModel.uiState.test {
            assertEquals(MainUiState.Ready(UserSettings(themeMode = ThemeMode.DARK)), expectMostRecentItem())
        }
    }

    @Test
    fun `tab selection defaults to report and survives in saved state`() {
        assertEquals(AppTab.REPORT, viewModel.selectedTab.value)
        viewModel.selectTab(AppTab.SETTINGS)
        assertEquals(AppTab.SETTINGS, viewModel.selectedTab.value)
    }

    @Test
    fun `the parental help tab sits in the middle and only while parental mode is on`() {
        val off = MainUiState.Ready(UserSettings())
        assertEquals(listOf(AppTab.REPORT, AppTab.APPS, AppTab.SETTINGS), off.tabs)
        // Turning parental mode off while on its tab falls back to Settings, where the switch is.
        assertEquals(AppTab.SETTINGS, off.visibleTab(AppTab.PARENTAL))
        assertEquals(AppTab.APPS, off.visibleTab(AppTab.APPS))

        val on = MainUiState.Ready(UserSettings(parental = ParentalSettings(enabled = true, childAge = 12)))
        assertEquals(listOf(AppTab.REPORT, AppTab.APPS, AppTab.PARENTAL, AppTab.SETTINGS), on.tabs)
        assertEquals(AppTab.PARENTAL, on.visibleTab(AppTab.PARENTAL))
    }

    @Test
    fun `resuming the app syncs monitoring`() = runTest {
        viewModel.onAppResumed()
        assertEquals(1, scheduler.ensured.size)
    }
}
