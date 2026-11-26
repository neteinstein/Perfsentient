package org.neteinstein.perfsentient.core.data.source

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import org.neteinstein.perfsentient.core.model.PackageSource
import org.neteinstein.perfsentient.core.model.SensitivePermission

private val SENSITIVE_PERMISSIONS: Map<String, SensitivePermission> = mapOf(
    Manifest.permission.READ_SMS to SensitivePermission.READ_SMS,
    Manifest.permission.RECEIVE_SMS to SensitivePermission.RECEIVE_SMS,
    Manifest.permission.SEND_SMS to SensitivePermission.SEND_SMS,
    Manifest.permission.READ_CALL_LOG to SensitivePermission.READ_CALL_LOG,
    Manifest.permission.CALL_PHONE to SensitivePermission.CALL_PHONE,
    Manifest.permission.ANSWER_PHONE_CALLS to SensitivePermission.ANSWER_PHONE_CALLS,
    Manifest.permission.READ_CONTACTS to SensitivePermission.READ_CONTACTS,
    Manifest.permission.RECORD_AUDIO to SensitivePermission.RECORD_AUDIO,
    Manifest.permission.CAMERA to SensitivePermission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION to SensitivePermission.FINE_LOCATION,
    Manifest.permission.USE_FULL_SCREEN_INTENT to SensitivePermission.USE_FULL_SCREEN_INTENT,
)

/** The sensitive permissions among [requested] whose matching [flags] entry says "granted". */
internal fun grantedSensitivePermissions(requested: Array<String>?, flags: IntArray?): Set<SensitivePermission> {
    if (requested == null || flags == null) return emptySet()
    return requested.indices
        .filter { it < flags.size && flags[it] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }
        .mapNotNull { SENSITIVE_PERMISSIONS[requested[it]] }
        .toSet()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun packageSourceOf(value: Int): PackageSource = when (value) {
    PackageInstaller.PACKAGE_SOURCE_STORE -> PackageSource.STORE
    PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE -> PackageSource.DOWNLOADED_FILE
    PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE -> PackageSource.LOCAL_FILE
    PackageInstaller.PACKAGE_SOURCE_OTHER -> PackageSource.OTHER
    else -> PackageSource.UNKNOWN
}

/** Build.VERSION.SECURITY_PATCH is "YYYY-MM-DD"; anything else (blank, custom ROM text) is unknown. */
internal fun parseSecurityPatch(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
