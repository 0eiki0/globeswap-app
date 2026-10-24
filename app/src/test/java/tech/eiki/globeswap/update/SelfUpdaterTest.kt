package tech.eiki.globeswap.update

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tech.eiki.globeswap.data.InstallChannel
import tech.eiki.globeswap.data.LatestRelease
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class SelfUpdaterTest {
    private lateinit var server: MockWebServer
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val apk = ByteArray(300_000) { (it % 251).toByte() }
    private val sha = MessageDigest.getInstance("SHA-256").digest(apk).joinToString("") { "%02x".format(it) }
    private val release get() = LatestRelease(130, "2.3", apkUrl = "/app/GlobeSwap.apk", apkSha256 = sha, apkSize = apk.size.toLong())

    private var installed: ByteArray? = null
    private var verifyResult: String? = null

    private fun updater(enabled: Boolean = true) = SelfUpdater(
        context, OkHttpClient(), { server.url(it).toString() }, currentCode = 120, enabled = enabled,
        verify = { verifyResult }, install = { installed = it.readBytes() },
    )

    @Before fun start() {
        server = MockWebServer().apply { start() }
        shadowOf(context.packageManager).setCanRequestPackageInstalls(true)
    }

    @After fun stop() = server.shutdown()

    @Test
    fun onlyGithubInstallsOfTheGithubVariantUpdateThemselves() {
        val u = updater()
        assertTrue(u.canSelfUpdate(release, InstallChannel.GITHUB))
        assertFalse("Play Store aktualisiert selbst", u.canSelfUpdate(release, InstallChannel.PLAY))
        assertFalse("F-Droid aktualisiert selbst", u.canSelfUpdate(release, InstallChannel.FDROID))
        assertFalse("Play-Variante", updater(enabled = false).canSelfUpdate(release, InstallChannel.GITHUB))
        assertFalse("Server hat keine APK", u.canSelfUpdate(release.copy(apkUrl = ""), InstallChannel.GITHUB))
        assertFalse("keine Prüfsumme", u.canSelfUpdate(release.copy(apkSha256 = "abc"), InstallChannel.GITHUB))
        assertFalse("nicht neuer", u.canSelfUpdate(release.copy(versionCode = 120), InstallChannel.GITHUB))
        assertFalse(u.canSelfUpdate(null, InstallChannel.GITHUB))
    }

    @Test
    fun downloadsChecksAndInstalls() = runBlocking {
        server.enqueue(MockResponse().setBody(Buffer().write(apk)))
        val u = updater()
        u.start(release)
        assertEquals("/app/GlobeSwap.apk", server.takeRequest().path)
        assertArrayEquals("Installer bekam genau die geprüfte APK", apk, installed)
        assertEquals(SelfUpdateState.Installing, u.state.value)
        u.onInstallFinished(false, "INSTALL_FAILED_UPDATE_INCOMPATIBLE")
        assertEquals(SelfUpdateState.Failed("INSTALL_FAILED_UPDATE_INCOMPATIBLE"), u.state.value)
    }

    @Test
    fun tamperedDownloadIsNotInstalled() = runBlocking {
        server.enqueue(MockResponse().setBody(Buffer().write(apk.copyOf().also { it[1000] = 42 })))
        val u = updater()
        u.start(release)
        assertNull("manipulierte Datei installiert", installed)
        assertTrue(u.state.value is SelfUpdateState.Failed)
        assertTrue("Datei nicht gelöscht", File(context.cacheDir, "updates").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun foreignSignatureIsNotInstalled() = runBlocking {
        server.enqueue(MockResponse().setBody(Buffer().write(apk)))
        verifyResult = "The update is not signed by the GlobeSwap developer and was not installed."
        val u = updater()
        u.start(release)
        assertNull(installed)
        assertEquals(SelfUpdateState.Failed(verifyResult!!), u.state.value)
    }

    @Test
    fun asksForInstallPermissionFirst() = runBlocking {
        shadowOf(context.packageManager).setCanRequestPackageInstalls(false)
        val u = updater()
        u.start(release)
        assertEquals(SelfUpdateState.NeedsPermission, u.state.value)
        val opened = shadowOf(context).nextStartedActivity
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, opened.action)
        assertEquals("package:${context.packageName}", opened.dataString)
        assertEquals(0, server.requestCount)

        // Zurück aus den Einstellungen, Erlaubnis erteilt: es geht von selbst weiter.
        shadowOf(context.packageManager).setCanRequestPackageInstalls(true)
        server.enqueue(MockResponse().setBody(Buffer().write(apk)))
        u.resumeIfAllowed(release)
        assertArrayEquals(apk, installed)
    }
}
