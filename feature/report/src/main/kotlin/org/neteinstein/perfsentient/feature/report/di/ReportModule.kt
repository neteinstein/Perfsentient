package org.neteinstein.perfsentient.feature.report.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.neteinstein.perfsentient.feature.report.presentation.ReportViewModel

val reportModule = module {
    viewModelOf(::ReportViewModel)
}
