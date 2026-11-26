package org.neteinstein.perfsentient.core.data.source

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process
import org.neteinstein.perfsentient.core.model.NetworkUsage

/**
 * Per-app traffic split by foreground / background state, for Wi-Fi and mobile, with the
 * background mobile part kept separately (mobile data is the one that costs money).
 * Requires Usage access. Mobile is queried with a null subscriber id (allowed since API 29).
 */
class NetworkStatsDataSource(
    private val networkStatsManager: NetworkStatsManager,
    private val packageManager: PackageManager,
) {
    fun usage(startMs: Long, endMs: Long): List<NetworkUsage> {
        val foreground = mutableMapOf<Int, Long>()
        val background = mutableMapOf<Int, Long>()
        val mobileBackground = mutableMapOf<Int, Long>()

        @Suppress("DEPRECATION")
        val networkTypes = listOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)
        networkTypes.forEach { type ->
            val stats = runCatching { networkStatsManager.querySummary(type, null, startMs, endMs) }.getOrNull()
                ?: return@forEach
            stats.use { summary ->
                val bucket = NetworkStats.Bucket()
                while (summary.hasNextBucket()) {
                    summary.getNextBucket(bucket)
                    if (bucket.uid < Process.FIRST_APPLICATION_UID) continue
                    val bytes = bucket.rxBytes + bucket.txBytes
                    if (bucket.state == NetworkStats.Bucket.STATE_FOREGROUND) {
                        foreground.merge(bucket.uid, bytes, Long::plus)
                    } else {
                        background.merge(bucket.uid, bytes, Long::plus)
                        @Suppress("DEPRECATION")
                        if (type == ConnectivityManager.TYPE_MOBILE) mobileBackground.merge(bucket.uid, bytes, Long::plus)
                    }
                }
            }
        }
        return (foreground.keys + background.keys).mapNotNull { uid ->
            val pkg = packageManager.getPackagesForUid(uid)?.firstOrNull() ?: return@mapNotNull null
            NetworkUsage(pkg, foreground[uid] ?: 0L, background[uid] ?: 0L, mobileBackground[uid] ?: 0L)
        }
    }
}
