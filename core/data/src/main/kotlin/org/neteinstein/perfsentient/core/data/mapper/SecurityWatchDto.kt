package org.neteinstein.perfsentient.core.data.mapper

import kotlinx.serialization.Serializable
import org.neteinstein.perfsentient.core.model.CapabilityHolders
import org.neteinstein.perfsentient.core.model.PackageChangeCursor
import org.neteinstein.perfsentient.core.model.SecurityWatchState

@Serializable
internal data class SecurityWatchDto(
    val bootCount: Int,
    val sequence: Int,
    val accessibility: Set<String> = emptySet(),
    val deviceAdmins: Set<String> = emptySet(),
    val notificationListeners: Set<String> = emptySet(),
    val defaultSms: String? = null,
    val alerted: List<String> = emptyList(),
)

internal fun SecurityWatchState.toJson(): String = reportJson.encodeToString(
    SecurityWatchDto.serializer(),
    SecurityWatchDto(
        bootCount = cursor.bootCount,
        sequence = cursor.sequence,
        accessibility = holders.accessibility,
        deviceAdmins = holders.deviceAdmins,
        notificationListeners = holders.notificationListeners,
        defaultSms = holders.defaultSms,
        alerted = alerted,
    ),
)

/** Corrupt data reads as "no baseline": the next check records a fresh one instead of crashing. */
internal fun String?.toSecurityWatchState(): SecurityWatchState? {
    if (isNullOrBlank()) return null
    val dto = runCatching { reportJson.decodeFromString(SecurityWatchDto.serializer(), this) }.getOrNull() ?: return null
    return SecurityWatchState(
        cursor = PackageChangeCursor(dto.bootCount, dto.sequence),
        holders = CapabilityHolders(dto.accessibility, dto.deviceAdmins, dto.notificationListeners, dto.defaultSms),
        alerted = dto.alerted,
    )
}
