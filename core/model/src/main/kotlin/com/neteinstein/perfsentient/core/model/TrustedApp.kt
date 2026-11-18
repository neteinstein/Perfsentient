package com.neteinstein.perfsentient.core.model

/**
 * An app the user marked as trusted (the allowlist). Its findings are left out of reports and
 * do not lower the health score. The [label] is remembered so the app can be listed even after
 * it was uninstalled.
 */
data class TrustedApp(
    val packageName: String,
    val label: String,
)
