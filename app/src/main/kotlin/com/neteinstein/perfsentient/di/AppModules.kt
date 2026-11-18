package com.neteinstein.perfsentient.di

import com.neteinstein.perfsentient.MainViewModel
import com.neteinstein.perfsentient.core.data.di.dataModule
import com.neteinstein.perfsentient.core.domain.di.domainModule
import com.neteinstein.perfsentient.feature.apps.di.appsModule
import com.neteinstein.perfsentient.feature.monitoring.di.monitoringModule
import com.neteinstein.perfsentient.feature.report.di.reportModule
import com.neteinstein.perfsentient.feature.settings.di.settingsModule
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
    appsModule,
    settingsModule,
    appModule,
)
