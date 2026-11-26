package org.neteinstein.perfsentient.core.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.neteinstein.perfsentient.core.domain.analysis.BatteryAttributor
import org.neteinstein.perfsentient.core.domain.analysis.BehaviorAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.DetectionThresholds
import org.neteinstein.perfsentient.core.domain.analysis.DeviceHealthAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.DeviceSecurityAnalyzer
import org.neteinstein.perfsentient.core.domain.analysis.HealthScoreCalculator
import org.neteinstein.perfsentient.core.domain.analysis.TrustedAppsFilter
import org.neteinstein.perfsentient.core.domain.parental.ParentalAppCatalog
import org.neteinstein.perfsentient.core.domain.parental.ParentalRiskAssessor
import org.neteinstein.perfsentient.core.domain.scam.ScamSignalCatalog
import org.neteinstein.perfsentient.core.domain.usecase.GenerateDailyReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveBatteryHistoryUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveFlaggedAppsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveLatestReportUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveMonitoringStateUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveParentalScanUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObservePermissionsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveReportHistoryUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.ObserveTrustedAppsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.RecordNotificationUseCase
import org.neteinstein.perfsentient.core.domain.usecase.RecordVitalSampleUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SetParentalAppReviewedUseCase
import org.neteinstein.perfsentient.core.domain.usecase.SyncMonitoringUseCase
import org.neteinstein.perfsentient.core.domain.usecase.TrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UntrustAppUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateDynamicColorUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateParentalSettingsUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateReportTimeUseCase
import org.neteinstein.perfsentient.core.domain.usecase.UpdateThemeModeUseCase

val domainModule = module {
    single { DetectionThresholds() }
    single { ScamSignalCatalog() }
    single { BehaviorAnalyzer(thresholds = get(), catalog = get()) }
    singleOf(::DeviceHealthAnalyzer)
    singleOf(::DeviceSecurityAnalyzer)
    singleOf(::BatteryAttributor)
    single { HealthScoreCalculator() }
    singleOf(::TrustedAppsFilter)
    single { ParentalAppCatalog() }
    single { ParentalRiskAssessor(catalog = get()) }

    factoryOf(::GenerateDailyReportUseCase)
    factoryOf(::ObserveLatestReportUseCase)
    factoryOf(::ObserveReportHistoryUseCase)
    factoryOf(::ObserveBatteryHistoryUseCase)
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
    factoryOf(::UpdateParentalSettingsUseCase)
    factoryOf(::ObserveParentalScanUseCase)
    factoryOf(::SetParentalAppReviewedUseCase)
}
