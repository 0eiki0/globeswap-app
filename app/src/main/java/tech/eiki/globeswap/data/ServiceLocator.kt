package tech.eiki.globeswap.data

import android.content.Context
import tech.eiki.globeswap.BuildConfig

object ServiceLocator {
    lateinit var api: ApiClient
        private set
    lateinit var repository: SwapRepository
        private set

    fun init(context: Context) {
        if (::api.isInitialized) return
        api = ApiClient(context.applicationContext, BuildConfig.SERVER_URL)
        repository = SwapRepository(api)
    }
}
