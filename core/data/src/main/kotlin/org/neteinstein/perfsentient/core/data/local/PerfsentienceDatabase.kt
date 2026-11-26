package org.neteinstein.perfsentient.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.neteinstein.perfsentient.core.data.local.dao.NotificationCountDao
import org.neteinstein.perfsentient.core.data.local.dao.ReportDao
import org.neteinstein.perfsentient.core.data.local.dao.VitalSampleDao
import org.neteinstein.perfsentient.core.data.local.entity.NotificationCountEntity
import org.neteinstein.perfsentient.core.data.local.entity.ReportEntity
import org.neteinstein.perfsentient.core.data.local.entity.VitalSampleEntity

@Database(
    entities = [VitalSampleEntity::class, NotificationCountEntity::class, ReportEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class PerfsentienceDatabase : RoomDatabase() {
    abstract fun vitalSampleDao(): VitalSampleDao

    abstract fun notificationCountDao(): NotificationCountDao

    abstract fun reportDao(): ReportDao

    companion object {
        const val NAME = "perfsentience.db"
    }
}
