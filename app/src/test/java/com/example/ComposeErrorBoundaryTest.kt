package com.example

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.ComposeErrorBoundary
import com.example.ui.components.LocalErrorBoundary
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ComposeErrorBoundaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testErrorBoundaryCatchesExceptionAndDisplaysFallback() {
        var shouldCrash by mutableStateOf(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                ComposeErrorBoundary(
                    componentName = "Test Component",
                    onReset = { shouldCrash = false }
                ) {
                    val errorBoundary = LocalErrorBoundary.current
                    if (shouldCrash) {
                        LaunchedEffect(Unit) {
                            errorBoundary?.invoke(RuntimeException("Intentional composition crash!"))
                        }
                    } else {
                        Text("Active Green Zone")
                    }
                }
            }
        }

        // Verify that under healthy states, normal content is showing successfully
        composeTestRule.onNodeWithText("Active Green Zone").assertIsDisplayed()

        // Trigger crash
        shouldCrash = true
        composeTestRule.waitForIdle()

        // Verify that the error fallback displays gracefully with the component title
        composeTestRule.onNodeWithTag("error_boundary_fallback_Test Component").assertIsDisplayed()
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText("An unexpected error occurred in the Test Component.").assertIsDisplayed()

        // Verify reset action recovers and restores Green Zone rendering!
        composeTestRule.onNodeWithTag("error_boundary_reset_Test Component").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Active Green Zone").assertIsDisplayed()
    }

    @Test
    fun testErrorBoundaryUsingLocalCompositionLocalReporter() {
        var triggerLocalError: ((Throwable) -> Unit)? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                ComposeErrorBoundary(componentName = "Local Broadcaster Component") {
                    val reporter = LocalErrorBoundary.current
                    remember { triggerLocalError = reporter }
                    Text("Idle State Display")
                }
            }
        }

        composeTestRule.onNodeWithText("Idle State Display").assertIsDisplayed()

        // Broadcast non-blocking interaction exception programmatically (mimicking real background thread network crash)
        composeTestRule.runOnUiThread {
            triggerLocalError?.invoke(IllegalStateException("Simulated asynchronous luxury-wallet error!"))
        }
        composeTestRule.waitForIdle()

        // Verify that the local composition exception gets propagated upwards to fallback display
        composeTestRule.onNodeWithTag("error_boundary_fallback_Local Broadcaster Component").assertIsDisplayed()
        composeTestRule.onNodeWithText("EXCEPTION TYPE: IllegalStateException", substring = true).assertDoesNotExist() // Details container starts closed

        // Click to expand custom debug logging details inside the boundary UI
        composeTestRule.onNodeWithText("Show Diagnostics").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("EXCEPTION TYPE: IllegalStateException").assertIsDisplayed()
        composeTestRule.onNodeWithText("Simulated asynchronous luxury-wallet error!").assertIsDisplayed()
    }
}
