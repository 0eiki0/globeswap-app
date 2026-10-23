package tech.eiki.globeswap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import tech.eiki.globeswap.ui.GlobeSwapApp
import tech.eiki.globeswap.ui.theme.GlobeSwapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlobeSwapTheme {
                GlobeSwapApp()
            }
        }
    }
}
