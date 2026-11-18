package org.neteinstein.perfsentient.feature.parental.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.neteinstein.perfsentient.feature.parental.presentation.ParentalViewModel

val parentalModule = module {
    viewModelOf(::ParentalViewModel)
}
