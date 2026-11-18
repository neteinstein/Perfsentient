package org.neteinstein.perfsentient.feature.settings.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.neteinstein.perfsentient.feature.settings.presentation.SettingsViewModel

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}
