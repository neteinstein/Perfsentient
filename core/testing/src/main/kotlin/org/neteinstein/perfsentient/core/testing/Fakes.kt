package org.neteinstein.perfsentient.core.testing

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import org.neteinstein.perfsentient.core.domain.repository.MonitoringScheduler
import org.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import org.neteinstein.perfsentient.core.domain.repository.PermissionRepository
import org.neteinstein.perfsentient.core.domain.repository.ReportRepository
import org.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import org.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import org.neteinstein.perfsentient.core.domain.repository.UsageRepository
import org.neteinstein.perfsentient.core.domain.repository.VitalsRepository
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceContext
import org.neteinstein.perfsentient.core.model.GrantMode
import org.neteinstein.perfsentient.core.model.NetworkUsage
import org.neteinstein.perfsentient.core.model.PermissionState
import org.neteinstein.perfsentient.core.model.PermissionType
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.model.UsageEventRecord
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.model.VitalSample

class FakePermissionRepository(granted: Boolean = false) : PermissionRepository {
    var systemGrants: MutableMap<PermissionType, Boolean> = PermissionType.entries.associateWith { granted }.toMutableMap()
    private val state = MutableStateFlow(snapshot())
    var refreshCount = 0
        private set

    override val permissions: StateFlow<List<PermissionState>> = state

    override fun refresh() {
        refreshCount++
        state.value = snapshot()
    }

    fun grantAll() {
        PermissionType.entries.forEach { systemGrants[it] = true }
        refresh()
    }

    private fun snapshot() = PermissionType.entries.map { PermissionState(it, systemGrants[it] == true, true, GrantMode.SYSTEM_SETTINGS) }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        state.value = state.value.copy(useDynamicColor = enabled)
    }

    override suspend fun setReportTime(time: ReportTime) {
        state.value = state.value.copy(reportTime = time)
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        state.value = state.value.copy(monitoringEnabled = enabled)
    }
}

class FakeTrustedAppsRepository(initial: List<TrustedApp> = emptyList()) : TrustedAppsRepository {
    val state = MutableStateFlow(initial)
    override val trustedApps: Flow<List<TrustedApp>> = state

    override suspend fun trust(app: TrustedApp) {
        state.value = state.value.filterNot { it.packageName == app.packageName } + app
    }

    override suspend fun untrust(packageName: String) {
        state.value = state.value.filterNot { it.packageName == packageName }
    }
}

class FakeMonitoringScheduler : MonitoringScheduler {
    val ensured = mutableListOf<ReportTime>()
    val rescheduled = mutableListOf<ReportTime>()
    var cancelCount = 0
        private set

    override fun ensureScheduled(reportTime: ReportTime) {
        ensured += reportTime
    }

    override fun reschedule(reportTime: ReportTime) {
        rescheduled += reportTime
    }

    override fun cancel() {
        cancelCount++
    }
}

class FakeUsageRepository(
    var usage: List<AppUsage> = emptyList(),
    var events: List<UsageEventRecord> = emptyList(),
    var network: List<NetworkUsage> = emptyList(),
) : UsageRepository {
    var failWith: Throwable? = null

    override suspend fun getAppUsage(startMs: Long, endMs: Long): List<AppUsage> {
        failWith?.let { throw it }
        return usage
    }

    override suspend fun getEvents(startMs: Long, endMs: Long): List<UsageEventRecord> = events

    override suspend fun getNetworkUsage(startMs: Long, endMs: Long): List<NetworkUsage> = network
}

class FakeAppInventoryRepository(
    var apps: List<AppProfile> = emptyList(),
    var context: DeviceContext = TestData.deviceContext(),
) : AppInventoryRepository {
    override suspend fun getInstalledApps(): List<AppProfile> = apps

    override suspend fun getDeviceContext(): DeviceContext = context
}

class FakeVitalsRepository(var next: VitalSample = TestData.vital(0)) : VitalsRepository {
    val samples = mutableListOf<VitalSample>()
    var deletedBefore: Long? = null

    override suspend fun capture(): VitalSample = next

    override suspend fun save(sample: VitalSample) {
        samples += sample
    }

    override suspend fun getSamples(startMs: Long, endMs: Long): List<VitalSample> = samples.filter { it.timestamp in startMs..endMs }

    override suspend fun deleteOlderThan(timestampMs: Long) {
        deletedBefore = timestampMs
        samples.removeAll { it.timestamp < timestampMs }
    }
}

class FakeNotificationStatsRepository : NotificationStatsRepository {
    val counts = mutableMapOf<Pair<LocalDate, String>, Int>()
    var deletedBefore: LocalDate? = null

    override suspend fun increment(packageName: String, day: LocalDate) {
        counts.merge(day to packageName, 1, Int::plus)
    }

    override suspend fun getCounts(day: LocalDate): Map<String, Int> =
        counts.filterKeys { it.first == day }.mapKeys { it.key.second }

    override suspend fun deleteOlderThan(day: LocalDate) {
        deletedBefore = day
    }
}

class FakeReportRepository : ReportRepository {
    val reports = MutableStateFlow<List<DailyReport>>(emptyList())
    var deletedBefore: LocalDate? = null

    override fun observeLatest(): Flow<DailyReport?> = reports.map { list -> list.maxByOrNull { it.date } }

    override fun observeHistory(limit: Int): Flow<List<DailyReport>> = reports.map { list ->
        list.sortedByDescending { it.date }.take(limit)
    }

    override suspend fun save(report: DailyReport) {
        reports.value = reports.value.filterNot { it.date == report.date } + report
    }

    override suspend fun deleteOlderThan(day: LocalDate) {
        deletedBefore = day
    }
}
