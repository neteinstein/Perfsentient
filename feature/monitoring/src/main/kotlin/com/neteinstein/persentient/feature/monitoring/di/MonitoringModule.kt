package com.neteinstein.persentient.feature.monitoring.di

import androidx.work.WorkManager
import com.neteinstein.persentient.core.domain.repository.MonitoringScheduler
import com.neteinstein.persentient.feature.monitoring.notification.ReportNotifier
import com.neteinstein.persentient.feature.monitoring.scheduler.WorkManagerMonitoringScheduler
import com.neteinstein.persentient.feature.monitoring.worker.DailyReportWorker
import com.neteinstein.persentient.feature.monitoring.worker.VitalsSamplerWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module

val monitoringModule = module {
    single { WorkManager.getInstance(androidContext()) }
    singleOf(::WorkManagerMonitoringScheduler) binds arrayOf(MonitoringScheduler::class, WorkManagerMonitoringScheduler::class)
    single { ReportNotifier(androidContext()) }
    workerOf(::VitalsSamplerWorker)
    workerOf(::DailyReportWorker)
}
