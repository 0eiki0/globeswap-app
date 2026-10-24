package tech.eiki.globeswap.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import tech.eiki.globeswap.update.SelfUpdater

/** Woher die App installiert wurde – bestimmt, wohin der Update-Knopf führt. */
enum class InstallChannel(val header: String) {
    PLAY("play"),
    FDROID("fdroid"),
    GITHUB("github"),
    ;

    companion object {
        private val FDROID_CLIENTS = setOf(
            "org.fdroid.fdroid", "org.fdroid.basic", "com.looker.droidify", "com.machiav3lli.fdroid", "in.sunilpaulmathew.izzyondroid",
        )

        fun fromInstaller(installer: String?): InstallChannel = when (installer) {
            "com.android.vending" -> PLAY
            in FDROID_CLIENTS -> FDROID
            else -> GITHUB // direkt installiert, über Obtainium oder unbekannt
        }

        fun detect(context: Context): InstallChannel {
            val installer = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getInstallerPackageName(context.packageName)
                }
            }.getOrNull()
            return fromInstaller(installer)
        }
    }
}

@Serializable
data class LatestRelease(
    val versionCode: Int,
    val versionName: String,
    val url: String = "",
    /** Geprüfte APK auf unserem Server (Pfad, z. B. "/app/GlobeSwap.apk") – für das Selbst-Update. */
    val apkUrl: String = "",
    val apkSha256: String = "",
    val apkSize: Long = 0,
)

@Serializable
data class UpdateConfig(
    val minVersionCode: Int = 0,
    val updateMessage: String = "",
    val latest: LatestRelease? = null,
    /** Der Server verschickt Push-Mitteilungen (Firebase). */
    val push: Boolean = false,
    /** Tagesthema, z. B. "The view from your window" (leer = keins). */
    val theme: String = "",
)

/** Was die Oberfläche zeigt: Pflicht-Update (sperrt die App) oder Hinweis auf eine neue Version. */
data class UpdateUi(
    val required: Boolean = false,
    val message: String = "",
    val available: LatestRelease? = null,
)

/**
 * Pflicht-Update, wenn die eigene Version unter der Mindestversion liegt. Hinweis auf eine neue Version nur bei
 * Installationen aus F-Droid/GitHub – den Play Store aktualisiert Google selbst, und neue GitHub-Releases sind dort
 * oft erst später freigegeben.
 */
fun decideUpdate(currentCode: Int, channel: InstallChannel, config: UpdateConfig, dismissedCode: Int): UpdateUi {
    val required = config.minVersionCode > 0 && currentCode < config.minVersionCode
    val available = config.latest?.takeIf {
        !required && channel != InstallChannel.PLAY && it.versionCode > currentCode && it.versionCode != dismissedCode
    }
    return UpdateUi(required = required, message = config.updateMessage, available = available)
}

/** Fragt beim eigenen Server nach Mindestversion und neuestem Release und hält den Zustand für die Oberfläche. */
class AppUpdates(
    context: Context,
    private val api: ApiClient,
    private val currentCode: Int,
    val channel: InstallChannel,
    /** Selbst-Update der GitHub-Variante (lädt und installiert die neue APK). */
    val selfUpdater: SelfUpdater = SelfUpdater(context.applicationContext, api.httpClient, { api.pageUrl(it.removePrefix("/")) }, currentCode),
) {
    private val prefs = context.getSharedPreferences("globeswap_updates", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(UpdateUi())
    val state: StateFlow<UpdateUi> = _state.asStateFlow()
    private val _theme = MutableStateFlow("")
    /** Tagesthema vom Server, kommt mit derselben Abfrage. */
    val theme: StateFlow<String> = _theme.asStateFlow()
    private var lastConfig: UpdateConfig? = null

    suspend fun refresh() {
        runCatching { api.config() }
            .onSuccess { config ->
                lastConfig = config
                _theme.value = config.theme
                _state.value = decideUpdate(currentCode, channel, config, prefs.getInt(KEY_DISMISSED, 0))
            }
            .onFailure { Log.w("AppUpdates", "Update-Info nicht abrufbar", it) }
    }

    /** Der Server hat eine Anfrage mit "update_required" abgelehnt. */
    fun onUpdateRequired(message: String) {
        _state.value = _state.value.copy(required = true, message = message.ifBlank { _state.value.message }, available = null)
    }

    fun dismiss(versionCode: Int) {
        prefs.edit().putInt(KEY_DISMISSED, versionCode).apply()
        _state.value = _state.value.copy(available = null)
    }

    private val newest: LatestRelease? get() = _state.value.available ?: lastConfig?.latest

    /** Kann sich diese Installation gleich selbst aktualisieren (statt auf GitHub zu verweisen)? */
    val canSelfUpdate: Boolean get() = selfUpdater.canSelfUpdate(newest, channel)

    /** Nach der Rückkehr in die App (z. B. aus den Einstellungen) ein angefangenes Selbst-Update fortsetzen. */
    suspend fun resumeSelfUpdate() = selfUpdater.resumeIfAllowed(newest?.takeIf { canSelfUpdate })

    /** Update starten: selbst installieren, wenn möglich, sonst die passende Quelle öffnen. */
    fun openUpdate(context: Context) {
        val release = newest
        if (release != null && selfUpdater.canSelfUpdate(release, channel)) {
            ServiceLocator.appScope.launch { selfUpdater.start(release) }
            return
        }
        openStore(context)
    }

    /** Öffnet die passende Quelle für das Update. */
    fun openStore(context: Context) {
        val pkg = context.packageName
        val candidates = when (channel) {
            InstallChannel.PLAY -> listOf("market://details?id=$pkg", "https://play.google.com/store/apps/details?id=$pkg")
            InstallChannel.FDROID -> listOf("market://details?id=$pkg", "https://apt.izzysoft.de/fdroid/index/apk/$pkg")
            InstallChannel.GITHUB -> listOfNotNull(
                newest?.url?.takeIf { it.isNotBlank() },
                RELEASES_URL,
            )
        }
        for (url in candidates) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                Log.w("AppUpdates", "Kein Programm für $url")
            }
        }
    }

    companion object {
        private const val KEY_DISMISSED = "dismissed_version_code"
        const val RELEASES_URL = "https://github.com/0eiki0/globeswap-app/releases/latest"
    }
}
