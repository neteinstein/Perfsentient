package org.neteinstein.perfsentient.core.data.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.DropBoxManager
import org.neteinstein.perfsentient.core.model.AppIssueRecord
import org.neteinstein.perfsentient.core.model.AppIssueType

/**
 * Reads app ANRs and crashes from the system error log ([DropBoxManager]).
 *
 * Needs `READ_LOGS` (Android 14 and older) or `READ_DROPBOX_DATA` (Android 15+), development
 * permissions granted with `adb shell pm grant`, on top of Usage access. Without them, or if the
 * system refuses, it returns an empty list. Only the package name and the time of each entry are
 * kept: stack traces are never read beyond the header.
 */
class SystemErrorLogDataSource(private val context: Context) {
    private val dropBox: DropBoxManager? = context.getSystemService(DropBoxManager::class.java)

    /** Which of the two the platform enforces depends on its version and flags, so either counts. */
    fun hasAccess(): Boolean = LOG_PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    fun issues(startMs: Long, endMs: Long): List<AppIssueRecord> {
        val manager = dropBox ?: return emptyList()
        if (!hasAccess()) return emptyList()
        return TAGS.flatMap { (tag, type) ->
            runCatching { read(manager, tag, type, startMs, endMs) }.getOrDefault(emptyList())
        }
    }

    private fun read(manager: DropBoxManager, tag: String, type: AppIssueType, startMs: Long, endMs: Long): List<AppIssueRecord> =
        buildList {
            // getNextEntry returns the first entry strictly after the given time.
            var cursor = startMs - 1
            while (size < MAX_ENTRIES_PER_TAG) {
                val entry = manager.getNextEntry(tag, cursor) ?: break
                val time = entry.timeMillis
                val header = entry.use { it.getText(HEADER_BYTES) }
                if (time > endMs) break
                cursor = time
                packageFromHeader(header)?.let { add(AppIssueRecord(it, type, time)) }
            }
        }

    internal companion object {
        // A flagged API constant on newer SDKs; the literal keeps it usable at every API level.
        private const val READ_DROPBOX_DATA = "android.permission.READ_DROPBOX_DATA"
        private val LOG_PERMISSIONS = listOf(Manifest.permission.READ_LOGS, READ_DROPBOX_DATA)
        private const val HEADER_BYTES = 1024
        private const val MAX_ENTRIES_PER_TAG = 500

        private val TAGS = listOf(
            "data_app_anr" to AppIssueType.ANR,
            "data_app_crash" to AppIssueType.CRASH,
            "data_app_native_crash" to AppIssueType.CRASH,
        )

        /**
         * The system writes a header such as `Process: com.example:sync` / `Package: com.example v12 (1.2)`.
         * Prefers the `Package:` line and falls back to the process name without its `:suffix`.
         */
        fun packageFromHeader(header: String?): String? {
            if (header == null) return null
            val lines = header.lineSequence().take(HEADER_LINES).toList()
            fun field(name: String) = lines.firstOrNull { it.startsWith("$name: ") }
                ?.removePrefix("$name: ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val pkg = field("Package")?.substringBefore(' ') ?: field("Process")?.substringBefore(':')
            return pkg?.takeIf { PACKAGE_NAME.matches(it) }
        }

        private const val HEADER_LINES = 20
        private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
    }
}
