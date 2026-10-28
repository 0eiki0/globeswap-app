package tech.eiki.globeswap.util

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

/**
 * Design der App: wie am Handy ([SYSTEM]), immer hell oder immer dunkel (Einstellungen → Design).
 * Die Wahl liegt in einem Compose-State, deshalb wechselt die App sofort, ohne Neustart.
 */
object AppTheme {
    const val SYSTEM = ""
    const val LIGHT = "light"
    const val DARK = "dark"
    private const val PREFS = "globeswap"
    private const val KEY = "app_theme"

    private val mode = mutableStateOf(SYSTEM)

    /** Einmal beim Start der Activity aus den Einstellungen laden. */
    fun load(context: Context) {
        mode.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM
    }

    val current: String get() = mode.value

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value).apply()
        mode.value = value
    }

    @Composable
    fun isDark(): Boolean = when (mode.value) {
        LIGHT -> false
        DARK -> true
        else -> isSystemInDarkTheme()
    }
}
