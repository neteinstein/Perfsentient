package org.neteinstein.perfsentient.core.designsystem

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.perfsentient.core.designsystem.behavior.FixAction
import org.neteinstein.perfsentient.core.designsystem.behavior.fixAction
import org.neteinstein.perfsentient.core.designsystem.behavior.fixIntent
import org.neteinstein.perfsentient.core.designsystem.behavior.openFix
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.ReadableData
import org.neteinstein.perfsentient.core.model.Severity
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class FixActionTest {
    private val app = RuntimeEnvironment.getApplication()

    private fun finding(type: BehaviorType, value: Long = 1) = Finding("com.bad", "Bad", type, Severity.HIGH, value)

    @Test
    fun `findings map to the screen that fixes them`() {
        assertEquals(FixAction.OVERLAY, finding(BehaviorType.OVERLAY_ABUSE).fixAction)
        assertEquals(FixAction.ACCESSIBILITY, finding(BehaviorType.ACCESSIBILITY_RISK).fixAction)
        assertEquals(FixAction.INSTALL_APPS, finding(BehaviorType.CAN_INSTALL_APPS).fixAction)
        assertEquals(FixAction.APP_NOTIFICATIONS, finding(BehaviorType.NOTIFICATION_SPAM).fixAction)
        assertEquals(FixAction.BACKGROUND_DATA, finding(BehaviorType.BACKGROUND_DATA).fixAction)
        assertEquals(FixAction.BATTERY_OPTIMIZATION, finding(BehaviorType.UNRESTRICTED_BATTERY).fixAction)
        assertEquals(FixAction.DEVICE_ADMIN, finding(BehaviorType.DEVICE_ADMIN).fixAction)
        assertEquals(FixAction.NOTIFICATION_ACCESS, finding(BehaviorType.DATA_THEFT_RISK, ReadableData.NOTIFICATIONS).fixAction)
        assertEquals(FixAction.APP_INFO, finding(BehaviorType.DATA_THEFT_RISK, ReadableData.TEXT_MESSAGES).fixAction)
        assertEquals(FixAction.APP_INFO, finding(BehaviorType.REMOTE_ACCESS).fixAction)
        BehaviorType.entries.forEach { finding(it).fixAction } // every type has an answer
    }

    @Test
    fun `device issues map to settings where one helps`() {
        assertEquals(FixAction.SCREEN_LOCK, DeviceIssueType.NO_SCREEN_LOCK.fixAction)
        assertEquals(FixAction.DEVELOPER_OPTIONS, DeviceIssueType.USB_DEBUGGING.fixAction)
        assertEquals(FixAction.SYSTEM_UPDATE, DeviceIssueType.OUTDATED_SECURITY_PATCH.fixAction)
        assertEquals(FixAction.BATTERY_USAGE, DeviceIssueType.SCREEN_OFF_DRAIN.fixAction)
        assertNull(DeviceIssueType.HIGH_TEMPERATURE.fixAction)
    }

    @Test
    fun `intents target the right screen and package`() {
        val overlay = fixIntent(FixAction.OVERLAY, "com.bad")
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, overlay.action)
        assertEquals("package:com.bad", overlay.dataString)
        assertEquals("com.bad", fixIntent(FixAction.APP_NOTIFICATIONS, "com.bad").getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertEquals(DevicePolicyManager.ACTION_SET_NEW_PASSWORD, fixIntent(FixAction.SCREEN_LOCK, null).action)
        assertEquals(Intent.ACTION_POWER_USAGE_SUMMARY, fixIntent(FixAction.BATTERY_USAGE, null).action)
        FixAction.entries.forEach {
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, fixIntent(it, "p").flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    @Test
    fun `a missing screen falls back to app info`() {
        val shadow = shadowOf(app)
        shadow.checkActivities(true)
        shadowOf(app.packageManager).addResolveInfoForIntent(
            fixIntent(FixAction.APP_INFO, "com.bad"),
            ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = "com.android.settings" } },
        )

        app.openFix(FixAction.BACKGROUND_DATA, "com.bad")

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, shadow.nextStartedActivity.action)
    }

    @Test
    fun `the fix opens directly when the screen exists`() {
        app.openFix(FixAction.ACCESSIBILITY)
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, shadowOf(app).nextStartedActivity.action)
    }
}
