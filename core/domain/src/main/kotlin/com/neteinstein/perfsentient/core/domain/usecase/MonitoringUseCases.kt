package com.neteinstein.perfsentient.core.domain.usecase

import com.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import com.neteinstein.perfsentient.core.common.TimeProvider
import com.neteinstein.perfsentient.core.domain.repository.MonitoringScheduler
import com.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import com.neteinstein.perfsentient.core.domain.repository.PermissionRepository
import com.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import com.neteinstein.perfsentient.core.domain.repository.VitalsRepository
import com.neteinstein.perfsentient.core.model.allRequiredGranted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

enum class MonitoringState { ACTIVE, WAITING_FOR_PERMISSIONS }

class ObserveMonitoringStateUseCase(
    private val permissionRepository: PermissionRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<MonitoringState> = combine(
        permissionRepository.permissions,
        settingsRepository.settings,
    ) { permissions, settings ->
        if (permissions.allRequiredGranted && settings.monitoringEnabled) {
            MonitoringState.ACTIVE
        } else {
            MonitoringState.WAITING_FOR_PERMISSIONS
        }
    }
}

/**
 * Starts monitoring as soon as every required permission is granted and stops it if one is
 * revoked. Idempotent: safe to call on every resume.
 */
class SyncMonitoringUseCase(
    private val permissionRepository: PermissionRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: MonitoringScheduler,
) {
    suspend operator fun invoke() {
        permissionRepository.refresh()
        val granted = permissionRepository.permissions.value.allRequiredGranted
        val settings = settingsRepository.settings.first()
        when {
            granted -> {
                // Ensure unconditionally: unique work makes this cheap and it self-heals if the
                // OS dropped our jobs (e.g. after a force-stop).
                scheduler.ensureScheduled(settings.reportTime)
                if (!settings.monitoringEnabled) settingsRepository.setMonitoringEnabled(true)
            }

            settings.monitoringEnabled -> {
                scheduler.cancel()
                settingsRepository.setMonitoringEnabled(false)
            }
        }
    }
}

/** Captures one vitals sample and prunes stale data. Called periodically by the sampler. */
class RecordVitalSampleUseCase(
    private val vitalsRepository: VitalsRepository,
    private val notificationStatsRepository: NotificationStatsRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke() {
        vitalsRepository.save(vitalsRepository.capture())
        val cutoff = timeProvider.now().toEpochMilli() - RETENTION_HOURS * MILLIS_IN_HOUR
        vitalsRepository.deleteOlderThan(cutoff)
        notificationStatsRepository.deleteOlderThan(timeProvider.today().minusDays(RETENTION_HOURS / 24))
    }

    private companion object {
        const val RETENTION_HOURS = 24L * 7
    }
}

class RecordNotificationUseCase(
    private val repository: NotificationStatsRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(packageName: String) = repository.increment(packageName, timeProvider.today())
}
