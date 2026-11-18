package org.neteinstein.perfsentient.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.UserSettings

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data
        .map { prefs ->
            val defaults = UserSettings()
            UserSettings(
                themeMode = prefs[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: defaults.themeMode,
                useDynamicColor = prefs[DYNAMIC_COLOR] ?: defaults.useDynamicColor,
                reportTime = runCatching {
                    ReportTime(prefs[REPORT_HOUR] ?: defaults.reportTime.hour, prefs[REPORT_MINUTE] ?: defaults.reportTime.minute)
                }.getOrDefault(defaults.reportTime),
                monitoringEnabled = prefs[MONITORING] ?: defaults.monitoringEnabled,
            )
        }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setReportTime(time: ReportTime) {
        dataStore.edit {
            it[REPORT_HOUR] = time.hour
            it[REPORT_MINUTE] = time.minute
        }
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { it[MONITORING] = enabled }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val REPORT_HOUR = intPreferencesKey("report_hour")
        val REPORT_MINUTE = intPreferencesKey("report_minute")
        val MONITORING = booleanPreferencesKey("monitoring_enabled")
    }
}
