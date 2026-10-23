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
class DeleteTest {
    private lateinit var server: MockWebServer
    private lateinit var repo: SwapRepository

    @Before fun start() {
        server = MockWebServer().apply { start() }
        val context = ApplicationProvider.getApplicationContext<Application>()
        repo = SwapRepository(ApiClient(context, server.url("").toString().trimEnd('/')))
        server.enqueue(MockResponse().setBody("""{"token":"t","userId":"u","publicId":"p"}"""))
    }

    @After fun stop() = server.shutdown()

    @Test
    fun deleteChatAndOwnPhoto() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))
        repo.deleteMessage("c1", "m1")
        repo.deleteConversation("c1")
        server.takeRequest() // Registrierung
        server.takeRequest().let {
            assertEquals("DELETE", it.method)
            assertEquals("/api/conversations/c1/messages/m1", it.path)
            assertEquals("Bearer t", it.getHeader("Authorization"))
        }
        server.takeRequest().let {
            assertEquals("DELETE", it.method)
            assertEquals("/api/conversations/c1", it.path)
        }
    }

    @Test
    fun foreignPhotoCannotBeDeleted() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"not_yours"}"""))
        val error = runCatching { repo.deleteMessage("c1", "m2") }.exceptionOrNull()
        assertTrue(error is ApiException && error.status == 403 && error.code == "not_yours")
    }
}
