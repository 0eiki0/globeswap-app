package tech.eiki.globeswap.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.ui.settings.ThemeSection
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.util.AppTheme

/** Einstellungen → Design: Auswahl wird gespeichert und die App wechselt sofort. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ThemeSettingTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun reset() = AppTheme.set(context, AppTheme.SYSTEM)

    @Test
    fun switchesImmediatelyAndRemembers() {
        var background = Color.Unspecified
        compose.setContent {
            GlobeSwapTheme {
                val bg = MaterialTheme.colorScheme.background
                SideEffect { background = bg }
                Surface { Column { ThemeSection(AppTheme.current) { AppTheme.set(context, it) } } }
            }
        }
        val light = background
        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()
        assertEquals(AppTheme.DARK, AppTheme.current)
        assertTrue("Hintergrund sollte dunkel sein", background != light && background.red < 0.2f)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/theme-dark.png")

        // Nach einem Neustart aus den Einstellungen geladen
        AppTheme.load(context)
        assertEquals(AppTheme.DARK, AppTheme.current)
        compose.onNodeWithText("Light").performClick()
        compose.waitForIdle()
        assertEquals(light, background)
    }
}
