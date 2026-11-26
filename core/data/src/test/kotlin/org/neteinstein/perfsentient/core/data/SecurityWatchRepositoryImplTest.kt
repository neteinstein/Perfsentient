package org.neteinstein.perfsentient.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.neteinstein.perfsentient.core.data.repository.SecurityWatchRepositoryImpl
import org.neteinstein.perfsentient.core.model.CapabilityHolders
import org.neteinstein.perfsentient.core.model.PackageChangeCursor
import org.neteinstein.perfsentient.core.model.SecurityWatchState

class SecurityWatchRepositoryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { tmp.newFile("settings.preferences_pb") }
    private val repository = SecurityWatchRepositoryImpl(dataStore)

    @Test
    fun `there is no baseline until one is saved`() = scope.runTest {
        assertNull(repository.state())
    }

    @Test
    fun `the state survives a round trip`() = scope.runTest {
        val state = SecurityWatchState(
            cursor = PackageChangeCursor(bootCount = 3, sequence = 42),
            holders = CapabilityHolders(setOf("a11y"), setOf("admin"), setOf("listener"), "sms"),
            alerted = listOf("bad:REMOTE_ACCESS", "spy:DATA_THEFT_RISK"),
        )

        repository.save(state)

        assertEquals(state, repository.state())
    }

    @Test
    fun `corrupt data reads as no baseline`() = scope.runTest {
        dataStore.edit { it[stringPreferencesKey("security_watch")] = "{broken" }
        assertNull(repository.state())
    }
}
