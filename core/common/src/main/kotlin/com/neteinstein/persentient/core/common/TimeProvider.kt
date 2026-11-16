package com.neteinstein.persentient.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Single source of "now" so time-dependent logic stays deterministic in tests. */
interface TimeProvider {
    fun now(): Instant

    fun zone(): ZoneId

    fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    fun startOfToday(): Instant = today().atStartOfDay(zone()).toInstant()

    fun nowZoned(): ZonedDateTime = now().atZone(zone())
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
