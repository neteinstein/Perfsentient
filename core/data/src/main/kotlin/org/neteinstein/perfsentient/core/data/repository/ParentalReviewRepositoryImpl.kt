package org.neteinstein.perfsentient.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.neteinstein.perfsentient.core.domain.repository.ParentalReviewRepository

/** Stores the reviewed concerning apps next to the other user preferences, so they are included in backups. */
class ParentalReviewRepositoryImpl(private val dataStore: DataStore<Preferences>) : ParentalReviewRepository {

    override val reviewedPackages: Flow<Set<String>> = dataStore.data
        .map { prefs -> prefs[REVIEWED].orEmpty() }
        .distinctUntilChanged()

    override suspend fun setReviewed(packageName: String, reviewed: Boolean) {
        dataStore.edit { prefs ->
            val updated = prefs[REVIEWED].orEmpty().let { if (reviewed) it + packageName else it - packageName }
            if (updated.isEmpty()) prefs.remove(REVIEWED) else prefs[REVIEWED] = updated
        }
    }

    private companion object {
        val REVIEWED = stringSetPreferencesKey("parental_reviewed_apps")
    }
}
