package org.neteinstein.perfsentient.core.domain.scam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScamSignalCatalogTest {
    private val catalog = ScamSignalCatalog()

    @Test
    fun `labels are reduced to plain words`() {
        assertEquals(listOf("whatsapp"), LabelSkeleton.tokens("W h a t s A p p"))
        assertEquals(listOf("whatsapp", "plus"), LabelSkeleton.tokens("Wh4tsApp+ Plus"))
        assertEquals(listOf("google", "play"), LabelSkeleton.tokens("Gооgle  Plаy")) // Cyrillic о and а
        assertEquals(listOf("caixa", "geral", "de", "depositos"), LabelSkeleton.tokens("Caixa Geral de Depósitos"))
        assertEquals(listOf("n26"), LabelSkeleton.tokens("N26"))
        assertEquals(listOf("a", "b"), LabelSkeleton.tokens("a b"))
        assertTrue(LabelSkeleton.tokens("✓✓").isEmpty())
    }

    @Test
    fun `look-alike names are matched to what they imitate`() {
        assertEquals("WhatsApp", catalog.impersonatedBy("WhatsApp Plus")?.name)
        assertEquals("WhatsApp", catalog.impersonatedBy("GBWhatsApp")?.name)
        assertEquals("System update", catalog.impersonatedBy("SYSTEM-UPDATE")?.name)
        assertEquals("Google Play Protect", catalog.impersonatedBy("Play Prоtect")?.name)
        assertEquals("Parcel tracking", catalog.impersonatedBy("DHL Express")?.name)
        assertEquals("Settings", catalog.impersonatedBy("Settings")?.name)
        assertEquals("Chrome", catalog.impersonatedBy("Chrome")?.name)
        assertEquals("N26", catalog.impersonatedBy("N26")?.name)
    }

    @Test
    fun `everyday words and unrelated names do not match`() {
        listOf("Chromecast", "Signal Booster", "Settings Backup", "Purchase Tracker", "Notes", "", "Ledger Book").forEach {
            assertNull(it, catalog.impersonatedBy(it))
        }
    }

    @Test
    fun `lists answer membership and do not overlap`() {
        assertTrue(catalog.isMessagingApp("com.whatsapp"))
        assertTrue(!catalog.isMessagingApp(null))
        assertTrue(catalog.isBrowserOrFiles("com.android.chrome"))
        assertTrue(!catalog.isBrowserOrFiles(null))
        assertTrue(catalog.isRemoteAccessApp("com.anydesk.anydeskandroid"))
        assertTrue((catalog.messagingApps intersect catalog.browsersAndFiles).isEmpty())
        assertTrue((catalog.remoteAccessApps intersect (catalog.messagingApps + catalog.browsersAndFiles)).isEmpty())
        assertEquals(catalog.impersonatedNames.size, catalog.impersonatedNames.map { it.name }.toSet().size)
    }
}
