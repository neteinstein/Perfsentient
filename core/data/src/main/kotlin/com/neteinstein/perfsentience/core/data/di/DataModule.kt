package com.neteinstein.perfsentience.core.data.di

import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.neteinstein.perfsentience.core.common.DefaultDispatcherProvider
import com.neteinstein.perfsentience.core.common.DispatcherProvider
import com.neteinstein.perfsentience.core.common.SystemTimeProvider
import com.neteinstein.perfsentience.core.common.TimeProvider
import com.neteinstein.perfsentience.core.data.local.PerfsentienceDatabase
import com.neteinstein.perfsentience.core.data.repository.AppInventoryRepositoryImpl
import com.neteinstein.perfsentience.core.data.repository.NotificationStatsRepositoryImpl
import com.neteinstein.perfsentience.core.data.repository.PermissionRepositoryImpl
import com.neteinstein.perfsentience.core.data.repository.ReportRepositoryImpl
import com.neteinstein.perfsentience.core.data.repository.SettingsRepositoryImpl
import com.neteinstein.perfsentience.core.data.repository.UsageRepositoryImpl
import com.neteinstein.perfsentience.core.data.repository.VitalsRepositoryImpl
import com.neteinstein.perfsentience.core.data.source.AppInventoryDataSource
import com.neteinstein.perfsentience.core.data.source.DeviceVitalsDataSource
import com.neteinstein.perfsentience.core.data.source.NetworkStatsDataSource
import com.neteinstein.perfsentience.core.data.source.UsageStatsDataSource
import com.neteinstein.perfsentience.core.domain.repository.AppInventoryRepository
import com.neteinstein.perfsentience.core.domain.repository.NotificationStatsRepository
import com.neteinstein.perfsentience.core.domain.repository.PermissionRepository
import com.neteinstein.perfsentience.core.domain.repository.ReportRepository
import com.neteinstein.perfsentience.core.domain.repository.SettingsRepository
import com.neteinstein.perfsentience.core.domain.repository.UsageRepository
import com.neteinstein.perfsentience.core.domain.repository.VitalsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

val dataModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<TimeProvider> { SystemTimeProvider() }

    single {
        Room.databaseBuilder(androidContext(), PerfsentienceDatabase::class.java, PerfsentienceDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<PerfsentienceDatabase>().vitalSampleDao() }
    single { get<PerfsentienceDatabase>().notificationCountDao() }
    single { get<PerfsentienceDatabase>().reportDao() }
    single { androidContext().settingsDataStore }

    single { UsageStatsDataSource(androidContext().getSystemService(UsageStatsManager::class.java)) }
    single {
        NetworkStatsDataSource(
            androidContext().getSystemService(NetworkStatsManager::class.java),
            androidContext().packageManager,
        )
    }
    single { AppInventoryDataSource(androidContext()) }
    singleOf(::DeviceVitalsDataSource)

    single<PermissionRepository> { PermissionRepositoryImpl(androidContext()) }
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
    singleOf(::UsageRepositoryImpl) { bind<UsageRepository>() }
    singleOf(::AppInventoryRepositoryImpl) { bind<AppInventoryRepository>() }
    singleOf(::VitalsRepositoryImpl) { bind<VitalsRepository>() }
    singleOf(::NotificationStatsRepositoryImpl) { bind<NotificationStatsRepository>() }
    singleOf(::ReportRepositoryImpl) { bind<ReportRepository>() }
}
