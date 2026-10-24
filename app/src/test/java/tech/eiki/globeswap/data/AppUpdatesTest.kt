package tech.eiki.globeswap.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AppUpdatesTest {
    private lateinit var server: MockWebServer
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val release15 = LatestRelease(112, "1.5", "https://github.com/0eiki0/globeswap-app/releases/tag/v1.5")

    @Before fun start() { server = MockWebServer().apply { start() } }
    @After fun stop() = server.shutdown()

    private fun client() = ApiClient(context, server.url("").toString().trimEnd('/'), 110, "1.4", "fdroid")

    @Test
    fun decideUpdate() {
        val config = UpdateConfig(minVersionCode = 0, latest = release15)
        // Neuere Version bei F-Droid/GitHub: Hinweis.
        assertEquals(release15, decideUpdate(110, InstallChannel.FDROID, config, 0).available)
        assertEquals(release15, decideUpdate(110, InstallChannel.GITHUB, config, 0).available)
        // Play Store aktualisiert selbst, gleiche Version oder "Später" gedrückt: kein Hinweis.
        assertNull(decideUpdate(110, InstallChannel.PLAY, config, 0).available)
        assertNull(decideUpdate(112, InstallChannel.GITHUB, config, 0).available)
        assertNull(decideUpdate(110, InstallChannel.GITHUB, config, 112).available)
        // Mindestversion: Pflicht-Update, auch im Play Store; ersetzt den Hinweis.
        val forced = decideUpdate(110, InstallChannel.PLAY, config.copy(minVersionCode = 111, updateMessage = "Bitte aktualisieren"), 0)
        assertTrue(forced.required)
        assertEquals("Bitte aktualisieren", forced.message)
        assertNull(decideUpdate(110, InstallChannel.GITHUB, config.copy(minVersionCode = 111), 0).available)
        assertFalse(decideUpdate(111, InstallChannel.PLAY, config.copy(minVersionCode = 111), 0).required)
    }

    @Test
    fun installChannel() {
        assertEquals(InstallChannel.PLAY, InstallChannel.fromInstaller("com.android.vending"))
        assertEquals(InstallChannel.FDROID, InstallChannel.fromInstaller("org.fdroid.fdroid"))
        assertEquals(InstallChannel.FDROID, InstallChannel.fromInstaller("com.looker.droidify"))
        assertEquals(InstallChannel.GITHUB, InstallChannel.fromInstaller(null))
        assertEquals(InstallChannel.GITHUB, InstallChannel.fromInstaller("com.google.android.packageinstaller"))
    }

    @Test
    fun configIsParsedAndVersionHeadersAreSent() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"minVersionCode":105,"updateMessage":"Hi","latest":{"versionCode":112,"versionName":"1.5","url":"u","publishedAt":"x"}}"""))
        val config = client().config()
        assertEquals(UpdateConfig(105, "Hi", LatestRelease(112, "1.5", "u")), config)
        server.enqueue(MockResponse().setBody("""{"minVersionCode":0,"push":true}"""))
        assertTrue("Server meldet Push", client().config().push)
        val request = server.takeRequest()
        server.takeRequest()
        assertEquals("/api/config", request.path)
        assertEquals("110", request.getHeader("X-App-Version-Code"))
        assertEquals("1.4", request.getHeader("X-App-Version-Name"))
        assertEquals("fdroid", request.getHeader("X-App-Channel"))
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun updateRequiredResponseLocksTheApp() = runBlocking {
        val api = client()
        val updates = AppUpdates(context, api, 110, InstallChannel.GITHUB)
        api.onUpdateRequired = updates::onUpdateRequired
        server.enqueue(MockResponse().setBody("""{"token":"t","userId":"u","publicId":"p"}"""))
        server.enqueue(MockResponse().setResponseCode(426).setBody("""{"error":"update_required","minVersionCode":120,"message":"Too old"}"""))
        try {
            api.callRaw("GET", "/api/conversations")
            fail("426 erwartet")
        } catch (e: ApiException) {
            assertEquals("update_required", e.code)
        }
        assertTrue(updates.state.value.required)
        assertEquals("Too old", updates.state.value.message)

        // Admin hebt die Sperre auf: beim nächsten Abgleich ist die App wieder frei, Hinweis bleibt wegklickbar.
        server.enqueue(MockResponse().setBody("""{"minVersionCode":0,"updateMessage":"","latest":{"versionCode":112,"versionName":"1.5"}}"""))
        updates.refresh()
        assertFalse(updates.state.value.required)
        assertEquals(112, updates.state.value.available?.versionCode)
        updates.dismiss(112)
        assertNull(updates.state.value.available)
        server.enqueue(MockResponse().setBody("""{"minVersionCode":0,"latest":{"versionCode":112,"versionName":"1.5"}}"""))
        updates.refresh()
        assertNull("Später gedrückt – bis zur nächsten Version kein Hinweis mehr", updates.state.value.available)
    }

    @Test
    fun registrationOfTooOldAppAlsoLocks() = runBlocking {
        val api = client()
        var message: String? = null
        api.onUpdateRequired = { message = it }
        server.enqueue(MockResponse().setResponseCode(426).setBody("""{"error":"update_required","message":"Update please"}"""))
        runCatching { api.callRaw("GET", "/api/conversations") }
        assertEquals("Update please", message)
    }
}
