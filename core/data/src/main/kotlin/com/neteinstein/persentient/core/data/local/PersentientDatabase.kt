package com.neteinstein.persentient.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neteinstein.persentient.core.data.local.dao.NotificationCountDao
import com.neteinstein.persentient.core.data.local.dao.ReportDao
import com.neteinstein.persentient.core.data.local.dao.VitalSampleDao
import com.neteinstein.persentient.core.data.local.entity.NotificationCountEntity
import com.neteinstein.persentient.core.data.local.entity.ReportEntity
import com.neteinstein.persentient.core.data.local.entity.VitalSampleEntity

@Database(
    entities = [VitalSampleEntity::class, NotificationCountEntity::class, ReportEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PersentientDatabase : RoomDatabase() {
    abstract fun vitalSampleDao(): VitalSampleDao

    abstract fun notificationCountDao(): NotificationCountDao

    abstract fun reportDao(): ReportDao

    companion object {
        const val NAME = "persentient.db"
    }
}
