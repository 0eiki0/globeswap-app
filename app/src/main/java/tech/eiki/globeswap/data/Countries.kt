package tech.eiki.globeswap.data

import androidx.annotation.StringRes
import tech.eiki.globeswap.R
import java.util.Locale

/** Anzeige von Ländern: Flagge und Name aus dem ISO-Code (z. B. "JP" → 🇯🇵 Japan), auf Deutsch oder Englisch wie die App. */
object Countries {
    fun flag(code: String): String {
        if (code.length != 2 || !code.all { it in 'A'..'Z' }) return ""
        return String(Character.toChars(0x1F1E6 + (code[0] - 'A'))) + String(Character.toChars(0x1F1E6 + (code[1] - 'A')))
    }

    fun name(code: String): String =
        if (code.isBlank()) "" else Locale("", code).getDisplayCountry(appLocale()).ifBlank { code }

    /** Die App gibt es auf Deutsch und Englisch – Ländernamen passend dazu, nie in einer dritten Sprache. */
    private fun appLocale(): Locale = if (Locale.getDefault().language == "de") Locale.GERMAN else Locale.ENGLISH

    /** "🇯🇵 Japan" oder "" ohne Land. */
    fun label(code: String): String = if (code.isBlank()) "" else "${flag(code)} ${name(code)}".trim()

    /** Name des Kontinents als Text aus den Sprachdateien, null bei unbekanntem Code. */
    @StringRes
    fun continent(code: String): Int? = when (code) {
        "AF" -> R.string.cont_af
        "AN" -> R.string.cont_an
        "AS" -> R.string.cont_as
        "EU" -> R.string.cont_eu
        "NA" -> R.string.cont_na
        "OC" -> R.string.cont_oc
        "SA" -> R.string.cont_sa
        else -> null
    }
}
