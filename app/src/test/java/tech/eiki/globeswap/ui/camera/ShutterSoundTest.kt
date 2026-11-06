package tech.eiki.globeswap.ui.camera

import android.app.Application
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Auslöseton nur bei normalem Klingelmodus, im Lautlos- und Vibrationsmodus still. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ShutterSoundTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val audio = context.getSystemService(AudioManager::class.java)

    @Test
    fun playsOnlyWhenRingerIsNormal() {
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        assertTrue(ShutterSound.shouldPlay(context))
        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        assertFalse(ShutterSound.shouldPlay(context))
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        assertFalse(ShutterSound.shouldPlay(context))
    }
}
