package org.neteinstein.perfsentient.core.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.neteinstein.perfsentient.core.common.DispatcherProvider
import org.neteinstein.perfsentient.core.domain.parental.ParentalAppCatalog
import org.neteinstein.perfsentient.core.domain.parental.ParentalRiskAssessor
import org.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import org.neteinstein.perfsentient.core.domain.repository.ParentalReviewRepository
import org.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import org.neteinstein.perfsentient.core.model.ParentalScan
import org.neteinstein.perfsentient.core.model.ParentalSettings

/** Turns parental mode on (with the child's age) or off. */
class UpdateParentalSettingsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean, childAge: Int) = repository.setParentalSettings(ParentalSettings(enabled, childAge))
}

/**
 * Concerning apps installed on this phone for the child's age, or null while parental mode is off.
 * The installed apps are read once per collection: re-collect (e.g. on resume) to pick up installs.
 */
class ObserveParentalScanUseCase(
    private val settingsRepository: SettingsRepository,
    private val reviewRepository: ParentalReviewRepository,
    private val inventoryRepository: AppInventoryRepository,
    private val catalog: ParentalAppCatalog,
    private val assessor: ParentalRiskAssessor,
    private val dispatchers: DispatcherProvider,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ParentalScan?> = settingsRepository.settings
        .map { it.parental }
        .distinctUntilChanged()
        .flatMapLatest { parental ->
            if (!parental.enabled) {
                flowOf(null)
            } else {
                val installed = flow { emit(inventoryRepository.getInstalledAppLabels(catalog.packageNames)) }
                combine(installed, reviewRepository.reviewedPackages) { labels, reviewed ->
                    withContext(dispatchers.default) { assessor.scan(labels, parental.childAge, reviewed) }
                }
            }
        }
}

/** Moves a concerning app out of (or back into) the parental warnings. */
class SetParentalAppReviewedUseCase(private val repository: ParentalReviewRepository) {
    suspend operator fun invoke(packageName: String, reviewed: Boolean) = repository.setReviewed(packageName, reviewed)
}
