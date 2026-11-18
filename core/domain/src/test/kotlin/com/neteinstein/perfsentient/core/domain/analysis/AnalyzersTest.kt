package com.neteinstein.perfsentient.core.domain.analysis

import com.neteinstein.perfsentient.core.model.BehaviorType
import com.neteinstein.perfsentient.core.model.DeviceIssue
import com.neteinstein.perfsentient.core.model.DeviceIssueType
import com.neteinstein.perfsentient.core.model.Severity
import com.neteinstein.perfsentient.core.model.Verdict
import com.neteinstein.perfsentient.core.testing.TestData.app
import com.neteinstein.perfsentient.core.testing.TestData.finding
import com.neteinstein.perfsentient.core.testing.TestData.network
import com.neteinstein.perfsentient.core.testing.TestData.usage
import com.neteinstein.perfsentient.core.testing.TestData.vital
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzersTest {
    private val thresholds = DetectionThresholds()
    private val device = DeviceHealthAnalyzer(thresholds)

    @Test
    fun `device metrics compute drain, screen-off drain, temperature and memory`() {
        val vitals = (0..8).map { i ->
            vital(at = i * 30 * MIN, battery = 100 - i * 2, screenOn = false, temperature = 30f + i, lowMemory = i % 2 == 0)
        }
        val metrics = device.metrics(
            input(vitals = vitals, network = listOf(network("a", background = 5)), usage = listOf(usage("a", foregroundMs = 10))),
        )
        assertEquals(9, metrics.sampleCount)
        assertEquals(4f, metrics.drainPerHour, 0.01f)
        assertEquals(4f, metrics.screenOffDrainPerHour, 0.01f)
        assertEquals(38f, metrics.maxTemperatureC, 0.01f)
        assertEquals(75, metrics.averageMemoryUsedPercent)
        assertEquals(5, metrics.lowMemorySamples)
        assertEquals(5L, metrics.totalBackgroundBytes)
        assertEquals(10L, metrics.totalForegroundTimeMs)

        val issues = device.issues(metrics).associateBy { it.type }
        assertEquals(Severity.HIGH, issues.getValue(DeviceIssueType.SCREEN_OFF_DRAIN).severity)
        assertEquals(DeviceIssueType.MEMORY_PRESSURE in issues, true)
    }

    @Test
    fun `charging periods, large gaps and short screen-off windows are ignored`() {
        val vitals = listOf(
            vital(0, battery = 50, charging = true),
            vital(30 * MIN, battery = 60, charging = true),
            vital(5 * HOUR, battery = 59, screenOn = false),
            vital(5 * HOUR + 30 * MIN, battery = 58, screenOn = false),
        )
        val metrics = device.metrics(input(vitals = vitals))
        assertEquals(2f, metrics.drainPerHour, 0.01f)
        assertEquals("less than 2h of screen-off data", 0f, metrics.screenOffDrainPerHour, 0.0f)
    }

    @Test
    fun `heat and thermal throttling become device issues`() {
        val metrics = device.metrics(input(vitals = listOf(vital(0, temperature = 47f, thermal = 4), vital(MIN, temperature = 40f))))
        val issues = device.issues(metrics).associateBy { it.type }
        assertEquals(Severity.HIGH, issues.getValue(DeviceIssueType.HIGH_TEMPERATURE).severity)
        assertEquals(470L, issues.getValue(DeviceIssueType.HIGH_TEMPERATURE).value)
        assertEquals(Severity.HIGH, issues.getValue(DeviceIssueType.THERMAL_THROTTLING).severity)
    }

    @Test
    fun `empty input gives empty metrics and no issues`() {
        val metrics = device.metrics(input())
        assertEquals(0, metrics.sampleCount)
        assertTrue(device.issues(metrics).isEmpty())
    }

    @Test
    fun `score caps per-app penalty and subtracts device issues`() {
        val calculator = HealthScoreCalculator()
        val findings = listOf(
            finding("a", BehaviorType.BACKGROUND_DATA, Severity.HIGH),
            finding("a", BehaviorType.OVERLAY_ABUSE, Severity.HIGH),
            finding("a", BehaviorType.HIDDEN_ICON, Severity.HIGH), // 45 -> capped at 30
            finding("b", BehaviorType.STORAGE_BLOAT, Severity.LOW), // 2
        )
        val issues = listOf(DeviceIssue(DeviceIssueType.HIGH_TEMPERATURE, Severity.MEDIUM, 0)) // 7
        assertEquals(61, calculator.score(findings, issues))
        assertEquals(100, calculator.score(emptyList(), emptyList()))
        assertEquals(Verdict.GOOD, calculator.verdict(80))
        assertEquals(Verdict.FAIR, calculator.verdict(55))
        assertEquals(Verdict.POOR, calculator.verdict(54))
    }

    @Test
    fun `score never drops below zero`() {
        val findings = (1..10).map { finding("app$it", severity = Severity.HIGH) }
        assertEquals(0, HealthScoreCalculator().score(findings + findings, emptyList()))
    }

    @Test
    fun `behavior analyzer runs all detectors, dedupes and sorts by severity`() {
        val analyzer = BehaviorAnalyzer(BehaviorAnalyzer.defaultDetectors(thresholds) + BehaviorAnalyzer.defaultDetectors(thresholds))
        val findings = analyzer.analyze(
            input(
                apps = listOf(app("zeta", cacheBytes = 2L * 1024 * MB), app("alpha", overlay = true, launcher = false)),
            ),
        )
        assertEquals(listOf("alpha" to Severity.HIGH, "zeta" to Severity.LOW), findings.map { it.packageName to it.severity }.distinct())
        assertEquals(findings.size, findings.distinctBy { it.packageName to it.type }.size)
    }
}
