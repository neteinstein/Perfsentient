package org.neteinstein.perfsentient.core.domain.parental

import org.neteinstein.perfsentient.core.model.ConcerningApp
import org.neteinstein.perfsentient.core.model.ParentalScan
import org.neteinstein.perfsentient.core.model.ParentalWarning
import org.neteinstein.perfsentient.core.model.Severity

/**
 * Checks installed apps against the [catalog] for a child of a given age.
 *
 * An app is flagged while the child is younger than its recommended age. The severity starts at
 * the app's base severity and goes up one level when the child is below the platform's own
 * minimum age and one more when the child is [muchYoungerGapYears] or more below the recommended age.
 */
class ParentalRiskAssessor(
    private val catalog: ParentalAppCatalog,
    private val muchYoungerGapYears: Int = MUCH_YOUNGER_GAP_YEARS,
) {
    /** Severity of [app] for a [childAge]-year-old, or null when the app is fine at that age. */
    fun severityFor(app: ConcerningApp, childAge: Int): Severity? {
        if (childAge >= app.recommendedAge) return null
        var level = app.baseSeverity.ordinal
        if (childAge < app.minimumAge) level++
        if (app.recommendedAge - childAge >= muchYoungerGapYears) level++
        return Severity.entries[level.coerceAtMost(Severity.HIGH.ordinal)]
    }

    /**
     * @param installed label of every installed app, keyed by package name.
     * @param reviewed packages the parent already reviewed.
     */
    fun scan(installed: Map<String, String>, childAge: Int, reviewed: Set<String>): ParentalScan {
        val warnings = installed.mapNotNull { (pkg, label) ->
            val app = catalog.find(pkg) ?: return@mapNotNull null
            val severity = severityFor(app, childAge) ?: return@mapNotNull null
            ParentalWarning(pkg, label, app, severity, childAge, isReviewed = pkg in reviewed)
        }
        return ParentalScan(
            childAge = childAge,
            warnings = warnings.sortedWith(
                compareBy<ParentalWarning> { it.isReviewed }
                    .thenByDescending { it.severity }
                    .thenBy { it.appLabel.lowercase() }
                    .thenBy { it.packageName },
            ),
        )
    }

    companion object {
        const val MUCH_YOUNGER_GAP_YEARS = 5
    }
}
