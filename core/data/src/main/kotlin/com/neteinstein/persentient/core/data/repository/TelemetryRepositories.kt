package com.neteinstein.persentient.core.data.repository

import com.neteinstein.persentient.core.common.DispatcherProvider
import com.neteinstein.persentient.core.data.local.dao.NotificationCountDao
import com.neteinstein.persentient.core.data.local.dao.ReportDao
import com.neteinstein.persentient.core.data.local.dao.VitalSampleDao
import com.neteinstein.persentient.core.data.mapper.toDomain
import com.neteinstein.persentient.core.data.mapper.toEntity
import com.neteinstein.persentient.core.data.source.AppInventoryDataSource
import com.neteinstein.persentient.core.data.source.DeviceVitalsDataSource
import com.neteinstein.persentient.core.data.source.NetworkStatsDataSource
import com.neteinstein.persentient.core.data.source.UsageStatsDataSource
import com.neteinstein.persentient.core.domain.repository.AppInventoryRepository
import com.neteinstein.persentient.core.domain.repository.NotificationStatsRepository
import com.neteinstein.persentient.core.domain.repository.ReportRepository
import com.neteinstein.persentient.core.domain.repository.UsageRepository
import com.neteinstein.persentient.core.domain.repository.VitalsRepository
import com.neteinstein.persentient.core.model.AppProfile
import com.neteinstein.persentient.core.model.AppUsage
import com.neteinstein.persentient.core.model.DailyReport
import com.neteinstein.persentient.core.model.DeviceContext
import com.neteinstein.persentient.core.model.NetworkUsage
import com.neteinstein.persentient.core.model.UsageEventRecord
import com.neteinstein.persentient.core.model.VitalSample
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

    override suspend fun getDeviceContext(): DeviceContext = withContext(dispatchers.io) { dataSource.deviceContext() }
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
    override suspend fun increment(packageName: String, day: LocalDate) = dao.increment(day.toString(), packageName)

    override suspend fun getCounts(day: LocalDate): Map<String, Int> = dao.forDay(day.toString()).associate { it.packageName to it.count }

    override suspend fun deleteOlderThan(day: LocalDate) = dao.deleteOlderThan(day.toString())
}

class ReportRepositoryImpl(private val dao: ReportDao) : ReportRepository {
    override fun observeLatest(): Flow<DailyReport?> = dao.observeLatest().map { it?.toDomain() }

    override fun observeHistory(limit: Int): Flow<List<DailyReport>> = dao.observeHistory(limit).map { list ->
        list.mapNotNull { it.toDomain() }
    }

    override suspend fun save(report: DailyReport) = dao.upsert(report.toEntity())

    override suspend fun deleteOlderThan(day: LocalDate) = dao.deleteOlderThan(day.toString())
}
