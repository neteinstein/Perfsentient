package org.neteinstein.perfsentient.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.neteinstein.perfsentient.core.data.mapper.toTrustedApps
import org.neteinstein.perfsentient.core.data.mapper.toTrustedAppsJson
import org.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import org.neteinstein.perfsentient.core.model.TrustedApp

/** Stores the allowlist next to the other user preferences, so it is included in backups. */
class TrustedAppsRepositoryImpl(private val dataStore: DataStore<Preferences>) : TrustedAppsRepository {

    override val trustedApps: Flow<List<TrustedApp>> = dataStore.data
        .map { prefs -> prefs[TRUSTED_APPS].toTrustedApps() }
        .distinctUntilChanged()

    override suspend fun trust(app: TrustedApp) {
        dataStore.edit { prefs ->
            prefs.write(prefs[TRUSTED_APPS].toTrustedApps().filterNot { it.packageName == app.packageName } + app)
        }
    }

    override suspend fun untrust(packageName: String) {
        dataStore.edit { prefs ->
            prefs.write(prefs[TRUSTED_APPS].toTrustedApps().filterNot { it.packageName == packageName })
        }
    }

    private fun MutablePreferences.write(apps: List<TrustedApp>) {
        if (apps.isEmpty()) remove(TRUSTED_APPS) else this[TRUSTED_APPS] = apps.toTrustedAppsJson()
    }

    private companion object {
        val TRUSTED_APPS = stringPreferencesKey("trusted_apps")
    }
}
