package org.neteinstein.perfsentient.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vital_samples", indices = [Index("timestamp")])
data class VitalSampleEntity(
    @PrimaryKey val timestamp: Long,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTemperatureC: Float,
    val isScreenOn: Boolean,
    val thermalStatus: Int,
    val availableMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val isLowMemory: Boolean,
    /** Added in v3; null for older samples and devices that do not report it. */
    val chargeCounterUah: Long? = null,
)

@Entity(tableName = "notification_counts", primaryKeys = ["hourStartMs", "packageName"])
data class NotificationCountEntity(
    /** Epoch millis of the start of the hour the notifications were posted in. */
    val hourStartMs: Long,
    val packageName: String,
    val count: Int,
)

@Entity(tableName = "reports")
data class ReportEntity(
    /** ISO-8601 date (yyyy-MM-dd). */
    @PrimaryKey val day: String,
    val generatedAt: Long,
    val score: Int,
    /** Full report serialised with kotlinx.serialization (see ReportDto). */
    val payload: String,
)
