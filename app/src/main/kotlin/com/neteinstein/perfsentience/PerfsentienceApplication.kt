package com.neteinstein.perfsentience

import android.app.Application
import com.neteinstein.perfsentience.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class PerfsentienceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@PerfsentienceApplication)
            workManagerFactory()
            modules(appModules)
        }
    }
}
