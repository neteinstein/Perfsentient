package org.neteinstein.perfsentient.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import java.io.File
import org.junit.After
import org.junit.Assume.assumeTrue
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
 * Records the frames of the Play Console review walkthrough (see `store-listing/video/review/README.md`):
 * the real app UI, driven by real taps and drags, with the fictional [SampleData]. Writes numbered PNGs plus
 * `timeline.json` (when each frame is shown, where taps land, and caption markers) to the directory in the
 * `REVIEW_VIDEO_DIR` environment variable. Skipped when it is not set, so normal test runs stay fast.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h720dp-xxhdpi", sdk = [36], application = android.app.Application::class)
class ReviewVideoFramesTest {
    @get:Rule
    val compose = createComposeRule()

    private val outDir: File? = System.getenv("REVIEW_VIDEO_DIR")?.let(::File)

    private var timeMs = 0L
    private var frameIndex = 0
    private val frames = StringBuilder()
    private val events = StringBuilder()

    @Before
    fun setUp() = startSampleKoin()

    @After
    fun tearDown() = stopKoin()

    @Test
    fun recordWalkthrough() {
        val dir = outDir
        assumeTrue("Set REVIEW_VIDEO_DIR to record the review video frames", dir != null)
        requireNotNull(dir).mkdirs()

        var selected by mutableStateOf(AppTab.REPORT)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PerfsentienceTheme(darkTheme = false, dynamicColor = false) {
                PerfsentienceAppUi(selectedTab = selected, onTabSelected = { selected = it })
            }
        }

        // 1. App opens on today's report (entrance animations run at full frame rate).
        marker("report")
        animate(2_500)
        hold(2_500)
        marker("offenders")
        dragUntil("Misbehaving apps", targetTop = 0.12f)
        hold(5_000)

        // 2. Apps tab: every flagged app, including one that hides its launcher icon.
        marker("apps")
        tap(compose.onAllNodesWithText("Apps").onLast())
        animate(900)
        hold(2_500)
        marker("scroll-apps")
        dragUntil("Coupon Hunter", targetTop = 0.45f)
        hold(1_500)
        marker("hidden")
        tap(compose.onAllNodesWithText("Coupon Hunter").onLast())
        animate(900)
        hold(6_500)
        marker("dismiss")
        tapRoot(lastRoot(), Offset(0.5f, 0.12f))
        animate(800)
        hold(800)

        // 3. Settings: the "Installed apps list" (QUERY_ALL_PACKAGES) rationale, in the app's own words.
        marker("settings")
        tap(compose.onAllNodesWithText("Settings").onLast())
        animate(900)
        hold(2_500)
        marker("scroll-settings")
        dragUntil("Installed apps list", targetTop = 0.22f)
        hold(1_500)
        marker("why")
        tap(whyButtonBelow("Installed apps list"))
        animate(900)
        hold(9_000)
        marker("end")

