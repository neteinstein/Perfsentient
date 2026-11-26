package org.neteinstein.perfsentient.feature.monitoring.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.neteinstein.perfsentient.core.designsystem.behavior.presentation
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.feature.monitoring.MonitoringContract
import org.neteinstein.perfsentient.feature.monitoring.R

/**
 * Posts one high-priority notification per risky app found by the live security check. Tapping it
 * opens the Apps tab; the action button opens the app's system info page (uninstall, permissions).
 */
class SecurityAlertNotifier(private val context: Context) {

    fun notify(alerts: List<Finding>) {
        if (alerts.isEmpty() || !canNotify()) return
        ensureChannel()
        alerts.groupBy { it.packageName }.forEach { (pkg, findings) ->
            val worst = findings.maxBy { it.severity }
            val ui = worst.type.presentation
            val title = context.getString(R.string.monitoring_security_title, worst.appLabel, context.getString(ui.title))
            val body = context.getString(ui.description)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_report)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setColor(COLOR_ALERT)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openAppsTabIntent())
                .addAction(0, context.getString(R.string.monitoring_security_action_app_info), appInfoIntent(pkg))
                .build()
            if (hasPermission()) {
                @Suppress("MissingPermission") // Checked by hasPermission() right above.
                NotificationManagerCompat.from(context).notify(TAG, pkg.hashCode(), notification)
            }
        }
    }

    private fun canNotify(): Boolean = hasPermission() && NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.monitoring_security_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.monitoring_security_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openAppsTabIntent(): PendingIntent {
        val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent())
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MonitoringContract.EXTRA_OPEN_TAB, MonitoringContract.TAB_APPS)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APPS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun appInfoIntent(pkg: String): PendingIntent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, pkg.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val CHANNEL_ID = "security_alerts"
        const val TAG = "security_alert"
        const val REQUEST_OPEN_APPS = 43
        const val COLOR_ALERT = 0xFFE5484D.toInt()
    }
}
