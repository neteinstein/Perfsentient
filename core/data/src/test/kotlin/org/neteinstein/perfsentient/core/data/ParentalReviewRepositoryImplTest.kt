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
import org.neteinstein.perfsentient.core.data.repository.ParentalReviewRepositoryImpl

class ParentalReviewRepositoryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val repository = ParentalReviewRepositoryImpl(
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { tmp.newFile("settings.preferences_pb") },
    )

    @Test
    fun `apps are marked and unmarked as reviewed`() = scope.runTest {
        assertEquals(emptySet<String>(), repository.reviewedPackages.first())

        repository.setReviewed("com.roblox.client", reviewed = true)
        repository.setReviewed("com.discord", reviewed = true)
        repository.setReviewed("com.discord", reviewed = true)
        assertEquals(setOf("com.roblox.client", "com.discord"), repository.reviewedPackages.first())

        repository.setReviewed("com.roblox.client", reviewed = false)
        repository.setReviewed("com.unknown", reviewed = false)
        assertEquals(setOf("com.discord"), repository.reviewedPackages.first())

        repository.setReviewed("com.discord", reviewed = false)
        assertEquals(emptySet<String>(), repository.reviewedPackages.first())
    }
}
