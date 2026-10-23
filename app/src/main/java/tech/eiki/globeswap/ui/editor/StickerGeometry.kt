package tech.eiki.globeswap.ui.editor

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lage eines Stickers in Pixeln der Foto-Fläche: Mittelpunkt, halbe Breite/Höhe (mit Skalierung)
 * und Drehung in Grad. Damit werden Treffer, Rahmen und Anfasser berechnet.
 */
internal data class StickerBox(val center: Offset, val halfW: Float, val halfH: Float, val rotation: Float) {

    /** Liegt [point] innerhalb des (gedrehten) Stickers, mit [pad] Pixeln Spielraum? */
    fun contains(point: Offset, pad: Float): Boolean {
        val local = (point - center).rotateBy(-rotation)
        return kotlin.math.abs(local.x) <= halfW + pad && kotlin.math.abs(local.y) <= halfH + pad
    }

    /** Ecke des Rahmens; [sx]/[sy] = -1 oder +1 wählen die Ecke (z. B. +1/+1 = unten rechts). */
    fun corner(sx: Float, sy: Float, pad: Float): Offset =
        center + Offset(sx * (halfW + pad), sy * (halfH + pad)).rotateBy(rotation)

    fun frame(pad: Float): List<Offset> =
        listOf(corner(-1f, -1f, pad), corner(1f, -1f, pad), corner(1f, 1f, pad), corner(-1f, 1f, pad))
}

internal fun stickerBox(sticker: Sticker, layoutW: Int, layoutH: Int, boxW: Float, boxH: Float) = StickerBox(
    center = Offset(sticker.center.x * boxW, sticker.center.y * boxH),
    halfW = layoutW * sticker.scale / 2f,
    halfH = layoutH * sticker.scale / 2f,
    rotation = sticker.rotation,
)

internal fun Offset.rotateBy(degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    return Offset(x * c - y * s, x * s + y * c)
}

/** Winkel des Vektors in Grad. */
internal fun Offset.angle(): Float = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()

/** Winkeldifferenz auf -180..180 normiert (damit ein Sprung über ±180° nicht dreht). */
internal fun angleDelta(from: Float, to: Float): Float {
    var d = (to - from) % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

const val MIN_STICKER_SCALE = 0.3f
const val MAX_STICKER_SCALE = 8f
