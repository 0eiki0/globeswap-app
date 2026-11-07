package tech.eiki.globeswap.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Sprache der App: wie am Handy ([SYSTEM]) oder fest eine der [LANGUAGES] (Einstellungen → Sprache).
 * Die Activity und alle Texte außerhalb von Compose gehen über [wrap]; die Wahl gilt sofort, ohne Neustart.
 */
object AppLanguage {
    const val SYSTEM = ""
    const val ENGLISH = "en"
    const val GERMAN = "de"
    private const val PREFS = "globeswap"
    private const val KEY = "app_language"

    /** Eine Sprache der App: gespeicherter Code, Locale und Name in der eigenen Sprache (in keiner Sprache übersetzt). */
    data class Language(val code: String, val locale: Locale, val name: String)

    /**
     * Alle Sprachen, für die es eine Sprachdatei gibt (res/values-…). Neue Sprache: Datei anlegen und hier eine Zeile.
     * Portugiesisch ist brasilianisches Portugiesisch (values-pt gilt aber für alle Portugiesisch-Handys),
     * Indonesisch liegt bei Android unter dem alten Code "in" (values-in).
     */
    val LANGUAGES = listOf(
        Language(ENGLISH, Locale.ENGLISH, "English"),
        Language(GERMAN, Locale.GERMAN, "Deutsch"),
        Language("es", Locale.forLanguageTag("es"), "Español"),
        Language("fr", Locale.FRENCH, "Français"),
        Language("id", Locale.forLanguageTag("id"), "Bahasa Indonesia"),
        Language("it", Locale.ITALIAN, "Italiano"),
        Language("pl", Locale.forLanguageTag("pl"), "Polski"),
        Language("pt", Locale.forLanguageTag("pt-BR"), "Português (Brasil)"),
        Language("tr", Locale.forLanguageTag("tr"), "Türkçe"),
    )

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun set(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language).apply()
    }

    /** Gibt es die Sprache von [locale] in der App? Java kennt Indonesisch teils noch unter dem alten Code "in". */
    fun isSupported(locale: Locale): Boolean = LANGUAGES.any { norm(it.locale.language) == norm(locale.language) }

    private fun norm(language: String) = if (language == "in") "id" else language

    /** Kontext in der gewählten Sprache; setzt auch die Standard-Sprache für Ländernamen und Datum. */
    fun wrap(base: Context): Context {
        val locale = LANGUAGES.firstOrNull { it.code == get(base) }?.locale
        Locale.setDefault(locale ?: Resources.getSystem().configuration.locales[0])
        if (locale == null) return base
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
