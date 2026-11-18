package org.neteinstein.perfsentient.core.domain.analysis

import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
import org.neteinstein.perfsentient.core.model.DeviceContext
import org.neteinstein.perfsentient.core.model.NetworkUsage
import org.neteinstein.perfsentient.core.model.UsageEventRecord
import org.neteinstein.perfsentient.core.model.VitalSample
import java.time.ZoneId

/** Everything a detector may look at for the analysed window. */
data class AnalysisInput(
    val windowStartMs: Long,
    val windowEndMs: Long,
    val zone: ZoneId,
    val apps: Map<String, AppProfile>,
    val usage: Map<String, AppUsage>,
    val events: List<UsageEventRecord>,
    val network: Map<String, NetworkUsage>,
    val notifications: Map<String, Int>,
    val vitals: List<VitalSample>,
    val context: DeviceContext,
) {
    fun foregroundMs(pkg: String): Long = usage[pkg]?.foregroundTimeMs ?: 0L

    fun foregroundServiceMs(pkg: String): Long = usage[pkg]?.foregroundServiceTimeMs ?: 0L

    fun backgroundBytes(pkg: String): Long = network[pkg]?.backgroundBytes ?: 0L

    /** Apps that may legitimately be blamed: installed, not system, not ourselves. */
    fun candidate(pkg: String): AppProfile? = apps[pkg]?.takeIf { !it.isSystemApp && pkg != context.selfPackage }

    val candidates: List<AppProfile>
        get() = apps.values.filter { !it.isSystemApp && it.packageName != context.selfPackage }
}
