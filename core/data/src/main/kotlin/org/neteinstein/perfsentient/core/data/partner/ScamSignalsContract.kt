package org.neteinstein.perfsentient.core.data.partner

/**
 * The read-only contract between SMS Toolbox and Perfsentient (its on-device phone-health
 * monitor). SMS Toolbox *publishes* the time and kind of the scam warnings it shows; Perfsentient
 * reads them to catch "scam text with a link → app installed minutes later", which neither app can
 * see alone. Data flows one way only, and only after the user turns sharing on.
 *
 * Never add message text, phone numbers, contact names or links to this contract.
 *
 * This file is copied verbatim (only the package line differs) into Perfsentient at
 * core/data/src/main/kotlin/org/neteinstein/perfsentient/core/data/partner/ScamSignalsContract.kt.
 * Change both together, and bump [CONTRACT_VERSION] for anything a reader must know about.
 */
object ScamSignalsContract {
    const val CONTRACT_VERSION = 1

    const val PROVIDER_PACKAGE = "com.neteinstein.smsredirect"

    /** SMS Toolbox's debug build (applicationIdSuffix ".test"), so debug builds of both apps can talk. */
    const val DEBUG_PROVIDER_PACKAGE = "$PROVIDER_PACKAGE.test"
    const val READER_PACKAGE = "org.neteinstein.perfsentient"

    /** Perfsentient's debug build (applicationIdSuffix ".debug"). */
    const val DEBUG_READER_PACKAGE = "$READER_PACKAGE.debug"

    /** The provider authority of the SMS Toolbox installed as [providerPackage]. */
    fun authority(providerPackage: String): String = "$providerPackage.scamsignals"

    /**
     * signature|knownSigner, defined by [providerPackage]: only apps signed with a certificate SMS
     * Toolbox lists can hold it.
     */
    fun permission(providerPackage: String): String = "$providerPackage.permission.READ_SCAM_SIGNALS"

    /** One row: [Status] columns. Always available, so a reader can tell "not connected" from "nothing to report". */
    const val PATH_STATUS = "status"

    /** One row per suspicious message received after `?since=<epoch ms>`: [Signals] columns. Empty while sharing is off. */
    const val PATH_SIGNALS = "signals"
    const val PARAM_SINCE = "since"

    /** Opens SMS Toolbox's consent dialog. */
    const val CONNECT_DEEP_LINK = "smsredirect://app/connect?partner=perfsentient"
    const val CONNECT_PATH = "/connect"
    const val PARTNER_PARAM = "partner"
    const val PARTNER_PERFSENTIENT = "perfsentient"

    /** Opens a conversation: append the thread id. */
    const val CONVERSATION_DEEP_LINK_PREFIX = "smsredirect://app/conversation?threadId="

    object Status {
        const val CONTRACT_VERSION = "contract_version"
        const val SHARING_ENABLED = "sharing_enabled"
        const val IS_DEFAULT_SMS_APP = "is_default_sms_app"
        const val ASSESSED_TODAY = "assessed_today"
        const val SUSPICIOUS_TODAY = "suspicious_today"

        val COLUMNS = arrayOf(CONTRACT_VERSION, SHARING_ENABLED, IS_DEFAULT_SMS_APP, ASSESSED_TODAY, SUSPICIOUS_TODAY)
    }

    object Signals {
        const val RECEIVED_AT_MS = "received_at_ms"

        /** Comma-separated SpamSignal names, e.g. "SHORTENED_LINK,DELIVERY_OR_FEE". Readers ignore names they do not know. */
        const val SIGNALS = "signals"

        /** 1 when the message contained a link. */
        const val HAS_LINK = "has_link"

        /** The conversation's thread id (for [CONVERSATION_DEEP_LINK_PREFIX]), or null. */
        const val THREAD_ID = "thread_id"

        val COLUMNS = arrayOf(RECEIVED_AT_MS, SIGNALS, HAS_LINK, THREAD_ID)
    }
}
