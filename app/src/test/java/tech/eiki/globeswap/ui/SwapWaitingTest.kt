package tech.eiki.globeswap.ui

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.ui.swap.SwapContent
import tech.eiki.globeswap.ui.swap.SwapUiState
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme

/** Während ein Foto wartet, kann man gleich das nächste machen (ohne die App neu zu starten). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SwapWaitingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun waitingOffersAnotherPhoto() {
        var another = 0
        var takeBack = 0
        compose.setContent {
            GlobeSwapTheme {
                SwapContent(SwapUiState.Waiting("p1"), "", { _, _ -> }, { takeBack++ }, { another++ }, {})
            }
        }
        compose.onNodeWithText("You can already take your next photo", substring = true).assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/swap-waiting.png")
        compose.onNodeWithText("Take another photo").performClick()
        assertEquals(1, another)
        assertEquals(0, takeBack)
        compose.onNodeWithText("Take back").performClick()
        assertEquals(1, takeBack)
    }
}
