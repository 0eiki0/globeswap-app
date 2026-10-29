package tech.eiki.globeswap.ui

import android.Manifest
import android.app.Application
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.AchievementData
import tech.eiki.globeswap.data.Conversation
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.WorldCountry
import tech.eiki.globeswap.store.Scenes
import tech.eiki.globeswap.store.ScreenWithBar
import tech.eiki.globeswap.ui.camera.PhotoCapture
import tech.eiki.globeswap.ui.conversations.ConversationsScreen
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.ui.world.WorldContent
import tech.eiki.globeswap.ui.world.WorldShapes

/** Hauptseiten im Design A mit Leiste unten, hell und dunkel (build/outputs/roborazzi/design-*). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class DesignScreensTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() = ServiceLocator.initForTest(context, "http://127.0.0.1:9")

    @Composable
    private fun WithBar(tab: Tab, content: @Composable () -> Unit) = ScreenWithBar(tab, content = content)

    @OptIn(ExperimentalCoilApi::class)
    private fun shots(dark: Boolean) {
        val mode = if (dark) "dark" else "light"
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
            Conversation("c$i", id, now - listOf(2, 14, 55, 190, 900)[i] * 60_000L, i % 2 == 1, "scene$i", unread = i < 2, blocked = false, country = countries[i], ended = i == 4)
        }
        val world = World(
            listOf(WorldCountry("JP", "AS", 12), WorldCountry("BR", "SA", 5), WorldCountry("PT", "EU", 4), WorldCountry("CA", "NA", 3),
                WorldCountry("NZ", "OC", 2), WorldCountry("KE", "AF", 2), WorldCountry("US", "NA", 6)),
            continents = 6, percentile = 85, achievements = AchievementData(home = "DE"),
        )
        val screenState = androidx.compose.runtime.mutableIntStateOf(0)
        compose.setContent {
            GlobeSwapTheme(darkTheme = dark) {
                when (screenState.intValue) {
                    0 -> WithBar(Tab.CHATS) { ConversationsScreen(conversations, photoUrl = { it }, onOpen = {}, onDeleteAccount = {}) }
                    1 -> WithBar(Tab.WORLD) {
                        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) { Column {
                            ScreenTitle(androidx.compose.ui.res.stringResource(R.string.your_world))
                            WorldContent(world, WorldShapes.load(context))
                        } }
                    }
                    else -> WithBar(Tab.SWAP) {
                        PhotoCapture(confirmLabel = "Swap!", onConfirm = { _, _ -> }, hint = "Take a photo – it goes to a stranger.", theme = "Your breakfast")
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-chats-$mode.png")
        screenState.intValue = 1; compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-world-$mode.png")
        screenState.intValue = 2; compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-camera-$mode.png")
    }

    @Test fun light() {
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.CAMERA)
        shots(dark = false)
    }

    @Test fun dark() {
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.CAMERA)
        shots(dark = true)
    }
}
