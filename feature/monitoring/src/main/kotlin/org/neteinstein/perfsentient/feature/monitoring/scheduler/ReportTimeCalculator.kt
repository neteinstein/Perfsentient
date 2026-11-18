package org.neteinstein.perfsentient.feature.monitoring.scheduler

import java.time.Duration
import java.time.ZonedDateTime
import org.neteinstein.perfsentient.core.model.ReportTime

/** Pure helper: how long until the next occurrence of [time] (always in the future). */
internal object ReportTimeCalculator {
    private val MIN_DELAY: Duration = Duration.ofMinutes(1)

    fun delayUntilNext(now: ZonedDateTime, time: ReportTime): Duration {
        var target = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        if (!target.isAfter(now.plus(MIN_DELAY))) target = target.plusDays(1)
        return Duration.between(now, target)
    }
}
