package tech.eiki.globeswap.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.eiki.globeswap.util.AppTheme

/** Markenfarbe von GlobeSwap (auch Hintergrund des App-Icons). */
val GlobeBlue = Color(0xFF1E6FD9)

/** Akzent für Neues, Tagesthema und Erfolge. */
val GlobeYellow = Color(0xFFFFD54F)

/** Verlauf des großen Swap-Knopfs und des Auslösers. */
val GlobeGradient = Brush.linearGradient(listOf(Color(0xFF2D7FF0), Color(0xFF0B4FA8)))

// Hell: helle Fläche, weiße Karten (surfaceContainerLowest), fast schwarze Schrift
private val LightColors = lightColorScheme(
    primary = GlobeBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF00897B),
    secondaryContainer = Color(0xFFE6EEFB),
    onSecondaryContainer = Color(0xFF14171F),
    tertiary = GlobeYellow,
    onTertiary = Color(0xFF14171F),
    tertiaryContainer = Color(0xFFFFE58A),
    onTertiaryContainer = Color(0xFF3D2E00),
    background = Color(0xFFF6F8FD),
    onBackground = Color(0xFF14171F),
    surface = Color(0xFFF6F8FD),
    onSurface = Color(0xFF14171F),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceVariant = Color(0xFFE3E8F2),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F4FB),
    surfaceContainer = Color(0xFFEDF1F9),
    surfaceContainerHigh = Color(0xFFE6EBF5),
    surfaceContainerHighest = Color(0xFFDFE5F0),
    outlineVariant = Color(0xFFE3E8F2),
    inverseSurface = Color(0xFF14171F),
    inverseOnSurface = Color.White,
)

// Dunkel: fast schwarze Fläche, etwas hellere Karten, gleiche Akzente
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DB8FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF0B4FA8),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF4DB6AC),
    secondaryContainer = Color(0xFF232A36),
    onSecondaryContainer = Color(0xFFE6EAF2),
    tertiary = GlobeYellow,
    onTertiary = Color(0xFF14171F),
    tertiaryContainer = Color(0xFF4A3B00),
    onTertiaryContainer = Color(0xFFFFE58A),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE9ECF2),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFE9ECF2),
    onSurfaceVariant = Color(0xFF9AA3B2),
    surfaceVariant = Color(0xFF2A303B),
    surfaceContainerLowest = Color(0xFF181C24),
    surfaceContainerLow = Color(0xFF14181F),
    surfaceContainer = Color(0xFF1D222B),
    surfaceContainerHigh = Color(0xFF232933),
    surfaceContainerHighest = Color(0xFF2A303B),
    outlineVariant = Color(0xFF2A303B),
    inverseSurface = Color(0xFF232833),
    inverseOnSurface = Color.White,
)

// Große, fette Überschriften; sonst die Standardschrift
private val Base = Typography()
private val AppTypography = Base.copy(
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-0.8).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Bold),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

// Bewusst ohne Android-12-Wallpaper-Farben, damit die App überall im GlobeSwap-Blau erscheint.
// Hell oder dunkel nach Einstellungen → Design (Standard: wie am Handy).
@Composable
fun GlobeSwapTheme(darkTheme: Boolean = AppTheme.isDark(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
