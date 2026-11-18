package com.neteinstein.perfsentient.feature.monitoring.scheduler

import com.neteinstein.perfsentient.core.model.ReportTime
import java.time.Duration
import java.time.ZonedDateTime

/** Pure helper: how long until the next occurrence of [time] (always in the future). */
internal object ReportTimeCalculator {
    private val MIN_DELAY: Duration = Duration.ofMinutes(1)

    fun delayUntilNext(now: ZonedDateTime, time: ReportTime): Duration {
        var target = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        if (!target.isAfter(now.plus(MIN_DELAY))) target = target.plusDays(1)
        return Duration.between(now, target)
    }
}
