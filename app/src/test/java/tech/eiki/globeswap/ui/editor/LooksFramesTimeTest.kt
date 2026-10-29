package tech.eiki.globeswap.ui.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.eiki.globeswap.store.Scenes
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

/** Neue Filter-Looks, Rahmen und Uhrzeit-Sticker; Übersicht als Bild in build/outputs/roborazzi/editor-*.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class LooksFramesTimeTest {
    private val scene: Bitmap get() = Bitmap.createScaledBitmap(Scenes.sunset(), 300, 400, true)

    @Test
    fun everyLookChangesThePhotoAndKeepsItsSize() {
        val src = scene
        val tiles = PhotoFilter.entries.map { f ->
            val out = f.look?.let { LookRenderer.apply(src, it) } ?: src
            assertEquals(src.width, out.width)
            assertEquals(src.height, out.height)
            if (f != PhotoFilter.ORIGINAL) assertTrue("$f verändert nichts", !out.sameAs(src))
            out
        }
        sheet(tiles, "editor-filters.png")
    }

    @Test
    fun filterNamesFitTheServer() {
        // Server nimmt nur A–Z mit 2–12 Zeichen an (achievements.go)
        PhotoFilter.entries.forEach { assertTrue(it.name, Regex("^[A-Z]{2,12}$").matches(it.name)) }
        assertTrue("genug Filter für den Erfolg (7 verschiedene)", PhotoFilter.entries.size - 1 >= 7)
    }

    @Test
    fun framesDrawOverThePhoto() {
        val tiles = PhotoFrame.entries.map { frame ->
            val bmp = scene.copy(Bitmap.Config.ARGB_8888, true)
            FrameRenderer.draw(Canvas(bmp), frame, bmp.width.toFloat(), bmp.height.toFloat(), "29 OCT 2026")
            if (frame != PhotoFrame.NONE) assertNotEquals("$frame zeichnet nichts", scene.getPixel(2, 2), bmp.getPixel(2, 2))
            bmp
        }
        sheet(tiles, "editor-frames.png")
        // Polaroid: unten breiter weißer Rand
        val polaroid = scene.copy(Bitmap.Config.ARGB_8888, true)
        FrameRenderer.draw(Canvas(polaroid), PhotoFrame.POLAROID, 300f, 400f, "")
        assertEquals(0xFFFAFAF5.toInt(), polaroid.getPixel(150, 380))
    }

    @Test
    fun timeStickerTexts() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 29, 14, 32) }
        assertEquals("14:32", TimeSticker.text(TimeStyle.DIGITAL, now, true, "It's %1\$s here", Locale.ENGLISH))
        assertEquals("2:32 PM", TimeSticker.text(TimeStyle.DIGITAL, now, false, "It's %1\$s here", Locale.ENGLISH))
        assertEquals("'26 10 29  14:32", TimeSticker.text(TimeStyle.RETRO, now, true, "", Locale.ENGLISH))
        assertEquals("🕑 14:32\nThu, 29 Oct", TimeSticker.text(TimeStyle.CLOCK, now, true, "", Locale.ENGLISH))
        assertEquals("Hier ist es 14:32 ☀️", TimeSticker.text(TimeStyle.HERE, now, true, "Hier ist es %1\$s", Locale.GERMAN))
        assertEquals(TimeStyle.RETRO, TimeSticker.next(TimeStyle.DIGITAL))
        assertEquals(TimeStyle.DIGITAL, TimeSticker.next(TimeStyle.HERE))
    }

    @Test
    fun renderedPhotoWithEverything() {
        val src = Bitmap.createScaledBitmap(Scenes.mountains(), 900, 1600, true)
        val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 29, 7, 4) }
        val state = EditorState().apply {
            filter = PhotoFilter.FILM
            frame = PhotoFrame.POSTCARD
        }
        val tiles = TimeStyle.entries.map { style ->
            state.stickers.clear()
            state.addSticker(StickerKind.TIME, TimeSticker.text(style, now, true, "It's %1\$s here", Locale.ENGLISH), Color.White, Offset(0.45f, 0.7f), style)
            EditorRenderer.render(src, state).also { assertEquals(900, it.width); assertEquals(1600, it.height) }
        }
        sheet(tiles.map { Bitmap.createScaledBitmap(it, 300, 533, true) }, "editor-time.png")
    }

    private fun sheet(tiles: List<Bitmap>, name: String) {
        val tw = tiles.first().width
        val th = tiles.first().height
        val cols = minOf(8, tiles.size)
        val rows = (tiles.size + cols - 1) / cols
        val out = Bitmap.createBitmap(cols * (tw + 8), rows * (th + 8), Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(0xFF222222.toInt())
        tiles.forEachIndexed { i, t -> c.drawBitmap(t, (i % cols) * (tw + 8f), (i / cols) * (th + 8f), Paint()) }
        val dir = File("build/outputs/roborazzi").apply { mkdirs() }
        FileOutputStream(File(dir, name)).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
