package org.neteinstein.perfsentient.core.data.mapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.neteinstein.perfsentient.core.model.TrustedApp

@Serializable
internal data class TrustedAppDto(val packageName: String, val label: String)

private val trustedAppsSerializer = ListSerializer(TrustedAppDto.serializer())

internal fun List<TrustedApp>.toTrustedAppsJson(): String =
    reportJson.encodeToString(trustedAppsSerializer, map { TrustedAppDto(it.packageName, it.label) })

/** Corrupt or missing data reads as an empty allowlist instead of crashing. */
internal fun String?.toTrustedApps(): List<TrustedApp> {
    if (isNullOrBlank()) return emptyList()
    return runCatching { reportJson.decodeFromString(trustedAppsSerializer, this) }
        .getOrDefault(emptyList())
        .filter { it.packageName.isNotBlank() }
        .map { TrustedApp(it.packageName, it.label) }
}
