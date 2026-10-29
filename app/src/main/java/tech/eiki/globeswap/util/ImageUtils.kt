package tech.eiki.globeswap.util

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageUtils {
    private const val MAX_EDGE = 1600
    private const val JPEG_QUALITY = 85

    /**
     * Dreht, spiegelt (Frontkamera) und verkleinert das Kamerabild und liefert ein JPEG.
     * Mit [aspect] (Breite/Höhe, z. B. die des Suchers) wird mittig darauf zugeschnitten.
     */
    fun toJpeg(source: Bitmap, rotationDegrees: Int, mirror: Boolean, aspect: Float = 0f): ByteArray {
        val scale = min(1f, MAX_EDGE.toFloat() / max(source.width, source.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotationDegrees.toFloat())
            if (mirror) postScale(-1f, 1f)
        }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val result = if (aspect > 0f) centerCrop(rotated, aspect) else rotated
        return ByteArrayOutputStream().use { out ->
            result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    /** Schneidet mittig auf das Seitenverhältnis [aspect] (Breite/Höhe) zu. */
    fun centerCrop(bitmap: Bitmap, aspect: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        return if (w.toFloat() / h > aspect) {
            val cw = (h * aspect).roundToInt().coerceIn(1, w)
            Bitmap.createBitmap(bitmap, (w - cw) / 2, 0, cw, h)
        } else {
            val ch = (w / aspect).roundToInt().coerceIn(1, h)
            Bitmap.createBitmap(bitmap, 0, (h - ch) / 2, w, ch)
        }
    }

    fun encodeJpeg(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        out.toByteArray()
    }
}
