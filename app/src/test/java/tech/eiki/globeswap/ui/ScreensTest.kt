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
import tech.eiki.globeswap.data.InstallChannel
import tech.eiki.globeswap.data.LatestRelease
import tech.eiki.globeswap.data.Message
import tech.eiki.globeswap.ui.chat.DeletedPhoto
import tech.eiki.globeswap.ui.chat.PhotoLimitHint
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
    fun forcedUpdate() {
        compose.setContent {
            GlobeSwapTheme {
                ForcedUpdateScreen("Please update GlobeSwap to keep swapping.", "1.3 (108)", onUpdate = {}, onCheckAgain = {})
            }
        }
        compose.onNodeWithText("Update required").assertExists()
        compose.onNodeWithText("Please update GlobeSwap to keep swapping.").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/update-required.png")
    }

    @Test
    fun updateBanner() {
        compose.setContent {
            GlobeSwapTheme { UpdateBanner(LatestRelease(112, "1.5"), InstallChannel.FDROID, onUpdate = {}, onLater = {}) }
        }
        compose.onNodeWithText("GlobeSwap 1.5 is available").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/update-banner.png")
    }

    @Test
    fun photoLimitHint() {
        compose.setContent { GlobeSwapTheme { PhotoLimitHint(10) } }
        compose.onNodeWithText("Only the latest 10 photos are kept – older ones disappear for both of you.").assertExists()
    }

    @Test
    fun deletedPhotoPlaceholders() {
        compose.setContent {
            GlobeSwapTheme {
                androidx.compose.foundation.layout.Column {
                    DeletedPhoto(Message("m1", mine = false, photoId = "", createdAt = 0, deleted = true))
                    DeletedPhoto(Message("m2", mine = true, photoId = "", createdAt = 0, deleted = true))
                }
            }
        }
        compose.onNodeWithText("The stranger deleted a photo").assertExists()
        compose.onNodeWithText("You deleted a photo").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/deleted-photo.png")
    }

    @Test
    fun updateBannerDownloading() {
        compose.setContent {
            GlobeSwapTheme {
                UpdateBanner(
                    LatestRelease(130, "2.3"), InstallChannel.GITHUB, onUpdate = {}, onLater = {},
                    selfUpdate = tech.eiki.globeswap.update.SelfUpdateState.Downloading(0.45f),
                )
            }
        }
        compose.onNodeWithText("Downloading update… 45 %").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/update-downloading.png")
    }

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
        compose.onNodeWithText("New photo for you!").assertExists()
        compose.onNodeWithText("Waiting for their photo").assertExists()
        compose.onNodeWithText("Your turn").assertExists()
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
