package com.neteinstein.perfsentient.core.domain.analysis

import com.neteinstein.perfsentient.core.model.UsageEventRecord
import com.neteinstein.perfsentient.core.model.UsageEventType

/** A continuous period during which [packageName] owned the foreground. */
data class ForegroundSession(
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
    /** True when the session started within the unlock window after the keyguard was dismissed. */
    val startedRightAfterUnlock: Boolean,
) {
    val durationMs: Long get() = endMs - startMs
}

/**
 * Rebuilds the sequence of foreground apps from raw usage events.
 * System UI transitions (notification shade, permission dialogs) are ignored so that
 * "A -> SystemUI -> A" is not considered an app switch.
 */
object ForegroundSessions {
    fun build(
        events: List<UsageEventRecord>,
        ignoredPackages: Set<String>,
        unlockWindowMs: Long,
    ): List<ForegroundSession> {
        val sessions = mutableListOf<ForegroundSession>()
        var currentPkg: String? = null
        var currentStart = 0L
        var currentAfterUnlock = false
        var lastUnlockAt = Long.MIN_VALUE / 2

        fun close(at: Long) {
            val pkg = currentPkg ?: return
            if (at >= currentStart) {
                sessions += ForegroundSession(pkg, currentStart, at, currentAfterUnlock)
            }
            currentPkg = null
        }

        for (event in events.sortedBy { it.timestamp }) {
            when (event.type) {
                UsageEventType.ACTIVITY_RESUMED -> {
                    if (event.packageName in ignoredPackages || event.packageName == currentPkg) continue
                    close(event.timestamp)
                    currentPkg = event.packageName
                    currentStart = event.timestamp
                    currentAfterUnlock = event.timestamp - lastUnlockAt in 0..unlockWindowMs
                }

                UsageEventType.SCREEN_NON_INTERACTIVE, UsageEventType.KEYGUARD_SHOWN -> close(event.timestamp)

                UsageEventType.KEYGUARD_HIDDEN -> lastUnlockAt = event.timestamp

                else -> Unit
            }
        }
        events.maxOfOrNull { it.timestamp }?.let { close(it) }
        return sessions
    }
}
