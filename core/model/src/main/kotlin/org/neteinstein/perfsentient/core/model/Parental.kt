package org.neteinstein.perfsentient.core.model

/**
 * Parental mode. While [enabled], the apps installed on this phone are checked against the
 * [childAge] the parent entered and the Parental help tab is shown.
 */
data class ParentalSettings(
    val enabled: Boolean = false,
    val childAge: Int = DEFAULT_CHILD_AGE,
) {
    init {
        require(childAge in CHILD_AGE_RANGE) { "childAge must be in $CHILD_AGE_RANGE" }
    }

    companion object {
        const val DEFAULT_CHILD_AGE = 10
        val CHILD_AGE_RANGE = 3..17
    }
}

/** The kind of app a catalogue entry is. Drives the explanation and the advice shown to parents. */
enum class ParentalRiskCategory {
    GAMES_WITH_CHAT,
    VIRTUAL_WORLDS,
    SOCIAL_MEDIA,
    PRIVATE_MESSAGING,
    FRIEND_FINDING,
    RANDOM_VIDEO_CHAT,
    ANONYMOUS_MESSAGING,
    LIVE_STREAMING,
    DATING,
    AI_COMPANIONS,
    HIDDEN_VAULTS,
    GAMBLING,
    FILTER_BYPASS,
}

/** A documented hazard of an app. Declaration order is the display order (most serious first). */
enum class ParentalConcern {
    /** Police, courts or child-safety groups documented adults grooming children on it. */
    GROOMING_REPORTS,

    /** Named in sextortion research or law-enforcement warnings. */
    SEXTORTION_REPORTS,

    /** Sued, investigated by a regulator or pulled from app stores over child safety. */
    SAFETY_ACTION,

    /** Sexual or other adult content is easy to reach. */
    ADULT_CONTENT,

    /** Strangers can message, call or play with the child. */
    STRANGER_CONTACT,

    /** Live video with people the child does not know. */
    LIVE_VIDEO,

    /** Users do not need a real identity. */
    ANONYMITY,

    /** Messages vanish, so there is nothing to look back at. */
    DISAPPEARING_MESSAGES,

    /** Shows the child's location or matches them with people nearby. */
    LOCATION_SHARING,

    /** Known for anonymous abuse and pile-ons. */
    BULLYING,

    /** AI characters that act as friends or partners, including sexual role-play. */
    AI_ROLE_PLAY,

    /** Hides photos, videos, chats or other apps behind a disguise or PIN. */
    HIDES_CONTENT,

    /** Gets around parental controls or network filters. */
    BYPASSES_CONTROLS,

    /** Real-money betting or casino-style games. */
    GAMBLING,

    /** Violent content. */
    VIOLENT_CONTENT,

    /** Virtual currency, gifts or loot boxes paid with real money. */
    SPENDING,

    /** Designed for endless scrolling / compulsive use. */
    COMPULSIVE_USE,
}

/**
 * An app child-safety organisations and investigations warn parents about.
 *
 * @property minimumAge age the platform itself requires in its terms (0 when it sets none).
 * @property recommendedAge age from which the app is considered reasonable (expert guidance and
 *   the Play content rating, whichever is higher). Children younger than this get a warning.
 * @property baseSeverity how serious the warning is for a child between [minimumAge] and [recommendedAge].
 */
data class ConcerningApp(
    val id: String,
    val packageNames: Set<String>,
    val category: ParentalRiskCategory,
    val minimumAge: Int,
    val recommendedAge: Int,
    val baseSeverity: Severity,
    val concerns: Set<ParentalConcern>,
) {
    init {
        require(packageNames.isNotEmpty()) { "$id needs at least one package name" }
        require(minimumAge in 0..recommendedAge) { "$id: minimumAge must be between 0 and recommendedAge" }
        require(concerns.isNotEmpty()) { "$id needs at least one concern" }
    }
}

/** One concerning app installed on this phone, assessed for the child's age. */
data class ParentalWarning(
    val packageName: String,
    val appLabel: String,
    val app: ConcerningApp,
    val severity: Severity,
    val childAge: Int,
    /** The parent looked at it (e.g. set up the app's own controls) and moved it out of the warnings. */
    val isReviewed: Boolean,
) {
    /** The child is younger than the age the platform itself requires. */
    val isBelowMinimumAge: Boolean get() = childAge < app.minimumAge
}

/** Result of checking this phone's apps for the child's age. */
data class ParentalScan(
    val childAge: Int,
    /** Unreviewed first, then worst severity first, then by name. */
    val warnings: List<ParentalWarning>,
)
