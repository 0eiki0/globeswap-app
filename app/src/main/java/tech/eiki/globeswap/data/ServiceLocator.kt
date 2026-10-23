package tech.eiki.globeswap.data

import android.content.Context
import tech.eiki.globeswap.BuildConfig

object ServiceLocator {
    lateinit var api: ApiClient
        private set
    lateinit var repository: SwapRepository
        private set
    lateinit var updates: AppUpdates
        private set

    fun init(context: Context) {
        if (::api.isInitialized) return
        val app = context.applicationContext
        val channel = InstallChannel.detect(app)
        api = ApiClient(app, BuildConfig.SERVER_URL, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, channel.header)
        repository = SwapRepository(api)
        updates = AppUpdates(app, api, BuildConfig.VERSION_CODE, channel)
        api.onUpdateRequired = updates::onUpdateRequired
    }
}
