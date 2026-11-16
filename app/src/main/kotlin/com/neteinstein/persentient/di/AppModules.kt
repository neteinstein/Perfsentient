package com.neteinstein.persentient.di

import com.neteinstein.persentient.MainViewModel
import com.neteinstein.persentient.core.data.di.dataModule
import com.neteinstein.persentient.core.domain.di.domainModule
import com.neteinstein.persentient.feature.monitoring.di.monitoringModule
import com.neteinstein.persentient.feature.report.di.reportModule
import com.neteinstein.persentient.feature.settings.di.settingsModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::MainViewModel)
}

/** Whole dependency graph: data -> domain -> features -> app. */
val appModules = listOf(
    dataModule,
    domainModule,
    monitoringModule,
    reportModule,
    settingsModule,
    appModule,
)
