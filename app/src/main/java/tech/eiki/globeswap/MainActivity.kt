package tech.eiki.globeswap

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import tech.eiki.globeswap.ui.GlobeSwapApp
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme
import tech.eiki.globeswap.util.AppLanguage
import tech.eiki.globeswap.util.AppTheme

// Wie die Standardwerte von enableEdgeToEdge für die Navigationsleiste
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {
    /** Gewählte Sprache der App (Einstellungen → Sprache) statt der Sprache des Handys. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keine Screenshots und Bildschirmaufnahmen von Chats und Fotos; in der App-Übersicht erscheint die App schwarz.
        // Dialoge und Bottom Sheets erben die Einstellung von diesem Fenster.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        AppTheme.load(this)
        enableEdgeToEdge()
        setContent {
            // Symbole der Statusleiste passend zum gewählten Design (nicht nur zum Design des Handys)
            val dark = AppTheme.isDark()
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
            }
            GlobeSwapTheme(darkTheme = dark) {
                GlobeSwapApp()
            }
        }
    }
}
