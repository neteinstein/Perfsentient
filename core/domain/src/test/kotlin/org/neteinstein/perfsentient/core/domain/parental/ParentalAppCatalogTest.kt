package org.neteinstein.perfsentient.core.domain.parental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.model.ConcerningApp
import org.neteinstein.perfsentient.core.model.ParentalConcern
import org.neteinstein.perfsentient.core.model.ParentalRiskCategory
import org.neteinstein.perfsentient.core.model.Severity

class ParentalAppCatalogTest {
    private val catalog = ParentalAppCatalog()

    @Test
    fun `every entry has a unique id and valid package names`() {
        val ids = catalog.apps.map { it.id }
        assertEquals("duplicate ids", ids.size, ids.toSet().size)
        val packagePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
        catalog.apps.flatMap { it.packageNames }.forEach { pkg ->
            assertTrue("invalid package $pkg", packagePattern.matches(pkg))
            assertEquals(pkg, catalog.find(pkg)?.packageNames?.single { it == pkg })
        }
        assertEquals(catalog.apps.sumOf { it.packageNames.size }, catalog.packageNames.size)
    }

    @Test
    fun `every category is covered and ages stay within childhood`() {
        assertEquals(ParentalRiskCategory.entries.toSet(), catalog.apps.map { it.category }.toSet())
        catalog.apps.forEach { app ->
            assertTrue("${app.id} recommended age", app.recommendedAge in 10..18)
            assertTrue("${app.id} minimum age", app.minimumAge <= app.recommendedAge)
        }
    }

    @Test
    fun `well known risks are encoded`() {
        val roblox = catalog.find("com.roblox.client")!!
        assertEquals(ParentalRiskCategory.GAMES_WITH_CHAT, roblox.category)
        assertTrue(ParentalConcern.GROOMING_REPORTS in roblox.concerns)
        assertTrue(ParentalConcern.SEXTORTION_REPORTS in catalog.find("com.snapchat.android")!!.concerns)
        assertTrue(ParentalConcern.SEXTORTION_REPORTS in catalog.find("info.wizzapp")!!.concerns)
        assertEquals("tiktok", catalog.find("com.zhiliaoapp.musically.go")?.id)

        // Adults-only categories are never "fine" for a child.
        catalog.apps.filter { it.category in ADULT_ONLY }.forEach { app ->
            assertEquals(app.id, 18, app.recommendedAge)
            assertEquals(app.id, Severity.HIGH, app.baseSeverity)
        }
        assertNull(catalog.find("com.example.notes"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a package listed twice is rejected`() {
        val app =
            ConcerningApp("a", setOf("com.same"), ParentalRiskCategory.DATING, 18, 18, Severity.HIGH, setOf(ParentalConcern.ADULT_CONTENT))
        ParentalAppCatalog(listOf(app, app.copy(id = "b")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `minimum age above the recommended age is rejected`() {
        ConcerningApp("a", setOf("com.a"), ParentalRiskCategory.DATING, 18, 16, Severity.HIGH, setOf(ParentalConcern.ADULT_CONTENT))
    }

    private companion object {
        val ADULT_ONLY = setOf(
            ParentalRiskCategory.DATING,
            ParentalRiskCategory.RANDOM_VIDEO_CHAT,
            ParentalRiskCategory.AI_COMPANIONS,
        )
    }
}
