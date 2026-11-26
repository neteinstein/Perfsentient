package org.neteinstein.perfsentient.core.domain.analysis

import org.neteinstein.perfsentient.core.common.BYTES_IN_GB
import org.neteinstein.perfsentient.core.common.BYTES_IN_MB
import org.neteinstein.perfsentient.core.common.MILLIS_IN_HOUR
import org.neteinstein.perfsentient.core.common.MILLIS_IN_MINUTE
import org.neteinstein.perfsentient.core.common.MILLIS_IN_SECOND

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
    /** The third-party screen must appear at most this long after the call screen closed. */
    val afterCallMaxGapMs: Long = 10 * MILLIS_IN_SECOND,
    val afterCallMediumCount: Int = 2,
    val afterCallHighCount: Int = 4,
    val rarelyUsedForegroundMs: Long = 15 * MILLIS_IN_MINUTE,
    val persistentBackgroundMediumMs: Long = 2 * MILLIS_IN_HOUR,
    val persistentBackgroundHighMs: Long = 8 * MILLIS_IN_HOUR,
    val overlaySuspiciousServiceMs: Long = 30 * MILLIS_IN_MINUTE,
    val overlaySuspiciousBytes: Long = 20 * BYTES_IN_MB,
    val hiddenActivityBytes: Long = 1 * BYTES_IN_MB,
    val backgroundDataMediumBytes: Long = 50 * BYTES_IN_MB,
    val backgroundDataHighBytes: Long = 200 * BYTES_IN_MB,
    /** Unmetered (Wi-Fi) background traffic is cheap; only flag it when it is really large. */
    val backgroundWifiDataMediumBytes: Long = 500 * BYTES_IN_MB,
    val backgroundWifiDataHighBytes: Long = 2 * BYTES_IN_GB,
    val anrMediumCount: Int = 2,
    val anrHighCount: Int = 5,
    val crashMediumCount: Int = 3,
    val crashHighCount: Int = 10,
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
    /** An app installed this recently counts as new (remote-access apps, store apps given accessibility). */
    val freshInstallMs: Long = 72 * MILLIS_IN_HOUR,
    /** A remote-access app installed during a call or this soon after it is a strong scam signal. */
    val remoteAccessAfterCallMs: Long = 30 * MILLIS_IN_MINUTE,
    /** Security patch age, in days, from which it is flagged (Android publishes patches monthly). */
    val patchLowDays: Long = 90,
    val patchMediumDays: Long = 180,
    /** An app installed this soon after a scam text with a link (from SMS Toolbox) is part of the scam chain. */
    val scamTextToInstallWindowMs: Long = 2 * MILLIS_IN_HOUR,
    /*
     * Battery estimate weights (see BatteryAttributor). Rough, relative costs: an app on screen
     * keeps the screen, CPU and radio busy; a foreground service off screen (music, navigation)
     * costs a fraction of that; an idle phone with the screen off costs least.
     */
    val batteryForegroundWeight: Float = 1f,
    val batteryServiceWeight: Float = 0.2f,
    val batteryIdleWeight: Float = 0.1f,
    /** How many apps a report keeps in its battery breakdown. */
    val batteryTopApps: Int = 30,
    /** Below this time open, "battery per hour open" is too noisy to show. */
    val batteryMinOpenMsForRate: Long = 5 * MILLIS_IN_MINUTE,
    /** Share of an app's battery spent off screen from which it is "mostly in background". */
    val batteryMostlyBackgroundRatio: Float = 0.5f,
)
