package tech.eiki.globeswap.ui.camera

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme

/** Aufbau der Kamera: Tagesthema oben, Sucher 3:4, Auslöser darunter (ohne echte Kamera bleibt der Sucher schwarz). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CameraLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun viewfinderLayout() {
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.CAMERA)
        compose.setContent {
            GlobeSwapTheme {
                PhotoCapture(confirmLabel = "Swap!", onConfirm = { _, _ -> }, hint = "Take a photo – it goes to a stranger.", theme = "Your breakfast")
            }
        }
        compose.onNodeWithText("Your breakfast").assertExists()
        // Sucher randlos bis kurz vor den unteren Rand, Format zwischen 3:4 und 9:20; Tagesthema liegt oben auf dem Bild
        val box = compose.onNodeWithTag("viewfinder").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val ratio = box.height / box.width
        assertTrue("Format $ratio", ratio in 4f / 3f - 0.01f..20f / 9f + 0.01f)
        assertTrue("höher als 3:4", ratio > 4f / 3f + 0.05f)
        assertTrue("fast bis unten: $box in $root", root.bottom - box.bottom <= 8 * 3f + 6f)
        val theme = compose.onNodeWithText("Your breakfast").fetchSemanticsNode().boundsInRoot
        assertTrue("Tagesthema auf dem Bild", theme.top >= box.top && theme.bottom < box.bottom)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/camera-layout.png")
    }
}
