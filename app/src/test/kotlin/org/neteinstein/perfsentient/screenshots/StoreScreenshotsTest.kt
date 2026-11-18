package org.neteinstein.perfsentient.screenshots

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import java.io.File
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.neteinstein.perfsentient.AppTab
import org.neteinstein.perfsentient.core.designsystem.theme.PerfsentienceTheme
import org.neteinstein.perfsentient.ui.PerfsentienceAppUi
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real app UI (all three tabs, light and dark) with fictional sample data and writes PNGs
 * to `build/screenshots/` (override with `-Dscreenshots.dir=...`). They are used for the README and the
 * Play Store listing; see `store-listing/screenshots/README.md`. The test also doubles as a smoke test
 * that every tab renders. 1080x2160 px (360x720 dp at xxhdpi).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A plain Application: the real one would start the production Koin graph (Room, WorkManager) before our fakes.
@Config(qualifiers = "w360dp-h720dp-xxhdpi", sdk = [36], application = android.app.Application::class)
class StoreScreenshotsTest {
    @get:Rule
    val compose = createComposeRule()

    private val outDir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").also { it.mkdirs() }

    @Before
    fun setUp() = startSampleKoin()

    @After
    fun tearDown() = stopKoin()

    private fun capture(name: String, tab: AppTab, dark: Boolean, scrollToIndex: Int? = null) {
        var selected by mutableStateOf(tab)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PerfsentienceTheme(darkTheme = dark, dynamicColor = false) {
                PerfsentienceAppUi(selectedTab = selected, onTabSelected = { selected = it })
            }
        }
        // Let entrance animations finish; infinite ones (pulsing dot, aurora) never settle on their own.
        compose.mainClock.advanceTimeBy(3_000)
        if (scrollToIndex != null) {
            compose.onNode(hasScrollAction()).performScrollToIndex(scrollToIndex)
            compose.mainClock.advanceTimeBy(1_500)
        }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File(outDir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name should not be empty", file.length() > 10_000)
    }

    @Test
    fun reportLight() = capture("01-report", AppTab.REPORT, dark = false)

    @Test
    fun offendersLight() = capture("02-offenders", AppTab.REPORT, dark = false, scrollToIndex = OFFENDERS_INDEX)

    @Test
    fun appsLight() = capture("03-apps", AppTab.APPS, dark = false, scrollToIndex = 3)

    @Test
    fun settingsLight() = capture("04-settings", AppTab.SETTINGS, dark = false)

    @Test
    fun reportDark() = capture("05-report-dark", AppTab.REPORT, dark = true)

    @Test
    fun appsDark() = capture("06-apps-dark", AppTab.APPS, dark = true, scrollToIndex = 3)

    private companion object {
        const val OFFENDERS_INDEX = 4
    }
}
