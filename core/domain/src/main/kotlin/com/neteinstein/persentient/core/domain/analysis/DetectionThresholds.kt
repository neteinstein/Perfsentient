package com.neteinstein.persentient.core.domain.analysis

import com.neteinstein.persentient.core.common.BYTES_IN_GB
import com.neteinstein.persentient.core.common.BYTES_IN_MB
import com.neteinstein.persentient.core.common.MILLIS_IN_HOUR
import com.neteinstein.persentient.core.common.MILLIS_IN_MINUTE
import com.neteinstein.persentient.core.common.MILLIS_IN_SECOND

/**
 * Tunable thresholds. Defaults are based on Android vitals "bad behaviour" thresholds
 * (e.g. 50 MB/day background mobile data, 2 h/day of background wake locks) and on
 * published adware research (out-of-context ads, after-call ads, hidden icons).
 */
data class DetectionThresholds(
    val popupMaxDurationMs: Long = 20 * MILLIS_IN_SECOND,
    val popupMediumCount: Int = 3,
    val popupHighCount: Int = 8,
    val unlockPopupWindowMs: Long = 3 * MILLIS_IN_SECOND,
    val afterCallMaxDurationMs: Long = 60 * MILLIS_IN_SECOND,
    val afterCallMediumCount: Int = 1,
    val afterCallHighCount: Int = 3,
    val rarelyUsedForegroundMs: Long = 15 * MILLIS_IN_MINUTE,
    val persistentBackgroundMediumMs: Long = 2 * MILLIS_IN_HOUR,
    val persistentBackgroundHighMs: Long = 8 * MILLIS_IN_HOUR,
    val overlaySuspiciousServiceMs: Long = 30 * MILLIS_IN_MINUTE,
    val overlaySuspiciousBytes: Long = 20 * BYTES_IN_MB,
    val hiddenActivityBytes: Long = 1 * BYTES_IN_MB,
    val backgroundDataMediumBytes: Long = 50 * BYTES_IN_MB,
    val backgroundDataHighBytes: Long = 200 * BYTES_IN_MB,
    val notificationMediumCount: Int = 40,
    val notificationHighCount: Int = 100,
    val notificationAbsoluteCount: Int = 150,
    val nightStartHour: Int = 0,
    val nightEndHour: Int = 6,
    val nightMediumCount: Int = 3,
    val nightHighCount: Int = 10,
    val unrestrictedBatteryServiceMs: Long = 1 * MILLIS_IN_HOUR,
    val storageLowBytes: Long = 1 * BYTES_IN_GB,
    val storageMediumBytes: Long = 3 * BYTES_IN_GB,
    val screenOffDrainMedium: Float = 1.5f,
    val screenOffDrainHigh: Float = 3f,
    val minScreenOffHoursForDrain: Float = 2f,
    val temperatureMediumC: Float = 42f,
    val temperatureHighC: Float = 46f,
    val thermalMedium: Int = 3,
    val thermalHigh: Int = 4,
    val lowMemoryRatio: Float = 0.2f,
    val maxSampleGapMs: Long = 1 * MILLIS_IN_HOUR,
)
