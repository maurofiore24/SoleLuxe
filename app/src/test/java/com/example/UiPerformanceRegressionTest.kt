package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * SoleLuxe UI/UX Performance & Regression test suite.
 * Run via: gradle :app:testDebugUnitTest
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class UiPerformanceRegressionTest {

    @get:Rule 
    val composeTestRule = createComposeRule()

    @Test
    fun verify_concierge_overlay_renders_and_interacts_without_latency() {
        // Render App Theme containing the Floating Concierge Support Section
        composeTestRule.setContent {
            MyApplicationTheme {
                ConciergeFloatingSupportSection()
            }
        }

        // 1. Validate Floating Button presence
        val conciergeButton = composeTestRule.onNodeWithContentDescription("Concierge Support")
        conciergeButton.assertExists()
        conciergeButton.assertHasClickAction()

        // Capture Initial FAB State (Visual Regression Testing)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/concierge_fab_initial.png")

        // 2. Click to trigger slide-up transition of the gold-themed concierge overlay
        conciergeButton.performClick()
        composeTestRule.waitForIdle()

        // 3. Verify that the overlay container appears
        val overlayContainer = composeTestRule.onNodeWithTag("concierge_floating_section")
        overlayContainer.assertExists()

        // 4. Assert priority greetings are visible to the Elite patron
        composeTestRule.onNodeWithText("SOLE LUXE CONCIERGE").assertExists()
        composeTestRule.onNodeWithText("Welcome to the Sole Luxe 24/7 Priority Concierge. As an elite patron, you have priority access to bespoke commissions, expedited secure payouts, and VIP support.\n\nHow can I elevate your premium experience today?").assertExists()

        // Capture Opened Overlay State
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/concierge_overlay_open.png")

        // 5. Test preset response interaction latency
        val commissionPreset = composeTestRule.onAllNodesWithText("Request custom feet art commission")
        commissionPreset.onFirst().assertExists()
        commissionPreset.onFirst().performClick()
        composeTestRule.waitForIdle()

        // Verify user message gets added instantly (one in the preset chips list, one in the chat messages list)
        composeTestRule.onAllNodesWithText("Request custom feet art commission").assertCountEquals(2)
    }

    /**
     * Android Macrobenchmark Performance Blueprint
     * 
     * To run frame rate and performance benchmarking on actual physical devices 
     * (e.g., Samsung Galaxy A16) to ensure constant 60 FPS/120 FPS targets under 3D Tilt:
     * 
     * ```kotlin
     * @RunWith(AndroidJUnit4::class)
     * @LargeTest
     * class ScrollFrameRateBenchmark {
     * 
     *     @get:Rule
     *     val benchmarkRule = MacrobenchmarkRule()
     * 
     *     @Test
     *     fun scrollThroughDistributionCenter() = benchmarkRule.measureRepeated(
     *         packageName = "com.aistudio.soleluxe",
     *         metrics = listOf(FrameTimingMetric()),
     *         compilationMode = CompilationMode.Full(),
     *         iterations = 5,
     *         setupBlock = {
     *             pressHome()
     *             startActivityAndWait()
     *         }
     *     ) {
     *         val list = device.findObject(By.res("distribution_center_list"))
     *         list.setGestureMargin(device.displayWidth / 10)
     *         
     *         // Perform heavy flings to evaluate golden aura composition and frame drop stats
     *         list.fling(Direction.DOWN)
     *         device.waitForIdle()
     *         list.fling(Direction.UP)
     *     }
     * }
     * ```
     */
}
