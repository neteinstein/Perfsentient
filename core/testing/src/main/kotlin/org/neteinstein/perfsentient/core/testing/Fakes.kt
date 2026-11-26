package org.neteinstein.perfsentient.core.testing

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import org.neteinstein.perfsentient.core.domain.repository.DeviceSecurityRepository
import org.neteinstein.perfsentient.core.domain.repository.MonitoringScheduler
import org.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import org.neteinstein.perfsentient.core.domain.repository.ParentalReviewRepository
import org.neteinstein.perfsentient.core.domain.repository.PermissionRepository
import org.neteinstein.perfsentient.core.domain.repository.ReportRepository
import org.neteinstein.perfsentient.core.domain.repository.ScamMessageRepository
import org.neteinstein.perfsentient.core.domain.repository.SecurityWatchRepository
import org.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import org.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import org.neteinstein.perfsentient.core.domain.repository.UsageRepository
import org.neteinstein.perfsentient.core.domain.repository.VitalsRepository
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
import org.neteinstein.perfsentient.core.model.CapabilityHolders
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceContext
import org.neteinstein.perfsentient.core.model.DeviceSecurityPosture
import org.neteinstein.perfsentient.core.model.GrantMode
import org.neteinstein.perfsentient.core.model.NetworkUsage
import org.neteinstein.perfsentient.core.model.PackageChangeCursor
import org.neteinstein.perfsentient.core.model.PackageChanges
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.PermissionState
import org.neteinstein.perfsentient.core.model.PermissionType
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.ScamMessageSignal
import org.neteinstein.perfsentient.core.model.SecurityWatchState
import org.neteinstein.perfsentient.core.model.SmsPartnerStatus
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

    override suspend fun setParentalSettings(parental: ParentalSettings) {
        state.value = state.value.copy(parental = parental)
    }

    override suspend fun setSecurityAlertsEnabled(enabled: Boolean) {
        state.value = state.value.copy(securityAlertsEnabled = enabled)
    }
}

class FakeParentalReviewRepository(initial: Set<String> = emptySet()) : ParentalReviewRepository {
    val state = MutableStateFlow(initial)
    override val reviewedPackages: Flow<Set<String>> = state

    override suspend fun setReviewed(packageName: String, reviewed: Boolean) {
        state.value = if (reviewed) state.value + packageName else state.value - packageName
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

    /** The (start, end) window of the last [getEvents] call. */
    var lastEventsWindow: Pair<Long, Long>? = null
        private set

    override suspend fun getEvents(startMs: Long, endMs: Long): List<UsageEventRecord> {
        lastEventsWindow = startMs to endMs
        return events
    }

    override suspend fun getNetworkUsage(startMs: Long, endMs: Long): List<NetworkUsage> = network
}

class FakeAppInventoryRepository(
    var apps: List<AppProfile> = emptyList(),
    var context: DeviceContext = TestData.deviceContext(),
    var holders: CapabilityHolders = CapabilityHolders.NONE,
) : AppInventoryRepository {
    /** Returned by the next [getChangedPackages] call. */
    var changes: PackageChanges = PackageChanges(PackageChangeCursor(bootCount = 1, sequence = 0), emptySet())

    /** The cursor [getChangedPackages] was last called with. */
    var lastChangesSince: PackageChangeCursor? = null
        private set

    /** Package sets passed to [getProfiles]. */
    val profileLookups = mutableListOf<Set<String>>()

    override suspend fun getChangedPackages(since: PackageChangeCursor?): PackageChanges {
        lastChangesSince = since
        return changes
    }

    override suspend fun getProfiles(packageNames: Set<String>): List<AppProfile> {
        profileLookups += packageNames
        return apps.filter { it.packageName in packageNames }
    }

    override suspend fun getCapabilityHolders(): CapabilityHolders = holders

    var labelLookups = 0
        private set

    override suspend fun getInstalledApps(): List<AppProfile> = apps

    override suspend fun getInstalledAppLabels(packageNames: Set<String>): Map<String, String> {
        labelLookups++
        return apps.filter { it.packageName in packageNames }.associate { it.packageName to it.label }
    }

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

class FakeDeviceSecurityRepository(var posture: DeviceSecurityPosture = DeviceSecurityPosture.UNKNOWN) : DeviceSecurityRepository {
    override suspend fun posture(): DeviceSecurityPosture = posture
}

class FakeScamMessageRepository(
    var status: SmsPartnerStatus = SmsPartnerStatus.NOT_INSTALLED,
    var signals: List<ScamMessageSignal> = emptyList(),
) : ScamMessageRepository {
    /** The (start, end) window of the last [getSignals] call. */
    var lastWindow: Pair<Long, Long>? = null
        private set

    override suspend fun status(): SmsPartnerStatus = status

    override suspend fun getSignals(startMs: Long, endMs: Long): List<ScamMessageSignal> {
        lastWindow = startMs to endMs
        return signals.filter { it.receivedAtMs in startMs..endMs }
    }
}

class FakeSecurityWatchRepository(var stored: SecurityWatchState? = null) : SecurityWatchRepository {
    override suspend fun state(): SecurityWatchState? = stored

    override suspend fun save(state: SecurityWatchState) {
        stored = state
    }
}

class FakeNotificationStatsRepository : NotificationStatsRepository {
    /** Every recorded notification as (posted at, package). */
    val posted = mutableListOf<Pair<Long, String>>()
    var deletedBefore: Long? = null

    override suspend fun increment(packageName: String, atMs: Long) {
        posted += atMs to packageName
    }

    override suspend fun getCounts(startMs: Long, endMs: Long): Map<String, Int> =
        posted.filter { it.first in startMs..endMs }.groupingBy { it.second }.eachCount()

    override suspend fun deleteOlderThan(timestampMs: Long) {
        deletedBefore = timestampMs
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
