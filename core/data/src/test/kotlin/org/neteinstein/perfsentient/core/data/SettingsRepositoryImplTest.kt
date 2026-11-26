package org.neteinstein.perfsentient.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.neteinstein.perfsentient.core.data.repository.SettingsRepositoryImpl
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.UserSettings

class SettingsRepositoryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { tmp.newFile("settings.preferences_pb") }
    private val repository = SettingsRepositoryImpl(dataStore)

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
        repository.setParentalSettings(ParentalSettings(enabled = true, childAge = 8))
        repository.setSecurityAlertsEnabled(false)

        assertEquals(
            UserSettings(
                themeMode = ThemeMode.DARK,
                useDynamicColor = true,
                reportTime = ReportTime(7, 45),
                monitoringEnabled = true,
                parental = ParentalSettings(enabled = true, childAge = 8),
                securityAlertsEnabled = false,
            ),
            repository.settings.first(),
        )
    }

    @Test
    fun `an invalid stored child age falls back to the parental defaults`() = scope.runTest {
        repository.setParentalSettings(ParentalSettings(enabled = true, childAge = 12))
        dataStore.edit { it[intPreferencesKey("parental_child_age")] = 40 }
        assertEquals(ParentalSettings(), repository.settings.first().parental)
    }
}
