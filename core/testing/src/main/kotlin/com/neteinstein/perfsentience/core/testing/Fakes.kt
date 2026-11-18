package com.neteinstein.perfsentience.core.testing

import com.neteinstein.perfsentience.core.domain.repository.AppInventoryRepository
import com.neteinstein.perfsentience.core.domain.repository.MonitoringScheduler
import com.neteinstein.perfsentience.core.domain.repository.NotificationStatsRepository
import com.neteinstein.perfsentience.core.domain.repository.PermissionRepository
import com.neteinstein.perfsentience.core.domain.repository.ReportRepository
import com.neteinstein.perfsentience.core.domain.repository.SettingsRepository
import com.neteinstein.perfsentience.core.domain.repository.TrustedAppsRepository
import com.neteinstein.perfsentience.core.domain.repository.UsageRepository
import com.neteinstein.perfsentience.core.domain.repository.VitalsRepository
import com.neteinstein.perfsentience.core.model.AppProfile
import com.neteinstein.perfsentience.core.model.AppUsage
import com.neteinstein.perfsentience.core.model.DailyReport
import com.neteinstein.perfsentience.core.model.DeviceContext
import com.neteinstein.perfsentience.core.model.GrantMode
import com.neteinstein.perfsentience.core.model.NetworkUsage
import com.neteinstein.perfsentience.core.model.PermissionState
import com.neteinstein.perfsentience.core.model.PermissionType
import com.neteinstein.perfsentience.core.model.ReportTime
import com.neteinstein.perfsentience.core.model.ThemeMode
import com.neteinstein.perfsentience.core.model.TrustedApp
import com.neteinstein.perfsentience.core.model.UsageEventRecord
import com.neteinstein.perfsentience.core.model.UserSettings
import com.neteinstein.perfsentience.core.model.VitalSample
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

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
