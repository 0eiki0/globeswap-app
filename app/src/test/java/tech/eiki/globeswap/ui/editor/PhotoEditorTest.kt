package tech.eiki.globeswap.ui.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.util.ImageUtils
import java.io.File
import kotlin.math.abs

private const val OUT = "build/outputs/roborazzi"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class PhotoEditorTest {
    @get:Rule val compose = createComposeRule()

    /** Buntes Testfoto im Hochformat (Farbverlauf), wie es die Kamera liefern würde. */
    private fun testPhoto(w: Int = 900, h: Int = 1200): ByteArray {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFFFF8A50.toInt(), 0xFF2E7BE6.toInt(), Shader.TileMode.CLAMP)
        }
        Canvas(bitmap).drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return ImageUtils.encodeJpeg(bitmap)
    }

    private fun Bitmap.save(name: String) {
        File(OUT).mkdirs()
        File(OUT, name).outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun isGray(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return abs(r - g) < 12 && abs(g - b) < 12
    }

    @Test
    fun rendererDrawsFilterStrokesAndStickers() {
        val source = BitmapFactory.decodeByteArray(testPhoto(), 0, testPhoto().size)
        // eigener Snapshot, damit keine offene Änderung im globalen Snapshot hängen bleibt (siehe LooksFramesTimeTest)
        val snapshot = Snapshot.takeMutableSnapshot()
        snapshot.enter { runRendererChecks(source) }
        snapshot.dispose()
    }

    private fun runRendererChecks(source: Bitmap) {
        val state = EditorState().apply {
            filter = PhotoFilter.MONO
            addStroke(Stroke(listOf(Offset(0.1f, 0.5f), Offset(0.9f, 0.5f)), Color(0xFFFF3B30), BrushSize.LARGE.width))
            addSticker(StickerKind.TEXT, "Hello from Berlin", Color.White, Offset(0.5f, 0.2f))
            addSticker(StickerKind.EMOJI, "🔥", Color.Black, Offset(0.5f, 0.75f))
        }
        val result = EditorRenderer.render(source, state)
        result.save("renderer.png")

        assertEquals(900, result.width)
        assertEquals(1200, result.height)
        // Filter: Foto ist schwarz-weiß …
        assertTrue("Foto sollte grau sein", isGray(result.getPixel(80, 1100)))
        // … die rote Linie quer durch die Mitte aber nicht.
        val stroke = result.getPixel(450, 600)
        assertEquals(Color(0xFFFF3B30).toArgb(), stroke)
        // Text oben in der Mitte: irgendwo in der Zeile sind weiße Pixel.
        assertTrue("Text fehlt", (300..600).any { x -> result.getPixel(x, 240) == android.graphics.Color.WHITE })

        state.undo() // Emoji weg
        state.undo() // Text weg
        state.undo() // Linie weg
        assertTrue(state.strokes.isEmpty() && state.stickers.isEmpty() && !state.canUndo)
    }

    @Test
    fun editFlowTextDrawFilterAndSend() {
        var sent: ByteArray? = null
        val photo = testPhoto()
        compose.setContent {
            GlobeSwapTheme {
                PhotoEditor(jpeg = photo, confirmLabel = "Swap!", onRetake = {}, onConfirm = { jpeg, _ -> sent = jpeg })
            }
        }
        compose.onRoot().captureRoboImage("$OUT/1-editor.png")

        // Text hinzufügen
        compose.onNodeWithContentDescription("Add text").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Greetings from Germany!")
        compose.onRoot().captureRoboImage("$OUT/2-text-input.png")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithContentDescription("Text: Greetings from Germany!").assertExists()

        // Text nach oben ziehen
        compose.onNodeWithContentDescription("Text: Greetings from Germany!").performTouchInput {
            swipe(center, center + Offset(0f, -500f), durationMillis = 300)
        }

        // Emoji hinzufügen (Auswahl im Bottom Sheet)
        compose.onNodeWithContentDescription("Add sticker").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("🔥").performClick()
        compose.onNodeWithContentDescription("Sticker: 🔥").assertExists()

        // Zeichnen: Pinsel wählen, Linie über das Foto ziehen
        compose.onNodeWithContentDescription("Draw").performClick()
        compose.onRoot().captureRoboImage("$OUT/3-draw-panel.png")
        compose.onNodeWithContentDescription("Photo").performTouchInput {
            swipe(Offset(width * 0.15f, height * 0.65f), Offset(width * 0.85f, height * 0.6f), durationMillis = 400)
        }
        compose.onNodeWithContentDescription("Draw").performClick()

        // Filter wählen
        compose.onNodeWithContentDescription("Filters").performClick()
        compose.onRoot().captureRoboImage("$OUT/4-filters.png")
        compose.onNodeWithContentDescription("Golden").performClick()
        compose.onNodeWithContentDescription("Filters").performClick()
        compose.onRoot().captureRoboImage("$OUT/5-edited.png")

        // Senden
        compose.onNodeWithText("Swap!").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { sent != null }
        val result = BitmapFactory.decodeByteArray(sent, 0, sent!!.size)
        assertNotNull(result)
        assertEquals(900, result.width)
        assertEquals(1200, result.height)
        result.save("6-sent-photo.png")
    }

    @Test
    fun dragStickerToTrashDeletesIt() {
        compose.setContent {
            GlobeSwapTheme { PhotoEditor(jpeg = testPhoto(), confirmLabel = "Swap!", onRetake = {}, onConfirm = { _, _ -> }) }
        }
        compose.onNodeWithContentDescription("Add sticker").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("😎").performClick()
        compose.onAllNodesWithContentDescription("Sticker: 😎").assertCountEquals(1)
        compose.onNodeWithContentDescription("Sticker: 😎").performTouchInput {
            swipe(center, center + Offset(0f, 2000f), durationMillis = 400)
        }
        compose.onAllNodesWithContentDescription("Sticker: 😎").assertCountEquals(0)
    }

    @Test
    fun unchangedPhotoIsSentAsIs() {
        var sent: ByteArray? = null
        val photo = testPhoto()
        compose.setContent {
            GlobeSwapTheme { PhotoEditor(jpeg = photo, confirmLabel = "Reply", onRetake = {}, onConfirm = { jpeg, _ -> sent = jpeg }) }
        }
        compose.onNodeWithText("Reply").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { sent != null }
        assertTrue("ohne Bearbeitung soll das Original gesendet werden", sent!!.contentEquals(photo))
    }

    // ---------------------------------------------------------------- Drehen und Größe ändern

    private fun SemanticsNodeInteraction.sizeAndRotation(): Pair<Int, Int> {
        val state = fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        val m = Regex("""Size (\d+) %, rotated (\d+)°""").find(state) ?: error("unbekannter Zustand: $state")
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }

    private fun openEditorWithEmoji(emoji: String = "😎") {
        compose.setContent {
            GlobeSwapTheme { PhotoEditor(jpeg = testPhoto(), confirmLabel = "Swap!", onRetake = {}, onConfirm = { _, _ -> }) }
        }
        compose.onNodeWithContentDescription("Add sticker").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(emoji).performClick()
    }

    @Test
    fun cornerHandleResizesAndRotates() {
        openEditorWithEmoji()
        val sticker = compose.onNodeWithContentDescription("Sticker: 😎")
        assertEquals(100 to 0, sticker.sizeAndRotation())
        // Neuer Sticker ist ausgewählt: Anfasser und Löschen-Knopf sind da.
        compose.onNodeWithContentDescription("Resize and rotate").assertExists()
        compose.onNodeWithContentDescription("Remove").assertExists()
        compose.onRoot().captureRoboImage("$OUT/7-selected-sticker.png")

        // Anfasser vom Mittelpunkt weg ziehen: doppelter Abstand = doppelte Größe, ohne Drehung.
        compose.onNodeWithContentDescription("Resize and rotate").performTouchInput {
            swipe(center, center + Offset(220f, 220f), durationMillis = 400)
        }
        val (bigger, rot0) = sticker.sizeAndRotation()
        assertTrue("Sticker sollte größer werden: $bigger %", bigger > 150)
        assertTrue("Ohne Drehbewegung keine Drehung: $rot0°", rot0 <= 3 || rot0 >= 357)

        // Anfasser um den Mittelpunkt herum (90° im Uhrzeigersinn) führen: dreht, Größe bleibt.
        val handle = compose.onNodeWithContentDescription("Resize and rotate").fetchSemanticsNode().boundsInRoot.center
        val middle = compose.onNodeWithContentDescription("Sticker: 😎").fetchSemanticsNode().let {
            // Mittelpunkt des Stickers = Mitte seiner (gedrehten) Begrenzung
            it.boundsInRoot.center
        }
        val v = handle - middle
        compose.onNodeWithContentDescription("Resize and rotate").performTouchInput {
            val start = center
            down(start)
            for (i in 1..12) {
                val a = Math.toRadians(90.0 * i / 12)
                val rotated = Offset(
                    (v.x * Math.cos(a) - v.y * Math.sin(a)).toFloat(),
                    (v.x * Math.sin(a) + v.y * Math.cos(a)).toFloat(),
                )
                moveTo(start + (rotated - v))
            }
            up()
        }
        val (sizeAfterTurn, rotation) = sticker.sizeAndRotation()
        assertTrue("Drehung um ~90° erwartet, war $rotation°", rotation in 80..100)
        assertTrue("Größe soll beim Drehen gleich bleiben: $bigger → $sizeAfterTurn", kotlin.math.abs(sizeAfterTurn - bigger) < 15)
        compose.onRoot().captureRoboImage("$OUT/8-resized-rotated.png")
    }

    @Test
    fun pinchWithTwoFingersAnywhereScalesSelectedSticker() {
        openEditorWithEmoji("🔥")
        val sticker = compose.onNodeWithContentDescription("Sticker: 🔥")
        // Beide Finger unten auf dem Foto, weit weg vom (kleinen) Sticker.
        compose.onNodeWithContentDescription("Photo").performTouchInput {
            val y = height * 0.8f
            pinch(
                start0 = Offset(width * 0.4f, y), end0 = Offset(width * 0.15f, y),
                start1 = Offset(width * 0.6f, y), end1 = Offset(width * 0.85f, y),
                durationMillis = 400,
            )
        }
        val (size, _) = sticker.sizeAndRotation()
        assertTrue("Pinch auf dem Foto sollte den Sticker vergrößern: $size %", size > 200)
    }

    @Test
    fun twoFingerTwistRotatesSelectedSticker() {
        openEditorWithEmoji("🔥")
        val sticker = compose.onNodeWithContentDescription("Sticker: 🔥")
        compose.onNodeWithContentDescription("Photo").performTouchInput {
            val c = Offset(width * 0.5f, height * 0.75f)
            val r = width * 0.2f
            down(0, c + Offset(-r, 0f))
            down(1, c + Offset(r, 0f))
            for (i in 1..10) {
                val a = Math.toRadians(45.0 * i / 10)
                val d = Offset((r * Math.cos(a)).toFloat(), (r * Math.sin(a)).toFloat())
                moveTo(0, c - d)
                moveTo(1, c + d)
            }
            up(0)
            up(1)
        }
        val (size, rotation) = sticker.sizeAndRotation()
        assertTrue("Drehung um ~45° erwartet, war $rotation°", rotation in 38..52)
        assertTrue("Größe soll gleich bleiben, war $size %", size in 90..110)
    }

    @Test
    fun tapSelectsDeselectsAndRemoveButtonDeletes() {
        openEditorWithEmoji("😍")
        compose.onNodeWithContentDescription("Resize and rotate").assertExists()
        // Tippen auf freie Fläche hebt die Auswahl auf …
        compose.onNodeWithContentDescription("Photo").performTouchInput { click(Offset(width * 0.1f, height * 0.95f)) }
        compose.onNodeWithContentDescription("Resize and rotate").assertDoesNotExist()
        // … Tippen auf den Sticker wählt ihn wieder aus.
        compose.onNodeWithContentDescription("Sticker: 😍").performClick()
        compose.onNodeWithContentDescription("Resize and rotate").assertExists()
        // Löschen-Knopf entfernt ihn.
        compose.onNodeWithContentDescription("Remove").performClick()
        compose.onAllNodesWithContentDescription("Sticker: 😍").assertCountEquals(0)
    }

    @Test
    fun tappingSelectedTextOpensTextEditor() {
        compose.setContent {
            GlobeSwapTheme { PhotoEditor(jpeg = testPhoto(), confirmLabel = "Swap!", onRetake = {}, onConfirm = { _, _ -> }) }
        }
        compose.onNodeWithContentDescription("Add text").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Hi")
        compose.onNodeWithText("Done").performClick()
        // Neuer Text ist ausgewählt → erneutes Antippen öffnet die Bearbeitung.
        compose.onNodeWithContentDescription("Text: Hi").performClick()
        compose.onNodeWithText("Done").assertExists()
    }
}
