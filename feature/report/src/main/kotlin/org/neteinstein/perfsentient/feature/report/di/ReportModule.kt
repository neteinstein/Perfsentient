package org.neteinstein.perfsentient.feature.report.di

import org.neteinstein.perfsentient.feature.report.presentation.ReportViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportModule = module {
    viewModelOf(::ReportViewModel)
}
