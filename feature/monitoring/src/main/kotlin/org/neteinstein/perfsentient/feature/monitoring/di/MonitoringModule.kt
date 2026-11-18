package org.neteinstein.perfsentient.feature.monitoring.di

import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module
import org.neteinstein.perfsentient.core.domain.repository.MonitoringScheduler
import org.neteinstein.perfsentient.feature.monitoring.notification.ReportNotifier
import org.neteinstein.perfsentient.feature.monitoring.scheduler.WorkManagerMonitoringScheduler
import org.neteinstein.perfsentient.feature.monitoring.worker.DailyReportWorker
import org.neteinstein.perfsentient.feature.monitoring.worker.VitalsSamplerWorker

val monitoringModule = module {
    single { WorkManager.getInstance(androidContext()) }
    singleOf(::WorkManagerMonitoringScheduler) binds arrayOf(MonitoringScheduler::class, WorkManagerMonitoringScheduler::class)
    single { ReportNotifier(androidContext()) }
    workerOf(::VitalsSamplerWorker)
    workerOf(::DailyReportWorker)
}
