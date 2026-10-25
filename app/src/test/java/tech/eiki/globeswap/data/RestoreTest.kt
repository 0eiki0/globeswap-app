package tech.eiki.globeswap.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class RestoreTest {
    private lateinit var server: MockWebServer
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Before fun start() { server = MockWebServer().apply { start() } }
    @After fun stop() = server.shutdown()

    @Test
    fun restoreReplacesIdentity() = runBlocking {
        val api = ApiClient(context, server.url("").toString().trimEnd('/'))
        var registered = 0
        api.onRegistered = { registered++ }
        server.enqueue(MockResponse().setBody("""{"userId":"u-old","token":"old-token","publicId":"p-old","code":"NEW2-CODE-ABCD-EFGH"}"""))
        assertEquals("NEW2-CODE-ABCD-EFGH", api.restore("k7qm 2xpa 9rtd hv4e"))
        server.takeRequest().let {
            assertEquals("/api/restore", it.path)
            assertEquals("""{"code":"k7qm 2xpa 9rtd hv4e"}""", it.body.readUtf8())
            assertEquals(null, it.getHeader("Authorization"))
        }
        assertEquals("Push-Schlüssel neu anmelden", 1, registered)
        assertEquals("p-old", api.publicId())

        // Weitere Anfragen laufen mit dem zurückgeholten Zugang.
        server.enqueue(MockResponse().setBody("[]"))
        api.callRaw("GET", "/api/conversations")
        assertEquals("Bearer old-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun wrongCodeGivesClearMessage() = runBlocking {
        val api = ApiClient(context, server.url("").toString().trimEnd('/'))
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"invalid_code"}"""))
        val error = runCatching { api.restore("AAAA-BBBB-CCCC-DDDD") }.exceptionOrNull()
        assertTrue(error is ApiException)
        assertEquals("This backup code is not valid. Please check it and try again.", error!!.userMessage())
        assertTrue("keine Identität gespeichert", !api.hasToken)
    }
}
