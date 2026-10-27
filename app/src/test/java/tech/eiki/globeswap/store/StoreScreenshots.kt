package tech.eiki.globeswap.store

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.ImageLoader
import coil.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.data.Conversation
import tech.eiki.globeswap.ui.conversations.ConversationsScreen
import tech.eiki.globeswap.ui.editor.PhotoEditor
import tech.eiki.globeswap.ui.onboarding.OnboardingScreen
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.util.ImageUtils
import kotlin.random.Random

private const val OUT = "build/outputs/store-raw"

/** Rohbilder für die Play-Store-Screenshots (werden danach mit Überschrift gerahmt). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StoreScreenshots {
    @get:Rule val compose = createComposeRule()

    @Test
    fun editorWithText() {
        compose.setContent {
            GlobeSwapTheme { PhotoEditor(jpeg = ImageUtils.encodeJpeg(Scenes.sunset()), confirmLabel = "Swap!", onRetake = {}, onConfirm = { _, _ -> }) }
        }
        compose.onNodeWithContentDescription("Add text").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Sunset in Lisbon!")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithContentDescription("Text: Sunset in Lisbon!").performTouchInput {
            swipe(center, center + Offset(0f, -330f), durationMillis = 300)
        }
        compose.onNodeWithContentDescription("Add sticker").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("😍").performClick()
        compose.onNodeWithContentDescription("Sticker: 😍").performTouchInput {
            swipe(center, center + Offset(170f, 420f), durationMillis = 300)
        }
        // Auswahl aufheben, damit kein Rahmen im Bild ist
        compose.onNodeWithContentDescription("Photo").performTouchInput { click(Offset(width * 0.15f, height * 0.9f)) }
        compose.onRoot().captureRoboImage("$OUT/1-editor.png")
    }

    @Test
    fun editorStickerSelected() {
        compose.setContent {
            GlobeSwapTheme { PhotoEditor(jpeg = ImageUtils.encodeJpeg(Scenes.mountains()), confirmLabel = "Reply", onRetake = {}, onConfirm = { _, _ -> }) }
        }
        compose.onNodeWithContentDescription("Add sticker").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("😎").performClick()
        compose.onNodeWithContentDescription("Resize and rotate").performTouchInput {
            swipe(center, center + Offset(90f, 60f), durationMillis = 300)
        }
        compose.onNodeWithContentDescription("Draw").performClick()
        compose.onNodeWithContentDescription("Photo").performTouchInput {
            swipe(Offset(width * 0.12f, height * 0.78f), Offset(width * 0.88f, height * 0.74f), durationMillis = 400)
        }
        compose.onNodeWithContentDescription("Draw").performClick()
        compose.onNodeWithContentDescription("Sticker: 😎").performClick()
        compose.onRoot().captureRoboImage("$OUT/3-sticker.png")
    }

    @OptIn(ExperimentalCoilApi::class)
    @Test
    fun chatList() {
        val context = ApplicationProvider.getApplicationContext<Application>()
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
        val conversations = ids.mapIndexed { i, id ->
            Conversation("c$i", id, now - listOf(2, 14, 55, 190, 900)[i] * 60_000L, i % 2 == 1, "scene$i", unread = i < 2, blocked = false)
        }
        compose.setContent {
            GlobeSwapTheme { ConversationsScreen(conversations, photoUrl = { it }, onOpen = {}, onDeleteAccount = {}) }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$OUT/2-chats.png")
    }

    @Test
    fun onboarding() {
        compose.setContent { GlobeSwapTheme { OnboardingScreen(onAccept = {}) } }
        compose.onRoot().captureRoboImage("$OUT/4-onboarding.png")
    }
}

/** Gezeichnete Beispielfotos (Hochformat 1200×1600). */
object Scenes {
    private const val W = 1200
    private const val H = 1600

    private fun canvas(top: Int, bottom: Int, block: Canvas.(Paint) -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = null
        c.block(p)
        return bmp
    }

    private fun Canvas.ridge(p: Paint, color: Int, baseY: Float, amp: Float, seed: Int) {
        val r = Random(seed)
        val path = Path().apply {
            moveTo(0f, H.toFloat())
            lineTo(0f, baseY)
            var x = 0f
            while (x <= W) {
                lineTo(x, baseY - r.nextFloat() * amp)
                x += 60f + r.nextFloat() * 120f
            }
            lineTo(W.toFloat(), baseY)
            lineTo(W.toFloat(), H.toFloat())
            close()
        }
        p.color = color
        drawPath(path, p)
    }

