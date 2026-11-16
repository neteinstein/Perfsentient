package com.neteinstein.persentient.feature.report.di

import com.neteinstein.persentient.feature.report.presentation.ReportViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportModule = module {
    viewModelOf(::ReportViewModel)
}
