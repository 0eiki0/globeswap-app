package tech.eiki.globeswap.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tech.eiki.globeswap.data.ServiceLocator

/** Die Benachrichtigung verschwindet, sobald alles gelesen ist – nicht nur beim Antippen. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class NotifierTest {
    private lateinit var server: MockWebServer
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private var unread = true
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    @Before fun start() {
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when (request.path) {
                    "/api/register" -> MockResponse().setBody("""{"token":"t","userId":"u","publicId":"p"}""")
                    "/api/conversations" -> MockResponse().setBody(
                        """[{"id":"c1","label":"x","lastMessageAt":${System.currentTimeMillis()},"lastFromMe":false,"lastPhotoId":"p","unread":$unread,"blocked":false}]"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        ServiceLocator.initForTest(context, server.url("").toString().trimEnd('/'))
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NewPhotoNotifier.createChannel(context)
        runBlocking { ServiceLocator.repository.fetchConversations() } // anmelden
    }

    @After fun stop() = server.shutdown()

    @Test
    fun notificationDisappearsWhenEverythingIsRead() = runBlocking {
        NewPhotoNotifier.check(context)
        assertEquals("Benachrichtigung für das neue Foto", 1, manager.activeNotifications.size)

        NewPhotoNotifier.clearIfAllRead(context) // noch ungelesen: bleibt
        assertEquals(1, manager.activeNotifications.size)

        unread = false // Chat gelesen
        NewPhotoNotifier.clearIfAllRead(context)
        assertEquals("Benachrichtigung (und damit der Punkt am App-Symbol) weg", 0, manager.activeNotifications.size)
    }
}
