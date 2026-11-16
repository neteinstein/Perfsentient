package com.neteinstein.persentient.core.domain.di

import com.neteinstein.persentient.core.domain.analysis.BehaviorAnalyzer
import com.neteinstein.persentient.core.domain.analysis.DetectionThresholds
import com.neteinstein.persentient.core.domain.analysis.DeviceHealthAnalyzer
import com.neteinstein.persentient.core.domain.analysis.HealthScoreCalculator
import com.neteinstein.persentient.core.domain.usecase.GenerateDailyReportUseCase
import com.neteinstein.persentient.core.domain.usecase.ObserveLatestReportUseCase
import com.neteinstein.persentient.core.domain.usecase.ObserveMonitoringStateUseCase
import com.neteinstein.persentient.core.domain.usecase.ObservePermissionsUseCase
import com.neteinstein.persentient.core.domain.usecase.ObserveReportHistoryUseCase
import com.neteinstein.persentient.core.domain.usecase.ObserveSettingsUseCase
import com.neteinstein.persentient.core.domain.usecase.RecordNotificationUseCase
import com.neteinstein.persentient.core.domain.usecase.RecordVitalSampleUseCase
import com.neteinstein.persentient.core.domain.usecase.SyncMonitoringUseCase
import com.neteinstein.persentient.core.domain.usecase.UpdateDynamicColorUseCase
import com.neteinstein.persentient.core.domain.usecase.UpdateReportTimeUseCase
import com.neteinstein.persentient.core.domain.usecase.UpdateThemeModeUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    single { DetectionThresholds() }
    single { BehaviorAnalyzer(thresholds = get()) }
    singleOf(::DeviceHealthAnalyzer)
    single { HealthScoreCalculator() }

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
}
