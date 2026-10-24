package tech.eiki.globeswap.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import tech.eiki.globeswap.BuildConfig
import tech.eiki.globeswap.data.InstallChannel
import tech.eiki.globeswap.data.LatestRelease
import tech.eiki.globeswap.data.ServiceLocator
import java.io.File
import java.security.MessageDigest

/** Was beim Selbst-Update gerade passiert – die Oberfläche zeigt es im Update-Hinweis an. */
sealed interface SelfUpdateState {
    data object Idle : SelfUpdateState
    /** Der Nutzer muss GlobeSwap einmal erlauben, Apps zu installieren (Android-Einstellungen). */
    data object NeedsPermission : SelfUpdateState
    data class Downloading(val progress: Float) : SelfUpdateState
    data object Installing : SelfUpdateState
    data class Failed(val message: String) : SelfUpdateState
}

/**
 * Selbst-Update der GitHub-Variante: lädt die neue APK von unserem Server (der sie vorher bei GitHub geholt
 * und geprüft hat), prüft Prüfsumme, Paketname, Version und Signatur und übergibt sie dem Android-Installer.
 * Android fragt einmal nach ("Aktualisieren"); ab Android 12 gehen spätere Updates ohne Nachfrage.
 */
class SelfUpdater(
    private val context: Context,
    private val http: OkHttpClient,
    private val urlFor: (path: String) -> String,
    private val currentCode: Int,
    private val enabled: Boolean = BuildConfig.SELF_UPDATE,
    /** Prüft die heruntergeladene APK (Paket, Version, Signatur); in Tests ersetzbar. */
    private val verify: (File) -> String? = { verifyApk(context, it, currentCode) },
    /** Übergibt die APK dem Installer; in Tests ersetzbar. */
    private val install: (File) -> Unit = { installApk(context, it) },
) {
    private val _state = MutableStateFlow<SelfUpdateState>(SelfUpdateState.Idle)
    val state: StateFlow<SelfUpdateState> = _state.asStateFlow()

    /** Nur die GitHub-Variante, nur bei GitHub-Installationen und nur, wenn der Server die geprüfte APK hat. */
    fun canSelfUpdate(release: LatestRelease?, channel: InstallChannel): Boolean =
        enabled && channel == InstallChannel.GITHUB && release != null && release.versionCode > currentCode &&
            release.apkUrl.startsWith("/") && release.apkSha256.matches(SHA256)

    suspend fun start(release: LatestRelease) {
        if (_state.value is SelfUpdateState.Downloading || _state.value == SelfUpdateState.Installing) return
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = SelfUpdateState.NeedsPermission
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }.onFailure { Log.w(TAG, "Einstellungen nicht geöffnet", it) }
            return
        }
        _state.value = SelfUpdateState.Downloading(0f)
        try {
            val file = download(release)
            verify(file)?.let { problem ->
                file.delete()
                _state.value = SelfUpdateState.Failed(problem)
                return
            }
            _state.value = SelfUpdateState.Installing
            withContext(Dispatchers.IO) { install(file) }
        } catch (e: Exception) {
            Log.w(TAG, "Selbst-Update", e)
            _state.value = SelfUpdateState.Failed("The download didn't work. Please check your connection and try again.")
        }
    }

    /** Nach der Rückkehr aus den Einstellungen: weitermachen, wenn die Erlaubnis jetzt da ist. */
    suspend fun resumeIfAllowed(release: LatestRelease?) {
        if (_state.value == SelfUpdateState.NeedsPermission && release != null && context.packageManager.canRequestPackageInstalls()) {
            start(release)
        }
    }

    fun onInstallFinished(success: Boolean, message: String?) {
        _state.value = if (success) SelfUpdateState.Idle else SelfUpdateState.Failed(message ?: "The update could not be installed.")
    }

    fun reset() {
        _state.value = SelfUpdateState.Idle
    }

    private suspend fun download(release: LatestRelease): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // alte Downloads
        val file = File(dir, "GlobeSwap-${release.versionCode}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        http.newCall(Request.Builder().url(urlFor(release.apkUrl)).build()).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = checkNotNull(response.body)
            val total = body.contentLength().takeIf { it > 0 } ?: release.apkSize
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        if (total > 0) _state.value = SelfUpdateState.Downloading((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        if (sha != release.apkSha256) {
            file.delete()
            throw IllegalStateException("Prüfsumme passt nicht: $sha")
        }
        file
    }

    companion object {
        private const val TAG = "SelfUpdater"
        private val SHA256 = Regex("[0-9a-f]{64}")

        /** null, wenn die APK ein echtes, neueres GlobeSwap mit derselben Signatur ist; sonst der Grund. */
        fun verifyApk(context: Context, file: File, currentCode: Int): String? {
            val pm = context.packageManager
            val flags = PackageManager.GET_SIGNING_CERTIFICATES
            val archive = pm.getPackageArchiveInfo(file.path, flags) ?: return "The downloaded file is not a valid app."
            val installed = pm.getPackageInfo(context.packageName, flags)
            return when {
                archive.packageName != context.packageName -> "The downloaded file is not GlobeSwap."
                archive.longVersionCode <= currentCode -> "You already have this version."
                signers(archive) != signers(installed) -> "The update is not signed by the GlobeSwap developer and was not installed."
                else -> null
            }
        }

        private fun signers(info: PackageInfo): Set<String> =
            info.signingInfo?.apkContentsSigners.orEmpty().map { it.toCharsString() }.toSet()

        /** Übergibt die APK dem Paket-Installer; das Ergebnis kommt in [InstallResultReceiver] an. */
        fun installApk(context: Context, file: File) {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(file.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Spätere Updates ohne Nachfrage, sobald GlobeSwap sich einmal selbst installiert hat.
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("GlobeSwap.apk", 0, file.length()).use { out ->
                    file.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val intent = Intent(context, InstallResultReceiver::class.java)
                val pending = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pending.intentSender)
            }
        }
    }
}

/** Antwort des Paket-Installers: Nachfrage zeigen, Fehler melden. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ServiceLocator.init(context)
        val updater = ServiceLocator.updates.selfUpdater
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    updater.onInstallFinished(false, null)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> updater.onInstallFinished(true, null)
            PackageInstaller.STATUS_FAILURE_ABORTED -> updater.reset() // "Abbrechen" gedrückt
            else -> updater.onInstallFinished(false, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
        }
    }
}
