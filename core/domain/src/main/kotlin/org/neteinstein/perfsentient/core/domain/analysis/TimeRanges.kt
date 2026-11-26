package org.neteinstein.perfsentient.core.domain.analysis

import kotlin.math.max
import kotlin.math.min
import org.neteinstein.perfsentient.core.model.UsageEventRecord
import org.neteinstein.perfsentient.core.model.UsageEventType

/** A time range from [startMs] (inclusive) to [endMs] (exclusive). */
data class TimeRange(val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

    fun overlapMs(start: Long, end: Long): Long = (min(endMs, end) - max(startMs, start)).coerceAtLeast(0L)
}

/** Total time of these ranges inside [start]..[end]. The ranges must not overlap each other. */
fun List<TimeRange>.overlapMs(start: Long, end: Long): Long = sumOf { it.overlapMs(start, end) }

/** The parts of these ranges that are not covered by [other]. Both lists must be free of self-overlaps. */
fun List<TimeRange>.minus(other: List<TimeRange>): List<TimeRange> {
    if (other.isEmpty()) return this
    val cuts = other.sortedBy { it.startMs }
    return flatMap { range ->
        val pieces = mutableListOf<TimeRange>()
        var cursor = range.startMs
        for (cut in cuts) {
            if (cut.endMs <= cursor) continue
            if (cut.startMs >= range.endMs) break
            if (cut.startMs > cursor) pieces += TimeRange(cursor, cut.startMs)
            cursor = max(cursor, cut.endMs)
        }
        if (cursor < range.endMs) pieces += TimeRange(cursor, range.endMs)
        pieces
    }
}

/**
 * When each app ran at least one foreground service (music, navigation, a VPN…), rebuilt from
 * FOREGROUND_SERVICE_START/STOP events.
 *
 * - Several services of one app nest: the range ends when the last one stops.
 * - A service still running at the end of the window ends at [windowEndMs].
 * - A STOP before any START means the service was already running: the range starts at [windowStartMs].
 */
object ForegroundServiceIntervals {
    fun build(events: List<UsageEventRecord>, windowStartMs: Long, windowEndMs: Long): Map<String, List<TimeRange>> {
        val open = HashMap<String, OpenService>()
        val seen = HashSet<String>()
        val result = HashMap<String, MutableList<TimeRange>>()

        fun add(pkg: String, start: Long, end: Long) {
            if (end > start) result.getOrPut(pkg) { mutableListOf() } += TimeRange(start, end)
        }

        for (event in events.sortedBy { it.timestamp }) {
            val pkg = event.packageName
            when (event.type) {
                UsageEventType.FOREGROUND_SERVICE_START -> {
                    val current = open[pkg]
                    open[pkg] = current?.copy(depth = current.depth + 1) ?: OpenService(event.timestamp, depth = 1)
                    seen += pkg
                }

                UsageEventType.FOREGROUND_SERVICE_STOP -> {
                    val current = open[pkg]
                    when {
                        current == null && pkg !in seen -> add(pkg, windowStartMs, event.timestamp)

                        current == null -> Unit

                        current.depth > 1 -> open[pkg] = current.copy(depth = current.depth - 1)

                        else -> {
                            add(pkg, current.startMs, event.timestamp)
                            open.remove(pkg)
                        }
                    }
                    seen += pkg
                }

                else -> Unit
            }
        }
        open.forEach { (pkg, service) -> add(pkg, service.startMs, windowEndMs) }
        return result
    }

    private data class OpenService(val startMs: Long, val depth: Int)
}

/** When the screen was off, rebuilt from SCREEN_INTERACTIVE/NON_INTERACTIVE events. */
object ScreenOffIntervals {
    /**
     * [screenOnAtStart] is used only when no screen event says otherwise: the first screen event
     * reveals the state before it (turning on means it was off).
     */
    fun build(events: List<UsageEventRecord>, windowStartMs: Long, windowEndMs: Long, screenOnAtStart: Boolean): List<TimeRange> {
        val screenEvents = events
            .filter { it.type == UsageEventType.SCREEN_INTERACTIVE || it.type == UsageEventType.SCREEN_NON_INTERACTIVE }
            .sortedBy { it.timestamp }
        val first = screenEvents.firstOrNull()
        var offSince: Long? = when {
            first == null -> if (screenOnAtStart) null else windowStartMs
            first.type == UsageEventType.SCREEN_INTERACTIVE -> windowStartMs
            else -> null
        }
        val ranges = mutableListOf<TimeRange>()
        for (event in screenEvents) {
            val since = offSince
            if (event.type == UsageEventType.SCREEN_NON_INTERACTIVE) {
                if (since == null) offSince = event.timestamp
            } else if (since != null) {
                if (event.timestamp > since) ranges += TimeRange(since, event.timestamp)
                offSince = null
            }
        }
        offSince?.let { if (windowEndMs > it) ranges += TimeRange(it, windowEndMs) }
        return ranges
    }
}
