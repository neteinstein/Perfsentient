package org.neteinstein.perfsentient.feature.monitoring

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.feature.monitoring.notification.SecurityAlertNotifier
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class SecurityAlertNotifierTest {
    private val context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun `posts one high-importance notification per app`() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        SecurityAlertNotifier(context).notify(
            listOf(
                Finding("bad", "System Update", BehaviorType.IMPERSONATION, Severity.MEDIUM, 1),
                Finding("bad", "System Update", BehaviorType.RISKY_INSTALL_SOURCE, Severity.HIGH, 0),
                Finding("rat", "AnyDesk", BehaviorType.REMOTE_ACCESS, Severity.HIGH, 0),
            ),
        )

        val posted = shadowOf(manager).allNotifications
        assertEquals(2, posted.size)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel("security_alerts").importance)
        val titles = posted.map { it.extras.getString("android.title") }.toSet()
        assertEquals(setOf("System Update: Risky install source", "AnyDesk: Remote access app"), titles)
    }

    @Test
    fun `nothing is posted without the notification permission`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        SecurityAlertNotifier(context).notify(listOf(Finding("rat", "AnyDesk", BehaviorType.REMOTE_ACCESS, Severity.HIGH, 0)))

        assertEquals(0, shadowOf(manager).allNotifications.size)
    }
}
