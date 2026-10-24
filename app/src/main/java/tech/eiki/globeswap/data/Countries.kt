package tech.eiki.globeswap.data

import java.util.Locale

/** Anzeige von Ländern: Flagge und englischer Name aus dem ISO-Code (z. B. "JP" → 🇯🇵 Japan). */
object Countries {
    fun flag(code: String): String {
        if (code.length != 2 || !code.all { it in 'A'..'Z' }) return ""
        return String(Character.toChars(0x1F1E6 + (code[0] - 'A'))) + String(Character.toChars(0x1F1E6 + (code[1] - 'A')))
    }

    fun name(code: String): String =
        if (code.isBlank()) "" else Locale("", code).getDisplayCountry(Locale.ENGLISH).ifBlank { code }

    /** "🇯🇵 Japan" oder "" ohne Land. */
    fun label(code: String): String = if (code.isBlank()) "" else "${flag(code)} ${name(code)}".trim()

    fun continent(code: String): String = when (code) {
        "AF" -> "Africa"
        "AN" -> "Antarctica"
        "AS" -> "Asia"
        "EU" -> "Europe"
        "NA" -> "North America"
        "OC" -> "Oceania"
        "SA" -> "South America"
        else -> ""
    }
}
