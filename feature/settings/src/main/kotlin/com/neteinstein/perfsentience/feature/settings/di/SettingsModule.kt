package com.neteinstein.perfsentience.feature.settings.di

import com.neteinstein.perfsentience.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}
