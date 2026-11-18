package org.neteinstein.perfsentient

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.neteinstein.perfsentient.di.appModules

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
