package org.neteinstein.perfsentient.feature.apps.di

import org.neteinstein.perfsentient.feature.apps.presentation.AppsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appsModule = module {
    viewModelOf(::AppsViewModel)
}