    private fun Canvas.glow(p: Paint, x: Float, y: Float, r: Float, color: Int) {
        p.shader = RadialGradient(x, y, r * 2.4f, intArrayOf(color, color and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        drawCircle(x, y, r * 2.4f, p)
        p.shader = null
        p.color = color
        drawCircle(x, y, r, p)
    }

    fun sunset() = canvas(0xFF2B1B5A.toInt(), 0xFFFF8C42.toInt()) { p ->
        glow(p, 600f, 930f, 150f, 0xFFFFD27A.toInt())
        p.shader = LinearGradient(0f, 1000f, 0f, H.toFloat(), 0xFF1E3A6B.toInt(), 0xFF0B1A33.toInt(), Shader.TileMode.CLAMP)
        drawRect(0f, 1000f, W.toFloat(), H.toFloat(), p)
        p.shader = null
        p.color = 0x66FFD27A
        for (i in 0 until 9) drawRect(600f - 160f + i * 12f, 1030f + i * 40f, 600f + 160f - i * 12f, 1040f + i * 40f, p)
        ridge(p, 0xFF140C2A.toInt(), 1010f, 60f, 3)
    }

    fun mountains() = canvas(0xFF7EC8F2.toInt(), 0xFFE8F4FB.toInt()) { p ->
        glow(p, 900f, 280f, 90f, 0xFFFFF6D5.toInt())
        ridge(p, 0xFF9DB5CF.toInt(), 900f, 420f, 11)
        ridge(p, 0xFF5E7C99.toInt(), 1100f, 300f, 12)
        ridge(p, 0xFF2F5D3A.toInt(), 1320f, 160f, 13)
        p.color = 0xFF234A2C.toInt(); drawRect(0f, 1400f, W.toFloat(), H.toFloat(), p)
    }

    fun city() = canvas(0xFF0B1030.toInt(), 0xFF3A2A6E.toInt()) { p ->
        val r = Random(5)
        p.color = 0xFFFFFFFF.toInt()
        repeat(80) { drawCircle(r.nextFloat() * W, r.nextFloat() * 700f, 1.5f + r.nextFloat() * 2.5f, p) }
        glow(p, 260f, 300f, 70f, 0xFFF1EBD0.toInt())
        var x = 0f
        while (x < W) {
            val w = 90f + r.nextFloat() * 120f
            val h = 380f + r.nextFloat() * 620f
            p.color = 0xFF151A3D.toInt(); drawRect(x, H - h, x + w, H.toFloat(), p)
            p.color = 0xFFFFD36B.toInt()
            var wy = H - h + 30f
            while (wy < H - 40f) {
                var wx = x + 14f
                while (wx < x + w - 24f) { if (r.nextFloat() < 0.45f) drawRect(wx, wy, wx + 14f, wy + 20f, p); wx += 28f }
                wy += 42f
            }
            x += w + 8f
        }
    }

    fun beach() = canvas(0xFF4FC3F7.toInt(), 0xFFB3E5FC.toInt()) { p ->
        glow(p, 250f, 260f, 110f, 0xFFFFF59D.toInt())
        p.color = 0xFF0288D1.toInt(); drawRect(0f, 820f, W.toFloat(), 1150f, p)
        p.color = 0xFF4FC3F7.toInt(); repeat(6) { drawRect(0f, 860f + it * 50f, W.toFloat(), 866f + it * 50f, p) }
        p.color = 0xFFFFE0A3.toInt(); drawRect(0f, 1150f, W.toFloat(), H.toFloat(), p)
        p.color = 0xFF6D4C41.toInt(); drawRect(880f, 700f, 910f, 1300f, p)
        p.color = 0xFF2E7D32.toInt()
        for (a in 0 until 6) { save(); rotate(-70f + a * 28f, 895f, 700f); drawOval(895f, 680f, 1135f, 720f, p); restore() }
    }

    fun meadow() = canvas(0xFF90CAF9.toInt(), 0xFFFFFFFF.toInt()) { p ->
        p.color = 0xFFFFFFFF.toInt()
        drawOval(150f, 250f, 520f, 380f, p); drawOval(700f, 180f, 1050f, 300f, p)
        ridge(p, 0xFF66BB6A.toInt(), 1000f, 140f, 21)
        ridge(p, 0xFF43A047.toInt(), 1200f, 100f, 22)
        val r = Random(9)
        repeat(90) {
            p.color = listOf(0xFFFFEB3B, 0xFFFFFFFF, 0xFFE91E63, 0xFFFF9800)[it % 4].toInt()
            drawCircle(r.nextFloat() * W, 1150f + r.nextFloat() * 450f, 6f + r.nextFloat() * 8f, p)
        }
    }
}
