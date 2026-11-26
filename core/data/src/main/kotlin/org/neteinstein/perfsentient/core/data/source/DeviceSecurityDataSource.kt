package org.neteinstein.perfsentient.core.data.source

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import org.neteinstein.perfsentient.core.model.DeviceSecurityPosture

/** Screen lock, security patch level and USB debugging. None of these need a permission. */
class DeviceSecurityDataSource(private val context: Context) {
    fun posture(): DeviceSecurityPosture = DeviceSecurityPosture(
        hasSecureLock = runCatching { context.getSystemService(KeyguardManager::class.java).isDeviceSecure }.getOrDefault(true),
        securityPatch = parseSecurityPatch(Build.VERSION.SECURITY_PATCH),
        adbEnabled = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1 }
            .getOrDefault(false),
    )
}
