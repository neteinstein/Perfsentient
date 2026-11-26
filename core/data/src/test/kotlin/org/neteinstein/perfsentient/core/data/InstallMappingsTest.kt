package org.neteinstein.perfsentient.core.data

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.neteinstein.perfsentient.core.data.source.grantedSensitivePermissions
import org.neteinstein.perfsentient.core.data.source.packageSourceOf
import org.neteinstein.perfsentient.core.data.source.parseSecurityPatch
import org.neteinstein.perfsentient.core.model.PackageSource
import org.neteinstein.perfsentient.core.model.SensitivePermission

class InstallMappingsTest {
    private val granted = PackageInfo.REQUESTED_PERMISSION_GRANTED

    @Test
    fun `only granted sensitive permissions are kept`() {
        val permissions = grantedSensitivePermissions(
            requested = arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.RECEIVE_SMS,
            ),
            flags = intArrayOf(granted, 0, granted, granted or 0x4),
        )
        assertEquals(setOf(SensitivePermission.READ_SMS, SensitivePermission.RECEIVE_SMS), permissions)
    }

    @Test
    fun `missing or short permission arrays are tolerated`() {
        assertTrue(grantedSensitivePermissions(null, null).isEmpty())
        assertTrue(grantedSensitivePermissions(arrayOf(Manifest.permission.READ_SMS), null).isEmpty())
        assertTrue(
            grantedSensitivePermissions(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS), intArrayOf(0)).isEmpty(),
        )
    }

    @Test
    fun `package sources map to the domain enum`() {
        assertEquals(PackageSource.STORE, packageSourceOf(PackageInstaller.PACKAGE_SOURCE_STORE))
        assertEquals(PackageSource.DOWNLOADED_FILE, packageSourceOf(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE))
        assertEquals(PackageSource.LOCAL_FILE, packageSourceOf(PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE))
        assertEquals(PackageSource.OTHER, packageSourceOf(PackageInstaller.PACKAGE_SOURCE_OTHER))
        assertEquals(PackageSource.UNKNOWN, packageSourceOf(PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED))
    }

    @Test
    fun `security patch levels are parsed leniently`() {
        assertEquals(LocalDate.of(2026, 10, 5), parseSecurityPatch("2026-10-05"))
        assertEquals(LocalDate.of(2026, 10, 1), parseSecurityPatch(" 2026-10-01 "))
        assertNull(parseSecurityPatch(""))
        assertNull(parseSecurityPatch("October 2026"))
        assertNull(parseSecurityPatch(null))
    }
}
