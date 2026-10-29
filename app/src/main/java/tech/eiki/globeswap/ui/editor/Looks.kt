package tech.eiki.globeswap.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

/**
 * Ein Filter-Look: Farbmatrix, Kurven (Kontrast, matte Schwarztöne), Sättigung, Tönung von Schatten und Lichtern
 * oder Duoton – und Effekte darüber (Glow, Lichtleck, Vignette, Filmkorn). Alle Farben sind eigene Werte, keine
 * fremden LUT-Dateien. Werte relativ zur Bildgröße, damit Vorschau und fertiges Foto gleich aussehen.
 */
class Look(
    val matrix: FloatArray? = null,
    val saturation: Float = 1f,
    val contrast: Float = 1f,
    val lift: Float = 0f,
    val shadowTint: Int? = null,
    val shadowAmount: Float = 0f,
    val highlightTint: Int? = null,
    val highlightAmount: Float = 0f,
    val duotone: Pair<Int, Int>? = null,
    val glow: Float = 0f,
    val leak: Int? = null,
    val vignette: Float = 0f,
    val grain: Float = 0f,
)

object LookRenderer {

    /** Wendet [look] an und liefert ein neues Bild gleicher Größe. */
    fun apply(source: Bitmap, look: Look): Bitmap {
        val w = source.width
        val h = source.height
        val px = IntArray(w * h)
        source.getPixels(px, 0, w, 0, 0, w, h)
        val curve = curve(look.contrast, look.lift)
        val m = look.matrix
        val (sr, sg, sb) = rgb(look.shadowTint ?: 0x808080)
        val (hr, hg, hb) = rgb(look.highlightTint ?: 0x808080)
        val (d1r, d1g, d1b) = rgb(look.duotone?.first ?: 0)
        val (d2r, d2g, d2b) = rgb(look.duotone?.second ?: 0)
        for (i in px.indices) {
            val c = px[i]
            var r = ((c shr 16) and 255).toFloat()
            var g = ((c shr 8) and 255).toFloat()
            var b = (c and 255).toFloat()
            if (m != null) {
                val nr = m[0] * r + m[1] * g + m[2] * b + m[4]
                val ng = m[5] * r + m[6] * g + m[7] * b + m[9]
                val nb = m[10] * r + m[11] * g + m[12] * b + m[14]
                r = nr; g = ng; b = nb
            }
            var l = 0.299f * r + 0.587f * g + 0.114f * b
            if (look.saturation != 1f) {
                r = l + (r - l) * look.saturation
                g = l + (g - l) * look.saturation
                b = l + (b - l) * look.saturation
            }
            if (look.duotone != null) {
                val t = (l / 255f).coerceIn(0f, 1f)
                r = d1r + (d2r - d1r) * t
                g = d1g + (d2g - d1g) * t
                b = d1b + (d2b - d1b) * t
            }
            r = curve[clamp(r)].toFloat()
            g = curve[clamp(g)].toFloat()
            b = curve[clamp(b)].toFloat()
            if (look.shadowAmount > 0f || look.highlightAmount > 0f) {
                l = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val ws = (1f - l).pow(2) * look.shadowAmount
                val wh = l.pow(2) * look.highlightAmount
                r += (sr - 128f) * ws + (hr - 128f) * wh
                g += (sg - 128f) * ws + (hg - 128f) * wh
                b += (sb - 128f) * ws + (hb - 128f) * wh
            }
            px[i] = (c and -0x1000000) or (clamp(r) shl 16) or (clamp(g) shl 8) or clamp(b)
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, w, 0, 0, w, h)
        val canvas = Canvas(out)
        val wf = w.toFloat()
        val hf = h.toFloat()

        if (look.glow > 0f) {
            // Weiches Leuchten: stark verkleinert und wieder vergrößert (unscharf), aufgehellt darübergelegt
            val small = Bitmap.createScaledBitmap(out, max(1, w / 12), max(1, h / 12), true)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.SCREEN; alpha = (look.glow * 255).toInt() }
            canvas.drawBitmap(small, null, android.graphics.RectF(0f, 0f, wf, hf), paint)
        }
        look.leak?.let { color ->
            // Lichtleck von oben links wie bei alten Filmkameras
            val paint = Paint().apply {
                blendMode = BlendMode.SCREEN
                shader = LinearGradient(0f, 0f, wf * 0.75f, hf * 0.55f, (0xB0 shl 24) or (color and 0xFFFFFF), 0, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, wf, hf, paint)
        }
        if (look.vignette > 0f) {
            val paint = Paint().apply {
                shader = RadialGradient(
                    wf / 2f, hf / 2f, max(wf, hf) * 0.72f,
                    intArrayOf(0, 0, ((look.vignette * 255).toInt() shl 24)),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, wf, hf, paint)
        }
        if (look.grain > 0f) {
            // Filmkorn: fest vorgegebenes Rauschen (immer gleich), Korngröße relativ zur Bildbreite
            val paint = Paint().apply {
                blendMode = BlendMode.OVERLAY
                alpha = (look.grain * 255).toInt()
                shader = BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
                    setLocalMatrix(Matrix().apply { val s = max(1f, wf / 1200f); setScale(s, s) })
                }
            }
            canvas.drawRect(0f, 0f, wf, hf, paint)
        }
        return out
    }

    private val noise: Bitmap by lazy {
        val size = 128
        val random = Random(42)
        val px = IntArray(size * size) {
            val v = 128 + random.nextInt(-90, 91)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    }

    /** Tonkurve als Tabelle: S-Kurve für Kontrast, angehobenes Schwarz für matten Look. */
    private fun curve(contrast: Float, lift: Float): IntArray = IntArray(256) { i ->
        var x = i / 255f
        if (contrast != 1f) {
            // S-Kurve um die Mitte
            x = if (x < 0.5f) 0.5f * (2f * x).pow(contrast) else 1f - 0.5f * (2f * (1f - x)).pow(contrast)
        }
        x = lift + (1f - lift) * x
        (x * 255f).toInt().coerceIn(0, 255)
    }

    private fun rgb(c: Int) = Triple(((c shr 16) and 255).toFloat(), ((c shr 8) and 255).toFloat(), (c and 255).toFloat())

    private fun clamp(v: Float): Int = v.toInt().coerceIn(0, 255)
}
