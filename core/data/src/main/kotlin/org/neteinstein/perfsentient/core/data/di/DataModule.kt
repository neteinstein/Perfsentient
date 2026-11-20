package org.neteinstein.perfsentient.core.data.di

import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.neteinstein.perfsentient.core.common.DefaultDispatcherProvider
import org.neteinstein.perfsentient.core.common.DispatcherProvider
import org.neteinstein.perfsentient.core.common.SystemTimeProvider
import org.neteinstein.perfsentient.core.common.TimeProvider
import org.neteinstein.perfsentient.core.data.local.PerfsentienceDatabase
import org.neteinstein.perfsentient.core.data.repository.AppInventoryRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.NotificationStatsRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.ParentalReviewRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.PermissionRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.ReportRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.SettingsRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.TrustedAppsRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.UsageRepositoryImpl
import org.neteinstein.perfsentient.core.data.repository.VitalsRepositoryImpl
import org.neteinstein.perfsentient.core.data.source.AppInventoryDataSource
import org.neteinstein.perfsentient.core.data.source.DeviceVitalsDataSource
import org.neteinstein.perfsentient.core.data.source.NetworkStatsDataSource
import org.neteinstein.perfsentient.core.data.source.SystemErrorLogDataSource
import org.neteinstein.perfsentient.core.data.source.UsageStatsDataSource
import org.neteinstein.perfsentient.core.domain.repository.AppInventoryRepository
import org.neteinstein.perfsentient.core.domain.repository.NotificationStatsRepository
import org.neteinstein.perfsentient.core.domain.repository.ParentalReviewRepository
import org.neteinstein.perfsentient.core.domain.repository.PermissionRepository
import org.neteinstein.perfsentient.core.domain.repository.ReportRepository
import org.neteinstein.perfsentient.core.domain.repository.SettingsRepository
import org.neteinstein.perfsentient.core.domain.repository.TrustedAppsRepository
import org.neteinstein.perfsentient.core.domain.repository.UsageRepository
import org.neteinstein.perfsentient.core.domain.repository.VitalsRepository

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
    single { SystemErrorLogDataSource(androidContext()) }
    singleOf(::DeviceVitalsDataSource)

    single<PermissionRepository> { PermissionRepositoryImpl(androidContext(), get()) }
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
    singleOf(::TrustedAppsRepositoryImpl) { bind<TrustedAppsRepository>() }
    singleOf(::ParentalReviewRepositoryImpl) { bind<ParentalReviewRepository>() }
    singleOf(::UsageRepositoryImpl) { bind<UsageRepository>() }
    singleOf(::AppInventoryRepositoryImpl) { bind<AppInventoryRepository>() }
    singleOf(::VitalsRepositoryImpl) { bind<VitalsRepository>() }
    singleOf(::NotificationStatsRepositoryImpl) { bind<NotificationStatsRepository>() }
    singleOf(::ReportRepositoryImpl) { bind<ReportRepository>() }
}
