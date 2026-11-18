package com.neteinstein.perfsentience.feature.monitoring.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.neteinstein.perfsentience.core.common.TimeProvider
import com.neteinstein.perfsentience.core.domain.usecase.RecordNotificationUseCase
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Counts how many *new* notifications each app posts per day. Notification content is never
 * read or stored – only the posting package name.
 */
class AppNotificationListener :
    NotificationListenerService(),
    KoinComponent {
    private val recordNotification: RecordNotificationUseCase by inject()
    private val timeProvider: TimeProvider by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val seenKeys = mutableSetOf<String>()
    private var seenDay: LocalDate? = null

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName == packageName || notification.isOngoing) return
        if (notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (!markSeen(notification.key)) return
        scope.launch { recordNotification(notification.packageName) }
    }

    /** Updates of the same notification (progress, edits) are only counted once per day. */
    @Synchronized
    private fun markSeen(key: String): Boolean {
        val today = timeProvider.today()
        if (seenDay != today) {
            seenKeys.clear()
            seenDay = today
        }
        if (seenKeys.size > MAX_TRACKED_KEYS) seenKeys.clear()
        return seenKeys.add(key)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val MAX_TRACKED_KEYS = 5_000
    }
}
