package tech.eiki.globeswap.ui

import tech.eiki.globeswap.BuildConfig

/** Rechtliche Seiten, die der eigene Server ausliefert. */
object LegalLinks {
    val privacy get() = "${BuildConfig.SERVER_URL}/privacy?lang=en"
    val terms get() = "${BuildConfig.SERVER_URL}/terms?lang=en"
    val imprint get() = "${BuildConfig.SERVER_URL}/imprint?lang=en"
    val deleteAccount get() = "${BuildConfig.SERVER_URL}/delete-account?lang=en"
}
