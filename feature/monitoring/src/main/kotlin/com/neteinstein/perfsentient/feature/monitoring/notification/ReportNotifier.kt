package com.neteinstein.perfsentient.feature.monitoring.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.neteinstein.perfsentient.core.model.DailyReport
import com.neteinstein.perfsentient.core.model.Verdict
import com.neteinstein.perfsentient.feature.monitoring.MonitoringContract
import com.neteinstein.perfsentient.feature.monitoring.R

/** Posts the end-of-day notification. Tapping it opens the Report tab. */
class ReportNotifier(private val context: Context) {

    fun notify(report: DailyReport) {
        if (!canNotify()) return
        ensureChannel()
        val offenders = report.offenders.size
        val title = context.getString(
            when (report.verdict) {
                Verdict.GOOD -> R.string.monitoring_notification_title_good
                Verdict.FAIR -> R.string.monitoring_notification_title_fair
                Verdict.POOR -> R.string.monitoring_notification_title_poor
            },
        )
        val body = if (offenders == 0) {
            context.getString(R.string.monitoring_notification_body_clean, report.score)
        } else {
            context.resources.getQuantityString(R.plurals.monitoring_notification_body, offenders, report.score, offenders)
        }
        val color = when (report.verdict) {
            Verdict.GOOD -> COLOR_GOOD
            Verdict.FAIR -> COLOR_FAIR
            Verdict.POOR -> COLOR_POOR
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_report)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(color)
            .setColorized(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openReportIntent())
            .build()

        if (hasPermission()) {
            @Suppress("MissingPermission") // Checked by hasPermission() right above.
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun canNotify(): Boolean = hasPermission() && NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.monitoring_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.monitoring_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openReportIntent(): PendingIntent {
        val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent())
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MonitoringContract.EXTRA_OPEN_TAB, MonitoringContract.TAB_REPORT)
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val CHANNEL_ID = "daily_report"
        const val NOTIFICATION_ID = 2_100
        const val REQUEST_CODE = 42
        const val COLOR_GOOD = 0xFF1E9E62.toInt()
        const val COLOR_FAIR = 0xFFD48A00.toInt()
        const val COLOR_POOR = 0xFFE5484D.toInt()
    }
}
