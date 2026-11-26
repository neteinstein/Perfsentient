package org.neteinstein.perfsentient.core.data.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.neteinstein.perfsentient.core.common.DispatcherProvider
import org.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import org.neteinstein.perfsentient.core.data.local.dao.NotificationCountDao
import org.neteinstein.perfsentient.core.data.local.dao.ReportDao
import org.neteinstein.perfsentient.core.data.local.dao.VitalSampleDao
import org.neteinstein.perfsentient.core.data.mapper.toDomain
import org.neteinstein.perfsentient.core.data.mapper.toEntity
import org.neteinstein.perfsentient.core.data.source.AppInventoryDataSource
import org.neteinstein.perfsentient.core.data.source.DeviceSecurityDataSource
import org.neteinstein.perfsentient.core.data.source.DeviceVitalsDataSource
import org.neteinstein.perfsentient.core.data.source.NetworkStatsDataSource
import org.neteinstein.perfsentient.core.data.source.UsageStatsDataSource
import org.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import org.neteinstein.perfsentient.core.domain.repository.DeviceSecurityRepository
import org.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import org.neteinstein.perfsentient.core.domain.repository.ReportRepository
import org.neteinstein.perfsentient.core.domain.repository.UsageRepository
import org.neteinstein.perfsentient.core.domain.repository.VitalsRepository
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceContext
import org.neteinstein.perfsentient.core.model.DeviceSecurityPosture
import org.neteinstein.perfsentient.core.model.NetworkUsage
import org.neteinstein.perfsentient.core.model.UsageEventRecord
import org.neteinstein.perfsentient.core.model.VitalSample

class UsageRepositoryImpl(
    private val usageStats: UsageStatsDataSource,
    private val networkStats: NetworkStatsDataSource,
    private val dispatchers: DispatcherProvider,
) : UsageRepository {
    override suspend fun getAppUsage(startMs: Long, endMs: Long): List<AppUsage> =
        withContext(dispatchers.io) { usageStats.appUsage(startMs, endMs) }

    override suspend fun getEvents(startMs: Long, endMs: Long): List<UsageEventRecord> =
        withContext(dispatchers.io) { usageStats.events(startMs, endMs) }

    override suspend fun getNetworkUsage(startMs: Long, endMs: Long): List<NetworkUsage> =
        withContext(dispatchers.io) { networkStats.usage(startMs, endMs) }
}

class AppInventoryRepositoryImpl(
    private val dataSource: AppInventoryDataSource,
    private val dispatchers: DispatcherProvider,
) : AppInventoryRepository {
    override suspend fun getInstalledApps(): List<AppProfile> = withContext(dispatchers.io) { dataSource.installedApps() }

    override suspend fun getInstalledAppLabels(packageNames: Set<String>): Map<String, String> =
        withContext(dispatchers.io) { dataSource.installedAppLabels(packageNames) }

    override suspend fun getDeviceContext(): DeviceContext = withContext(dispatchers.io) { dataSource.deviceContext() }
}

class DeviceSecurityRepositoryImpl(
    private val dataSource: DeviceSecurityDataSource,
    private val dispatchers: DispatcherProvider,
) : DeviceSecurityRepository {
    override suspend fun posture(): DeviceSecurityPosture = withContext(dispatchers.io) { dataSource.posture() }
}

class VitalsRepositoryImpl(
    private val dataSource: DeviceVitalsDataSource,
    private val dao: VitalSampleDao,
    private val dispatchers: DispatcherProvider,
) : VitalsRepository {
    override suspend fun capture(): VitalSample = withContext(dispatchers.io) { dataSource.capture() }

    override suspend fun save(sample: VitalSample) = dao.insert(sample.toEntity())

    override suspend fun getSamples(startMs: Long, endMs: Long): List<VitalSample> = dao.between(startMs, endMs).map { it.toDomain() }

    override suspend fun deleteOlderThan(timestampMs: Long) = dao.deleteOlderThan(timestampMs)
}

class NotificationStatsRepositoryImpl(private val dao: NotificationCountDao) : NotificationStatsRepository {
    override suspend fun increment(packageName: String, atMs: Long) = dao.increment(hourStart(atMs), packageName)

    override suspend fun getCounts(startMs: Long, endMs: Long): Map<String, Int> = dao.between(hourStart(startMs), endMs)
        .groupingBy { it.packageName }
        .fold(0) { total, bucket -> total + bucket.count }

    override suspend fun deleteOlderThan(timestampMs: Long) = dao.deleteOlderThan(hourStart(timestampMs))

    private fun hourStart(ms: Long): Long = ms - Math.floorMod(ms, MILLIS_IN_HOUR)
}

class ReportRepositoryImpl(private val dao: ReportDao) : ReportRepository {
    override fun observeLatest(): Flow<DailyReport?> = dao.observeLatest().map { it?.toDomain() }

    override fun observeHistory(limit: Int): Flow<List<DailyReport>> = dao.observeHistory(limit).map { list ->
        list.mapNotNull { it.toDomain() }
    }

    override suspend fun save(report: DailyReport) = dao.upsert(report.toEntity())

    override suspend fun deleteOlderThan(day: LocalDate) = dao.deleteOlderThan(day.toString())
}
