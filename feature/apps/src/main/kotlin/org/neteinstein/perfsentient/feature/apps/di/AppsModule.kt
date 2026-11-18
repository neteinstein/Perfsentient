package org.neteinstein.perfsentient.feature.apps.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.neteinstein.perfsentient.feature.apps.presentation.AppsViewModel

val appsModule = module {
    viewModelOf(::AppsViewModel)
}
