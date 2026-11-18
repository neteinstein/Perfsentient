package org.neteinstein.perfsentient.core.domain.usecase

import org.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import org.neteinstein.perfsentient.core.model.TrustedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The allowlist, sorted by label for display. */
class ObserveTrustedAppsUseCase(private val repository: TrustedAppsRepository) {
    operator fun invoke(): Flow<List<TrustedApp>> = repository.trustedApps.map { apps ->
        apps.sortedWith(compareBy<TrustedApp> { it.label.lowercase() }.thenBy { it.packageName })
    }
}

/** Stops flagging an app: its findings disappear from every report and from the score. */
class TrustAppUseCase(private val repository: TrustedAppsRepository) {
    suspend operator fun invoke(packageName: String, label: String) = repository.trust(TrustedApp(packageName, label))
}

class UntrustAppUseCase(private val repository: TrustedAppsRepository) {
    suspend operator fun invoke(packageName: String) = repository.untrust(packageName)
}

/** Package names on the allowlist. */
internal fun TrustedAppsRepository.trustedPackages(): Flow<Set<String>> =
    trustedApps.map { apps -> apps.mapTo(HashSet()) { it.packageName } }.distinctUntilChanged()
