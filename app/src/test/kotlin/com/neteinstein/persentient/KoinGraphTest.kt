package com.neteinstein.persentient

import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkerParameters
import com.neteinstein.persentient.di.appModules
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/** Fails the build if a dependency is missing from the Koin graph. */
@OptIn(KoinExperimentalAPI::class)
class KoinGraphTest {
    @Test
    fun `dependency graph is complete`() {
        module { includes(appModules) }.verify(
            extraTypes = listOf(
                Context::class,
                SavedStateHandle::class,
                WorkerParameters::class,
                // Android system services resolved from the Context inside definitions.
                UsageStatsManager::class,
                NetworkStatsManager::class,
                PackageManager::class,
            ),
        )
    }
}
