package tech.eiki.globeswap

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.notifications.NewPhotoNotifier
import tech.eiki.globeswap.notifications.NewPhotoWorker
import tech.eiki.globeswap.notifications.Push

class GlobeSwapApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NewPhotoNotifier.createChannel(this)
        NewPhotoWorker.schedule(this)
        Push.init(this)
        // Bei jedem Start und direkt nach der ersten Anmeldung beim Server den Push-Schlüssel melden.
        ServiceLocator.api.onRegistered = { Push.register(this) }
        Push.register(this)
    }

    /** Coil lädt Fotos mit dem Token der App, denn der Server gibt sie nur Beteiligten heraus. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { ServiceLocator.api.imageClient }
            .crossfade(true)
            .build()
}
