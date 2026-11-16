package com.neteinstein.persentient.core.data

import com.neteinstein.persentient.core.data.local.entity.ReportEntity
import com.neteinstein.persentient.core.data.mapper.toDomain
import com.neteinstein.persentient.core.data.mapper.toEntity
import com.neteinstein.persentient.core.model.BehaviorType
import com.neteinstein.persentient.core.model.DeviceIssue
import com.neteinstein.persentient.core.model.DeviceIssueType
import com.neteinstein.persentient.core.model.DeviceMetrics
import com.neteinstein.persentient.core.model.Severity
import com.neteinstein.persentient.core.model.Verdict
import com.neteinstein.persentient.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
