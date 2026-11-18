package org.neteinstein.perfsentient.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.neteinstein.perfsentient.core.data.repository.SettingsRepositoryImpl
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.UserSettings

class SettingsRepositoryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val repository = SettingsRepositoryImpl(
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { tmp.newFile("settings.preferences_pb") },
    )

    @Test
    fun `defaults are returned when nothing is stored`() = scope.runTest {
        assertEquals(UserSettings(), repository.settings.first())
    }

    @Test
    fun `every setting is persisted`() = scope.runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setDynamicColor(true)
        repository.setReportTime(ReportTime(7, 45))
        repository.setMonitoringEnabled(true)

        assertEquals(
            UserSettings(themeMode = ThemeMode.DARK, useDynamicColor = true, reportTime = ReportTime(7, 45), monitoringEnabled = true),
            repository.settings.first(),
        )
    }
}
