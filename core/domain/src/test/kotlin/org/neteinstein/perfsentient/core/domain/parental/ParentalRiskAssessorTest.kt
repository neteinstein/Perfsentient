package org.neteinstein.perfsentient.core.domain.parental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.model.ConcerningApp
import org.neteinstein.perfsentient.core.model.ParentalConcern
import org.neteinstein.perfsentient.core.model.ParentalRiskCategory
import org.neteinstein.perfsentient.core.model.Severity

class ParentalRiskAssessorTest {
    private fun entry(id: String, minimumAge: Int = 0, recommendedAge: Int = 13, severity: Severity = Severity.LOW) = ConcerningApp(
        id = id,
        packageNames = setOf("com.$id"),
        category = ParentalRiskCategory.GAMES_WITH_CHAT,
        minimumAge = minimumAge,
        recommendedAge = recommendedAge,
        baseSeverity = severity,
        concerns = setOf(ParentalConcern.STRANGER_CONTACT),
    )

    private val game = entry("game", recommendedAge = 13, severity = Severity.MEDIUM)
    private val social = entry("social", minimumAge = 13, recommendedAge = 16, severity = Severity.LOW)
    private val dating = entry("dating", minimumAge = 18, recommendedAge = 18, severity = Severity.HIGH)
    private val assessor = ParentalRiskAssessor(ParentalAppCatalog(listOf(game, social, dating)))

    @Test
    fun `apps are fine from their recommended age`() {
        assertNull(assessor.severityFor(game, childAge = 13))
        assertNull(assessor.severityFor(game, childAge = 17))
        assertNull(assessor.severityFor(social, childAge = 16))
    }

    @Test
    fun `younger children get the base severity`() {
        assertEquals(Severity.MEDIUM, assessor.severityFor(game, childAge = 12))
        assertEquals(Severity.MEDIUM, assessor.severityFor(game, childAge = 9))
        assertEquals(Severity.LOW, assessor.severityFor(social, childAge = 14))
    }

    @Test
    fun `severity escalates below the platform minimum and when much younger`() {
        // Below the platform's own minimum age: one level up.
        assertEquals(Severity.MEDIUM, assessor.severityFor(social, childAge = 12))
        // Below the minimum and 5+ years under the recommended age: two levels up.
        assertEquals(Severity.HIGH, assessor.severityFor(social, childAge = 11))
        // Much younger only.
        assertEquals(Severity.HIGH, assessor.severityFor(game, childAge = 8))
        // Never above HIGH.
        assertEquals(Severity.HIGH, assessor.severityFor(dating, childAge = 5))
    }

    @Test
    fun `scan keeps installed catalogue apps flagged for the age, reviewed ones last`() {
        val installed = mapOf(
            "com.game" to "Block Game",
            "com.social" to "Feed",
            "com.dating" to "Dates",
            "com.notes" to "Notes", // not in the catalogue
        )
        val scan = assessor.scan(installed, childAge = 14, reviewed = setOf("com.dating"))

        assertEquals(14, scan.childAge)
        // game is fine at 14; dating is HIGH but reviewed, so it goes last.
        assertEquals(listOf("com.social", "com.dating"), scan.warnings.map { it.packageName })
        val (feed, dates) = scan.warnings
        assertEquals("Feed", feed.appLabel)
        assertFalse(feed.isReviewed)
        assertFalse(feed.isBelowMinimumAge)
        assertTrue(dates.isReviewed)
        assertTrue(dates.isBelowMinimumAge)
        assertEquals(Severity.HIGH, dates.severity)
    }

    @Test
    fun `scan orders by severity then label`() {
        val catalog = ParentalAppCatalog(listOf(entry("b", severity = Severity.LOW), entry("a", severity = Severity.LOW), dating))
        val scan = ParentalRiskAssessor(catalog).scan(
            mapOf("com.b" to "beta", "com.a" to "Alpha", "com.dating" to "Zed"),
            childAge = 12,
            reviewed = emptySet(),
        )
        assertEquals(listOf("Zed", "Alpha", "beta"), scan.warnings.map { it.appLabel })
    }
}
