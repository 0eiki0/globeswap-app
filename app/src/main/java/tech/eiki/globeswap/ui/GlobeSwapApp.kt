package tech.eiki.globeswap.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import tech.eiki.globeswap.BuildConfig
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.ui.changelog.Changelog
import tech.eiki.globeswap.ui.changelog.ChangelogScreen
import tech.eiki.globeswap.ui.changelog.WhatsNewDialog
import tech.eiki.globeswap.ui.chat.ChatScreen
import tech.eiki.globeswap.ui.conversations.ConversationsScreen
import tech.eiki.globeswap.ui.conversations.ConversationsViewModel
import tech.eiki.globeswap.ui.onboarding.AgeGateScreen
import tech.eiki.globeswap.ui.onboarding.OnboardingScreen
import tech.eiki.globeswap.ui.onboarding.TooYoungScreen
import tech.eiki.globeswap.ui.settings.SettingsScreen
import tech.eiki.globeswap.ui.swap.SwapScreen
import tech.eiki.globeswap.ui.world.WorldScreen

private const val ROUTE_SWAP = "swap"
private const val ROUTE_CONVERSATIONS = "conversations"
private const val ROUTE_CHAT = "chat/{conversationId}"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_WORLD = "world"
private const val ROUTE_CHANGELOG = "changelog"
private const val KEY_WHATS_NEW = "whats_new_seen_code"
private const val PREFS = "globeswap"
private const val KEY_ONBOARDED = "onboarded"
// Ergebnis der Altersabfrage samt Zustimmung zur Datenschutzerklärung. v1 (nur Monat/Jahr, ohne Zustimmung)
// zählt nur noch, wenn sie "zu jung" ergab.
private const val KEY_AGE = "age_check_v2"
private const val KEY_AGE_V1 = "age_check"
private const val AGE_OK = "ok"
private const val AGE_TOO_YOUNG = "too_young"

@Composable
fun GlobeSwapApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean(KEY_ONBOARDED, false)) }

    // Beim Start und bei jeder Rückkehr in die App: Mindestversion und neueste Version beim Server abfragen.
    val updates = ServiceLocator.updates
    val update by updates.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            updates.refresh()
            updates.resumeSelfUpdate() // zurück aus den Einstellungen "Apps installieren"
        }
    }
    val selfUpdate by updates.selfUpdater.state.collectAsStateWithLifecycle()
    if (update.required) {
        ForcedUpdateScreen(
            message = update.message,
            installedVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            onUpdate = { updates.openUpdate(context) },
            onCheckAgain = { scope.launch { updates.refresh() } },
            selfUpdate = selfUpdate,
        )
        return
    }

    // Altersabfrage vor allem anderen; gespeichert wird nur das Ergebnis.
    var ageState by remember {
        mutableStateOf(prefs.getString(KEY_AGE, null) ?: AGE_TOO_YOUNG.takeIf { prefs.getString(KEY_AGE_V1, null) == AGE_TOO_YOUNG })
    }
    when (ageState) {
        AGE_TOO_YOUNG -> { TooYoungScreen(); return }
        null -> {
            AgeGateScreen(onResult = { ok ->
                val result = if (ok) AGE_OK else AGE_TOO_YOUNG
                prefs.edit().putString(KEY_AGE, result).apply()
                ageState = result
            })
            return
        }
    }

    if (!onboarded) {
        OnboardingScreen(onAccept = {
            // Neue Installation: kein "What's new" für die erste Version.
            prefs.edit().putBoolean(KEY_ONBOARDED, true).putInt(KEY_WHATS_NEW, BuildConfig.VERSION_CODE).apply()
            onboarded = true
        })
        return
    }

    val nav = rememberNavController()
    // Nach einem Update einmal zeigen, was neu ist.
    var whatsNew by remember {
        mutableStateOf(
            Changelog.load(context, BuildConfig.VERSION_NAME).firstOrNull { it.current }
                ?.takeIf { prefs.getInt(KEY_WHATS_NEW, 0) < BuildConfig.VERSION_CODE },
        )
    }
    whatsNew?.let { entry ->
        val seen = { prefs.edit().putInt(KEY_WHATS_NEW, BuildConfig.VERSION_CODE).apply(); whatsNew = null }
        WhatsNewDialog(entry, onDismiss = seen, onSeeAll = { seen(); nav.navigate(ROUTE_CHANGELOG) })
    }
    val conversationsVm: ConversationsViewModel = viewModel()
    val conversations by conversationsVm.conversations.collectAsStateWithLifecycle()
    val unread = conversations.count { it.unread }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    // Leiste unten schwebt über dem Inhalt (Glas-Effekt); die Seiten lassen unten so viel Platz, wie sie hoch ist.
    val hazeState = remember { HazeState() }
    val showBar = route == ROUTE_SWAP || route == ROUTE_CONVERSATIONS || route == ROUTE_WORLD
    var barHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomBarSpace provides if (showBar) barHeight else 0.dp) {
        NavHost(nav, startDestination = ROUTE_SWAP, modifier = Modifier.fillMaxSize().hazeSource(hazeState)) {
            composable(ROUTE_SWAP) {
                SwapScreen(onMatched = { id -> nav.navigate("chat/$id") })
            }
            composable(ROUTE_CONVERSATIONS) {
                val publicId by conversationsVm.publicId.collectAsStateWithLifecycle()
                ConversationsScreen(
                    publicId = publicId,
                    onAboutOpened = conversationsVm::loadPublicId,
                    conversations = conversations,
                    photoUrl = conversationsVm::photoUrl,
                    onOpen = { id -> nav.navigate("chat/$id") },
                    onOpenSettings = { nav.navigate(ROUTE_SETTINGS) },
                    onOpenChangelog = { nav.navigate(ROUTE_CHANGELOG) },
                    onDeleteAccount = {
                        conversationsVm.deleteAccount { error ->
                            if (error != null) {
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.data_deleted), Toast.LENGTH_LONG).show()
                                prefs.edit().putBoolean(KEY_ONBOARDED, false).apply()
                                onboarded = false
                            }
                        }
                    },
                )
            }
            composable(
                ROUTE_CHAT,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            ) {
                ChatScreen(onBack = { nav.popBackStack() })
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() }, onOpenWorld = { nav.switchTab(ROUTE_WORLD) })
            }
            composable(ROUTE_CHANGELOG) {
                ChangelogScreen(onBack = { nav.popBackStack() })
            }
            composable(ROUTE_WORLD) {
                WorldScreen()
            }
        }
        }
        if (showBar) {
            Column(
                // Höhe samt Systemleiste messen (onSizeChanged vor dem Abstand)
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                    .navigationBarsPadding(),
            ) {
                update.available?.let { release ->
                    UpdateBanner(
                        release = release,
                        channel = updates.channel,
                        onUpdate = { updates.openUpdate(context) },
                        onLater = { updates.dismiss(release.versionCode) },
                        selfUpdate = selfUpdate,
                    )
                }
                FloatingNavBar(
                    selected = when (route) {
                        ROUTE_CONVERSATIONS -> Tab.CHATS
                        ROUTE_WORLD -> Tab.WORLD
                        else -> Tab.SWAP
                    },
                    unread = unread,
                    hazeState = hazeState,
                    onSelect = { tab ->
                        nav.switchTab(
                            when (tab) {
                                Tab.CHATS -> ROUTE_CONVERSATIONS
                                Tab.SWAP -> ROUTE_SWAP
                                Tab.WORLD -> ROUTE_WORLD
                            },
                        )
                    },
                )
            }
        }
      }
    }
}

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(ROUTE_SWAP) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
