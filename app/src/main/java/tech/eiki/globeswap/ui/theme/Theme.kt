package tech.eiki.globeswap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import tech.eiki.globeswap.util.AppTheme

/** Markenfarbe von GlobeSwap (auch Hintergrund des App-Icons). */
val GlobeBlue = Color(0xFF1E6FD9)

private val LightColors = lightColorScheme(
    primary = GlobeBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF00897B),
    background = Color(0xFFF9FAFE),
    surface = Color(0xFFF9FAFE),
    surfaceVariant = Color(0xFFE1E6F0),
    surfaceContainer = Color(0xFFEEF2FA),
    surfaceContainerLow = Color(0xFFF3F6FC),
    surfaceContainerHigh = Color(0xFFE8EDF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF0B4FA8),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF4DB6AC),
    background = Color(0xFF111418),
    surface = Color(0xFF111418),
    surfaceContainer = Color(0xFF1D2128),
)

// Bewusst ohne Android-12-Wallpaper-Farben, damit die App überall im GlobeSwap-Blau erscheint.
// Hell oder dunkel nach Einstellungen → Design (Standard: wie am Handy).
@Composable
fun GlobeSwapTheme(darkTheme: Boolean = AppTheme.isDark(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
