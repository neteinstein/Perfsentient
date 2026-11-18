package org.neteinstein.perfsentient

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
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
    fun `resuming the app syncs monitoring`() = runTest {
        viewModel.onAppResumed()
        assertEquals(1, scheduler.ensured.size)
    }
}
