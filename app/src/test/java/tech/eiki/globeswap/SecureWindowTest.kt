package tech.eiki.globeswap

import android.app.Application
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tech.eiki.globeswap.data.ServiceLocator

/** Screenshots und Bildschirmaufnahmen müssen in der ganzen App gesperrt sein. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class SecureWindowTest {
    @Test
    fun screenshotsAreBlocked() {
        ServiceLocator.init(ApplicationProvider.getApplicationContext())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assertTrue("FLAG_SECURE fehlt", flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
        }
    }
}
