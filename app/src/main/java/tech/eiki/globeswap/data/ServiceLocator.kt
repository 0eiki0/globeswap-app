package tech.eiki.globeswap.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import tech.eiki.globeswap.BuildConfig

object ServiceLocator {
    /** Für Hintergrundarbeit ohne Bildschirm, z. B. das Anmelden des Push-Schlüssels. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var api: ApiClient
        private set
    lateinit var repository: SwapRepository
        private set
    lateinit var updates: AppUpdates
        private set
    lateinit var appContext: Context
        private set

    fun init(context: Context) = init(context, BuildConfig.SERVER_URL, force = false)

    /** Für Tests: gegen einen Testserver statt den echten. */
    @VisibleForTesting
    internal fun initForTest(context: Context, serverUrl: String) = init(context, serverUrl, force = true)

    private fun init(context: Context, serverUrl: String, force: Boolean) {
        if (!force && ::api.isInitialized) return
        val app = context.applicationContext
        appContext = app
        val channel = InstallChannel.detect(app)
        api = ApiClient(app, serverUrl, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, channel.header)
        repository = SwapRepository(api)
        updates = AppUpdates(app, api, BuildConfig.VERSION_CODE, channel)
        api.onUpdateRequired = updates::onUpdateRequired
    }
}
