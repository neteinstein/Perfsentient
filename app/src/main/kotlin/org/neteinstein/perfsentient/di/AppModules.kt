package org.neteinstein.perfsentient.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.neteinstein.perfsentient.MainViewModel
import org.neteinstein.perfsentient.core.data.di.dataModule
import org.neteinstein.perfsentient.core.domain.di.domainModule
import org.neteinstein.perfsentient.feature.apps.di.appsModule
import org.neteinstein.perfsentient.feature.monitoring.di.monitoringModule
import org.neteinstein.perfsentient.feature.parental.di.parentalModule
import org.neteinstein.perfsentient.feature.report.di.reportModule
import org.neteinstein.perfsentient.feature.settings.di.settingsModule

val appModule = module {
    viewModelOf(::MainViewModel)
}

/** Whole dependency graph: data -> domain -> features -> app. */
val appModules = listOf(
    dataModule,
    domainModule,
    monitoringModule,
    reportModule,
    appsModule,
    parentalModule,
    settingsModule,
    appModule,
)
