package org.neteinstein.perfsentient.screenshots

import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.neteinstein.perfsentient.core.common.DispatcherProvider
import org.neteinstein.perfsentient.core.common.TimeProvider
import org.neteinstein.perfsentient.core.domain.di.domainModule
import org.neteinstein.perfsentient.core.domain.parental.ParentalAppCatalog
import org.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import org.neteinstein.perfsentient.core.domain.repository.DeviceSecurityRepository
import org.neteinstein.perfsentient.core.domain.repository.MonitoringScheduler
import org.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import org.neteinstein.perfsentient.core.domain.repository.ParentalReviewRepository
import org.neteinstein.perfsentient.core.domain.repository.PermissionRepository
import org.neteinstein.perfsentient.core.domain.repository.ReportRepository
import org.neteinstein.perfsentient.core.domain.repository.ScamMessageRepository
import org.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import org.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import org.neteinstein.perfsentient.core.domain.repository.UsageRepository
import org.neteinstein.perfsentient.core.domain.repository.VitalsRepository
import org.neteinstein.perfsentient.core.model.ParentalSettings
import org.neteinstein.perfsentient.core.model.TrustedApp
import org.neteinstein.perfsentient.core.model.UserSettings
import org.neteinstein.perfsentient.core.testing.FakeAppInventoryRepository
import org.neteinstein.perfsentient.core.testing.FakeDeviceSecurityRepository
import org.neteinstein.perfsentient.core.testing.FakeMonitoringScheduler
import org.neteinstein.perfsentient.core.testing.FakeNotificationStatsRepository
import org.neteinstein.perfsentient.core.testing.FakeParentalReviewRepository
import org.neteinstein.perfsentient.core.testing.FakePermissionRepository
import org.neteinstein.perfsentient.core.testing.FakeReportRepository
import org.neteinstein.perfsentient.core.testing.FakeScamMessageRepository
import org.neteinstein.perfsentient.core.testing.FakeSettingsRepository
import org.neteinstein.perfsentient.core.testing.FakeTimeProvider
import org.neteinstein.perfsentient.core.testing.FakeTrustedAppsRepository
import org.neteinstein.perfsentient.core.testing.FakeUsageRepository
import org.neteinstein.perfsentient.core.testing.FakeVitalsRepository
import org.neteinstein.perfsentient.core.testing.TestDispatcherProvider
import org.neteinstein.perfsentient.feature.apps.di.appsModule
import org.neteinstein.perfsentient.feature.parental.di.parentalModule
import org.neteinstein.perfsentient.feature.report.di.reportModule
import org.neteinstein.perfsentient.feature.settings.di.settingsModule

/** Starts the real feature/domain Koin modules backed by fakes holding [SampleData] (fictional apps only). */
internal fun startSampleKoin() {
    val reports = FakeReportRepository().apply { reports.value = SampleData.reports }
    val trusted = FakeTrustedAppsRepository(
        listOf(
            TrustedApp("com.example.musicplayer", "Beat Player"),
            TrustedApp("com.example.vpn", "SafeTunnel VPN"),
        ),
    )
    startKoin {
        modules(
            domainModule,
            reportModule,
            appsModule,
            settingsModule,
            parentalModule,
            module {
                single<DispatcherProvider> { TestDispatcherProvider() }
                single<TimeProvider> { FakeTimeProvider() }
                single<PermissionRepository> { FakePermissionRepository(granted = true) }
                single<SettingsRepository> {
                    FakeSettingsRepository(
                        UserSettings(
                            monitoringEnabled = true,
                            parental = ParentalSettings(enabled = true, childAge = SampleData.CHILD_AGE),
                        ),
                    )
                }
                single<TrustedAppsRepository> { trusted }
                single<ReportRepository> { reports }
                single<MonitoringScheduler> { FakeMonitoringScheduler() }
                single<UsageRepository> { FakeUsageRepository() }
                single<AppInventoryRepository> { FakeAppInventoryRepository(apps = SampleData.installedApps) }
                single<ParentalReviewRepository> { FakeParentalReviewRepository(SampleData.reviewedParentalApps) }
                // Overrides the real catalogue from domainModule with fictional apps.
                single { ParentalAppCatalog(SampleData.parentalCatalog) }
                single<VitalsRepository> { FakeVitalsRepository() }
                single<NotificationStatsRepository> { FakeNotificationStatsRepository() }
                single<DeviceSecurityRepository> { FakeDeviceSecurityRepository() }
                single<ScamMessageRepository> { FakeScamMessageRepository() }
            },
        )
    }
}
