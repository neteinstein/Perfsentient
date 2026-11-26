package org.neteinstein.perfsentient.core.data.source

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import org.neteinstein.perfsentient.core.common.TimeProvider
import org.neteinstein.perfsentient.core.model.VitalSample

/** Reads battery, thermal, screen and memory state. None of these need a permission. */
class DeviceVitalsDataSource(
    private val context: Context,
    private val timeProvider: TimeProvider,
) {
    private val power = context.getSystemService(PowerManager::class.java)
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val batteryManager = context.getSystemService(BatteryManager::class.java)

    fun capture(): VitalSample {
        val battery: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, PERCENT) ?: PERCENT
        val percent = if (level >= 0 &&
            scale > 0
        ) {
            level * PERCENT / scale
        } else {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
        val temperatureTenths = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val memory = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }

        return VitalSample(
            timestamp = timeProvider.now().toEpochMilli(),
            batteryPercent = percent.coerceIn(0, PERCENT),
            isCharging = batteryManager.isCharging,
            batteryTemperatureC = temperatureTenths / TENTHS,
            isScreenOn = power.isInteractive,
            thermalStatus = power.currentThermalStatus,
            availableMemoryBytes = memory.availMem,
            totalMemoryBytes = memory.totalMem,
            isLowMemory = memory.lowMemory,
            chargeCounterUah = chargeCounter(),
        )
    }

    /** Long.MIN_VALUE or 0 when the device does not report it. */
    private fun chargeCounter(): Long? = runCatching {
        batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
    }.getOrNull()?.takeIf { it > 0 }

    private companion object {
        const val PERCENT = 100
        const val TENTHS = 10f
    }
}
