package com.neteinstein.perfsentient.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neteinstein.perfsentient.core.data.repository.TrustedAppsRepositoryImpl
import com.neteinstein.perfsentient.core.model.TrustedApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TrustedAppsRepositoryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { tmp.newFile("settings.preferences_pb") }
    private val repository = TrustedAppsRepositoryImpl(dataStore)

    @Test
    fun `allowlist is empty by default`() = scope.runTest {
        assertTrue(repository.trustedApps.first().isEmpty())
    }

    @Test
    fun `apps are trusted once, relabelled and removed`() = scope.runTest {
        repository.trust(TrustedApp("com.chat", "Chat"))
        repository.trust(TrustedApp("com.vpn", "VPN"))
        repository.trust(TrustedApp("com.chat", "Chat Pro"))
        assertEquals(listOf(TrustedApp("com.vpn", "VPN"), TrustedApp("com.chat", "Chat Pro")), repository.trustedApps.first())

        repository.untrust("com.vpn")
        repository.untrust("com.unknown")
        assertEquals(listOf(TrustedApp("com.chat", "Chat Pro")), repository.trustedApps.first())

        repository.untrust("com.chat")
        assertTrue(repository.trustedApps.first().isEmpty())
    }

    @Test
    fun `corrupt stored data reads as an empty allowlist and can be overwritten`() = scope.runTest {
        dataStore.edit { it[stringPreferencesKey("trusted_apps")] = "{not json" }
        assertTrue(repository.trustedApps.first().isEmpty())

        repository.trust(TrustedApp("com.chat", "Chat"))
        assertEquals(listOf(TrustedApp("com.chat", "Chat")), repository.trustedApps.first())
    }
}
