package tech.eiki.globeswap.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import kotlin.math.ceil

/** Rechnet Filter, Rahmen, Zeichnungen und Sticker in voller Auflösung in das Foto ein. */
object EditorRenderer {

    fun render(source: Bitmap, state: EditorState): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()
        val out = state.filter.look?.let { LookRenderer.apply(source, it) }
            ?: source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        FrameRenderer.draw(canvas, state.frame, w, h, state.frameDate)

        for (stroke in state.strokes.toList()) drawStroke(canvas, stroke, w, h)

        for (sticker in state.stickers.toList()) {
            canvas.save()
            canvas.translate(sticker.center.x * w, sticker.center.y * h)
            canvas.rotate(sticker.rotation)
            canvas.scale(sticker.scale, sticker.scale)
            val layout = stickerLayout(sticker, w)
            canvas.translate(-layout.width / 2f, -layout.height / 2f)
            layout.draw(canvas)
            canvas.restore()
        }
        return out
    }

    /** Wird auch von der Vorschau verwendet, damit Vorschau und Ergebnis gleich aussehen. */
    fun drawStroke(canvas: Canvas, stroke: Stroke, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = stroke.width * w
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(stroke.toPath(w, h), paint)
    }

    /** Text, Emoji oder Uhrzeit als fertig umbrochenes Layout; [imageWidth] bestimmt die Schriftgröße. */
    fun stickerLayout(sticker: Sticker, imageWidth: Float): StaticLayout {
        val emoji = sticker.kind == StickerKind.EMOJI
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (if (emoji) EMOJI_SIZE else TEXT_SIZE) * imageWidth
            color = sticker.color.toArgb()
            if (!emoji) {
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(textSize * 0.08f, 0f, textSize * 0.04f, 0x99000000.toInt())
            }
            if (sticker.kind == StickerKind.TIME) when (sticker.timeStyle) {
                TimeStyle.DIGITAL -> {
                    textSize = TEXT_SIZE * 2.2f * imageWidth
                    letterSpacing = -0.02f
                    setShadowLayer(textSize * 0.06f, 0f, textSize * 0.03f, 0x99000000.toInt())
                }
                TimeStyle.RETRO -> {
                    // orange Leuchtziffern wie der Datumsstempel alter Kameras
                    textSize = TEXT_SIZE * 0.85f * imageWidth
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    color = 0xFFFF9F1C.toInt()
                    setShadowLayer(textSize * 0.3f, 0f, 0f, 0xCCFF6A00.toInt())
                }
                TimeStyle.CLOCK -> textSize = TEXT_SIZE * 1.05f * imageWidth
                TimeStyle.HERE -> textSize = TEXT_SIZE * 0.95f * imageWidth
            }
        }
        val width = ceil(sticker.text.split('\n').maxOf { paint.measureText(it) }).toInt().coerceAtLeast(1)
        return StaticLayout.Builder.obtain(sticker.text, 0, sticker.text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()
    }
}

/** Pfad einer Linie in Pixeln; ein einzelner Punkt wird als kurzer Strich gezeichnet (runder Klecks). */
internal fun Stroke.toPath(w: Float, h: Float): Path = Path().apply {
    val first = points.first()
    moveTo(first.x * w, first.y * h)
    if (points.size == 1) {
        lineTo(first.x * w + 0.1f, first.y * h)
    } else {
        for (p in points.drop(1)) lineTo(p.x * w, p.y * h)
    }
}
