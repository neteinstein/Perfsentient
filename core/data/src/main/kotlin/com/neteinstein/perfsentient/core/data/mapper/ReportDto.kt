package com.neteinstein.perfsentient.core.data.mapper

import com.neteinstein.perfsentient.core.data.local.entity.ReportEntity
import com.neteinstein.perfsentient.core.data.local.entity.VitalSampleEntity
import com.neteinstein.perfsentient.core.model.BehaviorType
import com.neteinstein.perfsentient.core.model.DailyReport
import com.neteinstein.perfsentient.core.model.DeviceIssue
import com.neteinstein.perfsentient.core.model.DeviceIssueType
import com.neteinstein.perfsentient.core.model.DeviceMetrics
import com.neteinstein.perfsentient.core.model.Finding
import com.neteinstein.perfsentient.core.model.Severity
import com.neteinstein.perfsentient.core.model.Verdict
import com.neteinstein.perfsentient.core.model.VitalSample
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
)

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
    findings = findings.map { FindingDto(it.packageName, it.appLabel, it.type.name, it.severity.name, it.value) },
    appsAnalysed = appsAnalysed,
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
        Finding(dto.packageName, dto.appLabel, type, severity, dto.value)
    },
    appsAnalysed = appsAnalysed,
)

internal fun VitalSample.toEntity() = VitalSampleEntity(
    timestamp, batteryPercent, isCharging, batteryTemperatureC, isScreenOn, thermalStatus,
    availableMemoryBytes, totalMemoryBytes, isLowMemory,
)

internal fun VitalSampleEntity.toDomain() = VitalSample(
    timestamp, batteryPercent, isCharging, batteryTemperatureC, isScreenOn, thermalStatus,
    availableMemoryBytes, totalMemoryBytes, isLowMemory,
)
