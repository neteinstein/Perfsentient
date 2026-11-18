package org.neteinstein.perfsentient.core.data

import org.neteinstein.perfsentient.core.data.local.dao.NotificationCountDao
import org.neteinstein.perfsentient.core.data.local.dao.ReportDao
import org.neteinstein.perfsentient.core.data.local.dao.VitalSampleDao
import org.neteinstein.perfsentient.core.data.local.entity.NotificationCountEntity
import org.neteinstein.perfsentient.core.data.mapper.toEntity
import org.neteinstein.perfsentient.core.data.repository.AppInventoryRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.NotificationStatsRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.ReportRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.UsageRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.VitalsRepositoryImpl
import org.neteinstein.perfsentient.core.data.source.AppInventoryDataSource
import org.neteinstein.perfsentient.core.data.source.DeviceVitalsDataSource
import org.neteinstein.perfsentient.core.data.source.NetworkStatsDataSource
import org.neteinstein.perfsentient.core.data.source.UsageStatsDataSource
import org.neteinstein.perfsentient.core.testing.TestData
import org.neteinstein.perfsentient.core.testing.TestDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryRepositoriesTest {
    private val dispatchers = TestDispatcherProvider()

    @Test
    fun `vitals repository maps entities both ways`() = runTest {
        val dao = mockk<VitalSampleDao>(relaxUnitFun = true)
        val source = mockk<DeviceVitalsDataSource>()
        val sample = TestData.vital(at = 42)
        every { source.capture() } returns sample
        coEvery { dao.between(0, 100) } returns listOf(sample.toEntity())
        val repository = VitalsRepositoryImpl(source, dao, dispatchers)

        assertEquals(sample, repository.capture())
        repository.save(sample)
        assertEquals(listOf(sample), repository.getSamples(0, 100))
        repository.deleteOlderThan(10)

        coVerify { dao.insert(sample.toEntity()) }
        coVerify { dao.deleteOlderThan(10) }
    }

    @Test
    fun `notification stats are keyed by ISO day`() = runTest {
        val dao = mockk<NotificationCountDao>(relaxUnitFun = true)
        coEvery { dao.forDay("2026-11-16") } returns listOf(NotificationCountEntity("2026-11-16", "spam", 12))
        val repository = NotificationStatsRepositoryImpl(dao)
        val day = LocalDate.of(2026, 11, 16)

        repository.increment("spam", day)
        assertEquals(mapOf("spam" to 12), repository.getCounts(day))
        repository.deleteOlderThan(day)

        coVerify { dao.increment("2026-11-16", "spam") }
        coVerify { dao.deleteOlderThan("2026-11-16") }
    }

    @Test
    fun `report repository decodes stored reports`() = runTest {
        val report = TestData.report()
        val dao = mockk<ReportDao>(relaxUnitFun = true)
        every { dao.observeLatest() } returns flowOf(report.toEntity())
        every { dao.observeHistory(7) } returns flowOf(listOf(report.toEntity()))
        val repository = ReportRepositoryImpl(dao)

        assertEquals(report, repository.observeLatest().first())
        assertEquals(listOf(report), repository.observeHistory(7).first())
        repository.save(report)
        repository.deleteOlderThan(report.date)

        coVerify { dao.upsert(report.toEntity()) }
        coVerify { dao.deleteOlderThan("2026-11-16") }
    }

    @Test
    fun `usage and inventory repositories delegate to their data sources`() = runTest {
        val usageSource = mockk<UsageStatsDataSource>()
        val networkSource = mockk<NetworkStatsDataSource>()
        val inventorySource = mockk<AppInventoryDataSource>()
        every { usageSource.appUsage(1, 2) } returns listOf(TestData.usage("a"))
        every { usageSource.events(1, 2) } returns listOf(TestData.resumed("a", 1))
        every { networkSource.usage(1, 2) } returns listOf(TestData.network("a"))
        every { inventorySource.installedApps() } returns listOf(TestData.app("a"))
        every { inventorySource.deviceContext() } returns TestData.deviceContext()

        val usage = UsageRepositoryImpl(usageSource, networkSource, dispatchers)
        val inventory = AppInventoryRepositoryImpl(inventorySource, dispatchers)

        assertEquals("a", usage.getAppUsage(1, 2).single().packageName)
        assertEquals("a", usage.getEvents(1, 2).single().packageName)
        assertEquals("a", usage.getNetworkUsage(1, 2).single().packageName)
        assertEquals("a", inventory.getInstalledApps().single().packageName)
        assertEquals(TestData.deviceContext(), inventory.getDeviceContext())
    }
}
