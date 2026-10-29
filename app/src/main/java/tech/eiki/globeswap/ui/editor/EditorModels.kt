package tech.eiki.globeswap.ui.editor

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import tech.eiki.globeswap.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Alle Positionen und Größen sind relativ zum Bild gespeichert (0..1), damit Vorschau
// und das fertig gerenderte Foto in voller Auflösung exakt übereinstimmen.

/** Schriftgröße eines Textes und Größe eines Emojis, relativ zur Bildbreite. */
const val TEXT_SIZE = 0.075f
const val EMOJI_SIZE = 0.2f

/** Eine gezeichnete Linie. [points] relativ zur Bildgröße, [width] relativ zur Bildbreite. */
data class Stroke(val points: List<Offset>, val color: Color, val width: Float)

enum class StickerKind { TEXT, EMOJI, TIME }

/** Stile des Uhrzeit-Stickers; Antippen des ausgewählten Stickers wechselt zum nächsten. */
enum class TimeStyle { DIGITAL, RETRO, CLOCK, HERE }

/** Text, Emoji oder Uhrzeit auf dem Foto, frei verschieb-, skalier- und drehbar. */
data class Sticker(
    val id: Long,
    val kind: StickerKind,
    val text: String,
    val color: Color,
    val center: Offset,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val timeStyle: TimeStyle = TimeStyle.DIGITAL,
)

enum class BrushSize(val label: String, val width: Float) {
    SMALL("S", 0.008f),
    MEDIUM("M", 0.016f),
    LARGE("L", 0.032f),
}

val EditorColors = listOf(
    Color.White,
    Color.Black,
    Color(0xFFFF3B30),
    Color(0xFFFF9500),
    Color(0xFFFFCC00),
    Color(0xFF34C759),
    Color(0xFF1E6FD9),
    Color(0xFFAF52DE),
    Color(0xFFFF2D92),
)

val EditorEmojis = listOf(
    "😀", "😂", "🥹", "😍", "🥰", "😎", "🤩", "😜", "🤔", "😴", "😱", "🥳",
    "😇", "🙃", "😬", "🤯", "😭", "😡", "🤗", "🫶", "👍", "👋", "🙌", "👏",
    "✌️", "🤘", "💪", "🙏", "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "💯",
    "🔥", "✨", "⭐", "🌈", "☀️", "🌙", "⚡", "❄️", "🌊", "🌍", "🌸", "🌻",
    "🌴", "🍀", "🐶", "🐱", "🦊", "🐼", "🦄", "🐝", "🍕", "🍔", "🍦", "🍩",
    "☕", "🍺", "🎉", "🎈", "🎁", "📸", "🎵", "⚽", "🏖️", "✈️", "🚀", "🏠",
)

/**
 * Filter-Looks (siehe [Look]). Die Namen gehen als Merkmal an den Server (Erfolg „Filter-Fan“): nur A–Z, 2–12 Zeichen,
 * bestehende Namen nie ändern.
 */
enum class PhotoFilter(@StringRes val label: Int, val look: Look?) {
    ORIGINAL(R.string.filter_original, null),
    VIVID(R.string.filter_vivid, Look(matrix = saturation(1.5f), contrast = 1.12f)),
    GOLDEN(R.string.filter_golden, Look(matrix = channels(r = 1.12f, g = 1.03f, b = 0.82f, offset = 10f), glow = 0.3f, leak = 0xFFB347, vignette = 0.18f)),
    FILM(
        R.string.filter_film,
        Look(
            matrix = channels(r = 1.06f, g = 1.0f, b = 0.9f, offset = 4f), contrast = 1.15f, lift = 0.06f,
            shadowTint = 0x2A6F7A, shadowAmount = 0.35f, highlightTint = 0xFFB870, highlightAmount = 0.25f, grain = 0.35f, vignette = 0.2f,
        ),
    ),
    CINEMA(
        R.string.filter_cinema,
        Look(saturation = 1.1f, contrast = 1.2f, shadowTint = 0x00A3A3, shadowAmount = 0.5f, highlightTint = 0xFF8A3D, highlightAmount = 0.4f, vignette = 0.3f),
    ),
    WARM(R.string.filter_warm, Look(matrix = channels(r = 1.10f, g = 1.02f, b = 0.85f, offset = 8f), vignette = 0.12f)),
    COOL(R.string.filter_cool, Look(matrix = channels(r = 0.88f, g = 1.0f, b = 1.12f, offset = 4f))),
    MATTE(R.string.filter_matte, Look(saturation = 0.85f, contrast = 0.95f, lift = 0.12f, highlightTint = 0xFFE0C0, highlightAmount = 0.2f)),
    DREAMY(R.string.filter_dreamy, Look(saturation = 0.9f, lift = 0.08f, glow = 0.45f, highlightTint = 0xFFD6F0, highlightAmount = 0.3f)),
    RETRO(
        R.string.filter_retro,
        Look(matrix = channels(r = 1.08f, g = 0.98f, b = 0.84f, offset = 12f), lift = 0.1f, grain = 0.45f, leak = 0xFF5E5E, vignette = 0.25f),
    ),
    VINTAGE(R.string.filter_vintage, Look(matrix = vintage(), vignette = 0.35f, grain = 0.25f)),
    FADE(R.string.filter_fade, Look(matrix = channels(r = 0.8f, g = 0.8f, b = 0.8f, offset = 34f))),
    DUOTONE(R.string.filter_duotone, Look(duotone = 0x1E2A78 to 0xFF7EB3, contrast = 1.1f)),
    NIGHT(R.string.filter_night, Look(matrix = channels(r = 0.85f, g = 0.95f, b = 1.15f, offset = -6f), saturation = 0.9f, contrast = 1.25f, vignette = 0.55f)),
    MONO(R.string.filter_mono, Look(matrix = saturation(0f))),
    NOIR(R.string.filter_noir, Look(matrix = noir(), vignette = 0.4f, grain = 0.2f)),
}

