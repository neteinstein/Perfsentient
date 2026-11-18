package com.neteinstein.perfsentient.core.domain.di

import com.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import com.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import com.neteinstein.perfsentient.core.domain.analysis.DeviceHealthAnalyzer
import com.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import com.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import com.neteinstein.perfsentient.core.domain.usecase.GenerateDailyReportUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveFlaggedAppsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveLatestReportUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObservePermissionsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveReportHistoryUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.ObserveTrustedAppsUseCase
import com.neteinstein.perfsentient.core.domain.usecase.RecordNotificationUseCase
import com.neteinstein.perfsentient.core.domain.usecase.RecordVitalSampleUseCase
import com.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
import com.neteinstein.perfsentient.core.domain.usecase.TrustAppUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UpdateDynamicColorUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UpdateReportTimeUseCase
import com.neteinstein.perfsentient.core.domain.usecase.UpdateThemeModeUseCase
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
