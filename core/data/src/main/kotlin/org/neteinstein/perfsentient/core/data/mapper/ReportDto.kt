package org.neteinstein.perfsentient.core.data.mapper

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.neteinstein.perfsentient.core.data.local.entity.ReportEntity
import org.neteinstein.perfsentient.core.data.local.entity.VitalSampleEntity
import org.neteinstein.perfsentient.core.model.AppBatteryUsage
import org.neteinstein.perfsentient.core.model.BatteryBreakdown
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.DailyReport
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.DeviceMetrics
import org.neteinstein.perfsentient.core.model.Evidence
import org.neteinstein.perfsentient.core.model.Finding
import org.neteinstein.perfsentient.core.model.ScamMessageKind
import org.neteinstein.perfsentient.core.model.ScamMessageSummary
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.Verdict
import org.neteinstein.perfsentient.core.model.VitalSample

@Serializable
internal data class ReportDto(
    val date: String,
    val generatedAt: Long,
    val score: Int,
    val verdict: String,
    val metrics: MetricsDto,
    val deviceIssues: List<DeviceIssueDto>,
    val findings: List<FindingDto>,
    val appsAnalysed: Int,
    /** Added with the battery estimate; older reports decode with none. */
    val battery: BatteryDto? = null,
    /** Added with the SMS Toolbox partnership; null when it was not connected. */
    val scamMessages: ScamMessagesDto? = null,
)

@Serializable
internal data class ScamMessagesDto(val total: Int, val withLink: Int, val byKind: Map<String, Int> = emptyMap())

@Serializable
internal data class BatteryDto(
    val apps: List<AppBatteryDto>,
    val screenAndSystemPct: Float,
    val standbyPct: Float,
    val measuredDrainPct: Float,
    val measuredHours: Float,
)

@Serializable
internal data class AppBatteryDto(
    val packageName: String,
    val appLabel: String,
    val drainPct: Float,
    val backgroundDrainPct: Float,
    val foregroundMs: Long,
    val backgroundMs: Long,
)

@Serializable
internal data class MetricsDto(
    val sampleCount: Int,
    val drainPerHour: Float,
    val screenOffDrainPerHour: Float,
    val maxTemperatureC: Float,
    val worstThermalStatus: Int,
    val averageMemoryUsedPercent: Int,
    val lowMemorySamples: Int,
    val totalBackgroundBytes: Long,
    val totalForegroundTimeMs: Long,
)

@Serializable
internal data class DeviceIssueDto(val type: String, val severity: String, val value: Long)

@Serializable
internal data class FindingDto(
    val packageName: String,
    val appLabel: String,
    val type: String,
    val severity: String,
    val value: Long,
    /** Added with evidence; older reports decode with none. */
    val evidence: List<EvidenceDto> = emptyList(),
)

@Serializable
internal data class EvidenceDto(val atMs: Long, val relatedPackage: String? = null, val relatedLabel: String? = null)

internal val reportJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Unknown enum names (e.g. from a newer app version) are dropped instead of crashing. */
private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = enumValues<T>().firstOrNull { it.name == name }

internal fun DailyReport.toEntity(): ReportEntity = ReportEntity(
    day = date.toString(),
    generatedAt = generatedAt,
    score = score,
    payload = reportJson.encodeToString(ReportDto.serializer(), toDto()),
)

internal fun ReportEntity.toDomain(): DailyReport? = runCatching {
    reportJson.decodeFromString(ReportDto.serializer(), payload).toDomain()
}.getOrNull()

