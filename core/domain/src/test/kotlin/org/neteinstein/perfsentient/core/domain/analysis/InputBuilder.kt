package org.neteinstein.perfsentient.core.domain.analysis

import java.time.ZoneOffset
import org.neteinstein.perfsentient.core.model.AppIssueRecord
import org.neteinstein.perfsentient.core.model.AppProfile
import org.neteinstein.perfsentient.core.model.AppUsage
import org.neteinstein.perfsentient.core.model.NetworkUsage
import org.neteinstein.perfsentient.core.model.UsageEventRecord
import org.neteinstein.perfsentient.core.model.VitalSample
import org.neteinstein.perfsentient.core.testing.TestData

internal fun input(
    apps: List<AppProfile> = emptyList(),
    usage: List<AppUsage> = emptyList(),
    events: List<UsageEventRecord> = emptyList(),
    network: List<NetworkUsage> = emptyList(),
    notifications: Map<String, Int> = emptyMap(),
    vitals: List<VitalSample> = emptyList(),
    appIssues: List<AppIssueRecord> = emptyList(),
) = AnalysisInput(
    windowStartMs = 0,
    windowEndMs = Long.MAX_VALUE,
    zone = ZoneOffset.UTC,
    apps = (apps + TestData.app(TestData.LAUNCHER, system = true) + TestData.app(TestData.DIALER, system = true)).associateBy {
        it.packageName
    },
    usage = usage.associateBy { it.packageName },
    events = events,
    network = network.associateBy { it.packageName },
    notifications = notifications,
    vitals = vitals,
    context = TestData.deviceContext(),
    appIssues = appIssues,
)

internal const val SEC = 1_000L
internal const val MIN = 60 * SEC
internal const val HOUR = 60 * MIN
internal const val MB = 1024L * 1024L
