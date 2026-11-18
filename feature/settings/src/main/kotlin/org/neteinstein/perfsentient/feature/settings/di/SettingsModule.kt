package org.neteinstein.perfsentient.feature.settings.di

import org.neteinstein.perfsentient.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}
