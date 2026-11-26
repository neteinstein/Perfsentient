package org.neteinstein.perfsentient.core.data.source

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import org.neteinstein.perfsentient.core.data.partner.PartnerCertificates
import org.neteinstein.perfsentient.core.data.partner.ScamSignalsContract
import org.neteinstein.perfsentient.core.model.ScamMessageKind
import org.neteinstein.perfsentient.core.model.ScamMessageSignal
import org.neteinstein.perfsentient.core.model.SmsPartnerStatus

/**
 * Reads the scam warnings SMS Toolbox shares (see [ScamSignalsContract]). Read-only and one-way:
 * nothing is ever sent to SMS Toolbox. Every call degrades to "not installed" / "nothing" when SMS
 * Toolbox is missing, not signed by its developer, not connected, or lacks the permission.
 */
class SmsToolboxDataSource(
    private val context: Context,
    /** Whether [packageName] is signed by SMS Toolbox's developer; replaced in tests. */
    private val isGenuine: (packageName: String) -> Boolean = { pkg -> context.isSignedByPartner(pkg) },
) {
    private val debuggable: Boolean get() = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    /** The SMS Toolbox package on the phone; its debug build only counts for a debug Perfsentient. */
    fun installedPackage(): String? = buildList {
        add(ScamSignalsContract.PROVIDER_PACKAGE)
        if (debuggable) add(ScamSignalsContract.DEBUG_PROVIDER_PACKAGE)
    }.firstOrNull { pkg -> runCatching { packageInfo(pkg) }.isSuccess }

    fun status(): SmsPartnerStatus {
        val pkg = installedPackage() ?: return SmsPartnerStatus.NOT_INSTALLED
        if (!isGenuine(pkg)) return SmsPartnerStatus.NOT_INSTALLED.copy(packageName = pkg)
        val unconnected =
            SmsPartnerStatus(pkg, verified = true, connected = false, isDefaultSmsApp = false, checkedToday = 0, suspiciousToday = 0)
        return query(uri(pkg, ScamSignalsContract.PATH_STATUS)) { cursor ->
            if (!cursor.moveToFirst()) return@query null
            unconnected.copy(
                connected = cursor.int(ScamSignalsContract.Status.SHARING_ENABLED) == 1,
                isDefaultSmsApp = cursor.int(ScamSignalsContract.Status.IS_DEFAULT_SMS_APP) == 1,
                checkedToday = cursor.int(ScamSignalsContract.Status.ASSESSED_TODAY) ?: 0,
                suspiciousToday = cursor.int(ScamSignalsContract.Status.SUSPICIOUS_TODAY) ?: 0,
            )
        } ?: unconnected
    }

    fun signals(startMs: Long, endMs: Long): List<ScamMessageSignal> {
        val pkg = installedPackage()?.takeIf(isGenuine) ?: return emptyList()
        val uri = uri(pkg, ScamSignalsContract.PATH_SIGNALS).buildUpon()
            .appendQueryParameter(ScamSignalsContract.PARAM_SINCE, (startMs - 1).toString())
            .build()
        return query(uri) { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val at = cursor.long(ScamSignalsContract.Signals.RECEIVED_AT_MS) ?: continue
                    if (at !in startMs..endMs) continue
                    add(
                        ScamMessageSignal(
                            receivedAtMs = at,
                            kinds = parseKinds(cursor.string(ScamSignalsContract.Signals.SIGNALS)),
                            hasLink = cursor.int(ScamSignalsContract.Signals.HAS_LINK) == 1,
                            threadId = cursor.long(ScamSignalsContract.Signals.THREAD_ID)?.takeIf { it > 0 },
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    private fun uri(pkg: String, path: String): Uri = Uri.Builder()
        .scheme("content")
        .authority(ScamSignalsContract.authority(pkg))
        .appendPath(path)
        .build()

    /** Null when the provider is missing or refuses (SecurityException without the permission). */
    private fun <T> query(uri: Uri, read: (Cursor) -> T?): T? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use(read)
    }.getOrNull()

    private fun packageInfo(pkg: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(pkg, 0)
    }

    private fun Cursor.column(name: String): Int? = getColumnIndex(name).takeIf { it >= 0 && !isNull(it) }

    private fun Cursor.int(name: String): Int? = column(name)?.let(::getInt)

    private fun Cursor.long(name: String): Long? = column(name)?.let(::getLong)

    private fun Cursor.string(name: String): String? = column(name)?.let(::getString)
}

/** "SHORTENED_LINK,DELIVERY_OR_FEE" → kinds; names a newer SMS Toolbox adds are dropped. */
internal fun parseKinds(value: String?): Set<ScamMessageKind> = value.orEmpty()
    .split(',')
    .mapNotNull { name -> ScamMessageKind.entries.firstOrNull { it.name == name.trim() } }
    .toSet()

private fun Context.isSignedByPartner(pkg: String): Boolean {
    val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val digests = PartnerCertificates.RELEASE + if (debuggable) PartnerCertificates.DEBUG else emptySet()
    return digests.any { digest ->
        runCatching { packageManager.hasSigningCertificate(pkg, digest.hexToByteArray(), PackageManager.CERT_INPUT_SHA256) }
            .getOrDefault(false)
    }
}
