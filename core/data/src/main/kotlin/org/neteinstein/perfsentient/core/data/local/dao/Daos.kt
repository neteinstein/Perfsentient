package org.neteinstein.perfsentient.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.neteinstein.perfsentient.core.data.local.entity.NotificationCountEntity
import org.neteinstein.perfsentient.core.data.local.entity.ReportEntity
import org.neteinstein.perfsentient.core.data.local.entity.VitalSampleEntity

@Dao
interface VitalSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: VitalSampleEntity)

    @Query("SELECT * FROM vital_samples WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp")
    suspend fun between(start: Long, end: Long): List<VitalSampleEntity>

    @Query("DELETE FROM vital_samples WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

@Dao
interface NotificationCountDao {
    @Query(
        """
        INSERT INTO notification_counts(hourStartMs, packageName, count) VALUES(:hourStartMs, :packageName, 1)
        ON CONFLICT(hourStartMs, packageName) DO UPDATE SET count = count + 1
        """,
    )
    suspend fun increment(hourStartMs: Long, packageName: String)

    @Query("SELECT * FROM notification_counts WHERE hourStartMs BETWEEN :start AND :end")
    suspend fun between(start: Long, end: Long): List<NotificationCountEntity>

    @Query("DELETE FROM notification_counts WHERE hourStartMs < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

@Dao
interface ReportDao {
    @Upsert
    suspend fun upsert(report: ReportEntity)

    @Query("SELECT * FROM reports ORDER BY day DESC LIMIT 1")
    fun observeLatest(): Flow<ReportEntity?>

    @Query("SELECT * FROM reports ORDER BY day DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<ReportEntity>>

    @Query("DELETE FROM reports WHERE day < :day")
    suspend fun deleteOlderThan(day: String)
}
