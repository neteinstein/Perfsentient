package org.neteinstein.perfsentient.core.model

import java.time.LocalDate

/**
 * Estimated battery use of one app over a report window.
 *
 * Android does not let third-party apps read another app's battery use, so this is an estimate:
 * each measured battery drop is shared between the apps that were on screen or running a
 * foreground service at the time (see BatteryAttributor in core:domain).
 *
 * Percentages are "% of a full battery".
 */
data class AppBatteryUsage(
    val packageName: String,
    val appLabel: String,
    val estimatedDrainPct: Float,
    /** The part of [estimatedDrainPct] spent while the app was not on screen. */
    val backgroundDrainPct: Float,
    /** Time the app was open on screen. */
    val foregroundMs: Long,
    /** Time the app ran a foreground service while it was not on screen. */
    val backgroundMs: Long,
)

/** How the battery drops measured in a report window were shared out. */
data class BatteryBreakdown(
    /** The apps with the highest estimate, highest first. */
    val apps: List<AppBatteryUsage>,
    /** Screen-on time with no app open (home screen, lock screen, notification shade). */
    val screenAndSystemPct: Float,
    /** Screen-off time that no app's background work explains. */
    val standbyPct: Float,
    /** Total drop measured while discharging. */
    val measuredDrainPct: Float,
    /** How many hours of discharging the samples covered. */
    val measuredHours: Float,
) {
    companion object {
        val EMPTY = BatteryBreakdown(emptyList(), 0f, 0f, 0f, 0f)
    }
}

/** One day of an app's battery history. Days without a report have zeros. */
data class DailyBatteryPoint(
    val date: LocalDate,
    val drainPct: Float,
    val foregroundMs: Long,
)

/** An app's estimated battery use and time open, summed over the history window. */
data class AppBatteryHistory(
    val packageName: String,
    val appLabel: String,
    val drainPct: Float,
    val backgroundDrainPct: Float,
    val foregroundMs: Long,
    val backgroundMs: Long,
    /**
     * Battery used while on screen, per hour on screen (background use is left out: it is what
     * [mostlyBackground] is about). Null when the app was open too briefly for the rate to mean anything.
     */
    val drainPerHourOpen: Float?,
    /** Most of the battery went to work done while the app was not on screen. */
    val mostlyBackground: Boolean,
    /** One point per day of [BatteryHistory.days], oldest first. */
    val daily: List<DailyBatteryPoint>,
)

/** Battery vs time open for the last few days. */
data class BatteryHistory(
    /** The days covered, oldest first. */
    val days: List<LocalDate>,
    val apps: List<AppBatteryHistory>,
    val screenAndSystemPct: Float,
    val standbyPct: Float,
    val measuredDrainPct: Float,
    val measuredHours: Float,
) {
    val isEmpty: Boolean get() = apps.isEmpty()

    fun sortedBy(sort: BatterySort): BatteryHistory = copy(
        apps = when (sort) {
            BatterySort.MOST_BATTERY -> apps.sortedWith(
                compareByDescending<AppBatteryHistory> { it.drainPct }.thenByDescending { it.foregroundMs },
            )

            BatterySort.PER_HOUR_OPEN -> apps.sortedWith(
                compareByDescending<AppBatteryHistory> { it.drainPerHourOpen != null }
                    .thenByDescending { it.drainPerHourOpen ?: 0f }
                    .thenByDescending { it.drainPct },
            )
        },
    )

    companion object {
        val EMPTY = BatteryHistory(emptyList(), emptyList(), 0f, 0f, 0f, 0f)
    }
}

/** How the battery list is ordered. */
enum class BatterySort {
    /** Highest estimated battery use first. */
    MOST_BATTERY,

    /** Highest battery per hour open first; apps barely opened go last. */
    PER_HOUR_OPEN,
}
