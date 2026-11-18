package org.neteinstein.perfsentient.core.domain.di

import org.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.DeviceHealthAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import org.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import org.neteinstein.perfsentient.core.domain.usecase.GenerateDailyReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveFlaggedAppsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveLatestReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObservePermissionsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveReportHistoryUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveTrustedAppsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.RecordNotificationUseCase
import org.neteinstein.perfsentient.core.domain.usecase.RecordVitalSampleUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
import org.neteinstein.perfsentient.core.domain.usecase.TrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateDynamicColorUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateReportTimeUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateThemeModeUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    single { DetectionThresholds() }
    single { BehaviorAnalyzer(thresholds = get()) }
    singleOf(::DeviceHealthAnalyzer)
    single { HealthScoreCalculator() }
    singleOf(::TrustedAppsFilter)

    factoryOf(::GenerateDailyReportUseCase)
    factoryOf(::ObserveLatestReportUseCase)
    factoryOf(::ObserveReportHistoryUseCase)
    factoryOf(::ObserveMonitoringStateUseCase)
    factoryOf(::SyncMonitoringUseCase)
    factoryOf(::RecordVitalSampleUseCase)
    factoryOf(::RecordNotificationUseCase)
    factoryOf(::ObservePermissionsUseCase)
    factoryOf(::ObserveSettingsUseCase)
    factoryOf(::UpdateThemeModeUseCase)
    factoryOf(::UpdateDynamicColorUseCase)
    factoryOf(::UpdateReportTimeUseCase)
    factoryOf(::ObserveTrustedAppsUseCase)
    factoryOf(::ObserveFlaggedAppsUseCase)
    factoryOf(::TrustAppUseCase)
    factoryOf(::UntrustAppUseCase)
}
