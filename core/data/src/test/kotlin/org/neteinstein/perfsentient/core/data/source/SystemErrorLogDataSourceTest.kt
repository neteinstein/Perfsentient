package org.neteinstein.perfsentient.core.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemErrorLogDataSourceTest {
    private fun parse(header: String?) = SystemErrorLogDataSource.packageFromHeader(header)

    @Test
    fun `prefers the package line of an ANR header`() {
        val header = """
            Process: com.example.app:sync
            PID: 1234
            UID: 10123
            Flags: 0x38c83e44
            Package: com.example.app v120 (1.2.0)
            Foreground: No
        """.trimIndent()
        assertEquals("com.example.app", parse(header))
    }

    @Test
    fun `falls back to the process name without its suffix`() {
        assertEquals("com.example.app", parse("Process: com.example.app:remote\nPID: 1"))
    }

    @Test
    fun `rejects missing or malformed headers`() {
        assertNull(parse(null))
        assertNull(parse(""))
        assertNull(parse("Something else entirely"))
        assertNull(parse("Process: system_server"))
    }
}
