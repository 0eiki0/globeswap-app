package tech.eiki.globeswap.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.data.MatchPref
import tech.eiki.globeswap.data.Message
import tech.eiki.globeswap.data.UserSettings
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.WorldCountry
import tech.eiki.globeswap.ui.camera.DailyTheme
import tech.eiki.globeswap.ui.chat.ChatEnded
import tech.eiki.globeswap.ui.chat.MessageList
import tech.eiki.globeswap.ui.chat.ReactionPicker
import tech.eiki.globeswap.ui.onboarding.AgeGateScreen
import tech.eiki.globeswap.ui.settings.SettingsContent
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.ui.world.WorldContent
import tech.eiki.globeswap.ui.world.WorldShapes

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class NewFeaturesTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    private fun messages(n: Int) = (1..n).map { i ->
        Message("m$i", mine = i % 2 == 0, photoId = "p$i", createdAt = 1_700_000_000_000 + i * 60_000L,
            country = if (i % 2 == 0) "" else "JP", reaction = if (i == n - 1) "❤️" else "")
    }

    /** Beim Öffnen steht der Chat ganz unten: das neueste Foto ist vollständig zu sehen. */
    @Test
    fun chatOpensAtTheBottom() {
        lateinit var state: androidx.compose.foundation.lazy.LazyListState
        compose.setContent {
            state = rememberLazyListState()
            GlobeSwapTheme {
                Surface { MessageList(messages(6), maxPhotos = 10, photoUrl = { "" }, listState = state, modifier = Modifier.height(700.dp)) }
            }
        }
        compose.waitForIdle()
        assertEquals("neuestes Foto ganz unten", 0, state.firstVisibleItemIndex)
        assertEquals(0, state.firstVisibleItemScrollOffset)
        assertTrue("nicht weiter nach unten scrollbar", !state.canScrollBackward)
        compose.onNodeWithText("You", substring = true).assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/chat-bottom.png")
    }

    @Test
    fun worldMap() {
        val shapes = WorldShapes.load(context)
        assertTrue("Länder in der Karte: ${shapes.countries.size}", shapes.countries.size > 170)
        assertTrue(listOf("DE", "FR", "NO", "JP", "BR", "US", "AU").all { it in shapes.countries })
        val world = World(
            listOf(WorldCountry("JP", "AS", 12), WorldCountry("BR", "SA", 5), WorldCountry("DE", "EU", 3), WorldCountry("CA", "NA", 1)),
            continents = 4,
        )
        compose.setContent { GlobeSwapTheme { Surface { WorldContent(world, shapes) } } }
        compose.onNodeWithText("Japan").assertExists()
        compose.onNodeWithText("12 photos").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/world.png")
    }

    @Test
    fun settings() {
        compose.setContent {
            GlobeSwapTheme {
                Surface {
                    Column {
                        SettingsContent(UserSettings(true, MatchPref.FAR, "DE", geoEnabled = true), {}, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Show my country").assertExists()
        compose.onNodeWithText("Far away").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/settings.png")
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun reactionPickerAndEndedChat() {
        compose.setContent {
            GlobeSwapTheme {
                Surface {
                    Column(Modifier.padding(16.dp)) { ChatEnded(byMe = false) }
                    ReactionPicker(current = "😂", onPick = {}, onDismiss = {})
                }
            }
        }
        compose.onNodeWithText("The stranger ended this chat").assertExists()
        compose.onNodeWithText("Remove reaction").assertExists()
        captureScreenRoboImage("build/outputs/roborazzi/reactions.png")
    }

    @Test
    fun dailyThemeAndAgeGate() {
        compose.setContent {
            GlobeSwapTheme {
                Box(Modifier.padding(0.dp)) {
                    AgeGateScreen(onResult = {})
                    DailyTheme("The view from your window", Modifier.padding(top = 780.dp, start = 24.dp))
                }
            }
        }
        compose.onNodeWithText("When were you born?").assertExists()
        compose.onNodeWithText("Day").assertExists()
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("The view from your window").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/age-and-theme.png")
    }
}
