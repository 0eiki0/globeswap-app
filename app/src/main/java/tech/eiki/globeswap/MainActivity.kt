package tech.eiki.globeswap

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import tech.eiki.globeswap.ui.GlobeSwapApp
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keine Screenshots und Bildschirmaufnahmen von Chats und Fotos; in der App-Übersicht erscheint die App schwarz.
        // Dialoge und Bottom Sheets erben die Einstellung von diesem Fenster.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            GlobeSwapTheme {
                GlobeSwapApp()
            }
        }
    }
}
