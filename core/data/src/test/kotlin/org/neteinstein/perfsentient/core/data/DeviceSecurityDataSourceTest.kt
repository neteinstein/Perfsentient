package org.neteinstein.perfsentient.core.data

import android.app.Application
import android.app.KeyguardManager
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.perfsentient.core.data.source.DeviceSecurityDataSource
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class DeviceSecurityDataSourceTest {
    private val context = RuntimeEnvironment.getApplication()
    private val source = DeviceSecurityDataSource(context)

    @Test
    fun `reads screen lock and usb debugging`() {
        shadowOf(context.getSystemService(KeyguardManager::class.java)).setIsDeviceSecure(false)
        Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 1)

        val posture = source.posture()

        assertFalse(posture.hasSecureLock)
        assertTrue(posture.adbEnabled)
    }

    @Test
    fun `a secured phone without adb raises nothing`() {
        shadowOf(context.getSystemService(KeyguardManager::class.java)).setIsDeviceSecure(true)
        Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)

        val posture = source.posture()

        assertEquals(true, posture.hasSecureLock)
        assertFalse(posture.adbEnabled)
    }
}
