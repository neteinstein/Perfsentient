package org.neteinstein.perfsentient.core.data.partner

/**
 * SHA-256 digests (lowercase hex, no colons) of the certificates that sign genuine SMS Toolbox
 * builds. Perfsentient reads scam warnings only from an SMS Toolbox signed with one of these, and
 * reports a copy signed by anyone else as an impersonation.
 */
internal object PartnerCertificates {
    /**
     * SMS Toolbox's Play app-signing certificate, its upload certificate and the key that signs its
     * GitHub release APKs (Play Console → App integrity, or `apksigner verify -print-certs`).
     * TODO before release: replace the placeholder, which matches no certificate.
     */
    val RELEASE: Set<String> = setOf(
        "0000000000000000000000000000000000000000000000000000000000000000",
    )

    /** The local Android SDK debug key that signs both apps' debug builds. Only used when Perfsentient itself is debuggable. */
    val DEBUG: Set<String> = setOf(
        "c0410202a1a371df5aa54cf9acb9d71ebdfc73dc026449aeee8c39ae689fbd93",
    )
}
