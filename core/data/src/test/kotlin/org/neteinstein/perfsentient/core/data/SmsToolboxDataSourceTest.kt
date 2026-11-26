package org.neteinstein.perfsentient.core.data

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.perfsentient.core.data.partner.ScamSignalsContract
import org.neteinstein.perfsentient.core.data.source.SmsToolboxDataSource
import org.neteinstein.perfsentient.core.data.source.parseKinds
import org.neteinstein.perfsentient.core.model.ScamMessageKind
import org.neteinstein.perfsentient.core.model.ScamMessageSignal
import org.neteinstein.perfsentient.core.model.SmsPartnerLinks
import org.neteinstein.perfsentient.core.model.SmsPartnerState
import org.neteinstein.perfsentient.core.model.SmsPartnerStatus
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class SmsToolboxDataSourceTest {
    private val context = RuntimeEnvironment.getApplication()
    private val pkg = ScamSignalsContract.PROVIDER_PACKAGE

    private fun install() {
        shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                packageName = pkg
                applicationInfo = ApplicationInfo().apply { packageName = pkg }
            },
        )
    }

    private fun provide(sharing: Boolean) {
        FakeSmsToolbox.sharing = sharing
        Robolectric.buildContentProvider(FakeSmsToolbox::class.java)
            .create(ProviderInfo().apply { authority = ScamSignalsContract.authority(pkg) })
    }

    @Test
    fun `nothing is read when SMS Toolbox is not installed`() {
        val source = SmsToolboxDataSource(context) { true }
        assertEquals(SmsPartnerStatus.NOT_INSTALLED, source.status())
        assertTrue(source.signals(0, Long.MAX_VALUE).isEmpty())
    }

    @Test
    fun `a copy not signed by its developer is reported and never read`() {
        install()
        provide(sharing = true)
        val source = SmsToolboxDataSource(context) { false }

        assertEquals(SmsPartnerState.UNVERIFIED, source.status().state)
        assertEquals(pkg, source.status().packageName)
        assertTrue(source.signals(0, Long.MAX_VALUE).isEmpty())
    }

    @Test
    fun `a connected SMS Toolbox shares its status and signals in the window`() {
        install()
        provide(sharing = true)
        val source = SmsToolboxDataSource(context) { it == pkg }

        assertEquals(
            SmsPartnerStatus(pkg, verified = true, connected = true, isDefaultSmsApp = true, checkedToday = 12, suspiciousToday = 2),
            source.status(),
        )
        assertEquals(
            listOf(
                ScamMessageSignal(
                    2_000,
                    setOf(ScamMessageKind.DELIVERY_OR_FEE, ScamMessageKind.SHORTENED_LINK),
                    hasLink = true,
                    threadId = 7,
                ),
            ),
            source.signals(1_500, 2_500),
        )
        assertEquals("1499", FakeSmsToolbox.lastSince)
    }

    @Test
    fun `without a provider or with sharing off it reads as not connected`() {
        install()
        val source = SmsToolboxDataSource(context) { true }
        assertEquals(SmsPartnerState.NOT_CONNECTED, source.status().state)

        provide(sharing = false)
        assertFalse(source.status().connected)
        assertTrue(source.signals(0, Long.MAX_VALUE).isEmpty())
    }

    @Test
    fun `unknown kinds are dropped and the links match the contract`() {
        assertEquals(setOf(ScamMessageKind.URGENCY), parseKinds("URGENCY, SOMETHING_NEW,"))
        assertTrue(parseKinds(null).isEmpty())
        assertEquals(ScamSignalsContract.PROVIDER_PACKAGE, SmsPartnerLinks.PACKAGE)
        assertEquals(ScamSignalsContract.CONNECT_DEEP_LINK, SmsPartnerLinks.CONNECT)
        assertEquals(ScamSignalsContract.CONVERSATION_DEEP_LINK_PREFIX, SmsPartnerLinks.CONVERSATION_PREFIX)
    }

    /** Stands in for SMS Toolbox's ScamSignalsProvider. */
    class FakeSmsToolbox : ContentProvider() {
        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor =
            when (uri.lastPathSegment) {
                ScamSignalsContract.PATH_STATUS -> MatrixCursor(ScamSignalsContract.Status.COLUMNS).apply {
                    addRow(arrayOf<Any>(1, if (sharing) 1 else 0, 1, if (sharing) 12 else 0, if (sharing) 2 else 0))
                }

                else -> MatrixCursor(ScamSignalsContract.Signals.COLUMNS).apply {
                    lastSince = uri.getQueryParameter(ScamSignalsContract.PARAM_SINCE)
                    if (sharing) {
                        addRow(arrayOf<Any?>(1_000L, "URGENCY", 0, null))
                        addRow(arrayOf<Any?>(2_000L, "DELIVERY_OR_FEE,SHORTENED_LINK,FUTURE_KIND", 1, 7L))
                        addRow(arrayOf<Any?>(3_000L, "MONEY_OR_PRIZE", 1, 0L))
                    }
                }
            }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0

        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0

        companion object {
            var sharing = false
            var lastSince: String? = null
        }
    }
}