/** Rahmen ums Foto; werden über das Foto gezeichnet (Größe bleibt gleich), siehe [FrameRenderer]. */
enum class PhotoFrame(@StringRes val label: Int) {
    NONE(R.string.frame_none),
    POLAROID(R.string.frame_polaroid),
    POSTCARD(R.string.frame_postcard),
    AIRMAIL(R.string.frame_airmail),
    FILMSTRIP(R.string.frame_film),
    POP(R.string.frame_pop),
}

private const val LUM_R = 0.213f
private const val LUM_G = 0.715f
private const val LUM_B = 0.072f

private fun saturation(s: Float): FloatArray {
    val r = LUM_R * (1 - s)
    val g = LUM_G * (1 - s)
    val b = LUM_B * (1 - s)
    return floatArrayOf(
        r + s, g, b, 0f, 0f,
        r, g + s, b, 0f, 0f,
        r, g, b + s, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun channels(r: Float, g: Float, b: Float, offset: Float) = floatArrayOf(
    r, 0f, 0f, 0f, offset,
    0f, g, 0f, 0f, offset,
    0f, 0f, b, 0f, offset,
    0f, 0f, 0f, 1f, 0f,
)

/** Sepia, zu 75 % mit dem Original gemischt, leicht aufgehellt. */
private fun vintage(): FloatArray {
    val sepia = floatArrayOf(
        0.393f, 0.769f, 0.189f,
        0.349f, 0.686f, 0.168f,
        0.272f, 0.534f, 0.131f,
    )
    val m = FloatArray(20)
    for (row in 0..2) {
        for (col in 0..2) {
            val identity = if (row == col) 1f else 0f
            m[row * 5 + col] = 0.25f * identity + 0.75f * sepia[row * 3 + col]
        }
        m[row * 5 + 4] = 12f
    }
    m[18] = 1f
    return m
}

/** Schwarz-Weiß mit kräftigem Kontrast. */
private fun noir(): FloatArray {
    val contrast = 1.45f
    val offset = -0.22f * 255f
    val m = saturation(0f)
    for (row in 0..2) {
        for (col in 0..2) m[row * 5 + col] *= contrast
        m[row * 5 + 4] = offset
    }
    return m
}

private sealed interface EditAction {
    data class AddStroke(val stroke: Stroke) : EditAction
    data class AddSticker(val id: Long) : EditAction
}

/** Zustand des Editors: Filter, Zeichnungen, Sticker und Rückgängig-Verlauf. */
class EditorState {
    var filter by mutableStateOf(PhotoFilter.ORIGINAL)
    var frame by mutableStateOf(PhotoFrame.NONE)
    /** Datum im Poststempel des Postkarten-Rahmens, z. B. „29 OCT 2026“. */
    val frameDate: String = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date()).uppercase(Locale.ENGLISH)
    val strokes = mutableStateListOf<Stroke>()
    val stickers = mutableStateListOf<Sticker>()
    private val history = mutableStateListOf<EditAction>()
    private var nextId = 1L

    val canUndo: Boolean get() = history.isNotEmpty()

    fun addStroke(stroke: Stroke) {
        strokes += stroke
        history += EditAction.AddStroke(stroke)
    }

    fun addSticker(
        kind: StickerKind,
        text: String,
        color: Color,
        center: Offset = Offset(0.5f, 0.45f),
        timeStyle: TimeStyle = TimeStyle.DIGITAL,
    ): Long {
        val sticker = Sticker(nextId++, kind, text, color, center, timeStyle = timeStyle)
        stickers += sticker
        history += EditAction.AddSticker(sticker.id)
        return sticker.id
    }

    fun updateSticker(id: Long, change: (Sticker) -> Sticker) {
        val index = stickers.indexOfFirst { it.id == id }
        if (index >= 0) stickers[index] = change(stickers[index])
    }

    fun removeSticker(id: Long) {
        stickers.removeAll { it.id == id }
        history.removeAll { it is EditAction.AddSticker && it.id == id }
    }

    fun undo() {
        when (val last = history.removeLastOrNull()) {
            is EditAction.AddStroke -> strokes.remove(last.stroke)
            is EditAction.AddSticker -> stickers.removeAll { it.id == last.id }
            null -> Unit
        }
    }
}
