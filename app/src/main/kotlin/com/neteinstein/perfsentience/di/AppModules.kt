package com.neteinstein.perfsentience.di

import com.neteinstein.perfsentience.MainViewModel
import com.neteinstein.perfsentience.core.data.di.dataModule
import com.neteinstein.perfsentience.core.domain.di.domainModule
import com.neteinstein.perfsentience.feature.apps.di.appsModule
import com.neteinstein.perfsentience.feature.monitoring.di.monitoringModule
import com.neteinstein.perfsentience.feature.report.di.reportModule
import com.neteinstein.perfsentience.feature.settings.di.settingsModule
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
