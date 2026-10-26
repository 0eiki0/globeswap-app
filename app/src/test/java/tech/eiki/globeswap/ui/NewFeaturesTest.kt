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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.data.ApiException
import tech.eiki.globeswap.data.MatchPref
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.Message
import tech.eiki.globeswap.data.UserSettings
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.WorldCountry
import tech.eiki.globeswap.data.displayName
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

    /** Fehlermeldungen kommen aus den Sprachdateien und brauchen dafür den App-Kontext. */
    @Before
    fun setUp() = ServiceLocator.initForTest(context, "http://127.0.0.1:9")

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
            percentile = 80,
        )
        compose.setContent { GlobeSwapTheme { Surface { WorldContent(world, shapes) } } }
        compose.onNodeWithText("You've seen more of the world than 80% of GlobeSwap users.").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/world.png")
        val list = compose.onNode(androidx.compose.ui.test.hasScrollAction())
        // 1 Länder-Meilenstein, 4 Kontinente, "10 photos" (21 Fotos) = 6 von 14
        list.performScrollToNode(androidx.compose.ui.test.hasText("Achievements · 6 of 14"))
        list.performScrollToNode(androidx.compose.ui.test.hasText("Hello Asia"))
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/world-achievements.png")
        list.performScrollToNode(androidx.compose.ui.test.hasText("Japan"))
        compose.onNodeWithText("12 photos").assertExists()
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
    fun restoreForm() {
        var restoredWith: String? = null
        var newCode: String? = null
        compose.setContent {
            GlobeSwapTheme {
                Surface {
                    tech.eiki.globeswap.ui.settings.RestoreForm(
                        restore = { restoredWith = it; if (it.startsWith("AAAA")) throw ApiException(404, "invalid_code") else "NEW2-CODE-ABCD-EFGH" },
                        onRestored = { newCode = it },
                    )
                }
            }
        }
        compose.onNodeWithText("Restore").assertIsNotEnabled()
        val field = compose.onNode(androidx.compose.ui.test.hasSetTextAction())
        field.performTextInput("aaaa-bbbb-cccc-dddd")
        compose.onNodeWithText("Restore").assertIsEnabled().performClick()
        compose.onNodeWithText("This backup code is not valid. Please check it and try again.").assertExists()
        field.performTextClearance()
        field.performTextInput("k7qm 2xpa 9rtd hv4e")
        compose.onNodeWithText("Restore").performClick()
        compose.waitForIdle()
        assertEquals("K7QM 2XPA 9RTD HV4E", restoredWith)
        assertEquals("NEW2-CODE-ABCD-EFGH", newCode)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun backupCodeDialog() {
        compose.setContent {
            GlobeSwapTheme { tech.eiki.globeswap.ui.settings.BackupCodeDialog("K7QM-2XPA-9RTD-HV4E", "Your backup code", onDismiss = {}) }
        }
        compose.onNodeWithText("K7QM-2XPA-9RTD-HV4E").assertExists()
        compose.onNodeWithText("Copy").assertExists()
        captureScreenRoboImage("build/outputs/roborazzi/backup-code.png")
    }

    @Test
    fun onboardingOffersRestore() {
        compose.setContent { GlobeSwapTheme { tech.eiki.globeswap.ui.onboarding.OnboardingScreen(onAccept = {}, restore = { "" }) } }
        compose.onNodeWithText("Used GlobeSwap before? Restore with backup code").performScrollTo().assertExists()
    }

    @Test
    fun nameEditor() {
        val saved = mutableListOf<String>()
        compose.setContent {
            GlobeSwapTheme { Surface { Column { tech.eiki.globeswap.ui.settings.NameEditor("Alice", { saved += it }, error = null) } } }
        }
        compose.onNodeWithText("Save").assertIsNotEnabled() // unverändert
        compose.onNodeWithText("Remove name").performClick()
        assertEquals(listOf(""), saved)
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("  Bob ")
        compose.onNodeWithText("Save").performClick()
        assertEquals(listOf("", "Bob"), saved)
    }

    @Test
    fun nameShownInsteadOfStranger() {
        lateinit var state: androidx.compose.foundation.lazy.LazyListState
        compose.setContent {
            state = rememberLazyListState()
            GlobeSwapTheme {
                Surface {
                    MessageList(messages(2), maxPhotos = 0, photoUrl = { "" }, listState = state, otherName = "Alice", modifier = Modifier.height(900.dp))
                }
            }
        }
        compose.onNodeWithText("Alice", substring = true).assertExists()
        val conversation = tech.eiki.globeswap.data.Conversation("c", "u", 0, false, "p", unread = false, blocked = false)
        assertEquals("Stranger", conversation.displayName("Stranger"))
        assertEquals("Alice", conversation.copy(name = "Alice").displayName("Stranger"))
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
