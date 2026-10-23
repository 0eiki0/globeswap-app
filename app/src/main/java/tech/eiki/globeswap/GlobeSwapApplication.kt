package tech.eiki.globeswap

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.notifications.NewPhotoWorker

class GlobeSwapApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NewPhotoWorker.createChannel(this)
        NewPhotoWorker.schedule(this)
    }

    /** Coil lädt Fotos mit dem Token der App, denn der Server gibt sie nur Beteiligten heraus. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { ServiceLocator.api.imageClient }
            .crossfade(true)
            .build()
}
