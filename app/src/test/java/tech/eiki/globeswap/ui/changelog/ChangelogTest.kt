package tech.eiki.globeswap.ui.changelog

import android.app.Application
import androidx.compose.material3.Surface
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ChangelogTest {
    @get:Rule val compose = createComposeRule()

    private val text = """
        # Kommentar
        ## Next
        - New thing

        ## Next-but-empty-is-ignored

        ## 2.6 (2026-10-25)
        - Zoom
        - Wide

        ## 2.5 (2026-10-24)
        - Fix
    """.trimIndent()

    @Test
    fun nextBelongsToTheInstalledReleaseBuild() {
        val entries = Changelog.parse(text, installedVersion = "2.7")
        assertEquals(listOf("2.7", "2.6", "2.5"), entries.map { it.version })
        assertTrue(entries[0].current)
        assertEquals(listOf("New thing"), entries[0].changes)
        assertEquals(LocalDate.of(2026, 10, 25), entries[1].date)
        assertEquals(listOf("Zoom", "Wide"), entries[1].changes)
    }

    @Test
    fun nextIsUpcomingInDevelopmentBuilds() {
        // Entwicklungs-Build mit Versionsname einer schon erschienenen Version: Next bleibt "Coming next".
        val entries = Changelog.parse(text, installedVersion = "2.6")
        assertEquals(listOf(Changelog.UPCOMING, "2.6", "2.5"), entries.map { it.version })
        assertFalse(entries[0].current)
        assertTrue(entries[1].current)
    }

    @Test
    fun bundledChangelogIsComplete() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val entries = Changelog.parse(context.assets.open("changelog.md").bufferedReader().readText(), "99.0")
        val versions = entries.map { it.version }
        for (v in listOf("1.3", "1.4", "1.5", "2.0", "2.6")) assertTrue("Version $v fehlt", v in versions)
        assertTrue("jede Version hat Punkte und ein Datum", entries.drop(1).all { it.changes.isNotEmpty() && it.date != null })
    }

    @Test
    fun changelogScreen() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val entries = Changelog.parse(context.assets.open("changelog.md").bufferedReader().readText(), "2.7")
        compose.setContent { GlobeSwapTheme { Surface { ChangelogList(entries) } } }
        // Neuere Versionen stehen darüber – erst zur installierten Version scrollen
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Version 2.7"))
        compose.onNodeWithText("Version 2.7").assertExists()
        compose.onNodeWithText("installed").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/changelog.png")
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun whatsNewDialog() {
        compose.setContent {
            GlobeSwapTheme {
                WhatsNewDialog(ChangelogEntry("2.7", null, listOf("Backup code to get your chats back", "This page")), onDismiss = {}, onSeeAll = {})
            }
        }
        compose.onNodeWithText("What's new in 2.7").assertExists()
        captureScreenRoboImage("build/outputs/roborazzi/whats-new.png")
    }
}
