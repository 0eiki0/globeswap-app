package tech.eiki.globeswap.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.Countries
import tech.eiki.globeswap.data.MatchPref
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.UserSettings
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.WorldCountry
import tech.eiki.globeswap.ui.onboarding.OnboardingScreen
import tech.eiki.globeswap.ui.settings.LanguageSection
import tech.eiki.globeswap.ui.settings.SettingsContent
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.ui.world.WorldContent
import tech.eiki.globeswap.ui.world.WorldShapes
import tech.eiki.globeswap.util.AppLanguage
import java.util.Locale

/**
 * Die weiteren Sprachen (alles außer Englisch und Deutsch, siehe [AppLanguage.LANGUAGES]): Texte kommen aus der richtigen
 * Sprachdatei, und Screenshots je Sprache zur Prüfung der Textlängen (build/outputs/roborazzi/<sprache>-*).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class OtherLanguagesTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() = ServiceLocator.initForTest(context, "http://127.0.0.1:9")

    /** Sprache, eine Zeile aus dem Onboarding und Japan als Ländername. */
    private fun check(language: String, rules: String, japan: String) {
        val world = World(
            listOf(WorldCountry("JP", "AS", 12), WorldCountry("BR", "SA", 5), WorldCountry("DE", "EU", 1)),
            continents = 3,
            percentile = 80,
        )
        var screen by mutableIntStateOf(0)
        compose.setContent {
            GlobeSwapTheme {
                when (screen) {
                    0 -> OnboardingScreen(onAccept = {}, restore = { "" })
                    1 -> Surface {
                        Column {
                            LanguageSection(language, onSelect = {})
                            SettingsContent(UserSettings(true, MatchPref.FAR, "JP", geoEnabled = true), {}, {}, {})
                        }
                    }
                    else -> Surface { WorldContent(world, WorldShapes.load(context)) }
                }
            }
        }
        compose.onNodeWithText(rules).assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$language-onboarding.png")
        assertEquals(japan, Countries.name("JP"))
        // Einstellungen und Weltkarte nur als Bild – lange Texte dürfen das Layout nicht sprengen
        screen = 1
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$language-settings.png")
        screen = 2
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$language-world.png")
    }

    @Test @Config(qualifiers = "+es")
    fun spanish() = check("es", "Cumpliré las reglas.", "Japón")

    @Test @Config(qualifiers = "+fr")
    fun french() = check("fr", "Je respecterai les règles.", "Japon")

    @Test @Config(qualifiers = "+in")
    fun indonesian() = check("id", "Aku akan mematuhi aturan.", "Jepang")

    @Test @Config(qualifiers = "+it")
    fun italian() = check("it", "Rispetterò le regole.", "Giappone")

    @Test @Config(qualifiers = "+pl")
    fun polish() = check("pl", "Będę przestrzegać zasad.", "Japonia")

    @Test @Config(qualifiers = "+pt-rBR")
    fun portuguese() = check("pt", "Vou seguir as regras.", "Japão")

    @Test @Config(qualifiers = "+tr")
    fun turkish() = check("tr", "Kurallara uyacağım.", "Japonya")

    /** Jede Sprache lässt sich in den Einstellungen wählen und liefert eigene Texte; „Swap!“ bleibt überall gleich. */
    @Test
    fun everyLanguageCanBeChosen() {
        compose.setContent { GlobeSwapTheme { Surface { Column { LanguageSection(AppLanguage.SYSTEM, onSelect = {}) } } } }
        try {
            AppLanguage.set(context, AppLanguage.ENGLISH)
            val english = AppLanguage.wrap(context).getString(R.string.swap_with)
            AppLanguage.LANGUAGES.forEach { language ->
                compose.onNodeWithText(language.name).assertExists()
                AppLanguage.set(context, language.code)
                val wrapped = AppLanguage.wrap(context)
                if (language.code != AppLanguage.ENGLISH) assertNotEquals(language.code, english, wrapped.getString(R.string.swap_with))
                assertEquals("Swap!", wrapped.getString(R.string.swap_confirm))
                assertTrue(language.code, AppLanguage.isSupported(Locale.getDefault()))
            }
        } finally {
            AppLanguage.set(context, AppLanguage.SYSTEM)
        }
    }

    /** Ländernamen gibt es nur in Sprachen der App – sonst Englisch wie die übrigen Texte. */
    @Test
    fun countryNamesFallBackToEnglish() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("nl"))
            assertEquals("Germany", Countries.name("DE"))
            Locale.setDefault(Locale.forLanguageTag("id"))
            assertEquals("Jerman", Countries.name("DE"))
        } finally {
            Locale.setDefault(before)
        }
    }
}
