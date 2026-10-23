package tech.eiki.globeswap.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.test.onNodeWithText
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.Conversation
import tech.eiki.globeswap.ui.conversations.AboutDialog
import tech.eiki.globeswap.ui.conversations.ConversationsScreen
import tech.eiki.globeswap.ui.onboarding.OnboardingScreen
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import java.io.File

/** Screenshots einzelner Bildschirme zur Sichtprüfung (build/outputs/roborazzi). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun onboarding() {
        compose.setContent { GlobeSwapTheme { OnboardingScreen(onAccept = {}) } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/onboarding.png")
    }

    @Test
    fun conversationsShowUniqueIds() {
        val now = System.currentTimeMillis()
        val conversations = listOf(
            Conversation("c1", "3f2a1c9e-7b4d-4e0a-9c1f-2d8e6b5a4c3f", now - 60_000, false, "p1", unread = true, blocked = false),
            Conversation("c2", "b81e0d47-52c6-4f39-a8d2-91c7e3f04b5a", now - 3_600_000, true, "p2", unread = false, blocked = false),
            Conversation("c3", "0c9d5e6f-1a2b-4c3d-8e4f-5a6b7c8d9e0f", now - 86_400_000, false, "p3", unread = false, blocked = false),
        )
        compose.setContent {
            GlobeSwapTheme { ConversationsScreen(conversations, photoUrl = { "" }, onOpen = {}, onDeleteAccount = {}) }
        }
        conversations.forEach { compose.onNodeWithText(it.label).assertExists() }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/conversations.png")
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun aboutDialog() {
        compose.setContent {
            GlobeSwapTheme { AboutDialog(publicId = "3f2a1c9e-7b4d-4e0a-9c1f-2d8e6b5a4c3f", onDismiss = {}) }
        }
        compose.onNodeWithText("3f2a1c9e-7b4d-4e0a-9c1f-2d8e6b5a4c3f").assertExists()
        compose.onNodeWithText("Privacy Policy").assertExists()
        compose.onNodeWithText("Deleting your data").assertExists()
        captureScreenRoboImage("build/outputs/roborazzi/about.png")
    }

    @Test
    fun launcherIcon() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val icon = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
        val bitmap = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 432, 432)
        icon.draw(Canvas(bitmap))
        File("build/outputs/roborazzi").mkdirs()
        File("build/outputs/roborazzi/icon.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
