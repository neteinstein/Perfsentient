package org.neteinstein.perfsentient.core.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.model.UsageEventType
import org.neteinstein.perfsentient.core.testing.TestData.SYSTEM_UI
import org.neteinstein.perfsentient.core.testing.TestData.event
import org.neteinstein.perfsentient.core.testing.TestData.resumed

class ForegroundSessionsTest {
    @Test
    fun `builds consecutive sessions and ignores system ui`() {
        val sessions = ForegroundSessions.build(
            events = listOf(
                resumed("a", 0),
                resumed("a", 5), // same package: no new session
                resumed(SYSTEM_UI, 10), // ignored
                resumed("b", 20),
                event("b", UsageEventType.SCREEN_NON_INTERACTIVE, 50),
            ),
            ignoredPackages = setOf(SYSTEM_UI),
            unlockWindowMs = 3,
        )
        assertEquals(listOf("a", "b"), sessions.map { it.packageName })
        assertEquals(20L, sessions[0].durationMs)
        assertEquals(30L, sessions[1].durationMs)
    }

    @Test
    fun `flags sessions that start right after unlock`() {
        val sessions = ForegroundSessions.build(
            events = listOf(
                event("android", UsageEventType.KEYGUARD_HIDDEN, 100),
                resumed("ad", 102),
                resumed("late", 500),
            ),
            ignoredPackages = emptySet(),
            unlockWindowMs = 3,
        )
        assertTrue(sessions.first { it.packageName == "ad" }.startedRightAfterUnlock)
        assertFalse(sessions.first { it.packageName == "late" }.startedRightAfterUnlock)
    }

    @Test
    fun `an app still open ends at the given time instead of the last event`() {
        val events = listOf(resumed("a", 0), resumed("b", 10))
        assertEquals(0L, ForegroundSessions.build(events, emptySet(), 0).last().durationMs)
        assertEquals(90L, ForegroundSessions.build(events, emptySet(), 0, openUntilMs = 100).last().durationMs)
    }

    @Test
    fun `empty events produce no sessions`() {
        assertTrue(ForegroundSessions.build(emptyList(), emptySet(), 0).isEmpty())
    }
}
