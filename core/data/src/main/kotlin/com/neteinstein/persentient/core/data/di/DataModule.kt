package com.neteinstein.persentient.core.data.di

import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.neteinstein.persentient.core.common.DefaultDispatcherProvider
import com.neteinstein.persentient.core.common.DispatcherProvider
import com.neteinstein.persentient.core.common.SystemTimeProvider
import com.neteinstein.persentient.core.common.TimeProvider
import com.neteinstein.persentient.core.data.local.PersentientDatabase
import com.neteinstein.persentient.core.data.repository.AppInventoryRepositoryImpl
import com.neteinstein.persentient.core.data.repository.NotificationStatsRepositoryImpl
import com.neteinstein.persentient.core.data.repository.PermissionRepositoryImpl
import com.neteinstein.persentient.core.data.repository.ReportRepositoryImpl
import com.neteinstein.persentient.core.data.repository.SettingsRepositoryImpl
import com.neteinstein.persentient.core.data.repository.UsageRepositoryImpl
import com.neteinstein.persentient.core.data.repository.VitalsRepositoryImpl
import com.neteinstein.persentient.core.data.source.AppInventoryDataSource
import com.neteinstein.persentient.core.data.source.DeviceVitalsDataSource
import com.neteinstein.persentient.core.data.source.NetworkStatsDataSource
import com.neteinstein.persentient.core.data.source.UsageStatsDataSource
import com.neteinstein.persentient.core.domain.repository.AppInventoryRepository
import com.neteinstein.persentient.core.domain.repository.NotificationStatsRepository
import com.neteinstein.persentient.core.domain.repository.PermissionRepository
import com.neteinstein.persentient.core.domain.repository.ReportRepository
import com.neteinstein.persentient.core.domain.repository.SettingsRepository
import com.neteinstein.persentient.core.domain.repository.UsageRepository
import com.neteinstein.persentient.core.domain.repository.VitalsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

val dataModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<TimeProvider> { SystemTimeProvider() }

    single {
        Room.databaseBuilder(androidContext(), PersentientDatabase::class.java, PersentientDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<PersentientDatabase>().vitalSampleDao() }
    single { get<PersentientDatabase>().notificationCountDao() }
    single { get<PersentientDatabase>().reportDao() }
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
