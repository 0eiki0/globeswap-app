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
        // Sucher genau im Fotoformat 3:4, fast über die volle Breite (8 dp Rand je Seite)
        val box = compose.onNodeWithTag("viewfinder").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertEquals(root.width - 2 * 8 * 3f, box.width, 1f) // xxhdpi: 1 dp = 3 px
        assertEquals(4f / 3f, box.height / box.width, 0.01f)
        assertTrue("Sucher unter dem Tagesthema", box.top > compose.onNodeWithText("Your breakfast").fetchSemanticsNode().boundsInRoot.bottom)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/camera-layout.png")
    }
}
