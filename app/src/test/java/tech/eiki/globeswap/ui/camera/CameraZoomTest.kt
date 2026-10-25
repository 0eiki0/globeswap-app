package tech.eiki.globeswap.ui.camera

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
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
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CameraZoomTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun presetsFollowWhatThePhoneCanDo() {
        assertEquals(listOf(0.6f, 1f, 2f, 5f), zoomPresets(0.6f, 8f)) // Weitwinkel als Zoomstufe
        assertEquals(listOf(0.5f, 1f, 2f, 5f), zoomPresets(0.5f, 30f)) // höchstens 4 Knöpfe
        assertEquals(listOf(1f, 2f), zoomPresets(1f, 4f)) // einfache Kamera
        assertEquals(listOf(WIDE, 1f, 2f), zoomPresets(1f, 4f, separateWide = true)) // Weitwinkel als eigene Kamera
        assertEquals(listOf(1f), zoomPresets(1f, 1f)) // kein Zoom: keine Knöpfe (ZoomChips zeigt <2 nicht)
    }

    @Test
    fun labelsAndActiveButton() {
        assertEquals("1", formatZoom(1f))
        assertEquals("0.6", formatZoom(0.6f))
        assertEquals("2", formatZoom(2.02f))
        assertEquals("1.4", formatZoom(1.43f))
        val presets = listOf(0.6f, 1f, 2f, 5f)
        assertEquals(1f, activePreset(presets, 1.4f))
        assertEquals(0.6f, activePreset(presets, 0.8f))
        assertEquals(5f, activePreset(presets, 7f))
    }

    @Test
    fun fieldOfView() {
        // typische Hauptkamera ~ 4,4 mm Brennweite bei 6,4 mm Sensorbreite ≈ 72°, Weitwinkel 2,2 mm ≈ 111°
        val main = horizontalFov(4.4f, 6.4f)
        val ultra = horizontalFov(2.2f, 6.4f)
        assertTrue("Hauptkamera $main°", main in 70.0..75.0)
        assertTrue("Weitwinkel deutlich breiter", ultra > main * 1.25)
    }

    @Test
    fun zoomChips() {
        compose.setContent {
            Box(Modifier.background(Color.DarkGray).padding(16.dp)) {
                ZoomChips(listOf(0.6f, 1f, 2f, 5f), current = 1.4f, onSelect = {})
            }
        }
        compose.onNodeWithText("1.4×").assertExists()
        compose.onNodeWithText("0.6").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/zoom-chips.png")
    }
}
