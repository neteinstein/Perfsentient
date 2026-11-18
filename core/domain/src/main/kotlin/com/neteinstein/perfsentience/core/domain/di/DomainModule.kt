package com.neteinstein.perfsentience.core.domain.di

import com.neteinstein.perfsentience.core.domain.analysis.BehaviorAnalyzer
import com.neteinstein.perfsentience.core.domain.analysis.DetectionThresholds
import com.neteinstein.perfsentience.core.domain.analysis.DeviceHealthAnalyzer
import com.neteinstein.perfsentience.core.domain.analysis.HealthScoreCalculator
import com.neteinstein.perfsentience.core.domain.analysis.TrustedAppsFilter
import com.neteinstein.perfsentience.core.domain.usecase.GenerateDailyReportUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObserveFlaggedAppsUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObserveLatestReportUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObserveMonitoringStateUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObservePermissionsUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObserveReportHistoryUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.perfsentience.core.domain.usecase.ObserveTrustedAppsUseCase
import com.neteinstein.perfsentience.core.domain.usecase.RecordNotificationUseCase
import com.neteinstein.perfsentience.core.domain.usecase.RecordVitalSampleUseCase
import com.neteinstein.perfsentience.core.domain.usecase.SyncMonitoringUseCase
import com.neteinstein.perfsentience.core.domain.usecase.TrustAppUseCase
import com.neteinstein.perfsentience.core.domain.usecase.UntrustAppUseCase
import com.neteinstein.perfsentience.core.domain.usecase.UpdateDynamicColorUseCase
import com.neteinstein.perfsentience.core.domain.usecase.UpdateReportTimeUseCase
import com.neteinstein.perfsentience.core.domain.usecase.UpdateThemeModeUseCase
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
