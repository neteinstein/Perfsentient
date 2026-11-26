package org.neteinstein.perfsentient.core.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.neteinstein.perfsentient.core.model.AppIssueRecord
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
import org.neteinstein.perfsentient.core.model.CapabilityHolders
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceContext
import org.neteinstein.perfsentient.core.model.DeviceSecurityPosture
import org.neteinstein.perfsentient.core.model.NetworkUsage
import org.neteinstein.perfsentient.core.model.PackageChangeCursor
import org.neteinstein.perfsentient.core.model.PackageChanges
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.PermissionState
import org.neteinstein.perfsentient.core.model.ReportTime
import org.neteinstein.perfsentient.core.model.ScamMessageSignal
import org.neteinstein.perfsentient.core.model.SecurityWatchState
import org.neteinstein.perfsentient.core.model.SmsPartnerStatus
import org.neteinstein.perfsentient.core.model.ThemeMode
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.model.UsageEventRecord
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.model.VitalSample

interface PermissionRepository {
    val permissions: StateFlow<List<PermissionState>>

    /** Re-reads the grant state from the system (call on resume / after returning from settings). */
    fun refresh()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setReportTime(time: ReportTime)

    suspend fun setMonitoringEnabled(enabled: Boolean)

    suspend fun setParentalSettings(parental: ParentalSettings)

    suspend fun setSecurityAlertsEnabled(enabled: Boolean)
}

/** Concerning apps the parent already reviewed (e.g. set up the app's own controls) in parental mode. */
interface ParentalReviewRepository {
    val reviewedPackages: Flow<Set<String>>

    suspend fun setReviewed(packageName: String, reviewed: Boolean)
}

/** The user's allowlist: apps that are never flagged in reports. */
interface TrustedAppsRepository {
    val trustedApps: Flow<List<TrustedApp>>

    /** Adds [app] to the allowlist, replacing the stored label if it was already there. */
    suspend fun trust(app: TrustedApp)

    suspend fun untrust(packageName: String)
}

interface UsageRepository {
    suspend fun getAppUsage(startMs: Long, endMs: Long): List<AppUsage>

    suspend fun getEvents(startMs: Long, endMs: Long): List<UsageEventRecord>

    suspend fun getNetworkUsage(startMs: Long, endMs: Long): List<NetworkUsage>

    /** ANRs and crashes of apps in the window; empty unless the optional system-log access was granted. */
    suspend fun getAppIssues(startMs: Long, endMs: Long): List<AppIssueRecord>
}

interface AppInventoryRepository {
    suspend fun getInstalledApps(): List<AppProfile>

    /** Labels of the apps among [packageNames] that are installed, keyed by package name. Cheap: no usage or storage stats. */
    suspend fun getInstalledAppLabels(packageNames: Set<String>): Map<String, String>

    suspend fun getDeviceContext(): DeviceContext

    /** Apps installed or updated since [since] (everything since boot when null or after a reboot). */
    suspend fun getChangedPackages(since: PackageChangeCursor?): PackageChanges

    /** Profiles of the installed apps among [packageNames]; cheaper than [getInstalledApps] (no storage stats). */
    suspend fun getProfiles(packageNames: Set<String>): List<AppProfile>

    suspend fun getCapabilityHolders(): CapabilityHolders
}

/** State of the live security check (see CheckSecurityChangesUseCase). */
interface SecurityWatchRepository {
    /** Null until the first check has recorded a baseline. */
    suspend fun state(): SecurityWatchState?

    suspend fun save(state: SecurityWatchState)
}

/** Scam warnings shared by SMS Toolbox (read-only, opt-in in SMS Toolbox, verified by signature). */
interface ScamMessageRepository {
    suspend fun status(): SmsPartnerStatus

    /** Flagged texts received in [startMs]..[endMs]; empty when SMS Toolbox is missing, unverified or not connected. */
    suspend fun getSignals(startMs: Long, endMs: Long): List<ScamMessageSignal>
}

/** Screen lock, security patch and USB debugging state. */
interface DeviceSecurityRepository {
    suspend fun posture(): DeviceSecurityPosture
}

interface VitalsRepository {
    /** Reads the current device vitals from the system. */
    suspend fun capture(): VitalSample

    suspend fun save(sample: VitalSample)

    suspend fun getSamples(startMs: Long, endMs: Long): List<VitalSample>

    suspend fun deleteOlderThan(timestampMs: Long)
}

/** Notifications posted per app, stored in hourly buckets so any time window can be summed. */
interface NotificationStatsRepository {
    /** Counts one notification from [packageName] in the hour that contains [atMs]. */
    suspend fun increment(packageName: String, atMs: Long)

    /** Notifications per app in every hour that overlaps [startMs]..[endMs]. */
    suspend fun getCounts(startMs: Long, endMs: Long): Map<String, Int>

    suspend fun deleteOlderThan(timestampMs: Long)
}

interface ReportRepository {
    fun observeLatest(): Flow<DailyReport?>

    fun observeHistory(limit: Int): Flow<List<DailyReport>>

    suspend fun save(report: DailyReport)

    suspend fun deleteOlderThan(day: LocalDate)
}

/** Background scheduling contract, implemented by feature:monitoring (WorkManager). */
interface MonitoringScheduler {
    /** Schedules sampling + the end-of-day report, keeping any work that is already queued. */
    fun ensureScheduled(reportTime: ReportTime)

    /** Replaces the queued end-of-day report (e.g. after the user changed the report time). */
    fun reschedule(reportTime: ReportTime)

    fun cancel()
}
