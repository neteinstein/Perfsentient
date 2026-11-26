package org.neteinstein.perfsentient.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.data.local.entity.ReportEntity
import org.neteinstein.perfsentient.core.data.mapper.toDomain
import org.neteinstein.perfsentient.core.data.mapper.toEntity
import org.neteinstein.perfsentient.core.model.BatteryBreakdown
import org.neteinstein.perfsentient.core.model.BehaviorType
import org.neteinstein.perfsentient.core.model.DeviceIssue
import org.neteinstein.perfsentient.core.model.DeviceIssueType
import org.neteinstein.perfsentient.core.model.DeviceMetrics
import org.neteinstein.perfsentient.core.model.Evidence
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.Verdict
import org.neteinstein.perfsentient.core.testing.TestData

class ReportMapperTest {
    @Test
    fun `report survives a round trip through the database entity`() {
        val report = TestData.report(
            score = 61,
            verdict = Verdict.FAIR,
            findings = listOf(TestData.finding("adware", BehaviorType.OVERLAY_ABUSE, Severity.HIGH)),
        ).copy(
            metrics = DeviceMetrics(10, 2.5f, 1.2f, 41.5f, 2, 70, 1, 1234L, 5678L),
            deviceIssues = listOf(DeviceIssue(DeviceIssueType.HIGH_TEMPERATURE, Severity.MEDIUM, 425)),
        )

        val entity = report.toEntity()

        assertEquals("2026-11-16", entity.day)
        assertEquals(61, entity.score)
        assertEquals(report, entity.toDomain())
    }

    @Test
    fun `battery breakdown survives a round trip`() {
        val report = TestData.report(
            battery = TestData.battery(
                TestData.batteryUsage("game", drain = 12.5f, foregroundMs = 3_600_000),
                TestData.batteryUsage("music", drain = 3f, background = 2.5f, backgroundMs = 7_200_000),
                screenAndSystem = 1.2f,
                standby = 0.8f,
            ),
        )

        assertEquals(report, report.toEntity().toDomain())
    }

    @Test
    fun `reports saved before the battery estimate decode with an empty breakdown`() {
        val json = TestData.report().toEntity().payload
        assertFalse("an empty breakdown is not stored", json.contains("\"battery\":{"))
        val legacy = json.replace(",\"battery\":null", "")

        val decoded = ReportEntity("2026-11-16", 0, 90, legacy).toDomain()!!

        assertEquals(BatteryBreakdown.EMPTY, decoded.battery)
    }

    @Test
    fun `finding evidence survives a round trip and old findings decode without it`() {
        val finding = TestData.finding("adware", BehaviorType.OUT_OF_CONTEXT_ADS).copy(
            evidence = listOf(Evidence(2_000, "com.chat", "Chat"), Evidence(1_000)),
        )
        val report = TestData.report(findings = listOf(finding))
        assertEquals(report, report.toEntity().toDomain())

        val legacy = TestData.report(findings = listOf(finding.copy(evidence = emptyList()))).toEntity().payload
            .replace(",\"evidence\":[]", "")
        assertTrue(ReportEntity("2026-11-16", 0, 90, legacy).toDomain()!!.findings.single().evidence.isEmpty())
    }

    @Test
    fun `unknown enum values from newer versions are dropped instead of crashing`() {
        val json = TestData.report(findings = listOf(TestData.finding("a"))).toEntity().payload
            .replace("BACKGROUND_DATA", "SOMETHING_NEW")
            .replace("\"GOOD\"", "\"EXCELLENT\"")
        val decoded = ReportEntity("2026-11-16", 0, 90, json).toDomain()!!
        assertTrue(decoded.findings.isEmpty())
        assertEquals(Verdict.FAIR, decoded.verdict)
    }

    @Test
    fun `corrupted payload maps to null`() {
        assertNull(ReportEntity("2026-11-16", 0, 0, "{not json").toDomain())
    }
}
