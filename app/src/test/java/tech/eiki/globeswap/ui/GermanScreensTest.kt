package tech.eiki.globeswap.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.data.ApiException
import tech.eiki.globeswap.data.Conversation
import tech.eiki.globeswap.data.Countries
import tech.eiki.globeswap.data.InstallChannel
import tech.eiki.globeswap.data.LatestRelease
import tech.eiki.globeswap.data.MatchPref
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.UserSettings
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.WorldCountry
import tech.eiki.globeswap.data.userMessage
import tech.eiki.globeswap.ui.chat.ChatEnded
import tech.eiki.globeswap.ui.conversations.ConversationsScreen
import tech.eiki.globeswap.ui.onboarding.AgeGateScreen
import tech.eiki.globeswap.ui.onboarding.OnboardingScreen
import tech.eiki.globeswap.ui.settings.SettingsContent
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.ui.world.WorldContent
import tech.eiki.globeswap.ui.world.WorldShapes
import org.junit.Assert.assertEquals

/** Die App auf Deutsch: Texte kommen aus values-de, Screenshots zur Prüfung der Textlängen (build/outputs/roborazzi/de-*). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "de-w411dp-h891dp-xxhdpi")
class GermanScreensTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() = ServiceLocator.initForTest(context, "http://127.0.0.1:9")

    @Test
    fun onboarding() {
        compose.setContent { GlobeSwapTheme { OnboardingScreen(onAccept = {}, restore = { "" }) } }
        compose.onNodeWithText("Ich halte mich an die Regeln.").assertExists()
        compose.onNodeWithText("Los geht's").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/de-onboarding.png")
    }

    @Test
    fun conversations() {
        val now = System.currentTimeMillis()
        val conversations = listOf(
            Conversation("c1", "3f2a1c9e-7b4d-4e0a-9c1f-2d8e6b5a4c3f", now - 60_000, false, "p1", unread = true, blocked = false, country = "JP"),
            Conversation("c2", "b81e0d47-52c6-4f39-a8d2-91c7e3f04b5a", now - 3_600_000, true, "p2", unread = false, blocked = false),
            Conversation("c3", "0c9d5e6f-1a2b-4c3d-8e4f-5a6b7c8d9e0f", now - 86_400_000, false, "p3", unread = false, blocked = false, ended = true),
        )
        compose.setContent { GlobeSwapTheme { ConversationsScreen(conversations, photoUrl = { "" }, onOpen = {}, onDeleteAccount = {}) } }
        compose.onNodeWithText("Neues Foto für dich!").assertExists()
        compose.onNodeWithText("Warte auf ihr Foto").assertExists()
        compose.onNodeWithText("Chat beendet").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/de-conversations.png")
    }

    @Test
    fun settings() {
        compose.setContent {
            GlobeSwapTheme { Surface { Column { SettingsContent(UserSettings(true, MatchPref.FAR, "DE", geoEnabled = true), {}, {}, {}) } } }
        }
        compose.onNodeWithText("Mein Land anzeigen").assertExists()
        compose.onNodeWithText("Fremde sehen „🇩🇪 Deutschland“ bei deinen Fotos. Nur das Land, nie deinen Standort.").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/de-settings.png")
    }

    @Test
    fun world() {
        val world = World(
            listOf(WorldCountry("JP", "AS", 12), WorldCountry("BR", "SA", 5), WorldCountry("DE", "EU", 1)),
            continents = 3,
            percentile = 80,
        )
        compose.setContent { GlobeSwapTheme { Surface { WorldContent(world, WorldShapes.load(context)) } } }
        compose.onNodeWithText("Du hast mehr von der Welt gesehen als 80 % der GlobeSwap-Nutzer.").assertExists()
        compose.onNodeWithText("Länder").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/de-world.png")
    }

    @Test
    fun ageGateUpdateAndEndedChat() {
        compose.setContent {
            GlobeSwapTheme {
                Surface {
                    Column {
                        UpdateBanner(LatestRelease(112, "2.9"), InstallChannel.GITHUB, onUpdate = {}, onLater = {})
                        Column(Modifier.padding(16.dp)) { ChatEnded(byMe = false, otherName = "") }
                        AgeGateScreen(onResult = {})
                    }
                }
            }
        }
        compose.onNodeWithText("GlobeSwap 2.9 ist verfügbar").assertExists()
        compose.onNodeWithText("Die fremde Person hat diesen Chat beendet").assertExists()
        compose.onNodeWithText("Wann bist du geboren?").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/de-misc.png")
    }

    @Test
    fun errorsAndCountries() {
        assertEquals("Warte auf das Foto der fremden Person, bevor du antwortest.", ApiException(409, "not_your_turn").userMessage())
        assertEquals("🇯🇵 Japan", Countries.label("JP"))
        assertEquals("🇩🇪 Deutschland", Countries.label("DE"))
    }
}