        File(dir, "timeline.json").writeText(
            """{"frameSize":[$width,$height],"durationMs":$timeMs,"frames":[$frames],"events":[$events]}""",
        )
    }

    // --- timeline -------------------------------------------------------------------------------

    private val width get() = compose.onRoot().fetchSemanticsNode().size.width
    private val height get() = compose.onRoot().fetchSemanticsNode().size.height

    private fun marker(name: String) = event("""{"t":$timeMs,"type":"marker","name":"$name"}""")

    private fun event(json: String) {
        if (events.isNotEmpty()) events.append(',')
        events.append(json)
    }

    /** Advances the UI clock by [stepMs], captures what is on screen and shows it for [stepMs] of video. */
    private fun frame(stepMs: Long) {
        compose.mainClock.advanceTimeBy(stepMs)
        val name = "f%05d.png".format(frameIndex++)
        File(requireNotNull(outDir), name).outputStream().use { composite().compress(Bitmap.CompressFormat.PNG, 90, it) }
        if (frames.isNotEmpty()) frames.append(',')
        frames.append("""{"t":$timeMs,"f":"$name"}""")
        timeMs += stepMs
    }

    private fun animate(ms: Long) = repeat((ms / FRAME_MS).toInt()) { frame(FRAME_MS) }

    /** Static stretches: a lower capture rate keeps the infinite animations alive without thousands of frames. */
    private fun hold(ms: Long) = repeat((ms / HOLD_MS).toInt()) { frame(HOLD_MS) }

    /** The activity window plus any dialog window on top of it (the bottom sheet is its own window). */
    private fun composite(): Bitmap {
        val roots = compose.onAllNodes(isRoot())
        val count = roots.fetchSemanticsNodes().size
        val base = roots[0].captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(base)
        for (i in 1 until count) {
            val layer = roots[i].captureToImage().asAndroidBitmap()
            val left = (base.width - layer.width).toFloat().coerceAtLeast(0f)
            val top = (base.height - layer.height).toFloat().coerceAtLeast(0f)
            canvas.drawBitmap(layer, left, top, null)
        }
        return base
    }

    private fun lastRoot(): SemanticsNodeInteraction {
        val roots = compose.onAllNodes(isRoot())
        return roots[roots.fetchSemanticsNodes().size - 1]
    }

    // --- gestures ------------------------------------------------------------------------------

    private fun tap(node: SemanticsNodeInteraction) {
        val c = node.fetchSemanticsNode().boundsInRoot.center
        tapEvent(c.x, c.y)
        animate(TAP_LEAD_MS)
        node.performClick()
    }

    private fun tapRoot(root: SemanticsNodeInteraction, fraction: Offset) {
        val size = root.fetchSemanticsNode().size
        val p = Offset(size.width * fraction.x, size.height * fraction.y)
        tapEvent(p.x, p.y)
        animate(TAP_LEAD_MS)
        root.performTouchInput { click(p) }
    }

    private fun tapEvent(x: Float, y: Float) = event("""{"t":$timeMs,"type":"tap","x":${x.toInt()},"y":${y.toInt()}}""")

    private fun topOf(text: String): Float? =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().lastOrNull()?.boundsInRoot?.top

    /** A finger drag, frame by frame, until [text] is visible with its top at [targetTop] of the screen height. */
    private fun dragUntil(text: String, targetTop: Float) {
        val list = compose.onNode(hasScrollAction())
        val start = Offset(width * 0.5f, height * 0.72f)
        val goal = height * targetTop
        event("""{"t":$timeMs,"type":"drag-start","x":${start.x.toInt()},"y":${start.y.toInt()}}""")
        list.performTouchInput { down(start) }
        var moved = 0f
        var guard = 0
        while (guard++ < MAX_DRAG_FRAMES) {
            val remaining = topOf(text)?.let { it - goal } ?: Float.MAX_VALUE
            if (remaining <= 1f) break
            // Ease out as the target approaches, so the release has almost no velocity (no fling overshoot).
            val step = minOf(remaining, if (remaining == Float.MAX_VALUE) DRAG_STEP_PX else maxOf(remaining / 5f, 6f), DRAG_STEP_PX)
            list.performTouchInput { moveBy(Offset(0f, -step), delayMillis = FRAME_MS) }
            moved += step
            event("""{"t":$timeMs,"type":"drag","x":${start.x.toInt()},"y":${(start.y - moved).toInt()}}""")
            frame(FRAME_MS)
        }
        list.performTouchInput {
            moveBy(Offset.Zero, delayMillis = 200)
            up()
        }
        event("""{"t":$timeMs,"type":"drag-end"}""")
        animate(300)
    }

    private fun whyButtonBelow(title: String): SemanticsNodeInteraction {
        val titleTop = requireNotNull(topOf(title)) { "$title not on screen" }
        val buttons = compose.onAllNodesWithText("Why is this needed?")
        val nodes = buttons.fetchSemanticsNodes()
        val index = nodes.indices.filter { nodes[it].boundsInRoot.top > titleTop }
            .minBy { nodes[it].boundsInRoot.top }
        return buttons[index]
    }

    private val Rect.center get() = Offset((left + right) / 2f, (top + bottom) / 2f)

    private companion object {
        const val FRAME_MS = 33L
        const val HOLD_MS = 100L
        const val TAP_LEAD_MS = 300L
        const val DRAG_STEP_PX = 34f
        const val MAX_DRAG_FRAMES = 400
    }
}
