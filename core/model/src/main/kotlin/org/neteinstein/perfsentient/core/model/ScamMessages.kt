package org.neteinstein.perfsentient.core.model

/**
 * Why SMS Toolbox flagged a text as a possible scam. Mirrors SMS Toolbox's SpamSignal; names a
 * newer SMS Toolbox adds are dropped when read.
 */
enum class ScamMessageKind {
    SHORTENED_LINK,
    IP_ADDRESS_LINK,
    DECEPTIVE_LINK,
    RISKY_DOMAIN_LINK,
    URGENCY,
    ACCOUNT_OR_CREDENTIALS,
    MONEY_OR_PRIZE,
    DELIVERY_OR_FEE,
}

/** A text SMS Toolbox flagged as a possible scam. Only metadata: never its content or sender. */
data class ScamMessageSignal(
    val receivedAtMs: Long,
    val kinds: Set<ScamMessageKind>,
    val hasLink: Boolean,
    /** SMS Toolbox's conversation id, to open it from a finding. */
    val threadId: Long?,
)

/** Whether SMS Toolbox can share scam warnings with Perfsentient, and how much it checked today. */
data class SmsPartnerStatus(
    /** The SMS Toolbox package found on the phone, or null. */
    val packageName: String?,
    /** Signed by SMS Toolbox's developer. An unverified copy is never read. */
    val verified: Boolean,
    /** The user turned on sharing in SMS Toolbox. */
    val connected: Boolean,
    /** SMS Toolbox only checks texts while it is the default SMS app. */
    val isDefaultSmsApp: Boolean,
    val checkedToday: Int,
    val suspiciousToday: Int,
) {
    val installed: Boolean get() = packageName != null

    val state: SmsPartnerState
        get() = when {
            !installed -> SmsPartnerState.NOT_INSTALLED
            !verified -> SmsPartnerState.UNVERIFIED
            !connected -> SmsPartnerState.NOT_CONNECTED
            !isDefaultSmsApp -> SmsPartnerState.CONNECTED_NOT_DEFAULT
            else -> SmsPartnerState.CONNECTED
        }

    companion object {
        val NOT_INSTALLED =
            SmsPartnerStatus(null, verified = false, connected = false, isDefaultSmsApp = false, checkedToday = 0, suspiciousToday = 0)
    }
}

enum class SmsPartnerState { NOT_INSTALLED, UNVERIFIED, NOT_CONNECTED, CONNECTED_NOT_DEFAULT, CONNECTED }

/** Links into SMS Toolbox. Kept equal to ScamSignalsContract in core:data (a test checks). */
object SmsPartnerLinks {
    const val PACKAGE = "com.neteinstein.smsredirect"
    const val CONNECT = "smsredirect://app/connect?partner=perfsentient"
    const val CONVERSATION_PREFIX = "smsredirect://app/conversation?threadId="
    const val PLAY_STORE = "market://details?id=$PACKAGE"
    const val RELEASES = "https://github.com/neteinstein/SMS-Redirect/releases/latest"
}

/** Scam texts SMS Toolbox flagged in a report's window. Information only: it does not lower the score. */
data class ScamMessageSummary(
    val total: Int,
    val withLink: Int,
    val byKind: Map<ScamMessageKind, Int>,
) {
    companion object {
        fun of(signals: List<ScamMessageSignal>): ScamMessageSummary? = if (signals.isEmpty()) {
            null
        } else {
            ScamMessageSummary(
                total = signals.size,
                withLink = signals.count { it.hasLink },
                byKind = signals.flatMap { it.kinds }.groupingBy { it }.eachCount(),
            )
        }
    }
}
