package org.neteinstein.perfsentient.core.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.Verdict
import org.neteinstein.perfsentient.core.testing.TestData.finding
import org.neteinstein.perfsentient.core.testing.TestData.report

class TrustedAppsFilterTest {
    private val calculator = HealthScoreCalculator()
    private val filter = TrustedAppsFilter(calculator)

    private val findings = listOf(
        finding("adware", BehaviorType.OVERLAY_ABUSE, Severity.HIGH),
        finding("adware", BehaviorType.HIDDEN_ICON, Severity.HIGH),
        finding("chat", BehaviorType.NOTIFICATION_SPAM, Severity.MEDIUM),
        finding("vpn", BehaviorType.PERSISTENT_BACKGROUND, Severity.HIGH),
    )
    private val raw = report(findings = findings).let { it.copy(score = calculator.score(it.findings, it.deviceIssues)) }

    @Test
    fun `no trusted apps leaves the report untouched`() {
        assertSame(raw, filter.apply(raw, emptySet()))
    }

    @Test
    fun `trusting an app that was not flagged leaves the report untouched`() {
        assertSame(raw, filter.apply(raw, setOf("calculator")))
    }

    @Test
    fun `findings of trusted apps are removed and the day is re-scored`() {
        val filtered = filter.apply(raw, setOf("chat", "vpn"))

        assertEquals(listOf("adware", "adware"), filtered.findings.map { it.packageName })
        assertEquals(2, filtered.hiddenTrustedApps)
        assertEquals(70, filtered.score) // only adware's capped 30-point penalty is left
        assertEquals(Verdict.FAIR, filtered.verdict)
    }

    @Test
    fun `trusting every offender gives a clean score while device issues still count`() {
        val issue = DeviceIssue(DeviceIssueType.HIGH_TEMPERATURE, Severity.MEDIUM, 450)
        val withIssue = raw.copy(deviceIssues = listOf(issue))

        val filtered = filter.apply(withIssue, setOf("adware", "chat", "vpn"))

        assertEquals(emptyList<Any>(), filtered.findings)
        assertEquals(3, filtered.hiddenTrustedApps)
        assertEquals(100 - Severity.MEDIUM.weight, filtered.score)
        assertEquals(Verdict.GOOD, filtered.verdict)
        assertEquals(listOf(issue), filtered.deviceIssues)
    }
}
