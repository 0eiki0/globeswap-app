package tech.eiki.globeswap.ui.editor

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Text des Uhrzeit-Stickers – Uhrzeit des Handys beim Bearbeiten (kurz nach dem Foto).
 * [is24h] wie am Handy eingestellt, [hereFormat] z. B. „It's %1$s here“ aus den Texten der App.
 */
object TimeSticker {

    fun text(style: TimeStyle, now: Calendar, is24h: Boolean, hereFormat: String, locale: Locale = Locale.getDefault()): String {
        val time = SimpleDateFormat(if (is24h) "HH:mm" else "h:mm a", locale).format(now.time)
        return when (style) {
            TimeStyle.DIGITAL -> time
            // Datumsstempel wie bei alten Analogkameras: '26 10 29
            TimeStyle.RETRO -> SimpleDateFormat("''yy MM dd", Locale.ROOT).format(now.time) + "  " + SimpleDateFormat("HH:mm", Locale.ROOT).format(now.time)
            TimeStyle.CLOCK -> clockEmoji(now) + " " + time + "\n" + SimpleDateFormat("EEE, d MMM", locale).format(now.time)
            TimeStyle.HERE -> String.format(hereFormat, time) + " " + dayEmoji(now)
        }
    }

    /** Uhr-Emoji passend zur Stunde (🕐 … 🕛). */
    fun clockEmoji(now: Calendar): String {
        val hour = now.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        return String(Character.toChars(0x1F54F + hour))
    }

    /** Tageszeit: Sonnenaufgang, Sonne, Abend, Mond. */
    fun dayEmoji(now: Calendar): String = when (now.get(Calendar.HOUR_OF_DAY)) {
        in 5..8 -> "🌅"
        in 9..16 -> "☀️"
        in 17..20 -> "🌆"
        else -> "🌙"
    }

    fun next(style: TimeStyle): TimeStyle = TimeStyle.entries[(style.ordinal + 1) % TimeStyle.entries.size]
}
