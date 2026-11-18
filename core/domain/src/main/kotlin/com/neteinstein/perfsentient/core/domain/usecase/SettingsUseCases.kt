package com.neteinstein.perfsentient.core.domain.usecase

import com.neteinstein.perfsentient.core.domain.repository.MonitoringScheduler
import com.neteinstein.perfsentient.core.domain.repository.PermissionRepository
import com.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import com.neteinstein.perfsentient.core.model.PermissionState
import com.neteinstein.perfsentient.core.model.ReportTime
import com.neteinstein.perfsentient.core.model.ThemeMode
import com.neteinstein.perfsentient.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class ObservePermissionsUseCase(private val repository: PermissionRepository) {
    operator fun invoke(): StateFlow<List<PermissionState>> = repository.permissions
}

class ObserveSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<UserSettings> = repository.settings
}

class UpdateThemeModeUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class UpdateDynamicColorUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDynamicColor(enabled)
}

/** Persists the report time and reschedules the end-of-day worker if monitoring runs. */
class UpdateReportTimeUseCase(
    private val repository: SettingsRepository,
    private val scheduler: MonitoringScheduler,
) {
    suspend operator fun invoke(time: ReportTime) {
        repository.setReportTime(time)
        if (repository.settings.first().monitoringEnabled) scheduler.reschedule(time)
    }
}
