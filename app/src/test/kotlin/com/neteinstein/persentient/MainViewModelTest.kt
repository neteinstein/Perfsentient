package com.neteinstein.persentient

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.neteinstein.persentient.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.persentient.core.domain.usecase.SyncMonitoringUseCase
import com.neteinstein.persentient.core.model.ThemeMode
import com.neteinstein.persentient.core.model.UserSettings
import com.neteinstein.persentient.core.testing.FakeMonitoringScheduler
import com.neteinstein.persentient.core.testing.FakePermissionRepository
import com.neteinstein.persentient.core.testing.FakeSettingsRepository
import com.neteinstein.persentient.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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
