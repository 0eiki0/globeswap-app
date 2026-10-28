package tech.eiki.globeswap.store

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.StringRes
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.AchievementData
import tech.eiki.globeswap.data.Conversation
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.WorldCountry
import tech.eiki.globeswap.ui.conversations.ConversationsScreen
import tech.eiki.globeswap.ui.editor.PhotoEditor
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.ui.world.WorldContent
import tech.eiki.globeswap.ui.world.WorldShapes

private const val WEB_OUT = "build/outputs/website"

/**
 * Bilder für die Homepage (globeswap.de), je Sprache: Editor, Chats, Weltkarte – nur gezeichnete Beispielfotos.
 * Danach mit Fenster zuschneiden und als WebP in den Server (`web/`) kopieren.
 */
abstract class WebsiteScreenshots(private val lang: String) {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private fun s(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    @Before
    fun setUp() = ServiceLocator.initForTest(context, "http://127.0.0.1:9")

    @Test
    fun editor() {
        val text = if (lang == "de") "Abendrot in Lissabon!" else "Sunset in Lisbon!"
        compose.setContent {
            GlobeSwapTheme { PhotoEditor(jpeg = tech.eiki.globeswap.util.ImageUtils.encodeJpeg(Scenes.sunset()), confirmLabel = "Swap!", onRetake = {}, onConfirm = { _, _ -> }) }
        }
        compose.onNodeWithContentDescription(s(R.string.add_text)).performClick()
        compose.onNode(hasSetTextAction()).performTextInput(text)
        compose.onNodeWithText(s(R.string.done)).performClick()
        compose.onNodeWithContentDescription(s(R.string.sticker_desc, s(R.string.kind_text), text)).performTouchInput {
            swipe(center, center + Offset(0f, -330f), durationMillis = 300)
        }
        compose.onNodeWithContentDescription(s(R.string.add_sticker)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("😍").performClick()
        compose.onNodeWithContentDescription(s(R.string.sticker_desc, s(R.string.kind_sticker), "😍")).performTouchInput {
            swipe(center, center + Offset(170f, 420f), durationMillis = 300)
        }
        compose.onNodeWithContentDescription(s(R.string.photo)).performTouchInput { click(Offset(width * 0.15f, height * 0.9f)) }
        compose.onRoot().captureRoboImage("$WEB_OUT/$lang-editor.png")
    }

    @OptIn(ExperimentalCoilApi::class)
    @Test
    fun chats() {
        val scenes = listOf(Scenes.city(), Scenes.beach(), Scenes.sunset(), Scenes.mountains(), Scenes.meadow())
        val engine = FakeImageLoaderEngine.Builder().apply {
            scenes.forEachIndexed { i, bmp -> intercept("scene$i", BitmapDrawable(context.resources, bmp)) }
        }.build()
        Coil.setImageLoader(ImageLoader.Builder(context).components { add(engine) }.build())
        val now = System.currentTimeMillis()
        val ids = listOf(
            "3f2a1c9e-7b4d-4e0a-9c1f-2d8e6b5a4c3f", "b81e0d47-52c6-4f39-a8d2-91c7e3f04b5a", "0c9d5e6f-1a2b-4c3d-8e4f-5a6b7c8d9e0f",
            "7d41a9c2-3e8f-4b6a-9d0c-1f2e3a4b5c6d", "e5f60718-293a-4b4c-8d5e-6f708192a3b4",
        )
        val countries = listOf("JP", "BR", "PT", "CA", "NZ")
        val conversations = ids.mapIndexed { i, id ->
            Conversation(
                "c$i", id, now - listOf(2, 14, 55, 190, 900)[i] * 60_000L, i % 2 == 1, "scene$i",
                unread = i < 2, blocked = false, country = countries[i],
            )
        }
        compose.setContent {
            GlobeSwapTheme { ConversationsScreen(conversations, photoUrl = { it }, onOpen = {}, onDeleteAccount = {}) }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$WEB_OUT/$lang-chats.png")
    }

    @Test
    fun world() {
        val world = World(
            listOf(
                WorldCountry("JP", "AS", 12), WorldCountry("BR", "SA", 5), WorldCountry("PT", "EU", 4), WorldCountry("CA", "NA", 3),
                WorldCountry("NZ", "OC", 2), WorldCountry("KE", "AF", 2), WorldCountry("IN", "AS", 3), WorldCountry("US", "NA", 6),
                WorldCountry("AU", "OC", 1), WorldCountry("MX", "NA", 2), WorldCountry("NO", "EU", 1),
            ),
            continents = 6,
            percentile = 85,
            achievements = AchievementData(home = "DE"),
        )
        compose.setContent { GlobeSwapTheme { Surface { WorldContent(world, WorldShapes.load(context)) } } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$WEB_OUT/$lang-world.png")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class WebsiteScreenshotsEn : WebsiteScreenshots("en")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "de-w411dp-h891dp-xxhdpi")
class WebsiteScreenshotsDe : WebsiteScreenshots("de")
