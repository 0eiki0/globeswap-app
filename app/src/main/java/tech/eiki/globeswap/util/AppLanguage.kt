package tech.eiki.globeswap.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Sprache der App: wie am Handy ([SYSTEM]) oder fest Englisch/Deutsch (Einstellungen → Sprache).
 * Die Activity und alle Texte außerhalb von Compose gehen über [wrap]; die Wahl gilt sofort, ohne Neustart.
 */
object AppLanguage {
    const val SYSTEM = ""
    const val ENGLISH = "en"
    const val GERMAN = "de"
    private const val PREFS = "globeswap"
    private const val KEY = "app_language"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun set(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language).apply()
    }

    /** Kontext in der gewählten Sprache; setzt auch die Standard-Sprache für Ländernamen und Datum. */
    fun wrap(base: Context): Context {
        val locale = when (get(base)) {
            ENGLISH -> Locale.ENGLISH
            GERMAN -> Locale.GERMAN
            else -> null
        }
        Locale.setDefault(locale ?: Resources.getSystem().configuration.locales[0])
        if (locale == null) return base
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
