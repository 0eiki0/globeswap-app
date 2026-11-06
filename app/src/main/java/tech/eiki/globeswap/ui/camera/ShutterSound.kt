package tech.eiki.globeswap.ui.camera

import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Auslöseton des Systems (wie in der Kamera-App). Im Lautlos- oder Vibrationsmodus still –
 * außer das Land schreibt den Ton vor (z. B. Japan, Korea), dann immer.
 */
class ShutterSound(private val context: Context) {
    private val sound = MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }

    fun play() {
        if (shouldPlay(context)) sound.play(MediaActionSound.SHUTTER_CLICK)
    }

    fun release() = sound.release()

    companion object {
        fun shouldPlay(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && MediaActionSound.mustPlayShutterSound()) return true
            val audio = context.getSystemService(AudioManager::class.java) ?: return false
            return audio.ringerMode == AudioManager.RINGER_MODE_NORMAL
        }
    }
}

@Composable
fun rememberShutterSound(): ShutterSound {
    val context = LocalContext.current
    val shutter = remember { ShutterSound(context.applicationContext) }
    DisposableEffect(shutter) { onDispose { shutter.release() } }
    return shutter
}
