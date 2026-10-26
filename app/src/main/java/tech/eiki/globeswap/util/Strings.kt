package tech.eiki.globeswap.util

import androidx.annotation.StringRes
import tech.eiki.globeswap.data.ServiceLocator

/** Text aus den Sprachdateien außerhalb von Compose (ViewModels, Fehlermeldungen) – in der Sprache des Geräts. */
fun appString(@StringRes id: Int, vararg args: Any): String = ServiceLocator.appContext.getString(id, *args)
