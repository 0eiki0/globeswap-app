package tech.eiki.globeswap.ui.editor

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.min

/**
 * Zeichnet die Rahmen direkt über das Foto (Vorschau und fertiges Foto mit demselben Code).
 * Alle Maße relativ zur Bildbreite [w]; [date] erscheint im Poststempel der Postkarte.
 */
object FrameRenderer {

    fun draw(canvas: Canvas, frame: PhotoFrame, w: Float, h: Float, date: String) {
        when (frame) {
            PhotoFrame.NONE -> Unit
            PhotoFrame.POLAROID -> polaroid(canvas, w, h)
            PhotoFrame.POSTCARD -> postcard(canvas, w, h, date)
            PhotoFrame.AIRMAIL -> airmail(canvas, w, h)
            PhotoFrame.FILMSTRIP -> filmstrip(canvas, w, h)
            PhotoFrame.POP -> pop(canvas, w, h)
        }
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    /** Fläche zwischen Außenrand und innerem Ausschnitt. */
    private fun border(canvas: Canvas, w: Float, h: Float, inner: RectF, paint: Paint, innerRadius: Float = 0f) {
        val path = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, w, h, Path.Direction.CW)
            addRoundRect(inner, innerRadius, innerRadius, Path.Direction.CW)
        }
        canvas.drawPath(path, paint)
    }

    private fun polaroid(canvas: Canvas, w: Float, h: Float) {
        val side = w * 0.05f
        val inner = RectF(side, side, w - side, h - w * 0.2f)
        border(canvas, w, h, inner, fill(0xFFFAFAF5.toInt()))
        // feine Schattenkante am Foto
        canvas.drawRect(inner, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = w * 0.003f; color = 0x22000000 })
    }

    private fun postcard(canvas: Canvas, w: Float, h: Float, date: String) {
        val b = w * 0.045f
        border(canvas, w, h, RectF(b, b, w - b, h - b), fill(0xFFFFF8EC.toInt()))
        // Briefmarke oben rechts mit gezacktem Rand
        val sw = w * 0.2f
        val sh = sw * 1.2f
        val stamp = RectF(w - b - sw - w * 0.03f, b + w * 0.03f, w - b - w * 0.03f, b + w * 0.03f + sh)
        canvas.drawRect(stamp, fill(0xFFFFFFFF.toInt()))
        val r = sw * 0.035f
        val holes = fill(0xFFFFF8EC.toInt())
        var x = stamp.left
        while (x <= stamp.right) {
            canvas.drawCircle(x, stamp.top, r, holes); canvas.drawCircle(x, stamp.bottom, r, holes); x += r * 3
        }
        var y = stamp.top
        while (y <= stamp.bottom) {
            canvas.drawCircle(stamp.left, y, r, holes); canvas.drawCircle(stamp.right, y, r, holes); y += r * 3
        }
        val art = RectF(stamp.left + sw * 0.1f, stamp.top + sw * 0.1f, stamp.right - sw * 0.1f, stamp.bottom - sw * 0.1f)
        canvas.drawRect(art, fill(0xFF1E6FD9.toInt()))
        val emoji = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sw * 0.42f; textAlign = Paint.Align.CENTER }
        canvas.drawText("🌍", art.centerX(), art.centerY() + emoji.textSize * 0.2f, emoji)
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sw * 0.1f; textAlign = Paint.Align.CENTER; color = 0xFFFFFFFF.toInt(); typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("GLOBESWAP", art.centerX(), art.bottom - sw * 0.08f, small)
        // Poststempel mit Datum, überlappt die Marke
        val cx = stamp.left - sw * 0.05f
        val cy = stamp.top + sh * 0.55f
        val radius = sw * 0.38f
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = w * 0.004f; color = 0xCC3A3A44.toInt() }
        canvas.drawCircle(cx, cy, radius, ink)
        canvas.drawCircle(cx, cy, radius * 0.78f, ink)
        val stampText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = radius * 0.24f; textAlign = Paint.Align.CENTER; color = 0xCC3A3A44.toInt(); typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(date, cx, cy + stampText.textSize * 0.35f, stampText)
        for (i in 0..3) {
            val ly = cy - radius * 0.5f + i * radius * 0.33f
            val wave = Path().apply {
                moveTo(cx - radius * 2.6f, ly)
                var px = cx - radius * 2.6f
                var up = true
                while (px < cx - radius) {
                    quadTo(px + radius * 0.15f, ly + (if (up) -1 else 1) * radius * 0.12f, px + radius * 0.3f, ly); px += radius * 0.3f; up = !up
                }
            }
            canvas.drawPath(wave, ink)
        }
    }

    private fun airmail(canvas: Canvas, w: Float, h: Float) {
        val b = w * 0.06f
        val inner = RectF(b, b, w - b, h - b)
        border(canvas, w, h, inner, fill(0xFFFFFFFF.toInt()))
        // rot-blaue Schrägstreifen im Rand
        canvas.save()
        canvas.clipPath(Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(b * 0.2f, b * 0.2f, w - b * 0.2f, h - b * 0.2f, Path.Direction.CW)
            addRect(RectF(b * 0.8f, b * 0.8f, w - b * 0.8f, h - b * 0.8f), Path.Direction.CW)
        })
        val stripe = b * 1.2f
        val paints = listOf(fill(0xFFE53935.toInt()), fill(0xFFFFFFFF.toInt()), fill(0xFF1E4FB0.toInt()), fill(0xFFFFFFFF.toInt()))
        var x = -h
        var i = 0
        while (x < w + h) {
            canvas.drawPath(Path().apply { moveTo(x, 0f); lineTo(x + stripe, 0f); lineTo(x + stripe - h, h); lineTo(x - h, h); close() }, paints[i % 4])
            x += stripe; i++
        }
        canvas.restore()
    }

    private fun filmstrip(canvas: Canvas, w: Float, h: Float) {
        val band = w * 0.1f
        val black = fill(0xFF111111.toInt())
        canvas.drawRect(0f, 0f, band, h, black)
        canvas.drawRect(w - band, 0f, w, h, black)
        canvas.drawRect(0f, 0f, w, w * 0.015f, black)
        canvas.drawRect(0f, h - w * 0.015f, w, h, black)
        val holeW = band * 0.42f
        val holeH = band * 0.3f
        val holePaint = fill(0xFFEDE7DA.toInt())
        var y = band * 0.3f
        while (y + holeH < h) {
            for (x in listOf((band - holeW) / 2f, w - band + (band - holeW) / 2f)) {
                canvas.drawRoundRect(RectF(x, y, x + holeW, y + holeH), holeH * 0.25f, holeH * 0.25f, holePaint)
            }
            y += holeH * 2.2f
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = band * 0.28f; color = 0xFFF4A21E.toInt(); typeface = Typeface.MONOSPACE; isFakeBoldText = true
        }
        canvas.save()
        canvas.rotate(90f)
        canvas.drawText("GLOBESWAP 400  ▸ 12A", h * 0.08f, -(w - band * 0.12f), label)
        canvas.restore()
    }

    private fun pop(canvas: Canvas, w: Float, h: Float) {
        val b = w * 0.06f
        border(canvas, w, h, RectF(b, b, w - b, h - b), fill(0xFFFFD54F.toInt()), innerRadius = min(w, h) * 0.06f)
    }
}
