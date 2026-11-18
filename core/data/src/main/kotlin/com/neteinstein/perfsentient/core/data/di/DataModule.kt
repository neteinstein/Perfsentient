package com.neteinstein.perfsentient.core.data.di

import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.neteinstein.perfsentient.core.common.DefaultDispatcherProvider
import com.neteinstein.perfsentient.core.common.DispatcherProvider
import com.neteinstein.perfsentient.core.common.SystemTimeProvider
import com.neteinstein.perfsentient.core.common.TimeProvider
import com.neteinstein.perfsentient.core.data.local.PerfsentienceDatabase
import com.neteinstein.perfsentient.core.data.repository.AppInventoryRepositoryImpl
import com.neteinstein.perfsentient.core.data.repository.NotificationStatsRepositoryImpl
import com.neteinstein.perfsentient.core.data.repository.PermissionRepositoryImpl
import com.neteinstein.perfsentient.core.data.repository.ReportRepositoryImpl
import com.neteinstein.perfsentient.core.data.repository.SettingsRepositoryImpl
import com.neteinstein.perfsentient.core.data.repository.TrustedAppsRepositoryImpl
import com.neteinstein.perfsentient.core.data.repository.UsageRepositoryImpl
import com.neteinstein.perfsentient.core.data.repository.VitalsRepositoryImpl
import com.neteinstein.perfsentient.core.data.source.AppInventoryDataSource
import com.neteinstein.perfsentient.core.data.source.DeviceVitalsDataSource
import com.neteinstein.perfsentient.core.data.source.NetworkStatsDataSource
import com.neteinstein.perfsentient.core.data.source.UsageStatsDataSource
import com.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import com.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import com.neteinstein.perfsentient.core.domain.repository.PermissionRepository
import com.neteinstein.perfsentient.core.domain.repository.ReportRepository
import com.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import com.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import com.neteinstein.perfsentient.core.domain.repository.UsageRepository
import com.neteinstein.perfsentient.core.domain.repository.VitalsRepository
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
    singleOf(::TrustedAppsRepositoryImpl) { bind<TrustedAppsRepository>() }
    singleOf(::UsageRepositoryImpl) { bind<UsageRepository>() }
    singleOf(::AppInventoryRepositoryImpl) { bind<AppInventoryRepository>() }
    singleOf(::VitalsRepositoryImpl) { bind<VitalsRepository>() }
    singleOf(::NotificationStatsRepositoryImpl) { bind<NotificationStatsRepository>() }
    singleOf(::ReportRepositoryImpl) { bind<ReportRepository>() }
}
