package org.neteinstein.perfsentient.core.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceContext
import org.neteinstein.perfsentient.core.model.NetworkUsage
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.PermissionState
import org.neteinstein.perfsentient.core.model.ReportTime
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
}

interface AppInventoryRepository {
    suspend fun getInstalledApps(): List<AppProfile>

    /** Labels of the apps among [packageNames] that are installed, keyed by package name. Cheap: no usage or storage stats. */
    suspend fun getInstalledAppLabels(packageNames: Set<String>): Map<String, String>

    suspend fun getDeviceContext(): DeviceContext
}

interface VitalsRepository {
    /** Reads the current device vitals from the system. */
    suspend fun capture(): VitalSample

    suspend fun save(sample: VitalSample)

    suspend fun getSamples(startMs: Long, endMs: Long): List<VitalSample>

    suspend fun deleteOlderThan(timestampMs: Long)
}

interface NotificationStatsRepository {
    suspend fun increment(packageName: String, day: LocalDate)

    suspend fun getCounts(day: LocalDate): Map<String, Int>

    suspend fun deleteOlderThan(day: LocalDate)
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
