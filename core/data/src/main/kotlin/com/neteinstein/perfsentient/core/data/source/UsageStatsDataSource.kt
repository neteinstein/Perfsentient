package com.neteinstein.perfsentient.core.data.source

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import com.neteinstein.perfsentient.core.model.AppUsage
import com.neteinstein.perfsentient.core.model.UsageEventRecord
import com.neteinstein.perfsentient.core.model.UsageEventType

/** Thin wrapper over [UsageStatsManager]; requires the Usage access special permission. */
class UsageStatsDataSource(private val usageStatsManager: UsageStatsManager) {

    fun appUsage(startMs: Long, endMs: Long): List<AppUsage> = runCatching {
        usageStatsManager.queryAndAggregateUsageStats(startMs, endMs).values.map { stats ->
            AppUsage(
                packageName = stats.packageName,
                foregroundTimeMs = stats.totalTimeInForeground,
                foregroundServiceTimeMs = stats.totalTimeForegroundServiceUsed,
                lastTimeUsed = stats.lastTimeUsed,
            )
        }
    }.getOrDefault(emptyList())

    fun events(startMs: Long, endMs: Long): List<UsageEventRecord> {
        val events = runCatching { usageStatsManager.queryEvents(startMs, endMs) }.getOrNull() ?: return emptyList()
        val result = ArrayList<UsageEventRecord>(INITIAL_CAPACITY)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = mapType(event.eventType) ?: continue
            result += UsageEventRecord(event.packageName, type, event.timeStamp)
        }
        return result
    }

    private fun mapType(type: Int): UsageEventType? = when (type) {
        UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ACTIVITY_RESUMED
        UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.ACTIVITY_PAUSED
        UsageEvents.Event.FOREGROUND_SERVICE_START -> UsageEventType.FOREGROUND_SERVICE_START
        UsageEvents.Event.FOREGROUND_SERVICE_STOP -> UsageEventType.FOREGROUND_SERVICE_STOP
        UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventType.SCREEN_INTERACTIVE
        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_NON_INTERACTIVE
        UsageEvents.Event.KEYGUARD_SHOWN -> UsageEventType.KEYGUARD_SHOWN
        UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEventType.KEYGUARD_HIDDEN
        else -> null
    }

    private companion object {
        const val INITIAL_CAPACITY = 4_096
    }
}
