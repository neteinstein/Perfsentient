package org.neteinstein.perfsentient.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.neteinstein.perfsentient.core.data.mapper.toJson
import org.neteinstein.perfsentient.core.data.mapper.toSecurityWatchState
import org.neteinstein.perfsentient.core.domain.repository.SecurityWatchRepository
import org.neteinstein.perfsentient.core.model.SecurityWatchState

/** Keeps the live security check's baseline next to the other preferences. */
class SecurityWatchRepositoryImpl(private val dataStore: DataStore<Preferences>) : SecurityWatchRepository {
    override suspend fun state(): SecurityWatchState? = dataStore.data.first()[SECURITY_WATCH].toSecurityWatchState()

    override suspend fun save(state: SecurityWatchState) {
        dataStore.edit { it[SECURITY_WATCH] = state.toJson() }
    }

    private companion object {
        val SECURITY_WATCH = stringPreferencesKey("security_watch")
    }
}