internal fun DailyReport.toDto() = ReportDto(
    date = date.toString(),
    generatedAt = generatedAt,
    score = score,
    verdict = verdict.name,
    metrics = with(metrics) {
        MetricsDto(
            sampleCount, drainPerHour, screenOffDrainPerHour, maxTemperatureC, worstThermalStatus,
            averageMemoryUsedPercent, lowMemorySamples, totalBackgroundBytes, totalForegroundTimeMs,
        )
    },
    deviceIssues = deviceIssues.map { DeviceIssueDto(it.type.name, it.severity.name, it.value) },
    findings = findings.map { finding ->
        FindingDto(
            finding.packageName,
            finding.appLabel,
            finding.type.name,
            finding.severity.name,
            finding.value,
            finding.evidence.map { EvidenceDto(it.atMs, it.relatedPackage, it.relatedLabel) },
        )
    },
    appsAnalysed = appsAnalysed,
    battery = battery.takeIf { it != BatteryBreakdown.EMPTY }?.let { breakdown ->
        BatteryDto(
            apps = breakdown.apps.map {
                AppBatteryDto(it.packageName, it.appLabel, it.estimatedDrainPct, it.backgroundDrainPct, it.foregroundMs, it.backgroundMs)
            },
            screenAndSystemPct = breakdown.screenAndSystemPct,
            standbyPct = breakdown.standbyPct,
            measuredDrainPct = breakdown.measuredDrainPct,
            measuredHours = breakdown.measuredHours,
        )
    },
    scamMessages = scamMessages?.let { summary ->
        ScamMessagesDto(summary.total, summary.withLink, summary.byKind.mapKeys { it.key.name })
    },
)

internal fun ReportDto.toDomain() = DailyReport(
    date = LocalDate.parse(date),
    generatedAt = generatedAt,
    score = score,
    verdict = enumOrNull<Verdict>(verdict) ?: Verdict.FAIR,
    metrics = with(metrics) {
        DeviceMetrics(
            sampleCount, drainPerHour, screenOffDrainPerHour, maxTemperatureC, worstThermalStatus,
            averageMemoryUsedPercent, lowMemorySamples, totalBackgroundBytes, totalForegroundTimeMs,
        )
    },
    deviceIssues = deviceIssues.mapNotNull { dto ->
        val type = enumOrNull<DeviceIssueType>(dto.type) ?: return@mapNotNull null
        val severity = enumOrNull<Severity>(dto.severity) ?: return@mapNotNull null
        DeviceIssue(type, severity, dto.value)
    },
    findings = findings.mapNotNull { dto ->
        val type = enumOrNull<BehaviorType>(dto.type) ?: return@mapNotNull null
        val severity = enumOrNull<Severity>(dto.severity) ?: return@mapNotNull null
        Finding(
            dto.packageName,
            dto.appLabel,
            type,
            severity,
            dto.value,
            dto.evidence.take(Finding.MAX_EVIDENCE).map { Evidence(it.atMs, it.relatedPackage, it.relatedLabel) },
        )
    },
    appsAnalysed = appsAnalysed,
    battery = battery?.let { dto ->
        BatteryBreakdown(
            apps = dto.apps.map {
                AppBatteryUsage(it.packageName, it.appLabel, it.drainPct, it.backgroundDrainPct, it.foregroundMs, it.backgroundMs)
            },
            screenAndSystemPct = dto.screenAndSystemPct,
            standbyPct = dto.standbyPct,
            measuredDrainPct = dto.measuredDrainPct,
            measuredHours = dto.measuredHours,
        )
    } ?: BatteryBreakdown.EMPTY,
    scamMessages = scamMessages?.let { dto ->
        ScamMessageSummary(
            total = dto.total,
            withLink = dto.withLink,
            byKind = dto.byKind.mapNotNull { (name, count) -> enumOrNull<ScamMessageKind>(name)?.let { it to count } }.toMap(),
        )
    },
)

internal fun VitalSample.toEntity() = VitalSampleEntity(
    timestamp, batteryPercent, isCharging, batteryTemperatureC, isScreenOn, thermalStatus,
    availableMemoryBytes, totalMemoryBytes, isLowMemory, chargeCounterUah,
)

internal fun VitalSampleEntity.toDomain() = VitalSample(
    timestamp, batteryPercent, isCharging, batteryTemperatureC, isScreenOn, thermalStatus,
    availableMemoryBytes, totalMemoryBytes, isLowMemory, chargeCounterUah,
)
