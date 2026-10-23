package tech.eiki.globeswap.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

// Alle Positionen und Größen sind relativ zum Bild gespeichert (0..1), damit Vorschau
// und das fertig gerenderte Foto in voller Auflösung exakt übereinstimmen.

/** Schriftgröße eines Textes und Größe eines Emojis, relativ zur Bildbreite. */
const val TEXT_SIZE = 0.075f
const val EMOJI_SIZE = 0.2f

/** Eine gezeichnete Linie. [points] relativ zur Bildgröße, [width] relativ zur Bildbreite. */
data class Stroke(val points: List<Offset>, val color: Color, val width: Float)

enum class StickerKind { TEXT, EMOJI }

/** Text oder Emoji auf dem Foto, frei verschieb-, skalier- und drehbar. */
data class Sticker(
    val id: Long,
    val kind: StickerKind,
    val text: String,
    val color: Color,
    val center: Offset,
    val scale: Float = 1f,
    val rotation: Float = 0f,
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

/** Farbfilter als 4×5-Farbmatrix (gleiches Format für Compose und android.graphics). */
enum class PhotoFilter(val label: String, val matrix: FloatArray?) {
    ORIGINAL("Original", null),
    VIVID("Vivid", saturation(1.6f)),
    WARM("Warm", channels(r = 1.10f, g = 1.02f, b = 0.85f, offset = 8f)),
    COOL("Cool", channels(r = 0.88f, g = 1.0f, b = 1.12f, offset = 4f)),
    VINTAGE("Vintage", vintage()),
    FADE("Fade", channels(r = 0.8f, g = 0.8f, b = 0.8f, offset = 34f)),
    MONO("B&W", saturation(0f)),
    NOIR("Noir", noir()),
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
    val strokes = mutableStateListOf<Stroke>()
    val stickers = mutableStateListOf<Sticker>()
    private val history = mutableStateListOf<EditAction>()
    private var nextId = 1L

    val canUndo: Boolean get() = history.isNotEmpty()

    fun addStroke(stroke: Stroke) {
        strokes += stroke
        history += EditAction.AddStroke(stroke)
    }

    fun addSticker(kind: StickerKind, text: String, color: Color, center: Offset = Offset(0.5f, 0.45f)): Long {
        val sticker = Sticker(nextId++, kind, text, color, center)
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
